package br.com.alfaschool.backend.domain.access.calendario;

import br.com.alfaschool.backend.domain.access.shared.TipoCalendarioDia;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Excecao registrada no calendario. O calendario so guarda o que foge do
 * padrao: dia sem registro cai na regra "segunda a sexta a escola abre".
 * Guardar 365 linhas por ano seria caro e nao acrescentaria informacao.
 */
@Entity
// A UNIQUE (calendario_id, data) vem da V33 e e' repetida aqui de proposito:
// em teste o schema nasce das entidades, e sem ela o duplo lancamento na mesma
// data passaria no teste e so estouraria em producao.
@Table(name = "acc_calendario_dias",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_calendario_dias",
                columnNames = {"calendario_id", "data"}))
public class AccCalendarioDia extends BaseEntity {

    @Column(name = "calendario_id", nullable = false)
    private UUID calendarioId;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCalendarioDia tipo;

    @Column(length = 255)
    private String descricao;

    public UUID getCalendarioId() { return calendarioId; }
    public void setCalendarioId(UUID calendarioId) { this.calendarioId = calendarioId; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public TipoCalendarioDia getTipo() { return tipo; }
    public void setTipo(TipoCalendarioDia tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
