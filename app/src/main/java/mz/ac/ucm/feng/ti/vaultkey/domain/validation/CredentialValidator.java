package mz.ac.ucm.feng.ti.vaultkey.domain.validation;

import java.util.regex.Pattern;

import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;

/** Regras de negócio para credenciais (puras, testáveis na JVM). */
public final class CredentialValidator {

    public static final int MAX_SERVICE = 120;
    public static final int MAX_FIELD = 500;

    // Validação permissiva de URL: aceita domínios, IPv4 e esquemas comuns,
    // sem rejeitar desnecessariamente URLs válidos.
    private static final Pattern URL_LIKE = Pattern.compile(
            "^[a-zA-Z][a-zA-Z0-9+.-]*://[^\\s/$.?#].[^\\s]*$|^[^\\s/:]+\\.[^\\s/:]{2,}(:\\d+)?(/[^\\s]*)?$");

    private CredentialValidator() {
    }

    /** Devolve null se válido, ou mensagem de erro em português. */
    public static String validate(Credential c) {
        if (c == null) {
            return "Credencial inválida.";
        }
        String service = safe(c.getServiceName()).trim();
        if (service.isEmpty() || service.length() > MAX_SERVICE) {
            return "O serviço é obrigatório (máx. 120 caracteres).";
        }
        if (c.getPassword() == null || c.getPassword().isEmpty()) {
            return "A senha é obrigatória.";
        }
        if (c.getPassword().length() > MAX_FIELD) {
            return "A senha excede o tamanho máximo.";
        }
        String url = safe(c.getUrl()).trim();
        if (!url.isEmpty() && !URL_LIKE.matcher(url).matches()) {
            return "O URL parece inválido.";
        }
        if (safe(c.getUsername()).length() > MAX_FIELD
                || safe(c.getNotes()).length() > 2000
                || safe(c.getCategory()).length() > MAX_FIELD) {
            return "Um dos campos excede o tamanho máximo.";
        }
        return null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
