package mz.ac.ucm.feng.ti.vaultkey.domain.validation;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.base32.Base32;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;

/** Regras de negócio para contas 2FA (puras, testáveis). */
public final class OtpParamsValidator {

    private OtpParamsValidator() {
    }

    /** Devolve null se válido, ou mensagem de erro em português. */
    public static String validate(AuthenticatorAccount a) {
        if (a == null) {
            return "Conta inválida.";
        }
        if (!Base32.isValidSecret(a.getSecretBase32())) {
            return "Segredo Base32 inválido (mín. 16 caracteres A–Z2–7).";
        }
        String algo = a.getHmacAlgorithm();
        if (!"HmacSHA1".equals(algo) && !"HmacSHA256".equals(algo) && !"HmacSHA512".equals(algo)) {
            return "Algoritmo não suportado.";
        }
        if (a.getDigits() < 6 || a.getDigits() > 10) {
            return "Dígitos fora do intervalo 6–10.";
        }
        if (a.getType() == AuthenticatorAccount.OtpType.TOTP) {
            if (a.getPeriod() < 10 || a.getPeriod() > 120) {
                return "Período fora do intervalo 10–120 s.";
            }
        } else {
            if (a.getCounter() < 0) {
                return "Contador HOTP inválido.";
            }
        }
        return null;
    }
}
