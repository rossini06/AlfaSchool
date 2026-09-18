import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista, qs } from "../../services/accessCadastrosApi";
import { Icon } from "../../components/Icon";
import { BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import { formatarDataHora } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const ICONE_POR_TIPO = {
  ENTRADA: "ArrowRight",
  SAIDA: "ArrowLeft",
  RETIRADA: "UserCheck",
  OCORRENCIA: "AlertCircle",
  AUTORIZACAO: "Key",
  AVISO: "Bell",
};

export function PortalNotificacoesPage() {
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [somenteNaoLidas, setSomenteNaoLidas] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    const r = await accessApi.get(`/access/portal/notificacoes?${qs({ naoLidas: somenteNaoLidas || "" })}`);
    if (r.ok) {
      setItens(comoLista(r.data));
      setErro("");
    } else {
      setItens([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [somenteNaoLidas]);

  useEffect(() => {
    carregar();
    // recarrega ao alternar o filtro de não lidos
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [somenteNaoLidas]);

  const marcarLida = async (item) => {
    const r = await accessApi.post(`/access/portal/notificacoes/${item.id}/lida`, {});
    if (!r.ok) {
      setFeedback({ tipo: "erro", mensagem: r.erro });
      return;
    }
    setItens((p) => p.map((n) => (n.id === item.id ? { ...n, lida: true } : n)));
  };

  return (
    <div>
      <h1 className="ac-portal-titulo">Avisos</h1>
      <p className="ac-portal-sub">Mensagens enviadas pela escola e alertas automáticos.</p>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card mb-4">
        <div className="card-body">
          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={somenteNaoLidas}
              onChange={(e) => setSomenteNaoLidas(e.target.checked)}
            />
            <span>Mostrar apenas os não lidos</span>
          </label>
        </div>
      </div>

      {(carregando || erro || itens.length === 0) && (
        <BlocoEstado
          carregando={carregando}
          erro={erro}
          vazio={itens.length === 0}
          icone="Bell"
          tituloVazio={somenteNaoLidas ? "Nenhum aviso não lido" : "Nenhum aviso recebido"}
          textoVazio="Avisos de entrada, saída e recados da escola aparecem aqui."
          onTentarNovamente={carregar}
        />
      )}

      {!carregando &&
        !erro &&
        itens.map((item) => (
          <article
            className="ac-filho-card"
            key={item.id}
            style={item.lida ? { opacity: 0.72 } : undefined}
          >
            <header className="ac-filho-head">
              <Icon name={ICONE_POR_TIPO[item.tipo] || "Bell"} size={18} />
              <div>
                <div className="ac-filho-nome">{item.titulo || item.tipo}</div>
                <div className="ac-filho-turma">
                  {formatarDataHora(item.dataHora || item.criadoEm)}
                  {item.alunoNome ? ` · ${item.alunoNome}` : ""}
                </div>
              </div>
              <span className="ac-filho-selo">
                {item.lida ? (
                  <span className="badge badge-secondary">Lido</span>
                ) : (
                  <button className="btn btn-ghost btn-sm" onClick={() => marcarLida(item)}>
                    <Icon name="Check" size={13} /> Marcar como lido
                  </button>
                )}
              </span>
            </header>
            <div className="ac-filho-body">
              <p style={{ fontSize: 13.5 }}>{item.mensagem || item.descricao}</p>
            </div>
          </article>
        ))}
    </div>
  );
}
