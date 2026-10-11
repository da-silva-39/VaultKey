package mz.ac.ucm.feng.ti.vaultkey.domain.repository;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;

/** Contrato do ciclo de vida do cofre: criação, desbloqueio, bloqueio, eliminação. */
public interface VaultRepository {

    boolean isInitialized();

    Result<Void> createVault(String pin, String pinConfirm);

    /** Devolve null em sucesso ou mensagem de erro segura. */
    Result<Void> unlock(String pin);

    void lock();

    boolean isUnlocked();

    void touch();

    Result<Void> deleteVault();

    // ---- Definições (não sensíveis) ----

    boolean isBiometricEnabled();

    void setBiometricEnabled(boolean enabled);

    int getAutoLockSeconds();

    void setAutoLockSeconds(int seconds);

    int getClipboardClearSeconds();

    void setClipboardClearSeconds(int seconds);

    boolean isLockOnExit();

    void setLockOnExit(boolean lock);

    Result<List<String>> listCategories();

    Result<Void> addCategory(String name);
}
