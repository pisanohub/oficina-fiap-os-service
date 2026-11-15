package br.com.fiap.soat15.osservice.domain.validator;

public class DocumentoValidator {

    public boolean isValid(String value) {
        if (value == null || value.isBlank()) return false;

        String doc = value.replaceAll("[^A-Za-z0-9]", "");

        if (doc.length() == 11 && doc.matches("\\d+")) {
            return isCpfValido(doc);
        }

        if (doc.length() == 14) {
            if (doc.matches("\\d+")) {
                return isCnpjValido(doc);
            }
            return isCnpjAlfanumericoValido(doc);
        }

        return false;
    }

    private boolean isCpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) return false;

        int d1 = calcularDigitoCpf(cpf, 10);
        int d2 = calcularDigitoCpf(cpf, 11);

        return cpf.equals(cpf.substring(0, 9) + d1 + d2);
    }

    private int calcularDigitoCpf(String cpf, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;

        for (int i = 0; i < pesoInicial - 1; i++) {
            soma += (cpf.charAt(i) - '0') * peso--;
        }

        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }

    private boolean isCnpjValido(String cnpj) {
        if (cnpj.chars().distinct().count() == 1) return false;

        int d1 = calcularDigitoCnpj(cnpj, 12);
        int d2 = calcularDigitoCnpj(cnpj, 13);

        return cnpj.equals(cnpj.substring(0, 12) + d1 + d2);
    }

    private int calcularDigitoCnpj(String cnpj, int tamanho) {
        int[] pesos = (tamanho == 12)
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int soma = 0;

        for (int i = 0; i < tamanho; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }

        int resto = soma % 11;
        return (resto < 2) ? 0 : 11 - resto;
    }

    private boolean isCnpjAlfanumericoValido(String cnpj) {
        return cnpj.matches("^[A-Z0-9]{14}$");
    }
}
