-- O boletim do Ensino Infantil e' por CONCEITO (nao por nota numerica). A
-- coluna guardava so' codigos de 2 chars (A/B/C), o que truncava rotulos
-- legiveis como "Ótimo"/"Regular" e derrubava o calculo da media com
-- "Data too long for column 'conceito'". Ampliamos para caber o rotulo.
ALTER TABLE medias MODIFY COLUMN conceito VARCHAR(20);
