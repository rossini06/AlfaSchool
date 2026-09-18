package br.com.alfaschool.backend.access.permanencia;

import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPresencaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava estrutural da regra "dia INCONSISTENTE nao entra em nenhum total".
 *
 * Nao adianta so' zerar os derivados na hora de gravar: basta alguem
 * escrever amanha um relatorio novo somando minutos_excedente sem filtro
 * para o numero falso voltar a entrar na fatura. Este teste quebra o build
 * se qualquer consulta agregada de presenca esquecer o filtro.
 */
class AgregadosIgnoramInconsistenteTest {

    @Test
    void todaConsultaAgregadaDePresencaExcluiOStatusInconsistente() {
        List<Method> agregadas = List.of(AccPresencaRepository.class.getDeclaredMethods()).stream()
                .filter(AgregadosIgnoramInconsistenteTest::devolveTotais)
                .toList();

        assertThat(agregadas)
                .as("as consultas agregadas precisam existir para serem verificadas")
                .isNotEmpty();

        for (Method metodo : agregadas) {
            Query query = metodo.getAnnotation(Query.class);
            assertThat(query)
                    .as("consulta agregada %s precisa de @Query explicita", metodo.getName())
                    .isNotNull();
            assertThat(query.value())
                    .as("consulta agregada %s precisa excluir StatusPresenca.INCONSISTENTE", metodo.getName())
                    .contains("StatusPresenca.INCONSISTENTE");
        }
    }

    private static boolean devolveTotais(Method metodo) {
        Type retorno = metodo.getGenericReturnType();
        if (!(retorno instanceof ParameterizedType parametrizado)) {
            return false;
        }
        Type[] argumentos = parametrizado.getActualTypeArguments();
        return argumentos.length == 1 && argumentos[0].equals(AccPresencaRepository.TotaisAluno.class);
    }
}
