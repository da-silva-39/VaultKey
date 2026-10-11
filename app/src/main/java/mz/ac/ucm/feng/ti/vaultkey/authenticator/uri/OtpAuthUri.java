package mz.ac.ucm.feng.ti.vaultkey.authenticator.uri;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.base32.Base32;

/**
 * Interpretação de URIs {@code otpauth://} (formato Google Authenticator).
 * Ex.: otpauth://totp/Emissor:conta?secret=BASE32&issuer=Emissor&algorithm=SHA1&digits=6&period=30
 * Classe pura de Java.
 */
public final class OtpAuthUri {

    public enum Type { TOTP, HOTP }

    private final Type type;
    private final String issuer;
    private final String account;
    private final String secret;
    private final String algorithm;
    private final int digits;
    private final int period;
    private final long counter;

    private OtpAuthUri(Type type, String issuer, String account, String secret,
                       String algorithm, int digits, int period, long counter) {
        this.type = type;
        this.issuer = issuer;
        this.account = account;
        this.secret = secret;
        this.algorithm = algorithm;
        this.digits = digits;
        this.period = period;
        this.counter = counter;
    }

    public Type getType() { return type; }
    public String getIssuer() { return issuer; }
    public String getAccount() { return account; }
    /** Segredo Base32 normalizado (maiúsculas, sem espaços). */
    public String getSecret() { return secret; }
    /** "HmacSHA1" | "HmacSHA256" | "HmacSHA512". */
    public String getAlgorithm() { return algorithm; }
    public int getDigits() { return digits; }
    public int getPeriod() { return period; }
    public long getCounter() { return counter; }

    /**
     * Interpreta e valida uma URI otpauth://. Não confia cegamente nos dados:
     * valida esquema, tipo, segredo, algoritmo, dígitos, período e contador.
     */
    public static OtpAuthUri parse(String raw) throws OtpAuthParseException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new OtpAuthParseException("URI vazia.");
        }
        final URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (Exception e) {
            throw new OtpAuthParseException("URI malformada.");
        }
        if (!"otpauth".equalsIgnoreCase(uri.getScheme())) {
            throw new OtpAuthParseException("Esquema inválido (esperado otpauth://).");
        }
        final Type type;
        String host = uri.getHost();
        if (host == null) {
            // URIs como otpauth://totp/... podem expor o tipo como autoridade;
            // fallback para a primeira parte do caminho.
            throw new OtpAuthParseException("Tipo de conta ausente (totp/hotp).");
        }
        if ("totp".equalsIgnoreCase(host)) {
            type = Type.TOTP;
        } else if ("hotp".equalsIgnoreCase(host)) {
            type = Type.HOTP;
        } else {
            throw new OtpAuthParseException("Tipo desconhecido: " + host);
        }

        String label = uri.getPath();
        String issuer = "";
        String account = "";
        if (label != null && label.length() > 1) {
            label = decodeComponent(label.substring(1));
            int colon = label.indexOf(':');
            if (colon >= 0) {
                issuer = label.substring(0, colon).trim();
                account = label.substring(colon + 1).trim();
            } else {
                account = label.trim();
            }
        }

        Map<String, String> q = parseQuery(uri.getRawQuery());
        String secretRaw = q.get("secret");
        if (secretRaw == null || secretRaw.isEmpty()) {
            throw new OtpAuthParseException("Segredo ausente.");
        }
        String secret = decodeComponent(secretRaw).replaceAll("\\s+", "").toUpperCase();
        if (!Base32.isValidSecret(secret)) {
            throw new OtpAuthParseException("Segredo Base32 inválido.");
        }

        String issuerParam = q.get("issuer");
        if (issuer.isEmpty() && issuerParam != null) {
            issuer = decodeComponent(issuerParam).trim();
        }

        String algoParam = q.get("algorithm");
        String algorithm = "HmacSHA1";
        if (algoParam != null && !algoParam.isEmpty()) {
            String a = decodeComponent(algoParam).trim().toUpperCase().replace("-", "");
            if (a.equals("SHA1")) {
                algorithm = "HmacSHA1";
            } else if (a.equals("SHA256")) {
                algorithm = "HmacSHA256";
            } else if (a.equals("SHA512")) {
                algorithm = "HmacSHA512";
            } else {
                throw new OtpAuthParseException("Algoritmo não suportado.");
            }
        }

        int digits = parseIntParam(q.get("digits"), 6, "dígitos");
        if (digits < 6 || digits > 10) {
            throw new OtpAuthParseException("Dígitos fora do intervalo 6–10.");
        }

        int period = 30;
        long counter = 0;
        if (type == Type.TOTP) {
            period = parseIntParam(q.get("period"), 30, "período");
            if (period < 10 || period > 120) {
                throw new OtpAuthParseException("Período fora do intervalo 10–120 s.");
            }
        } else {
            String c = q.get("counter");
            if (c != null && !c.isEmpty()) {
                try {
                    counter = Long.parseLong(c.trim());
                } catch (NumberFormatException e) {
                    throw new OtpAuthParseException("Contador inválido.");
                }
                if (counter < 0) {
                    throw new OtpAuthParseException("Contador negativo.");
                }
            }
        }
        return new OtpAuthUri(type, issuer, account, secret, algorithm, digits, period, counter);
    }

    private static int parseIntParam(String raw, int def, String nome) throws OtpAuthParseException {
        if (raw == null || raw.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new OtpAuthParseException("Parâmetro de " + nome + " inválido.");
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                map.put(pair.substring(0, eq).toLowerCase(), pair.substring(eq + 1));
            }
        }
        return map;
    }

    private static String decodeComponent(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
