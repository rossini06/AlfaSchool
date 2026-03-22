-- Extend cursos with academic and financial fields
ALTER TABLE cursos
    ADD COLUMN tipo                        VARCHAR(30)    NULL,
    ADD COLUMN duracao_meses               INT            NULL,
    ADD COLUMN idade_minima                INT            NULL,
    ADD COLUMN idade_maxima                INT            NULL,
    ADD COLUMN preco_base                  DECIMAL(10,2)  NULL,
    ADD COLUMN nota_minima_aprovacao       DECIMAL(4,2)   NULL DEFAULT 5.00,
    ADD COLUMN frequencia_minima_aprovacao DECIMAL(5,2)   NULL DEFAULT 75.00;
