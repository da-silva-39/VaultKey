package mz.ac.ucm.feng.ti.vaultkey.domain.model;

/** Resultado da avaliação de força de uma senha (estimativa, não garantia). */
public class PasswordStrength {

    public enum Level { WEAK, FAIR, GOOD, STRONG, VERY_STRONG }

    private final Level level;
    private final int score;
    private final String advice;

    public PasswordStrength(Level level, int score, String advice) {
        this.level = level;
        this.score = score;
        this.advice = advice;
    }

    public Level getLevel() { return level; }
    public int getScore() { return score; }
    public String getAdvice() { return advice; }
}
