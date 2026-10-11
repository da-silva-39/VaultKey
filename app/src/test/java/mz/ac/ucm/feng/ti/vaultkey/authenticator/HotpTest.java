package mz.ac.ucm.feng.ti.vaultkey.authenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.hotp.HotpGenerator;

/**
 * Vectores oficiais do RFC 4226 (Apêndice D).
 * Segredo ASCII "12345678901234567890" (SHA-1), 6 dígitos.
 */
public class HotpTest {

    private static final byte[] SECRET = ascii("12345678901234567890");
    private static final String[] EXPECTED = {
            "755224", "287082", "359152", "969429", "338314",
            "254676", "287922", "162583", "399871", "520489"
    };

    @Test
    public void rfc4226AppendixD() {
        for (int counter = 0; counter < EXPECTED.length; counter++) {
            assertEquals("Contador " + counter,
                    EXPECTED[counter],
                    HotpGenerator.generateSha1(SECRET, counter, 6));
        }
    }

    @Test
    public void rejectsInvalidParams() {
        try {
            HotpGenerator.generate(SECRET, -1, 6, "HmacSHA1");
            fail("Contador negativo");
        } catch (IllegalArgumentException expected) {
        }
        try {
            HotpGenerator.generate(SECRET, 0, 5, "HmacSHA1");
            fail("Dígitos < 6");
        } catch (IllegalArgumentException expected) {
        }
        try {
            HotpGenerator.generate(SECRET, 0, 6, "HmacMD5");
            fail("Algoritmo não suportado");
        } catch (IllegalArgumentException expected) {
        }
        try {
            HotpGenerator.generate(new byte[0], 0, 6, "HmacSHA1");
            fail("Segredo vazio");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void supportsSha256AndSha512() {
        // Apenas consistência de formato (8 dígitos, zeros à esquerda preservados).
        String sha256 = HotpGenerator.generate(SECRET, 0, 8, "HmacSHA256");
        String sha512 = HotpGenerator.generate(SECRET, 0, 8, "HmacSHA512");
        assertEquals(8, sha256.length());
        assertEquals(8, sha512.length());
    }

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
