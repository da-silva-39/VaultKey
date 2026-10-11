package mz.ac.ucm.feng.ti.vaultkey.common;

/**
 * Contentor simples de resultado para operações que podem falhar,
 * evitando excepções verificadas na fronteira apresentação/domínio.
 *
 * @param <T> tipo do valor em caso de sucesso
 */
public final class Result<T> {

    private final T value;
    private final String error;
    private final boolean success;

    private Result(T value, String error, boolean success) {
        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, null, true);
    }

    public static <T> Result<T> fail(String errorMessage) {
        return new Result<>(null, errorMessage, false);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    /** Mensagem de erro segura (nunca contém segredos). */
    public String getError() {
        return error;
    }
}
