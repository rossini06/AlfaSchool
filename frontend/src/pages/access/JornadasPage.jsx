import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { DIAS_SEMANA } from "../../utils/diasSemana";
import { calcularCarga, formatarDuracao, minutosParaHora, horaParaMinutos } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const REGRAS_EXCEDENTE = [
  {
    valor: "DURACAO",
    label: "Duração",
    resumo: "conta o tempo total do dia que passar da carga contratada, não importa a que horas o aluno entrou.",
  },
  {
    valor: "HORARIO",
    label: "Horário",
    resumo: "conta apenas o tempo fora da janela de entrada e saída previstas, mesmo que o total do dia caiba na carga.",
  },
  {
    valor: "AMBOS",
    label: "Ambos",
    resumo: "soma as duas apurações: estourar a carga OU ficar fora da janela já gera excedente.",
  },
];

function diasVazios() {
  return DIAS_SEMANA.map((d) => ({
    diaSemana: d.valor,
    frequenta: d.valor <= 5,
    entradaPrevista: d.valor <= 5 ? "07:00" : "",
    saidaPrevista: d.valor <= 5 ? "17:00" : "",
    cargaMinutos: d.valor <= 5 ? 600 : null,
  }));
}

const FORM_VAZIO = {
  nome: "",
  descricao: "",
  toleranciaEntradaMin: 10,
  toleranciaSaidaMin: 10,
  regraExcedente: "DURACAO",
  ativo: true,
  dias: diasVazios(),
};

