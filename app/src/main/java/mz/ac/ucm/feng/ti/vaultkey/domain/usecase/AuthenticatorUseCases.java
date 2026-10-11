package mz.ac.ucm.feng.ti.vaultkey.domain.usecase;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.base32.Base32;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.hotp.HotpGenerator;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.totp.TotpGenerator;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.uri.OtpAuthUri;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.PasswordStrength;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordGenerator;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordOptions;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordStrengthEvaluator;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.AuthenticatorRepository;
import mz.ac.ucm.feng.ti.vaultkey.domain.validation.OtpParamsValidator;

/** Casos de uso do Authenticator e do gerador de senhas. */
public final class AuthenticatorUseCases {

    private AuthenticatorUseCases() {
    }

    public static final class CreateAuthenticatorAccountUseCase {
        private final AuthenticatorRepository repo;

        public CreateAuthenticatorAccountUseCase(AuthenticatorRepository repo) {
            this.repo = repo;
        }

        public Result<Long> execute(AuthenticatorAccount account) {
            String error = OtpParamsValidator.validate(account);
            if (error != null) {
                return Result.fail(error);
            }
            return repo.save(account);
        }

        /** Cria uma conta a partir de uma URI otpauth:// já interpretada. */
        public Result<Long> execute(OtpAuthUri parsed) {
            if (parsed == null) {
                return Result.fail("Conta inválida.");
            }
            AuthenticatorAccount a = new AuthenticatorAccount();
            a.setIssuer(parsed.getIssuer());
            a.setAccountName(parsed.getAccount());
            a.setSecretBase32(parsed.getSecret());
            a.setType(parsed.getType() == OtpAuthUri.Type.TOTP
                    ? AuthenticatorAccount.OtpType.TOTP
                    : AuthenticatorAccount.OtpType.HOTP);
            a.setHmacAlgorithm(parsed.getAlgorithm());
            a.setDigits(parsed.getDigits());
            a.setPeriod(parsed.getPeriod());
            a.setCounter(parsed.getCounter());
            return execute(a);
        }
    }

    public static final class GenerateTotpUseCase {
        public String execute(byte[] secret, long timeMillis, int period,
                             int digits, String algorithm) {
            byte[] key = secret;
            try {
                // Aceita também o segredo em Base32.
                if (key == null) {
                    throw new IllegalArgumentException("Segredo inválido.");
                }
                return TotpGenerator.generate(key, timeMillis, period, digits, algorithm);
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }

        public String executeBase32(String secretBase32, long timeMillis, int period,
                                    int digits, String algorithm) {
            return TotpGenerator.generate(Base32.decode(secretBase32),
                    timeMillis, period, digits, algorithm);
        }
    }

    public static final class GenerateHotpUseCase {
        public String execute(byte[] secret, long counter, int digits, String algorithm) {
            return HotpGenerator.generate(secret, counter, digits, algorithm);
        }

        public String executeBase32(String secretBase32, long counter, int digits,
                                    String algorithm) {
            return HotpGenerator.generate(Base32.decode(secretBase32),
                    counter, digits, algorithm);
        }
    }

    public static final class GeneratePasswordUseCase {
        private final PasswordGenerator generator = new PasswordGenerator();

        public String execute(PasswordOptions options) {
            return generator.generate(options);
        }
    }

    public static final class EvaluatePasswordStrengthUseCase {
        public PasswordStrength execute(String password) {
            return PasswordStrengthEvaluator.evaluate(password);
        }
    }
}
