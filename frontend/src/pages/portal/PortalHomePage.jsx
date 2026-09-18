import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista } from "../../services/accessCadastrosApi";
import { Icon } from "../../components/Icon";
import { Avatar } from "../../components/Avatar";
import { BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import { formatarHora, formatarDuracao, minutosDesde } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

function minutosAtuais(filho) {
  if (typeof filho.minutosRealizados === "number") return filho.minutosRealizados;
  if (filho.presente && filho.entrada) return minutosDesde(filho.entrada);
  return null;
}

export function PortalHomePage() {
  const [filhos, setFilhos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    setCarregando(true);
    const r = await accessApi.get("/access/portal/filhos");
    if (r.ok) {
      setFilhos(comoLista(r.data));
      setErro("");
    } else {
      setFilhos([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, []);

  useEffect(() => {
    carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div>
      <h1 className="ac-portal-titulo">Acompanhamento</h1>
      <p className="ac-portal-sub">O que aconteceu hoje com quem está sob a sua responsabilidade.</p>

      {(carregando || erro || filhos.length === 0) && (
        <BlocoEstado
          carregando={carregando}
          erro={erro}
          vazio={filhos.length === 0}
          icone="GraduationCap"
          tituloVazio="Nenhum aluno vinculado ao seu acesso"
          textoVazio="Procure a secretaria da escola para vincular o seu cadastro ao aluno."
          onTentarNovamente={carregar}
        />
      )}

      {!carregando &&
        !erro &&
        filhos.map((filho) => {
          const realizado = minutosAtuais(filho);
          const contratado = filho.minutosContratados ?? null;
          const percentual =
            realizado !== null && contratado ? Math.min(999, Math.round((realizado / contratado) * 100)) : null;
          const excedeu = percentual !== null && percentual > 100;
          const eventos = comoLista(filho.eventos);

          return (
            <article className="ac-filho-card" key={filho.alunoId || filho.id}>
              <header className="ac-filho-head">
                <Avatar foto={filho.foto} nome={filho.nome} size={42} />
                <div>
                  <div className="ac-filho-nome">{filho.nome}</div>
                  <div className="ac-filho-turma">
                    {filho.turmaNome || "Turma não informada"}
                    {filho.unidadeNome ? ` · ${filho.unidadeNome}` : ""}
                  </div>
                </div>
                <span className="ac-filho-selo">
                  <span className={`badge ${filho.presente ? "badge-success" : "badge-secondary"}`}>
                    {filho.presente ? "Aluno presente" : "Aluno ausente"}
                  </span>
                </span>
              </header>

              <div className="ac-filho-body">
                <div className="ac-filho-sec-titulo">Resumo do dia</div>
                <div className="ac-kv">
                  <div className="ac-kv-item">
                    <span className="ac-kv-rot">Entrada</span>
                    <span className="ac-kv-val">{formatarHora(filho.entrada)}</span>
                  </div>
                  <div className="ac-kv-item">
                    <span className="ac-kv-rot">{filho.presente ? "Tempo atual" : "Tempo do dia"}</span>
                    <span className="ac-kv-val">{formatarDuracao(realizado)}</span>
                  </div>
                  <div className="ac-kv-item">
                    <span className="ac-kv-rot">Saída</span>
                    <span className="ac-kv-val">{filho.saida ? formatarHora(filho.saida) : "—"}</span>
                  </div>
                  <div className="ac-kv-item">
                    <span className="ac-kv-rot">Jornada contratada</span>
                    <span className="ac-kv-val">{formatarDuracao(contratado)}</span>
                  </div>
                </div>

                {contratado ? (
                  <div className="mt-4">
                    <div className="ac-filho-sec-titulo">Jornada contratada</div>
                    <div className="ac-progress">
                      <div
                        className={`ac-progress-bar ${excedeu ? "excedeu" : ""}`}
                        style={{ width: `${Math.min(100, percentual ?? 0)}%` }}
                      />
                    </div>
                    <div className="ac-progress-legenda">
                      <span>
                        {formatarDuracao(realizado)} de {formatarDuracao(contratado)}
                      </span>
                      <span className={excedeu ? "text-warning font-bold" : ""}>
                        {percentual !== null ? `${percentual}% cumprido` : "—"}
                        {excedeu ? ` · excedente de ${formatarDuracao(realizado - contratado)}` : ""}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="ac-meta mt-3">
                    Sem jornada contratada cadastrada para hoje — fale com a secretaria se isso não
                    estiver certo.
                  </p>
                )}

                <div className="mt-4">
                  <div className="ac-filho-sec-titulo">Últimos eventos</div>
                  {eventos.length === 0 ? (
                    <p className="ac-meta">Nenhum evento registrado hoje.</p>
                  ) : (
                    <div className="ac-eventos">
                      {eventos.slice(0, 6).map((ev, i) => (
                        <div className="ac-evento" key={ev.id || i}>
                          <Icon
                            name={
                              ev.tipo === "ENTRADA"
                                ? "ArrowRight"
                                : ev.tipo === "SAIDA"
                                ? "ArrowLeft"
                                : ev.tipo === "OCORRENCIA"
                                ? "AlertCircle"
                                : "Activity"
                            }
                            size={14}
                          />
                          <span className="ac-evento-hora">{formatarHora(ev.hora || ev.dataHora)}</span>
                          <span>{ev.descricao || ev.tipo}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </article>
          );
        })}

      {!carregando && !erro && filhos.length > 0 && (
        <Feedback
          tipo="info"
          mensagem="Os horários vêm da leitura feita na portaria. Se algo parecer errado, fale com a escola — o registro pode ser corrigido."
        />
      )}
    </div>
  );
}
