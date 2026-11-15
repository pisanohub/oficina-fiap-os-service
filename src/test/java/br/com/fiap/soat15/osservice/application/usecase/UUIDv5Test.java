
package br.com.fiap.soat15.osservice.application.usecase;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDv5Test {

    @Test
    void deveGerarUuidDeterministicoParaMesmoId() {
        UUID primeiro = UUIDv5.gerarUuidV5PorId(1L);
        UUID segundo = UUIDv5.gerarUuidV5PorId(1L);

        assertThat(primeiro).isEqualTo(segundo);
    }

    @Test
    void deveGerarUuidsDiferentesParaIdsDiferentes() {
        UUID primeiro = UUIDv5.gerarUuidV5PorId(1L);
        UUID segundo = UUIDv5.gerarUuidV5PorId(2L);

        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    void deveGerarUuidNaVersaoCinco() {
        UUID uuid = UUIDv5.gerarUuidV5PorId(1L);

        assertThat(uuid.version()).isEqualTo(5);
    }

    @Test
    void deveGerarUuidComVarianteRfc4122() {
        UUID uuid = UUIDv5.gerarUuidV5PorId(1L);

        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void deveGerarUuidParaIdZero() {
        UUID uuid = UUIDv5.gerarUuidV5PorId(0L);

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(5);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void deveGerarUuidParaIdNegativo() {
        UUID uuid = UUIDv5.gerarUuidV5PorId(-1L);

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(5);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    void deveGerarUuidComFormatoValido() {
        UUID uuid = UUIDv5.gerarUuidV5PorId(12345L);

        assertThat(uuid.toString())
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    }
}
