package mz.ac.ucm.feng.ti.vaultkey.domain.password;

import mz.ac.ucm.feng.ti.vaultkey.domain.model.PasswordStrength;

/**
 * Avaliação de força: comprimento, diversidade e padrões previsíveis.
 * O resultado é uma estimativa — nunca uma garantia absoluta.
 */
public final class PasswordStrengthEvaluator {

    private static final String[] COMMON = {
            "123456", "password", "qwerty", "abc123", "111111", "123123",
            "admin", "vaultkey", "senha", "palavra"
    };

    private PasswordStrengthEvaluator() {
    }

    public static PasswordStrength evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(PasswordStrength.Level.WEAK, 0,
                    "Escolhe uma senha mais longa.");
        }
        String lower = password.toLowerCase();
        for (String c : COMMON) {
            if (lower.contains(c)) {
                return new PasswordStrength(PasswordStrength.Level.WEAK, 5,
                        "Evita padrões comuns como \"" + c + "\".");
            }
        }
        int score = 0;
        int len = password.length();
        if (len >= 8) {
            score += 10;
        }
        if (len >= 12) {
            score += 15;
        }
        if (len >= 16) {
            score += 15;
        }
        if (len >= 20) {
            score += 10;
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < len; i++) {
            char ch = password.charAt(i);
            if (Character.isLowerCase(ch)) {
                hasLower = true;
            } else if (Character.isUpperCase(ch)) {
                hasUpper = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }
        int variety = 0;
        if (hasLower) {
            variety++;
        }
        if (hasUpper) {
            variety++;
        }
        if (hasDigit) {
            variety++;
        }
        if (hasSymbol) {
            variety++;
        }
        score += variety * 8;

        if (hasRepeatedSequence(password)) {
            score -= 15;
        }
        if (hasSequentialRun(password)) {
            score -= 10;
        }
        score = Math.max(0, Math.min(100, score));

        PasswordStrength.Level level;
        String advice;
        if (score < 25) {
            level = PasswordStrength.Level.WEAK;
            advice = "Muito curta ou pouco variada. Usa 16+ caracteres.";
        } else if (score < 45) {
            level = PasswordStrength.Level.FAIR;
            advice = "Razoável. Aumenta o comprimento e mistura categorias.";
        } else if (score < 65) {
            level = PasswordStrength.Level.GOOD;
            advice = "Boa. Mais comprimento torna-a ainda melhor.";
        } else if (score < 85) {
            level = PasswordStrength.Level.STRONG;
            advice = "Forte. Bom equilíbrio de comprimento e variedade.";
        } else {
            level = PasswordStrength.Level.VERY_STRONG;
            advice = "Muito forte.";
        }
        return new PasswordStrength(level, score, advice);
    }

    private static boolean hasRepeatedSequence(String s) {
        for (int i = 0; i + 5 < s.length(); i++) {
            String sub = s.substring(i, i + 3);
            if (s.indexOf(sub, i + 3) >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSequentialRun(String s) {
        int run = 1;
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == s.charAt(i - 1) + 1) {
                if (++run >= 4) {
                    return true;
                }
            } else {
                run = 1;
            }
        }
        return false;
    }
}
