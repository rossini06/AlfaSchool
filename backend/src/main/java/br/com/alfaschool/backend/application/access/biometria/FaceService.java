package br.com.alfaschool.backend.application.access.biometria;

import br.com.alfaschool.backend.application.access.biometria.dto.CadastroFaceRequest;
import br.com.alfaschool.backend.application.access.controlid.ControlIdClient;
import br.com.alfaschool.backend.application.access.controlid.ControlIdException;
import br.com.alfaschool.backend.application.access.controlid.FotoRecusadaException;
import br.com.alfaschool.backend.application.access.controlid.dto.ControlIdUsuario;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.biometria.AccFaceSync;
import br.com.alfaschool.backend.domain.access.shared.StatusFaceSync;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceSyncRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Cadastro e sincronizacao de faces.
 *
 * Duas invariantes, ambas com historia:
 *
 * 1. Nenhuma foto sai daqui para um equipamento sem base legal declarada
 *    E consentimento registrado. A checagem esta no caminho de
 *    exportacao, nao numa tela — porque telas se contornam.
 *
 * 2. O veredito e' gravado POR EQUIPAMENTO. "Sincronizado" nao e' um
 *    estado da face: e' um estado do par (face, leitor). Sem isso a tela
 *    afirma que a crianca esta cadastrada enquanto a catraca da entrada
 *    nunca aceitou a imagem.
 */
@Service
public class FaceService {

    private static final Logger log = LoggerFactory.getLogger(FaceService.class);

    private final AccFaceRepository faces;
    private final AccFaceSyncRepository sincronizacoes;
    private final DispositivoRepository dispositivos;
    private final FotoStorage fotos;
    private final DeviceUserIdService deviceUserIds;
    private final ControlIdClient client;

    public FaceService(AccFaceRepository faces,
                       AccFaceSyncRepository sincronizacoes,
                       DispositivoRepository dispositivos,
                       FotoStorage fotos,
                       DeviceUserIdService deviceUserIds,
                       ControlIdClient client) {
        this.faces = faces;
        this.sincronizacoes = sincronizacoes;
        this.dispositivos = dispositivos;
        this.fotos = fotos;
        this.deviceUserIds = deviceUserIds;
        this.client = client;
    }

    // =================================================================
    // Cadastro
    // =================================================================

