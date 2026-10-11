package mz.ac.ucm.feng.ti.vaultkey.authenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.totp.TotpGenerator;

/**
 * Vectores oficiais do RFC 6238 (Apêndice B, T0=0, X=30, 8 dígitos).
 */
public class TotpTest {

    private static final byte[] SHA1_SECRET = ascii("12345678901234567890");
    private static final byte[] SHA256_SECRET = ascii("12345678901234567890123456789012");
    private static final byte[] SHA512_SECRET = ascii(
            "1234567890123456789012345678901234567890123456789012345678901234");

    // {instanteSegundos, esperadoSHA1, esperadoSHA256, esperadoSHA512}
    private static final Object[][] VECTORS = {
            {59L, "94287082", "46119246", "90693936"},
            {1111111109L, "07081804", "68084774", "25091201"},
            {1111111111L, "14050471", "67062674", "99943326"},
            {1234567890L, "89005924", "91819424", "93441116"},
            {2000000000L, "69279037", "90698825", "38618901"},
            {20000000000L, "65353130", "77737706", "47863826"},
    };

    @Test
    public void rfc6238AppendixB() {
        for (Object[] v : VECTORS) {
            long timeSecs = (Long) v[0];
            long millis = timeSecs * 1000L;
            assertEquals("SHA1 @ " + timeSecs, v[1],
                    TotpGenerator.generate(SHA1_SECRET, millis, 30, 8, "HmacSHA1"));
            assertEquals("SHA256 @ " + timeSecs, v[2],
                    TotpGenerator.generate(SHA256_SECRET, millis, 30, 8, "HmacSHA256"));
            assertEquals("SHA512 @ " + timeSecs, v[3],
                    TotpGenerator.generate(SHA512_SECRET, millis, 30, 8, "HmacSHA512"));
        }
    }

    @Test
    public void secondsRemaining() {
        assertEquals(30, TotpGenerator.secondsRemaining(0, 30));
        assertEquals(1, TotpGenerator.secondsRemaining(29_000, 30));
        assertEquals(30, TotpGenerator.secondsRemaining(30_000, 30));
        assertEquals(15, TotpGenerator.secondsRemaining(45_000, 30));
    }

    @Test
    public void rejectsInvalidPeriod() {
        try {
            TotpGenerator.generate(SHA1_SECRET, 59_000, 5, 6, "HmacSHA1");
            fail("Período < 10");
        } catch (IllegalArgumentException expected) {
        }
        try {
            TotpGenerator.generate(SHA1_SECRET, 59_000, 300, 6, "HmacSHA1");
            fail("Período > 120");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static byte[] ascii(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
