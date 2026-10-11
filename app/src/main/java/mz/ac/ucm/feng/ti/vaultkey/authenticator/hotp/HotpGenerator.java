package mz.ac.ucm.feng.ti.vaultkey.authenticator.hotp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HOTP — HMAC-Based One-Time Password (RFC 4226).
 * Implementação pura de Java, verificada com os vectores oficiais do RFC 4226.
 */
public final class HotpGenerator {

    private HotpGenerator() {
    }

    /**
     * Gera um código HOTP.
     *
     * @param secret    segredo partilhado (bytes, >= 16 bytes recomendado)
     * @param counter   contador (0..2^63-1)
     * @param digits    6 a 10 dígitos
     * @param hmacAlgo  "HmacSHA1", "HmacSHA256" ou "HmacSHA512"
     * @throws IllegalArgumentException em parâmetros inválidos
     */
    public static String generate(byte[] secret, long counter, int digits, String hmacAlgo) {
        if (secret == null || secret.length == 0) {
            throw new IllegalArgumentException("Segredo HOTP vazio.");
        }
        if (counter < 0) {
            throw new IllegalArgumentException("Contador HOTP negativo.");
        }
        if (digits < 6 || digits > 10) {
            throw new IllegalArgumentException("Dígitos HOTP fora do intervalo 6–10.");
        }
        final String algo = hmacAlgo == null ? "HmacSHA1" : hmacAlgo;
        if (!algo.equals("HmacSHA1") && !algo.equals("HmacSHA256") && !algo.equals("HmacSHA512")) {
            throw new IllegalArgumentException("Algoritmo HMAC não suportado: " + algo);
        }
        try {
            byte[] counterBytes = new byte[8];
            long c = counter;
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (c & 0xFF);
                c >>= 8;
            }
            Mac mac = Mac.getInstance(algo);
            mac.init(new SecretKeySpec(secret, algo));
            byte[] hash = mac.doFinal(counterBytes);

            // Truncagem dinâmica (RFC 4226, secção 5.3)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            long mod = 1;
            for (int i = 0; i < digits; i++) {
                mod *= 10;
            }
            long otp = (binary & 0xFFFFFFFFL) % mod;

            String text = Long.toString(otp);
            while (text.length() < digits) {
                text = "0" + text;
            }
            return text;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha criptográfica ao gerar HOTP.", e);
        }
    }

    /** Atalho com SHA-1 (perfil padrão de interoperabilidade). */
    public static String generateSha1(byte[] secret, long counter, int digits) {
        return generate(secret, counter, digits, "HmacSHA1");
    }
}
