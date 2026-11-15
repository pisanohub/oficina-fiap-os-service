package br.com.fiap.soat15.osservice.domain.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlacaValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlacaValida {

    String message() default "Placa inválida. Use o formato antigo (ABC-1234) ou Mercosul (ABC1D23)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
