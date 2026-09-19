import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { StatusAutorizacaoBadge } from "../../components/access/StatusAutorizacaoBadge";
import { DiasSemanaChips } from "../../components/access/DiasSemanaChips";
import { nomesDosDias } from "../../utils/diasSemana";
import { formatarData, formatarHora, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PARENTESCOS = ["Mãe", "Pai", "Avó", "Avô", "Tia", "Tio", "Irmã", "Irmão", "Motorista", "Babá", "Outro"];

const FORM_VAZIO = {
  nome: "",
  cpf: "",
  parentesco: "",
  telefone: "",
  temporaria: false,
  vigenciaInicio: hojeIso(),
  vigenciaFim: "",
  diasSemana: [1, 2, 3, 4, 5],
  horaInicio: "",
  horaFim: "",
  observacoes: "",
};

export function PortalAutorizacoesPage() {
  const [filhos, setFilhos] = useState([]);
  const [alunoId, setAlunoId] = useState("");
  const [itens, setItens] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [erroForm, setErroForm] = useState("");
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    accessApi.get("/access/portal/meus-alunos").then((r) => {
      const lista = r.ok ? comoLista(r.data) : [];
      setFilhos(lista);
      if (lista.length > 0) setAlunoId(lista[0].alunoId || lista[0].id);
      else setCarregando(false);
    });
  }, []);

  const carregar = useCallback(async () => {
    if (!alunoId) return;
    setCarregando(true);
    const r = await accessApi.get(`/access/portal/aluno/${alunoId}/autorizacoes`);
    if (r.ok) {
      setItens(comoLista(r.data));
      setErro("");
    } else {
      setItens([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [alunoId]);

  useEffect(() => {
    carregar();
    // recarrega ao trocar o aluno selecionado
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [alunoId]);

  const validar = () => {
    const e = {};
    if (!alunoId) e.alunoId = "Escolha o aluno.";
    if (!form.nome.trim()) e.nome = "Informe o nome completo da pessoa.";
    if (!form.cpf.replace(/\D/g, "")) e.cpf = "O CPF é conferido na portaria.";
    else if (form.cpf.replace(/\D/g, "").length !== 11) e.cpf = "O CPF deve ter 11 dígitos.";
    if (!form.parentesco) e.parentesco = "Diga qual é a relação com o aluno.";
    if (!form.telefone.trim()) e.telefone = "Informe um telefone de contato.";
    if (form.temporaria && !form.vigenciaFim)
      e.vigenciaFim = "Para uma autorização temporária, informe até quando ela deve valer.";
    if (form.vigenciaFim && form.vigenciaFim < form.vigenciaInicio)
      e.vigenciaFim = "A data final não pode ser anterior à inicial.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const enviar = async () => {
    if (!validar()) return;
    setEnviando(true);
    setErroForm("");
    // O aluno vai no caminho, nao no corpo. E os nomes sao os da API:
    // documento (nao cpf), validoAte (nao vigenciaFim), justificativa
    // (nao observacoes). Os dias da semana viajam como CSV, que e' o
    // formato que a autorizacao guarda.
    const r = await accessApi.post(`/access/portal/aluno/${alunoId}/autorizacoes/solicitar`, {
      nome: form.nome.trim(),
      documento: form.cpf.replace(/\D/g, ""),
      parentesco: form.parentesco,
      telefone: form.telefone.trim(),
      validoDe: form.temporaria ? form.vigenciaInicio : null,
      validoAte: form.temporaria ? form.vigenciaFim || null : null,
      diasSemana: (form.diasSemana || []).join(",") || null,
      horaInicio: form.horaInicio || null,
      horaFim: form.horaFim || null,
      justificativa: form.observacoes.trim() || null,
    });
    setEnviando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setForm(FORM_VAZIO);
    setFeedback({
      tipo: "sucesso",
      mensagem:
        "Pedido enviado. Ele fica como pendente até a escola aprovar — a pessoa ainda não pode retirar o aluno.",
    });
    carregar();
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  return (
    <div>
      <h1 className="ac-portal-titulo">Quem pode retirar</h1>
      <p className="ac-portal-sub">Pessoas autorizadas a buscar o aluno na escola.</p>

      <Aviso tipo="alerta" titulo="A inclusão passa por aprovação da escola">
        Enviar o pedido <strong>não libera a retirada automaticamente</strong>. A escola confere os
        dados e só depois a autorização passa a valer. Enquanto estiver pendente, a portaria não
        entrega o aluno a essa pessoa.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card mb-4">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Aluno</label>
              <select className="form-select" value={alunoId} onChange={(e) => setAlunoId(e.target.value)}>
                {filhos.length === 0 && <option value="">Nenhum aluno vinculado</option>}
                {filhos.map((f) => (
                  <option key={f.alunoId || f.id} value={f.alunoId || f.id}>
                    {f.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">&nbsp;</label>
              <button className="btn btn-brand" onClick={() => setModalAberto(true)} disabled={!alunoId}>
                <Icon name="UserPlus" size={14} /> Solicitar inclusão
              </button>
            </div>
          </div>
        </div>
      </div>

      {(carregando || erro || itens.length === 0) && (
        <BlocoEstado
          carregando={carregando}
          erro={erro}
          vazio={itens.length === 0}
          icone="Users2"
          tituloVazio="Ninguém autorizado ainda"
          textoVazio="Use o botão acima para pedir a inclusão de uma pessoa."
          onTentarNovamente={carregar}
        />
      )}

      {!carregando &&
        !erro &&
        itens.map((item) => (
          <article className="ac-filho-card" key={item.id}>
            <header className="ac-filho-head">
              <div>
                <div className="ac-filho-nome">{item.nome}</div>
                <div className="ac-filho-turma">{item.parentesco || "Relação não informada"}</div>
              </div>
              <span className="ac-filho-selo">
                <StatusAutorizacaoBadge status={item.status} />
              </span>
            </header>
            <div className="ac-filho-body">
              <div className="ac-kv">
                <div className="ac-kv-item">
                  <span className="ac-kv-rot">Vigência</span>
                  <span className="ac-kv-val">
                    {formatarData(item.vigenciaInicio)} →{" "}
                    {item.validoAte ? formatarData(item.validoAte) : "sem prazo"}
                  </span>
                </div>
                <div className="ac-kv-item">
                  <span className="ac-kv-rot">Dias</span>
                  <span className="ac-kv-val">{nomesDosDias(item.diasSemana)}</span>
                </div>
                <div className="ac-kv-item">
                  <span className="ac-kv-rot">Horário</span>
                  <span className="ac-kv-val">
                    {item.horaInicio && item.horaFim
                      ? `${formatarHora(item.horaInicio)}–${formatarHora(item.horaFim)}`
                      : "qualquer horário"}
                  </span>
                </div>
              </div>
              {item.status === "PENDENTE" && (
                <p className="ac-meta mt-3">
                  <Icon name="Clock" size={12} /> Aguardando aprovação da escola. Enquanto isso, essa
                  pessoa não pode retirar o aluno.
                </p>
              )}
              {item.status === "SUSPENSA" && item.motivoUltimaAlteracao && (
                <p className="ac-meta mt-3">
                  <Icon name="Info" size={12} /> Suspensa pela escola: {item.motivoUltimaAlteracao}
                </p>
              )}
            </div>
          </article>
        ))}

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title="Solicitar inclusão de pessoa autorizada"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalAberto(false)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={enviar} disabled={enviando}>
              {enviando ? "Enviando..." : "Enviar pedido"}
            </button>
          </>
        }
      >
        <Aviso tipo="info">
          O pedido vai para a coordenação. A escola pode pedir documentos antes de aprovar.
        </Aviso>
        <Feedback tipo="erro" mensagem={erroForm} />
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Nome completo</label>
              <input
                className={`form-input ${erros.nome ? "error" : ""}`}
                value={form.nome}
                onChange={campo("nome")}
              />
              {erros.nome && <span className="form-error">{erros.nome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">CPF</label>
              <input
                className={`form-input ${erros.cpf ? "error" : ""}`}
                value={form.cpf}
                onChange={campo("cpf")}
                inputMode="numeric"
                placeholder="000.000.000-00"
              />
              {erros.cpf && <span className="form-error">{erros.cpf}</span>}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Relação com o aluno</label>
              <select
                className={`form-select ${erros.parentesco ? "error" : ""}`}
                value={form.parentesco}
                onChange={campo("parentesco")}
              >
                <option value="">Selecione</option>
                {PARENTESCOS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
              {erros.parentesco && <span className="form-error">{erros.parentesco}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Telefone</label>
              <input
                className={`form-input ${erros.telefone ? "error" : ""}`}
                value={form.telefone}
                onChange={campo("telefone")}
                placeholder="(27) 99999-0000"
              />
              {erros.telefone && <span className="form-error">{erros.telefone}</span>}
            </div>
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.temporaria}
              onChange={(e) => setForm((p) => ({ ...p, temporaria: e.target.checked }))}
            />
            <span>É uma autorização temporária (por alguns dias)</span>
          </label>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">A partir de</label>
              <input
                className="form-input"
                type="date"
                value={form.vigenciaInicio}
                onChange={campo("vigenciaInicio")}
              />
            </div>
            <div className="form-field">
              <label className={`form-label ${form.temporaria ? "required" : ""}`}>Até</label>
              <input
                className={`form-input ${erros.vigenciaFim ? "error" : ""}`}
                type="date"
                value={form.vigenciaFim}
                onChange={campo("vigenciaFim")}
              />
              {erros.vigenciaFim ? (
                <span className="form-error">{erros.vigenciaFim}</span>
              ) : (
                <span className="form-hint">
                  {form.temporaria ? "Obrigatório para autorização temporária." : "Deixe em branco para sem prazo."}
                </span>
              )}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Dias em que pode retirar</label>
            <DiasSemanaChips value={form.diasSemana} onChange={(v) => setForm((p) => ({ ...p, diasSemana: v }))} />
            <span className="form-hint">Selecionados: {nomesDosDias(form.diasSemana)}</span>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Horário — de</label>
              <input className="form-input" type="time" value={form.horaInicio} onChange={campo("horaInicio")} />
            </div>
            <div className="form-field">
              <label className="form-label">Horário — até</label>
              <input className="form-input" type="time" value={form.horaFim} onChange={campo("horaFim")} />
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Alguma observação para a escola?</label>
            <textarea className="form-textarea" rows={2} value={form.observacoes} onChange={campo("observacoes")} />
          </div>
        </div>
      </Modal>
    </div>
  );
}
