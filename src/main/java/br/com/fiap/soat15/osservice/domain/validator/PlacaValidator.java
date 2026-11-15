package br.com.fiap.soat15.osservice.domain.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlacaValidator implements ConstraintValidator<PlacaValida, String> {

    private static final String PADRAO_ANTIGO = "^[A-Za-z]{3}-?\\d{4}$";
    private static final String PADRAO_MERCOSUL = "^[A-Za-z]{3}\\d[A-Za-z]\\d{2}$";

    @Override
    public boolean isValid(String placa, ConstraintValidatorContext context) {
        if (placa == null || placa.isBlank()) {
            return false;
        }
        String placaNormalizada = placa.trim().toUpperCase();
        return placaNormalizada.matches(PADRAO_ANTIGO) ||
                placaNormalizada.matches(PADRAO_MERCOSUL);
    }
}
