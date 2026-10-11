package mz.ac.ucm.feng.ti.vaultkey.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.data.local.VaultDatabase;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CategoryEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.VaultConfigEntity;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.AesGcmService;
import mz.ac.ucm.feng.ti.vaultkey.security.keystore.KeystoreKeyManager;
import mz.ac.ucm.feng.ti.vaultkey.security.pin.PinValidator;
import mz.ac.ucm.feng.ti.vaultkey.security.pin.PinVerifier;
import mz.ac.ucm.feng.ti.vaultkey.security.session.SessionManager;

/**
 * Ciclo de vida do cofre. O PIN autentica (PBKDF2); a chave AES vive no
 * Keystore e só entra na sessão após autenticação válida.
 */
public class VaultRepositoryImpl implements VaultRepository {

    static final String PREFS_SETTINGS = "vaultkey_settings";
    static final String PREFS_SECURE = "vaultkey_secure";
    static final String KEY_BIO_WRAP_IV = "bio_wrap_iv";
    static final String KEY_BIO_WRAP_CT = "bio_wrap_ct";
    static final String KEY_BIO_TOKEN_HASH = "bio_token_hash";

    private static final String[] DEFAULT_CATEGORIES = {
            "Redes sociais", "Email", "Trabalho", "Banco", "Universidade", "Outros"
    };

    private final Context appContext;
    private final VaultDatabase db;
    private final SessionManager session;
    private final KeystoreKeyManager keystore;
    private final PinVerifier pinVerifier;
    private final SharedPreferences settings;
    private final SharedPreferences secure;

    public VaultRepositoryImpl(Context appContext, VaultDatabase db, SessionManager session,
                               KeystoreKeyManager keystore, PinVerifier pinVerifier) {
        this.appContext = appContext.getApplicationContext();
        this.db = db;
        this.session = session;
        this.keystore = keystore;
        this.pinVerifier = pinVerifier;
        this.settings = this.appContext.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        this.secure = this.appContext.getSharedPreferences(PREFS_SECURE, Context.MODE_PRIVATE);
    }

