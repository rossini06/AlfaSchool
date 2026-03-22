import { Icon } from "./Icon";

export function Pagination({ page, totalPages, total, pageSize, onPageChange }) {
  if (totalPages <= 1 && total <= pageSize) return null;

  const from = total === 0 ? 0 : page * pageSize + 1;
  const to = Math.min((page + 1) * pageSize, total);

  // Build page number range (max 5 pages shown)
  const pages = [];
  const maxPages = 5;
  let start = Math.max(0, page - Math.floor(maxPages / 2));
  let end = Math.min(totalPages - 1, start + maxPages - 1);
  if (end - start < maxPages - 1) start = Math.max(0, end - maxPages + 1);
  for (let i = start; i <= end; i++) pages.push(i);

  return (
    <div className="table-pagination">
      <span>
        {total > 0
          ? `Mostrando ${from}–${to} de ${total} registros`
          : "Nenhum registro"}
      </span>
      <div className="pagination-controls">
        <button
          className="pagination-btn"
          onClick={() => onPageChange(0)}
          disabled={page === 0}
          title="Primeira página"
        >
          «
        </button>
        <button
          className="pagination-btn"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          title="Página anterior"
        >
          <Icon name="ChevronLeft" size={14} />
        </button>
        {pages.map((p) => (
          <button
            key={p}
            className={`pagination-btn ${p === page ? "active" : ""}`}
            onClick={() => onPageChange(p)}
          >
            {p + 1}
          </button>
        ))}
        <button
          className="pagination-btn"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          title="Próxima página"
        >
          <Icon name="ChevronRight" size={14} />
        </button>
        <button
          className="pagination-btn"
          onClick={() => onPageChange(totalPages - 1)}
          disabled={page >= totalPages - 1}
          title="Última página"
        >
          »
        </button>
      </div>
    </div>
  );
}
