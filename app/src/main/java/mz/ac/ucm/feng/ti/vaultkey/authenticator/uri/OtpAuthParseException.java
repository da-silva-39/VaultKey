package mz.ac.ucm.feng.ti.vaultkey.authenticator.uri;

/** Erro de interpretação de URI otpauth:// (mensagem segura, sem segredos). */
public class OtpAuthParseException extends Exception {
    public OtpAuthParseException(String message) {
        super(message);
    }
}
