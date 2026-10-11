package mz.ac.ucm.feng.ti.vaultkey.domain.password;

import java.security.SecureRandom;

/**
 * Gerador de senhas com SecureRandom. Garante pelo menos uma categoria
 * seleccionada e, por omissão, a presença de cada categoria seleccionada
 * (baralhamento Fisher-Yates com SecureRandom, sem viés relevante).
 */
public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String UPPER_AMBIGUOUS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String LOWER_AMBIGUOUS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String DIGITS_AMBIGUOUS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?";

    /** Caracteres ambíguos excluídos pela opção correspondente. */
    public static final String AMBIGUOUS = "0O1lI";

    private final SecureRandom random;

    public PasswordGenerator() {
        this(new SecureRandom());
    }

    public PasswordGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate(PasswordOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Opções nulas.");
        }
        String upper = options.isExcludeAmbiguous() ? UPPER : UPPER_AMBIGUOUS;
        String lower = options.isExcludeAmbiguous() ? LOWER : LOWER_AMBIGUOUS;
        String digits = options.isExcludeAmbiguous() ? DIGITS : DIGITS_AMBIGUOUS;

        StringBuilder pool = new StringBuilder();
        StringBuilder required = new StringBuilder();
        if (options.isUpper()) {
            pool.append(upper);
            required.append(pickOne(upper));
        }
        if (options.isLower()) {
            pool.append(lower);
            required.append(pickOne(lower));
        }
        if (options.isDigits()) {
            pool.append(digits);
            required.append(pickOne(digits));
        }
        if (options.isSymbols()) {
            pool.append(SYMBOLS);
            required.append(pickOne(SYMBOLS));
        }
        if (pool.length() == 0) {
            throw new IllegalArgumentException("Selecciona pelo menos uma categoria.");
        }
        int length = Math.max(4, Math.min(128, options.getLength()));
        if (options.isRequireEachCategory() && length < required.length()) {
            throw new IllegalArgumentException("Comprimento insuficiente para as categorias.");
        }
        char[] out = new char[length];
        int pos = 0;
        if (options.isRequireEachCategory()) {
            for (int i = 0; i < required.length(); i++) {
                out[pos++] = required.charAt(i);
            }
        }
        while (pos < length) {
            out[pos++] = pool.charAt(random.nextInt(pool.length()));
        }
        // Fisher-Yates com SecureRandom
        for (int i = out.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char t = out[i];
            out[i] = out[j];
            out[j] = t;
        }
        return new String(out);
    }

    private char pickOne(String chars) {
        return chars.charAt(random.nextInt(chars.length()));
    }
}
