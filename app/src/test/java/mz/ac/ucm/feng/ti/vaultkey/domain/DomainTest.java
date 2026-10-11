package mz.ac.ucm.feng.ti.vaultkey.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.PasswordStrength;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordGenerator;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordOptions;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordStrengthEvaluator;
import mz.ac.ucm.feng.ti.vaultkey.domain.validation.CredentialValidator;
import mz.ac.ucm.feng.ti.vaultkey.domain.validation.OtpParamsValidator;

public class DomainTest {

    private static Credential baseCredential() {
        Credential c = new Credential();
        c.setServiceName("Banco X");
        c.setUsername("alice");
        c.setPassword("s3nh4-f0rt3!");
        c.setUrl("https://banco.example.com");
        c.setCategory("Banco");
        return c;
    }

    @Test
    public void credentialValidation() {
        assertNull(CredentialValidator.validate(baseCredential()));

        Credential noService = baseCredential();
        noService.setServiceName("   ");
        assertNotNull(CredentialValidator.validate(noService));

        Credential noPass = baseCredential();
        noPass.setPassword("");
        assertNotNull(CredentialValidator.validate(noPass));

        Credential badUrl = baseCredential();
        badUrl.setUrl("ht tp://in válido");
        assertNotNull(CredentialValidator.validate(badUrl));

        Credential plainDomain = baseCredential();
        plainDomain.setUrl("banco.example.com");
        assertNull(CredentialValidator.validate(plainDomain));

        assertNotNull(CredentialValidator.validate(null));
    }

    @Test
    public void otpParamsValidation() {
        AuthenticatorAccount a = new AuthenticatorAccount();
        a.setSecretBase32("JBSWY3DPEHPK3PXP");
        a.setHmacAlgorithm("HmacSHA256");
        a.setDigits(8);
        a.setType(AuthenticatorAccount.OtpType.TOTP);
        a.setPeriod(30);
        assertNull(OtpParamsValidator.validate(a));

        AuthenticatorAccount badSecret = new AuthenticatorAccount();
        badSecret.setSecretBase32("curto");
        assertNotNull(OtpParamsValidator.validate(badSecret));

        AuthenticatorAccount badDigits = new AuthenticatorAccount();
        badDigits.setSecretBase32("JBSWY3DPEHPK3PXP");
        badDigits.setDigits(4);
        assertNotNull(OtpParamsValidator.validate(badDigits));

        AuthenticatorAccount badPeriod = new AuthenticatorAccount();
        badPeriod.setSecretBase32("JBSWY3DPEHPK3PXP");
        badPeriod.setPeriod(5);
        assertNotNull(OtpParamsValidator.validate(badPeriod));
    }

    @Test
    public void passwordGenerator() {
        PasswordOptions options = new PasswordOptions();
        options.setLength(20);
        String pass = new PasswordGenerator().generate(options);
        assertEquals(20, pass.length());
        assertTrue(pass.matches(".*[A-Z].*"));
        assertTrue(pass.matches(".*[a-z].*"));
        assertTrue(pass.matches(".*[0-9].*"));

        PasswordOptions noAmbiguous = new PasswordOptions();
        noAmbiguous.setLength(64);
        noAmbiguous.setExcludeAmbiguous(true);
        String clean = new PasswordGenerator().generate(noAmbiguous);
        for (char c : PasswordGenerator.AMBIGUOUS.toCharArray()) {
            assertFalse(clean.indexOf(c) >= 0, "Contém ambíguo: " + c);
        }

        PasswordOptions none = new PasswordOptions();
        none.setUpper(false);
        none.setLower(false);
        none.setDigits(false);
        none.setSymbols(false);
        try {
            new PasswordGenerator().generate(none);
            fail("Devia exigir uma categoria");
        } catch (IllegalArgumentException expected) {
        }

        // Unicidade básica entre gerações.
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            seen.add(new PasswordGenerator().generate(options));
        }
        assertTrue(seen.size() > 1);
    }

    private static void assertFalse(boolean condition, String message) {
        org.junit.Assert.assertFalse(message, condition);
    }

    @Test
    public void passwordStrength() {
        PasswordStrength weak = PasswordStrengthEvaluator.evaluate("123456");
        assertEquals(PasswordStrength.Level.WEAK, weak.getLevel());

        PasswordStrength strong = PasswordStrengthEvaluator.evaluate("Tr7$kQ9!mZx2@vBn4#Qw");
        assertTrue(strong.getLevel() == PasswordStrength.Level.STRONG
                || strong.getLevel() == PasswordStrength.Level.VERY_STRONG);

        PasswordStrength empty = PasswordStrengthEvaluator.evaluate("");
        assertEquals(PasswordStrength.Level.WEAK, empty.getLevel());
    }
}
