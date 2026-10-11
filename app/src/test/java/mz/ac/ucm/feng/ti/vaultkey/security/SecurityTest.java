package mz.ac.ucm.feng.ti.vaultkey.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import mz.ac.ucm.feng.ti.vaultkey.security.crypto.AesGcmService;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.CryptoException;
import mz.ac.ucm.feng.ti.vaultkey.security.pin.PinValidator;
import mz.ac.ucm.feng.ti.vaultkey.security.pin.PinVerifier;

public class SecurityTest {

    // ---- PIN ----

    @Test
    public void pinFormat() {
        assertTrue(PinValidator.isValidFormat("1234"));
        assertTrue(PinValidator.isValidFormat("000000000000"));
        assertFalse(PinValidator.isValidFormat("123"));
        assertFalse(PinValidator.isValidFormat("1234567890123"));
        assertFalse(PinValidator.isValidFormat("12a4"));
        assertFalse(PinValidator.isValidFormat(null));
        assertFalse(PinValidator.isValidFormat(""));
    }

    @Test
    public void pinCreateAndVerify() {
        PinVerifier verifier = new PinVerifier();
        String stored = verifier.create("4829");
        assertTrue(stored.startsWith("pbkdf2-sha256$"));
        assertTrue(verifier.verify("4829", stored));
        assertFalse(verifier.verify("4830", stored));
        assertFalse(verifier.verify(null, stored));
        assertFalse(verifier.verify("4829", "formato-invalido"));
    }

    @Test
    public void pinSaltsAreUnique() {
        PinVerifier verifier = new PinVerifier();
        String a = verifier.create("4829");
        String b = verifier.create("4829");
        assertFalse(a.equals(b));
        assertTrue(verifier.verify("4829", a));
        assertTrue(verifier.verify("4829", b));
    }

    @Test
    public void pinRejectsBadFormat() {
        try {
            new PinVerifier().create("12");
            fail("Devia rejeitar PIN curto");
        } catch (IllegalArgumentException expected) {
        }
    }

    // ---- AES-GCM ----

    private static SecretKey testKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        return kg.generateKey();
    }

    @Test
    public void aesGcmRoundTrip() throws Exception {
        AesGcmService crypto = new AesGcmService();
        SecretKey key = testKey();
        byte[] aad = "cred:42:password".getBytes("UTF-8");
        String payload = crypto.encryptToString(key, "s3nh4-s3cr3t4!", aad);
        assertTrue(payload.startsWith("v1."));
        assertEquals("s3nh4-s3cr3t4!", crypto.decryptToString(key, payload, aad));
    }

    private static void assertEquals(String expected, String actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }

    @Test
    public void aesGcmNoncesAreUnique() throws Exception {
        AesGcmService crypto = new AesGcmService();
        SecretKey key = testKey();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String p = crypto.encryptToString(key, "mesmo-texto", null);
            assertTrue("Nonce repetido", seen.add(p));
        }
    }

    @Test
    public void aesGcmDetectsTampering() throws Exception {
        AesGcmService crypto = new AesGcmService();
        SecretKey key = testKey();
        String payload = crypto.encryptToString(key, "dado", null);
        String[] parts = payload.split("\\.");
        // Altera o último carácter do texto cifrado.
        String ct = parts[2];
        char last = ct.charAt(ct.length() - 1);
        String tampered = parts[0] + "." + parts[1] + "."
                + ct.substring(0, ct.length() - 1) + (last == 'A' ? 'B' : 'A');
        try {
            crypto.decryptToString(key, tampered, null);
            fail("Devia detectar adulteração");
        } catch (CryptoException expected) {
        }
    }

    @Test
    public void aesGcmRejectsWrongAad() throws Exception {
        AesGcmService crypto = new AesGcmService();
        SecretKey key = testKey();
        String payload = crypto.encryptToString(key, "dado", "cred:1:password".getBytes("UTF-8"));
        try {
            crypto.decryptToString(key, payload, "cred:2:password".getBytes("UTF-8"));
            fail("Devia rejeitar AAD diferente");
        } catch (CryptoException expected) {
        }
    }

    @Test
    public void aesGcmRejectsWrongKeyAndFormat() throws Exception {
        AesGcmService crypto = new AesGcmService();
        String payload = crypto.encryptToString(testKey(), "dado", null);
        try {
            crypto.decryptToString(testKey(), payload, null);
            fail("Devia rejeitar chave errada");
        } catch (CryptoException expected) {
        }
        try {
            crypto.decryptToString(testKey(), "lixo", null);
            fail("Devia rejeitar formato inválido");
        } catch (CryptoException expected) {
        }
    }
}
