package br.com.fiap.soat15.osservice.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UUIDv5 {
    // Namespace fixo para a sua aplicação
    private static final UUID NAMESPACE_CUSTOM = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    public static UUID gerarUuidV5PorId(long idUnico) {
        // Monta a string base usando apenas o ID
        String entrada = "ordemId:" + idUnico;

        return uuidV5(NAMESPACE_CUSTOM, entrada);
    }

    private static UUID uuidV5(UUID namespace, String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(toBytes(namespace));
            md.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest();

            // Ajusta os bits para o formato padrão do UUID v5
            hash[6] &= 0x0f;
            hash[6] |= 0x50;
            hash[8] &= 0x3f;
            hash[8] |= (byte) 0x80;

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not supported", e);
        }
    }

    private static byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];
        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> (7 - i) * 8);
            buffer[i + 8] = (byte) (lsb >>> (7 - i) * 8);
        }
        return buffer;
    }
}
