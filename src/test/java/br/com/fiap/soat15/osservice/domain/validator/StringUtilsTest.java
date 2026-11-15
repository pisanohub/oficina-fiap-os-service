package br.com.fiap.soat15.osservice.domain.validator;

import br.com.fiap.soat15.osservice.domain.validator.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StringUtilsTest {

    @Test
    @DisplayName("Deve remover acentos de texto com caracteres especiais")
    void deveRemoverAcentos() {
        assertThat(StringUtils.removerAcentos("João")).isEqualTo("Joao");
        assertThat(StringUtils.removerAcentos("óleo")).isEqualTo("oleo");
        assertThat(StringUtils.removerAcentos("Manutenção")).isEqualTo("Manutencao");
        assertThat(StringUtils.removerAcentos("ção")).isEqualTo("cao");
    }

    @Test
    @DisplayName("Deve retornar o mesmo texto quando não há acentos")
    void deveRetornarTextoSemAcentos() {
        assertThat(StringUtils.removerAcentos("Toyota")).isEqualTo("Toyota");
        assertThat(StringUtils.removerAcentos("ABC1D23")).isEqualTo("ABC1D23");
    }

    @Test
    @DisplayName("Deve retornar null quando o texto for null")
    void deveRetornarNullParaEntradaNull() {
        assertThat(StringUtils.removerAcentos(null)).isNull();
    }

    @Test
    @DisplayName("Deve retornar string vazia quando o texto for vazio")
    void deveRetornarVazioParaTextoVazio() {
        assertThat(StringUtils.removerAcentos("")).isEmpty();
    }
}
