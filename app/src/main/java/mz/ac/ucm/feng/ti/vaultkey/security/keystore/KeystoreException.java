package mz.ac.ucm.feng.ti.vaultkey.security.keystore;

/** Erro de gestão de chaves (mensagem genérica, sem material sensível). */
public class KeystoreException extends Exception {
    public KeystoreException(String message) {
        super(message);
    }

    public KeystoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
