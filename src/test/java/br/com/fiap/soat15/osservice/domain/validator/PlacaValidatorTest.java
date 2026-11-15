package br.com.fiap.soat15.osservice.domain.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PlacaValidatorTest {

    private PlacaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PlacaValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ABC1234",
            "ABC-1234",
            "abc1234",
            "abc-1234",
            "ABC1D23",
            "XYZ9W87",
            "abc1d23",
    })
    @DisplayName("Deve aceitar placas válidas")
    void deveAceitarPlacasValidas(String placa) {
        assertThat(validator.isValid(placa, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "AB1234",
            "ABCD1234",
            "ABC123",
            "ABC12345",
            "1BC1234",
            "ABC1234X",
            "ABC12D3",
            "ABC1DD3",
            "ABCD123",
            "ABC-1D23",
            "123-4567",
            "ABCDEFG",
    })
    @DisplayName("Deve rejeitar placas inválidas")
    void deveRejeitarPlacasInvalidas(String placa) {
        assertThat(validator.isValid(placa, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Deve rejeitar placas em branco")
    void deveRejeitarPlacasEmBranco(String placa) {
        assertThat(validator.isValid(placa, null)).isFalse();
    }
}