    @Transactional
    public AccFace cadastrar(UUID tenantId, CadastroFaceRequest req) {
        byte[] conteudo = decodificar(req.fotoBase64());
        try {
            ImagemValidador.validar(conteudo, Long.MAX_VALUE);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Optional<AccFace> existente = faces
                .findByTenantIdAndTitularTipoAndTitularIdAndDeletedFalse(
                        tenantId, req.titularTipo(), req.titularId());

        AccFace face = existente.orElseGet(AccFace::new);
        boolean nova = existente.isEmpty();
        if (nova) {
            face.setTenantId(tenantId);
            face.setTitularTipo(req.titularTipo());
            face.setTitularId(req.titularId());
            // O device_user_id e' estavel por pessoa: trocar a foto NAO
            // troca o numero, senao o equipamento acumularia usuarios
            // fantasma a cada atualizacao de retrato.
            face.setDeviceUserId(deviceUserIds.proximo(tenantId));
        } else if (face.getFotoKey() != null) {
            fotos.remover(face.getFotoKey());
        }

        String chave;
        try {
            chave = fotos.salvar(tenantId, conteudo);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        face.setFotoKey(chave);
        face.setAtivo(true);
        face.setBaseLegal(req.baseLegal());
        face.setConsentimentoObtido(req.consentimentoObtido());
        if (req.consentimentoObtido() && face.getConsentimentoEm() == null) {
            face.setConsentimentoEm(Instant.now());
        }
        face.setConsentimentoVersao(req.consentimentoVersao());
        face.setConsentimentoOrigem(req.consentimentoOrigem());
        face.setConsentimentoPor(req.consentimentoPor());

        AccFace salva = faces.save(face);

        // Foto trocada invalida todo veredito anterior: o leitor precisa
        // avaliar a imagem NOVA. Deixar ACEITA de uma foto que nao existe
        // mais e' a mentira que acc_face_sync existe para impedir.
        sincronizacoes.findByTenantIdAndFaceId(tenantId, salva.getId()).forEach(s -> {
            s.setStatus(StatusFaceSync.PENDENTE);
            s.setFotoHash(null);
            s.setCodigoErro(null);
            s.setDetalhe(null);
            sincronizacoes.save(s);
        });

        return salva;
    }

    // =================================================================
    // Exportacao para o equipamento
    // =================================================================

    /**
     * Envia a face para um equipamento e grava o veredito.
     *
     * @throws ExportacaoBiometriaBloqueadaException sem base legal ou
     *         consentimento. A tentativa fica registrada em
     *         acc_face_sync como RECUSADA com codigo SEM_BASE_LEGAL /
     *         SEM_CONSENTIMENTO: auditoria de LGPD precisa ver que o
     *         sistema barrou, nao apenas que nada aconteceu.
     */
    @Transactional
    public AccFaceSync sincronizar(UUID tenantId, UUID faceId, UUID dispositivoId) {
        AccFace face = faces.findByIdAndTenantIdAndDeletedFalse(faceId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Face não encontrada."));
        Dispositivo dispositivo = dispositivos.findById(dispositivoId)
                .filter(d -> tenantId.equals(d.getTenantId()))
                .filter(d -> Boolean.FALSE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado."));

        AccFaceSync sync = obterOuCriarSync(tenantId, face.getId(), dispositivo.getId());

        String bloqueio = motivoDeBloqueio(face);
        if (bloqueio != null) {
            String mensagem = "Biometria não exportada: " + descricaoDoBloqueio(bloqueio)
                    + " (LGPD Art. 11 e Art. 14).";
            log.warn("Exportação de face {} para o equipamento {} bloqueada: {}",
                    face.getId(), dispositivo.getNome(), bloqueio);
            sync.recusar(bloqueio, mensagem);
            sincronizacoes.save(sync);
            throw new ExportacaoBiometriaBloqueadaException(bloqueio, mensagem);
        }

        byte[] conteudo = fotos.ler(face.getFotoKey());
        String hash = hashDaFoto(conteudo);

        // Mesma imagem ja aceita neste leitor: nao reenvia. Num ciclo
        // diario com 2000 alunos isso e' a diferenca entre reenviar tudo
        // toda madrugada e reenviar so' quem mudou de foto.
        if (sync.getStatus() == StatusFaceSync.ACEITA && hash.equals(sync.getFotoHash())) {
            return sync;
        }

        sync.setStatus(StatusFaceSync.ENVIADA);
        sincronizacoes.save(sync);

        try {
            client.sincronizarUsuario(dispositivo, new ControlIdUsuario(
                    face.getDeviceUserId(),
                    String.valueOf(face.getDeviceUserId()),
                    nomeParaEquipamento(face)));
            client.enviarFoto(dispositivo, face.getDeviceUserId(), conteudo);
            sync.aceitar(hash);
        } catch (FotoRecusadaException e) {
            // Recusa e' definitiva para ESTES bytes: guarda o motivo
            // legivel e NAO guarda o hash, para que a mesma foto nao seja
            // considerada "ja enviada" numa proxima varredura.
            sync.recusar(e.getCodigo(), e.getMessage());
        } catch (ControlIdException e) {
            // Falha de comunicacao e' transitoria: volta para PENDENTE
            // para o proximo ciclo tentar de novo.
            sync.setStatus(StatusFaceSync.PENDENTE);
            sync.setCodigoErro("COMUNICACAO");
            sync.setDetalhe(e.getMessage());
            sync.setTentativas(sync.getTentativas() + 1);
        }
        return sincronizacoes.save(sync);
    }

    /**
     * Remove a face do equipamento e marca REMOVIDA.
     *
     * Falha NAO e' engolida: face que continua no leitor depois de uma
     * "remocao" bem-sucedida e' uma crianca desligada da escola ainda
     * entrando pela catraca.
     */
    @Transactional
    public AccFaceSync remover(UUID tenantId, UUID faceId, UUID dispositivoId) {
        AccFace face = faces.findByIdAndTenantIdAndDeletedFalse(faceId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Face não encontrada."));
        Dispositivo dispositivo = dispositivos.findById(dispositivoId)
                .filter(d -> tenantId.equals(d.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado."));

        client.removerFoto(dispositivo, face.getDeviceUserId());
        client.revogarAcesso(dispositivo, face.getDeviceUserId());

        AccFaceSync sync = obterOuCriarSync(tenantId, face.getId(), dispositivo.getId());
        sync.setStatus(StatusFaceSync.REMOVIDA);
        sync.setFotoHash(null);
        sync.setCodigoErro(null);
        sync.setDetalhe(null);
        return sincronizacoes.save(sync);
    }

    // =================================================================
    // Apoio
    // =================================================================

    /** @return codigo do bloqueio, ou null quando a exportacao e' permitida */
    public static String motivoDeBloqueio(AccFace face) {
        if (face.getBaseLegal() == null || face.getBaseLegal().isBlank()) {
            return "SEM_BASE_LEGAL";
        }
        if (!face.isConsentimentoObtido()) {
            return "SEM_CONSENTIMENTO";
        }
        if (!face.isAtivo()) {
            return "FACE_INATIVA";
        }
        return null;
    }

    private String descricaoDoBloqueio(String codigo) {
        return switch (codigo) {
            case "SEM_BASE_LEGAL" -> "não há base legal declarada para o tratamento";
            case "SEM_CONSENTIMENTO" -> "o consentimento do responsável não foi registrado";
            case "FACE_INATIVA" -> "o cadastro biométrico está inativo";
            default -> codigo;
        };
    }

    private AccFaceSync obterOuCriarSync(UUID tenantId, UUID faceId, UUID dispositivoId) {
        return sincronizacoes.findByFaceIdAndDispositivoId(faceId, dispositivoId)
                .orElseGet(() -> {
                    AccFaceSync s = new AccFaceSync();
                    s.setTenantId(tenantId);
                    s.setFaceId(faceId);
                    s.setDispositivoId(dispositivoId);
                    s.setStatus(StatusFaceSync.PENDENTE);
                    return s;
                });
    }

    /**
     * O equipamento so' precisa de um rotulo para a tela dele. Mandar o
     * nome completo da crianca para dentro do leitor amplia a superficie
     * de exposicao sem necessidade: quem le a tela da portaria ja sabe
     * quem passou pelo painel do sistema.
     */
    private String nomeParaEquipamento(AccFace face) {
        return face.getTitularTipo().name() + "-" + face.getDeviceUserId();
    }

    public static String hashDaFoto(byte[] conteudo) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(conteudo);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    private byte[] decodificar(String base64) {
        try {
            String limpo = base64.contains(",")
                    ? base64.substring(base64.indexOf(',') + 1)
                    : base64;
            return Base64.getDecoder().decode(limpo.replaceAll("\\s", "")
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto em base64 inválida.");
        }
    }
}
