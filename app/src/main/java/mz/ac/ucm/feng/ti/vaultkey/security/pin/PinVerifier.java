package mz.ac.ucm.feng.ti.vaultkey.security.pin;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Verificador de PIN com PBKDF2-HMAC-SHA256 (210 000 iterações, salt de 128 bits,
 * saída de 256 bits). O PIN nunca é guardado em texto simples nem usado
 * directamente como chave AES.
 *
 * <p>Formato persistido: {@code pbkdf2-sha256$iterações$saltB64$hashB64}</p>
 */
public final class PinVerifier {

    private static final String PREFIX = "pbkdf2-sha256";
    private static final int ITERATIONS = 210000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    /** Cria um novo verificador para um PIN válido. */
    public String create(String pin) {
        if (!PinValidator.isValidFormat(pin)) {
            throw new IllegalArgumentException("Formato de PIN inválido.");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(pin.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Verifica um PIN contra o verificador guardado (comparação em tempo constante). */
    public boolean verify(String pin, String stored) {
        if (pin == null || stored == null) {
            return false;
        }
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(pin.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(char[] pin, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] out = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 indisponível neste dispositivo.", e);
        }
    }
}
