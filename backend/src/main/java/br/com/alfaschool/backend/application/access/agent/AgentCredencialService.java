package br.com.alfaschool.backend.application.access.agent;

import br.com.alfaschool.backend.application.access.agent.dto.AgentDtos;
import br.com.alfaschool.backend.domain.access.equipamento.AccAgentCredencial;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccAgentCredencialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Credenciais do agente local.
 *
 * A senha e' gerada pelo servidor, com 256 bits de entropia, e mostrada
 * UMA UNICA VEZ. Nao existe "reenviar senha": o instalador copia na hora
 * ou gera outra credencial. Guardar o valor recuperavel em algum lugar
 * seria criar uma chave mestra da portaria de cada escola.
 *
 * BCrypt aqui (e nao SHA-256 como no token de webhook) porque o login do
 * agente acontece uma vez por sessao, nao a cada passagem na catraca — o
 * custo do KDF cabe, e a protecao contra forca bruta compensa.
 */
@Service
public class AgentCredencialService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccAgentCredencialRepository credenciais;
    private final PasswordEncoder encoder;

    public AgentCredencialService(AccAgentCredencialRepository credenciais,
                                  PasswordEncoder encoder) {
        this.credenciais = credenciais;
        this.encoder = encoder;
    }

    @Transactional
    public AgentDtos.CredencialCriadaDto criar(UUID tenantId, AgentDtos.NovaCredencialRequest req) {
        if (credenciais.existsByTenantIdAndUsernameAndDeletedFalse(tenantId, req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma credencial de agente com este usuário.");
        }
        byte[] bruto = new byte[32];
        RANDOM.nextBytes(bruto);
        String senha = Base64.getUrlEncoder().withoutPadding().encodeToString(bruto);

        AccAgentCredencial c = new AccAgentCredencial();
        c.setTenantId(tenantId);
        c.setUnitId(req.unitId());
        c.setUsername(req.username());
        c.setPasswordHash(encoder.encode(senha));
        c.setDescricao(req.descricao());
        c.setAtivo(true);
        AccAgentCredencial salva = credenciais.save(c);

        return new AgentDtos.CredencialCriadaDto(salva.getId(), tenantId, req.unitId(),
                req.username(), senha,
                "Copie a senha agora: ela não será exibida novamente.");
    }

    /**
     * Autentica o agente.
     *
     * A busca e' por (tenant, username) porque o username so' e' unico
     * dentro do tenant: "agente-portaria" existe em varias escolas.
     *
     * Credencial inexistente e senha errada devolvem o MESMO resultado
     * vazio, e ambos passam pelo mesmo custo de BCrypt — distinguir os
     * dois casos, ou responder mais rapido quando o usuario nao existe,
     * entrega a lista de usernames validos.
     */
    @Transactional
    public Optional<AccAgentCredencial> autenticar(UUID tenantId, String username, String password) {
        Optional<AccAgentCredencial> achada =
                credenciais.findByTenantIdAndUsernameAndDeletedFalse(tenantId, username);
        if (achada.isEmpty()) {
            // Hash descartavel so' para gastar o mesmo tempo de CPU.
            encoder.matches(password, "$2a$10$ihHVTaQPbgOhLCa4A/AOI.JlVeJXKCUdXPRJWNPpTLjLZMSdPMWTi");
            return Optional.empty();
        }
        AccAgentCredencial c = achada.get();
        if (!c.isAtivo() || !encoder.matches(password, c.getPasswordHash())) {
            return Optional.empty();
        }
        c.setUltimoLogin(Instant.now());
        credenciais.save(c);
        return Optional.of(c);
    }
}
