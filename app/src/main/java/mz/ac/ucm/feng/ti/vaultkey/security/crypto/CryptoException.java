package mz.ac.ucm.feng.ti.vaultkey.security.crypto;

/** Erro criptográfico (mensagem genérica — nunca inclui segredos ou chaves). */
public class CryptoException extends Exception {
    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