export function JornadasPage() {
  const [itens, setItens] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [busca, setBusca] = useState("");
  const [filtroRegra, setFiltroRegra] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/jornadas?${qs({ page: p, size: PAGE_SIZE, q: busca, regraExcedente: filtroRegra })}`
      );
      if (r.ok) {
        setItens(comoLista(r.data));
        setTotal(comoTotal(r.data));
        setPagina(p);
      } else {
        setItens([]);
        setTotal(0);
        setErro(r.erro);
      }
      setCarregando(false);
    },
    [busca, filtroRegra]
  );

  useEffect(() => {
    carregar(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const abrirNovo = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, dias: diasVazios() });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const abrirEdicao = (item) => {
    const base = diasVazios().map((d) => {
      const vindo = (item.dias || []).find((x) => x.diaSemana === d.diaSemana);
      if (!vindo) return { ...d, frequenta: false, entradaPrevista: "", saidaPrevista: "", cargaMinutos: null };
      return {
        diaSemana: d.diaSemana,
        frequenta: vindo.frequenta !== false,
        entradaPrevista: (vindo.entradaPrevista || "").slice(0, 5),
        saidaPrevista: (vindo.saidaPrevista || "").slice(0, 5),
        cargaMinutos: vindo.cargaMinutos ?? null,
      };
    });
    setEditando(item);
    setForm({
      nome: item.nome || "",
      descricao: item.descricao || "",
      toleranciaEntradaMin: item.toleranciaEntradaMin ?? 0,
      toleranciaSaidaMin: item.toleranciaSaidaMin ?? 0,
      regraExcedente: item.regraExcedente || "DURACAO",
      ativo: item.ativo !== false,
      dias: base,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  /** Atualiza um dia da grade; recalcula a carga quando entrada/saída mudam. */
  const mudarDia = (diaSemana, campo, valor) => {
    setForm((p) => ({
      ...p,
      dias: p.dias.map((d) => {
        if (d.diaSemana !== diaSemana) return d;
        const novo = { ...d, [campo]: valor };
        if (campo === "frequenta" && !valor) {
          return { ...novo, entradaPrevista: "", saidaPrevista: "", cargaMinutos: null };
        }
        if (campo === "entradaPrevista" || campo === "saidaPrevista") {
          const carga = calcularCarga(
            campo === "entradaPrevista" ? valor : d.entradaPrevista,
            campo === "saidaPrevista" ? valor : d.saidaPrevista
          );
          if (carga !== null) novo.cargaMinutos = carga;
        }
        return novo;
      }),
    }));
    if (erros.dias) setErros((p) => ({ ...p, dias: "" }));
  };

  const mudarCarga = (diaSemana, texto) => {
    // aceita "10:00" ou minutos puros ("600")
    let minutos = null;
    if (texto.includes(":")) minutos = horaParaMinutos(texto);
    else if (texto.trim() !== "" && !Number.isNaN(Number(texto))) minutos = Number(texto);
    setForm((p) => ({
      ...p,
      dias: p.dias.map((d) => (d.diaSemana === diaSemana ? { ...d, cargaMinutos: minutos } : d)),
    }));
  };

  const validar = () => {
    const e = {};
    if (!form.nome.trim()) e.nome = "Informe o nome da jornada.";
    if (form.toleranciaEntradaMin === "" || Number(form.toleranciaEntradaMin) < 0)
      e.toleranciaEntradaMin = "Informe a tolerância em minutos (0 ou mais).";
    if (form.toleranciaSaidaMin === "" || Number(form.toleranciaSaidaMin) < 0)
      e.toleranciaSaidaMin = "Informe a tolerância em minutos (0 ou mais).";
    const frequentados = form.dias.filter((d) => d.frequenta);
    if (frequentados.length === 0) e.dias = "Marque ao menos um dia com frequência.";
    else if (frequentados.some((d) => !d.entradaPrevista || !d.saidaPrevista))
      e.dias = "Todo dia marcado precisa de entrada e saída.";
    else if (frequentados.some((d) => !d.cargaMinutos))
      e.dias = "Todo dia marcado precisa de uma carga maior que zero.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      nome: form.nome.trim(),
      descricao: form.descricao.trim() || null,
      toleranciaEntradaMin: Number(form.toleranciaEntradaMin),
      toleranciaSaidaMin: Number(form.toleranciaSaidaMin),
      regraExcedente: form.regraExcedente,
      ativo: form.ativo,
      dias: form.dias.map((d) => ({
        diaSemana: d.diaSemana,
        frequenta: d.frequenta,
        entradaPrevista: d.frequenta ? d.entradaPrevista : null,
        saidaPrevista: d.frequenta ? d.saidaPrevista : null,
        cargaMinutos: d.frequenta ? d.cargaMinutos : null,
      })),
    };
    const r = editando
      ? await accessApi.put(`/access/jornadas/${editando.id}`, corpo)
      : await accessApi.post("/access/jornadas", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Jornada atualizada." : "Jornada cadastrada." });
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/jornadas/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Jornada excluída." });
    carregar(pagina);
  };

  const cargaSemanal = (dias) =>
    (dias || []).reduce((soma, d) => soma + (d.frequenta !== false ? d.cargaMinutos || 0 : 0), 0);

  const regraAtual = REGRAS_EXCEDENTE.find((r) => r.valor === form.regraExcedente);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Jornadas</h1>
          <p className="page-subtitle">Carga contratada por dia da semana e regra de apuração do excedente</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Nova Jornada
        </button>
      </div>

      <Aviso tipo="info" titulo="As três regras de excedente são contas diferentes">
        <strong>Duração</strong> — {REGRAS_EXCEDENTE[0].resumo} <strong>Horário</strong> —{" "}
        {REGRAS_EXCEDENTE[1].resumo} <strong>Ambos</strong> — {REGRAS_EXCEDENTE[2].resumo}
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar jornada..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroRegra} onChange={(e) => setFiltroRegra(e.target.value)}>
                <option value="">Todas as regras</option>
                {REGRAS_EXCEDENTE.map((r) => (
                  <option key={r.valor} value={r.valor}>
                    {r.label}
                  </option>
                ))}
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setBusca("");
                setFiltroRegra("");
                setTimeout(() => carregar(0), 0);
              }}
            >
              Limpar
            </button>
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Jornada</th>
              <th>Dias com frequência</th>
              <th>Carga semanal</th>
              <th>Tolerâncias</th>
              <th>Regra</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={7}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Clock"
              tituloVazio="Nenhuma jornada cadastrada"
              textoVazio="A jornada define a carga contratada usada na apuração do excedente."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nome}</strong>
                    {item.descricao && <div className="ac-meta">{item.descricao}</div>}
                  </td>
                  <td className="td-muted">
                    {(item.dias || []).filter((d) => d.frequenta !== false).length} de 7
                  </td>
                  <td className="td-muted">{formatarDuracao(cargaSemanal(item.dias))}</td>
                  <td className="td-muted">
                    +{item.toleranciaEntradaMin ?? 0} min entrada · +{item.toleranciaSaidaMin ?? 0} min saída
                  </td>
                  <td>
                    <span className="badge badge-info">
                      {REGRAS_EXCEDENTE.find((r) => r.valor === item.regraExcedente)?.label || item.regraExcedente}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativa" : "Inativa"}
                    </span>
                  </td>
                  <td>
                    <div className="ac-linha-acoes">
                      <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(item)} title="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm text-danger"
                        onClick={() => {
                          setErroExcluir("");
                          setExcluirId(item.id);
                        }}
                        title="Excluir"
                      >
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        <Pagination
          page={pagina}
          totalPages={Math.ceil(total / PAGE_SIZE)}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => carregar(p)}
        />
      </div>

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar Jornada" : "Nova Jornada"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalAberto(false)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
              {salvando ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <Feedback tipo="erro" mensagem={erroForm} />
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome</label>
            <input
              className={`form-input ${erros.nome ? "error" : ""}`}
              value={form.nome}
              onChange={(e) => {
                setForm((p) => ({ ...p, nome: e.target.value }));
                if (erros.nome) setErros((p) => ({ ...p, nome: "" }));
              }}
              placeholder="Ex.: Integral 10h"
            />
            {erros.nome && <span className="form-error">{erros.nome}</span>}
          </div>

          <div className="form-field">
            <label className="form-label">Descrição</label>
            <input
              className="form-input"
              value={form.descricao}
              onChange={(e) => setForm((p) => ({ ...p, descricao: e.target.value }))}
              placeholder="Como esta jornada é vendida à família"
            />
          </div>

          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label required">Tolerância de entrada (min)</label>
              <input
                className={`form-input ${erros.toleranciaEntradaMin ? "error" : ""}`}
                type="number"
                min="0"
                value={form.toleranciaEntradaMin}
                onChange={(e) => {
                  setForm((p) => ({ ...p, toleranciaEntradaMin: e.target.value }));
                  if (erros.toleranciaEntradaMin) setErros((p) => ({ ...p, toleranciaEntradaMin: "" }));
                }}
              />
              {erros.toleranciaEntradaMin && <span className="form-error">{erros.toleranciaEntradaMin}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Tolerância de saída (min)</label>
              <input
                className={`form-input ${erros.toleranciaSaidaMin ? "error" : ""}`}
                type="number"
                min="0"
                value={form.toleranciaSaidaMin}
                onChange={(e) => {
                  setForm((p) => ({ ...p, toleranciaSaidaMin: e.target.value }));
                  if (erros.toleranciaSaidaMin) setErros((p) => ({ ...p, toleranciaSaidaMin: "" }));
                }}
              />
              {erros.toleranciaSaidaMin && <span className="form-error">{erros.toleranciaSaidaMin}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Regra de excedente</label>
              <select
                className="form-select"
                value={form.regraExcedente}
                onChange={(e) => setForm((p) => ({ ...p, regraExcedente: e.target.value }))}
              >
                {REGRAS_EXCEDENTE.map((r) => (
                  <option key={r.valor} value={r.valor}>
                    {r.label}
                  </option>
                ))}
              </select>
              <span className="form-hint">
                <strong>{regraAtual?.label}</strong> — {regraAtual?.resumo}
              </span>
            </div>
          </div>

          <div className="ac-subcard">
            <div className="ac-subcard-titulo">
              <Icon name="Calendar" size={14} /> Grade da semana
            </div>
            <p className="form-hint" style={{ marginBottom: 8 }}>
              A carga é calculada a partir da entrada e da saída, mas continua editável — há contratos
              em que o tempo pago difere da janela de presença.
            </p>
            <div style={{ overflowX: "auto" }}>
              <table className="ac-grade-jornada">
                <thead>
                  <tr>
                    <th>Dia</th>
                    <th>Frequenta</th>
                    <th>Entrada</th>
                    <th>Saída</th>
                    <th>Carga</th>
                  </tr>
                </thead>
                <tbody>
                  {form.dias.map((d) => {
                    const info = DIAS_SEMANA.find((x) => x.valor === d.diaSemana);
                    return (
                      <tr key={d.diaSemana} className={d.frequenta ? "" : "nao-frequenta"}>
                        <td>
                          <strong>{info?.nome}</strong>
                        </td>
                        <td>
                          <label className="form-checkbox" style={{ margin: 0 }}>
                            <input
                              type="checkbox"
                              checked={d.frequenta}
                              onChange={(e) => mudarDia(d.diaSemana, "frequenta", e.target.checked)}
                            />
                            <span />
                          </label>
                        </td>
                        <td>
                          <input
                            className="form-input"
                            type="time"
                            value={d.entradaPrevista || ""}
                            disabled={!d.frequenta}
                            onChange={(e) => mudarDia(d.diaSemana, "entradaPrevista", e.target.value)}
                          />
                        </td>
                        <td>
                          <input
                            className="form-input"
                            type="time"
                            value={d.saidaPrevista || ""}
                            disabled={!d.frequenta}
                            onChange={(e) => mudarDia(d.diaSemana, "saidaPrevista", e.target.value)}
                          />
                        </td>
                        <td>
                          <input
                            className="form-input"
                            style={{ maxWidth: 110 }}
                            value={d.cargaMinutos ? minutosParaHora(d.cargaMinutos) : ""}
                            disabled={!d.frequenta}
                            placeholder="hh:mm"
                            onChange={(e) => mudarCarga(d.diaSemana, e.target.value)}
                          />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="mt-2 ac-meta">
              Carga semanal contratada: <strong>{formatarDuracao(cargaSemanal(form.dias))}</strong>
            </div>
            {erros.dias && <span className="form-error">{erros.dias}</span>}
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.ativo}
              onChange={(e) => setForm((p) => ({ ...p, ativo: e.target.checked }))}
            />
            <span>Jornada ativa (disponível para novos vínculos)</span>
          </label>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir jornada"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        Alunos vinculados a esta jornada ficam sem carga contratada e param de gerar apuração de
        excedente. Confirma a exclusão?
      </ConfirmarModal>
    </div>
  );
}
