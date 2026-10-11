package mz.ac.ucm.feng.ti.vaultkey.authenticator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.base32.Base32;

/** Vectores RFC 4648 §10 (confirmados) + casos de rejeição. */
public class Base32Test {

    @Test
    public void rfc4648Vectors() {
        assertEquals("MY======", Base32.encode(bytes("f")));
        assertEquals("MZXQ====", Base32.encode(bytes("fo")));
        assertEquals("MZXW6===", Base32.encode(bytes("foo")));
        assertEquals("MZXW6YQ=", Base32.encode(bytes("foob")));
        assertEquals("MZXW6YTB", Base32.encode(bytes("fooba")));
        assertEquals("MZXW6YTBOI======", Base32.encode(bytes("foobar")));
    }

    @Test
    public void decodeRoundTrip() {
        assertArrayEquals(bytes("foobar"), Base32.decode("MZXW6YTBOI======"));
        assertArrayEquals(bytes("foobar"), Base32.decode("mzxw6ytboi")); // minúsculas, sem padding
        assertArrayEquals(bytes("foo"), Base32.decode("MZ XW 6===")); // espaços tolerados
    }

    @Test
    public void rejectsInvalid() {
        try {
            Base32.decode("ABC!2345");
            fail("Devia rejeitar carácter inválido");
        } catch (IllegalArgumentException expected) {
        }
        try {
            Base32.decode("A");
            fail("Devia rejeitar comprimento % 8 == 1");
        } catch (IllegalArgumentException expected) {
        }
        try {
            Base32.decode("   ");
            fail("Devia rejeitar vazio");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void secretValidation() {
        assertTrue(Base32.isValidSecret("JBSWY3DPEHPK3PXP")); // 16 chars
        assertTrue(Base32.isValidSecret("jbswy3dpehpk3pxp")); // minúsculas
        assertFalse(Base32.isValidSecret("ABC")); // demasiado curto
        assertFalse(Base32.isValidSecret("JBSWY3D!EHPK3PXP")); // carácter inválido
        assertFalse(Base32.isValidSecret(null));
    }

    private static byte[] bytes(String s) {
        try {
            return s.getBytes("US-ASCII");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
