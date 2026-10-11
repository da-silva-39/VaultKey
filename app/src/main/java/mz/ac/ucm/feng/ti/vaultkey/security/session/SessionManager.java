package mz.ac.ucm.feng.ti.vaultkey.security.session;

import javax.crypto.SecretKey;

/**
 * Gestão central da sessão do cofre (bloqueado/desbloqueado).
 *
 * <p>Regras: a sessão expira por inactividade (tempo configurado); o estado é
 * verificado antes de cada operação sensível (check-on-access, sem depender
 * de temporizadores visuais); ao bloquear, a referência lógica à chave é
 * invalidada e os dados sensíveis apresentados devem ser ocultados pela UI.</p>
 */
public final class SessionManager {

    public static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 5;
    public static final long BASE_LOCKOUT_SECONDS = 30;
    public static final long MAX_LOCKOUT_SECONDS = 15 * 60;

    private SecretKey dataKey;
    private boolean unlocked;
    private long lastActivityMillis;
    private int autoLockSeconds = 60;

    private int failedAttempts;
    private long lockoutUntilMillis;

    public synchronized void unlock(SecretKey key) {
        this.dataKey = key;
        this.unlocked = true;
        this.failedAttempts = 0;
        this.lockoutUntilMillis = 0;
        touch();
    }

    public synchronized void lock() {
        this.dataKey = null;
        this.unlocked = false;
    }

    public synchronized boolean isUnlocked() {
        if (!unlocked || dataKey == null) {
            return false;
        }
        if (autoLockSeconds > 0
                && System.currentTimeMillis() - lastActivityMillis > autoLockSeconds * 1000L) {
            lock();
            return false;
        }
        return true;
    }

    /** Chave apenas se a sessão estiver válida; caso contrário, null. */
    public synchronized SecretKey requireKey() {
        return isUnlocked() ? dataKey : null;
    }

    public synchronized void touch() {
        lastActivityMillis = System.currentTimeMillis();
    }

    public synchronized void setAutoLockSeconds(int seconds) {
        this.autoLockSeconds = Math.max(0, seconds);
        touch();
    }

    public synchronized int getAutoLockSeconds() {
        return autoLockSeconds;
    }

    /** Regista uma tentativa falhada e aplica bloqueio progressivo. */
    public synchronized long registerFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            long shift = Math.min(failedAttempts - MAX_ATTEMPTS_BEFORE_LOCKOUT, 6);
            long lockout = Math.min(BASE_LOCKOUT_SECONDS * (1L << shift), MAX_LOCKOUT_SECONDS);
            lockoutUntilMillis = System.currentTimeMillis() + lockout * 1000L;
            return lockout;
        }
        return 0;
    }

    public synchronized long lockoutRemainingSeconds() {
        long remaining = lockoutUntilMillis - System.currentTimeMillis();
        return remaining > 0 ? (remaining + 999) / 1000 : 0;
    }

    public synchronized boolean isInLockout() {
        return lockoutRemainingSeconds() > 0;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    public synchronized int remainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS_BEFORE_LOCKOUT - failedAttempts);
    }
}
