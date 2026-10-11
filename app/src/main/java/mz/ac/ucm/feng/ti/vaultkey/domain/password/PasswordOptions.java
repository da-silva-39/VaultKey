package mz.ac.ucm.feng.ti.vaultkey.domain.password;

/** Opções do gerador de senhas. */
public class PasswordOptions {

    private int length = 16;
    private boolean upper = true;
    private boolean lower = true;
    private boolean digits = true;
    private boolean symbols = true;
    private boolean excludeAmbiguous = false;
    private boolean requireEachCategory = true;

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public boolean isUpper() { return upper; }
    public void setUpper(boolean upper) { this.upper = upper; }
    public boolean isLower() { return lower; }
    public void setLower(boolean lower) { this.lower = lower; }
    public boolean isDigits() { return digits; }
    public void setDigits(boolean digits) { this.digits = digits; }
    public boolean isSymbols() { return symbols; }
    public void setSymbols(boolean symbols) { this.symbols = symbols; }
    public boolean isExcludeAmbiguous() { return excludeAmbiguous; }
    public void setExcludeAmbiguous(boolean excludeAmbiguous) { this.excludeAmbiguous = excludeAmbiguous; }
    public boolean isRequireEachCategory() { return requireEachCategory; }
    public void setRequireEachCategory(boolean requireEachCategory) {
        this.requireEachCategory = requireEachCategory;
    }
}
