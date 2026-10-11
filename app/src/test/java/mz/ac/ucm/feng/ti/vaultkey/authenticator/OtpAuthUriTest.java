package mz.ac.ucm.feng.ti.vaultkey.authenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.uri.OtpAuthParseException;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.uri.OtpAuthUri;

public class OtpAuthUriTest {

    @Test
    public void parsesTotpUri() throws Exception {
        OtpAuthUri u = OtpAuthUri.parse(
                "otpauth://totp/Example:alice@example.com"
                        + "?secret=JBSWY3DPEHPK3PXP&issuer=Example"
                        + "&algorithm=SHA1&digits=6&period=30");
        assertEquals(OtpAuthUri.Type.TOTP, u.getType());
        assertEquals("Example", u.getIssuer());
        assertEquals("alice@example.com", u.getAccount());
        assertEquals("JBSWY3DPEHPK3PXP", u.getSecret());
        assertEquals("HmacSHA1", u.getAlgorithm());
        assertEquals(6, u.getDigits());
        assertEquals(30, u.getPeriod());
    }

    @Test
    public void parsesHotpUriWithCounter() throws Exception {
        OtpAuthUri u = OtpAuthUri.parse(
                "otpauth://hotp/Banco:bob?secret=JBSWY3DPEHPK3PXP&counter=7&digits=8");
        assertEquals(OtpAuthUri.Type.HOTP, u.getType());
        assertEquals("Banco", u.getIssuer());
        assertEquals("bob", u.getAccount());
        assertEquals(7, u.getCounter());
        assertEquals(8, u.getDigits());
    }

    @Test
    public void appliesDefaults() throws Exception {
        OtpAuthUri u = OtpAuthUri.parse("otpauth://totp/Site?secret=JBSWY3DPEHPK3PXP");
        assertEquals("HmacSHA1", u.getAlgorithm());
        assertEquals(6, u.getDigits());
        assertEquals(30, u.getPeriod());
        assertEquals("Site", u.getAccount());
    }

    @Test
    public void rejectsBadUris() {
        assertRejects("http://totp/Site?secret=JBSWY3DPEHPK3PXP", "esquema");
        assertRejects("otpauth://totp/Site?issuer=X", "sem segredo");
        assertRejects("otpauth://totp/Site?secret=ABC!!!", "segredo inválido");
        assertRejects("otpauth://totp/Site?secret=JBSWY3DPEHPK3PXP&digits=4", "dígitos");
        assertRejects("otpauth://totp/Site?secret=JBSWY3DPEHPK3PXP&period=5", "período");
        assertRejects("otpauth://steam/Site?secret=JBSWY3DPEHPK3PXP", "tipo");
        assertRejects("otpauth://hotp/S?secret=JBSWY3DPEHPK3PXP&counter=-2", "contador");
        assertRejects("otpauth://totp/S?secret=JBSWY3DPEHPK3PXP&algorithm=MD5", "algoritmo");
    }

    private static void assertRejects(String uri, String reason) {
        try {
            OtpAuthUri.parse(uri);
            fail("Devia rejeitar (" + reason + "): " + uri);
        } catch (OtpAuthParseException expected) {
        }
    }
}
