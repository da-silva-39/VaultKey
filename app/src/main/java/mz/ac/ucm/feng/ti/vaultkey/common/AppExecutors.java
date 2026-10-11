package mz.ac.ucm.feng.ti.vaultkey.common;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executores partilhados: I/O em thread de fundo, UI via Handler gerido
 * pela camada de apresentação.
 */
public final class AppExecutors {

    private static final Executor DISK_IO = Executors.newSingleThreadExecutor();
    private static final Executor BACKGROUND = Executors.newFixedThreadPool(2);

    private AppExecutors() {
    }

    public static Executor diskIo() {
        return DISK_IO;
    }

    public static Executor background() {
        return BACKGROUND;
    }
}
