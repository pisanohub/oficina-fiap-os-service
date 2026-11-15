package br.com.fiap.soat15.osservice.domain.validator;

import java.text.Normalizer;

public class StringUtils {

    public static String removerAcentos(String texto) {
        if (texto == null) return null;

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
