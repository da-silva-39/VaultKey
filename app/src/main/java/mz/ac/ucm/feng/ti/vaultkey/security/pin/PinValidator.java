package mz.ac.ucm.feng.ti.vaultkey.security.pin;

/** Validação de formato do PIN (pura, testável). */
public final class PinValidator {

    public static final int MIN_LENGTH = 4;
    public static final int MAX_LENGTH = 12;

    private PinValidator() {
    }

    public static boolean isValidFormat(String pin) {
        if (pin == null) {
            return false;
        }
        int n = pin.length();
        if (n < MIN_LENGTH || n > MAX_LENGTH) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (!Character.isDigit(pin.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
