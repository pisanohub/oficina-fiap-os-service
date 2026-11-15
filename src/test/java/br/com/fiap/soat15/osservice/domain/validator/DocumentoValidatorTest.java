package br.com.fiap.soat15.osservice.domain.validator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DocumentoValidatorTest {

    private DocumentoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DocumentoValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // CPFs válidos (dígitos verificadores corretos)
            "11144477735",
            "52998224725",
            "12345678909",
            "98765432100",
            "54938271079",
            // CNPJs alfanuméricos válidos (14 chars, somente maiúsculas/dígitos)
            "ABC12345000120",
            "12A34B56000138",
            "Z9Y8X7W6000105",
            "L1M2N3O4000192",
            "A1B2C3D4000109"
    })
    @DisplayName("Deve aceitar cpf/cnpj válidos")
    void deveAceitarCpfCnpjValidos(String cpfCnpj) {
        Assertions.assertThat(validator.isValid(cpfCnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "12345678900",    // CPF com d2 errado (correto seria 12345678909)
            "00000000000",    // todos dígitos iguais
            "98765432199",    // CPF com dígitos verificadores errados
            "11122233344",    // CPF com dígitos verificadores errados
            "45678912355",    // CPF com dígitos verificadores errados
            "abc12345000100", // CNPJ alfanumérico com letras minúsculas
            "12a34b56000199", // CNPJ alfanumérico com letras minúsculas
            "ABC1234500012",  // CNPJ alfanumérico com 13 chars (tamanho inválido)
            "ABCDEFGHIJKLMNO", // CNPJ alfanumérico com 15 chars (tamanho inválido)
            "A1B2C3D"         // CNPJ alfanumérico com 7 chars (tamanho inválido)
    })
    @DisplayName("Deve rejeitar cpf/cnpj inválidos")
    void deveRejeitarCpfCnpjInvalidos(String cpfCnpj) {
        assertThat(validator.isValid(cpfCnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Deve rejeitar cpf/cnpj em branco")
    void deveRejeitarCpfCnpjEmBranco(String cpfCnpj) {
        assertThat(validator.isValid(cpfCnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000181",
            "60701190000104",
            "45997418000153",
            "33014556000196"
    })
    @DisplayName("Deve aceitar CNPJs numéricos válidos")
    void deveAceitarCnpjNumericoValido(String cnpj) {
        assertThat(validator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11111111111111",   // todos dígitos iguais
            "12345678000100",   // dígitos verificadores errados
            "00000000000000"    // todos zeros
    })
    @DisplayName("Deve rejeitar CNPJs numéricos inválidos")
    void deveRejeitarCnpjNumericoInvalido(String cnpj) {
        assertThat(validator.isValid(cnpj)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11.222.333/0001-81",   // CNPJ numérico com formatação
            "111.444.777-35"        // CPF com formatação
    })
    @DisplayName("Deve aceitar documentos com máscara de formatação")
    void deveAceitarDocumentosComMascara(String documento) {
        assertThat(validator.isValid(documento)).isTrue();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Deve rejeitar null")
    void deveRejeitarNull() {
        assertThat(validator.isValid(null)).isFalse();
    }
}