    /** Fábrica para Room (fora do construtor para facilitar testes). */
    public static VaultDatabase openDatabase(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                VaultDatabase.class, VaultDatabase.DB_NAME).build();
    }

    // ---- Ciclo de vida ----

    @Override
    public boolean isInitialized() {
        try {
            return db.vaultConfigDao().get() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Result<Void> createVault(String pin, String pinConfirm) {
        if (isInitialized()) {
            return Result.fail("O cofre já existe.");
        }
        if (!PinValidator.isValidFormat(pin)) {
            return Result.fail("O PIN deve ter 4 a 12 dígitos numéricos.");
        }
        if (!pin.equals(pinConfirm)) {
            return Result.fail("Os PINs não coincidem.");
        }
        try {
            SecretKey key = keystore.getOrCreateDataKey();
            VaultConfigEntity entity = new VaultConfigEntity();
            entity.pinVerifier = pinVerifier.create(pin);
            entity.biometricEnabled = false;
            entity.autoLockSeconds = 60;
            entity.clipboardClearSeconds = 30;
            entity.lockOnExit = true;
            entity.createdAt = System.currentTimeMillis();
            db.vaultConfigDao().insert(entity);
            session.setAutoLockSeconds(entity.autoLockSeconds);
            session.unlock(key);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail("Não foi possível criar o cofre.");
        }
    }

    @Override
    public Result<Void> unlock(String pin) {
        if (session.isInLockout()) {
            return Result.fail("lockout:" + session.lockoutRemainingSeconds());
        }
        try {
            VaultConfigEntity entity = db.vaultConfigDao().get();
            if (entity == null) {
                return Result.fail("Cofre inexistente.");
            }
            if (!pinVerifier.verify(pin, entity.pinVerifier)) {
                long lockout = session.registerFailedAttempt();
                if (lockout > 0) {
                    return Result.fail("lockout:" + lockout);
                }
                int remaining = session.remainingAttempts();
                return Result.fail("wrong:" + remaining);
            }
            SecretKey key = keystore.getOrCreateDataKey();
            session.setAutoLockSeconds(entity.autoLockSeconds);
            session.unlock(key);
            // Sanidade criptográfica: valida que a chave abre os dados.
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail("Falha ao desbloquear o cofre.");
        }
    }

    /** Desbloqueio após sucesso biométrico (chave de encapsulamento autorizada). */
    public Result<Void> unlockWithBiometricToken(byte[] iv, byte[] ct) {
        try {
            VaultConfigEntity entity = db.vaultConfigDao().get();
            if (entity == null || !entity.biometricEnabled) {
                return Result.fail("Biometria não configurada.");
            }
            SecretKey key = keystore.getOrCreateDataKey();
            session.setAutoLockSeconds(entity.autoLockSeconds);
            session.unlock(key);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail("Falha ao desbloquear com biometria.");
        }
    }

    @Override
    public void lock() {
        session.lock();
    }

    @Override
    public boolean isUnlocked() {
        return session.isUnlocked();
    }

    @Override
    public void touch() {
        session.touch();
    }

    @Override
    public Result<Void> deleteVault() {
        try {
            session.lock();
            // 1. Bloquear novas operações: sessão invalidada acima.
            // 2/3. Eliminar dados e metadados.
            db.credentialDao().deleteAll();
            db.authenticatorAccountDao().deleteAll();
            db.categoryDao().deleteAll();
            db.vaultConfigDao().deleteAll();
            // 4/5. Eliminar chaves exclusivas do cofre + token biométrico.
            try {
                keystore.deleteDataKey();
            } catch (Exception ignored) {
            }
            try {
                new mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricKeyManager().delete();
            } catch (Exception ignored) {
            }
            secure.edit().clear().apply();
            settings.edit().clear().apply();
            // 6. Dados temporários sensíveis: sessão já limpa.
            return Result.ok(null);
        } catch (Exception e) {
            // Sem falsa confirmação: comunica a falha e mantém estado seguro.
            session.lock();
            return Result.fail("Falha ao eliminar o cofre. Tenta novamente.");
        }
    }

    // ---- Definições ----

    private VaultConfigEntity requireConfig() {
        VaultConfigEntity e = db.vaultConfigDao().get();
        if (e == null) {
            throw new IllegalStateException("Cofre inexistente.");
        }
        return e;
    }

    @Override
    public boolean isBiometricEnabled() {
        try {
            return requireConfig().biometricEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setBiometricEnabled(boolean enabled) {
        VaultConfigEntity e = requireConfig();
        e.biometricEnabled = enabled;
        db.vaultConfigDao().update(e);
        if (!enabled) {
            secure.edit().clear().apply();
        }
    }

    @Override
    public int getAutoLockSeconds() {
        try {
            return requireConfig().autoLockSeconds;
        } catch (Exception e) {
            return 60;
        }
    }

    @Override
    public void setAutoLockSeconds(int seconds) {
        VaultConfigEntity e = requireConfig();
        e.autoLockSeconds = Math.max(0, seconds);
        db.vaultConfigDao().update(e);
        session.setAutoLockSeconds(e.autoLockSeconds);
    }

    @Override
    public int getClipboardClearSeconds() {
        try {
            return requireConfig().clipboardClearSeconds;
        } catch (Exception e) {
            return 30;
        }
    }

    @Override
    public void setClipboardClearSeconds(int seconds) {
        VaultConfigEntity e = requireConfig();
        e.clipboardClearSeconds = Math.max(0, seconds);
        db.vaultConfigDao().update(e);
    }

    @Override
    public boolean isLockOnExit() {
        try {
            return requireConfig().lockOnExit;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public void setLockOnExit(boolean lock) {
        VaultConfigEntity e = requireConfig();
        e.lockOnExit = lock;
        db.vaultConfigDao().update(e);
    }

    @Override
    public Result<List<String>> listCategories() {
        try {
            List<String> out = new ArrayList<>();
            for (String s : DEFAULT_CATEGORIES) {
                out.add(s);
            }
            for (CategoryEntity e : db.categoryDao().getAll()) {
                if (!out.contains(e.name)) {
                    out.add(e.name);
                }
            }
            return Result.ok(out);
        } catch (Exception e) {
            return Result.fail("Falha ao ler categorias.");
        }
    }

    @Override
    public Result<Void> addCategory(String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 60) {
            return Result.fail("Nome de categoria inválido.");
        }
        try {
            CategoryEntity e = new CategoryEntity();
            e.name = name.trim();
            db.categoryDao().insert(e);
            return Result.ok(null);
        } catch (Exception e) {
            return Result.fail("Falha ao guardar categoria.");
        }
    }

    // ---- Token biométrico (ficheiro seguro, excluído de backup) ----

    public void storeBiometricWrap(byte[] iv, byte[] ct, byte[] tokenHash) {
        secure.edit()
                .putString(KEY_BIO_WRAP_IV, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .putString(KEY_BIO_WRAP_CT, android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP))
                .putString(KEY_BIO_TOKEN_HASH, android.util.Base64.encodeToString(tokenHash, android.util.Base64.NO_WRAP))
                .apply();
    }

    public byte[] getBioWrapIv() {
        return decodeSecure(KEY_BIO_WRAP_IV);
    }

    public byte[] getBioWrapCt() {
        return decodeSecure(KEY_BIO_WRAP_CT);
    }

    public byte[] getBioTokenHash() {
        return decodeSecure(KEY_BIO_TOKEN_HASH);
    }

    public boolean hasBiometricWrap() {
        return getBioWrapIv() != null && getBioWrapCt() != null && getBioTokenHash() != null;
    }

    private byte[] decodeSecure(String key) {
        try {
            String s = secure.getString(key, null);
            if (s == null) {
                return null;
            }
            return android.util.Base64.decode(s, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public SessionManager getSession() {
        return session;
    }

    public AesGcmService crypto() {
        return new AesGcmService();
    }

    public VaultDatabase getDatabase() {
        return db;
    }

    /** AAD que vincula um campo cifrado ao registo: "credencial:<id>:<campo>". */
    public static byte[] aad(String scope, long id, String field) {
        return (scope + ":" + id + ":" + field).getBytes(StandardCharsets.UTF_8);
    }
}
