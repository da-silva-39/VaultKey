package mz.ac.ucm.feng.ti.vaultkey.di;

import android.content.Context;

import mz.ac.ucm.feng.ti.vaultkey.data.local.VaultDatabase;
import mz.ac.ucm.feng.ti.vaultkey.data.repository.AuthenticatorRepositoryImpl;
import mz.ac.ucm.feng.ti.vaultkey.data.repository.CredentialRepositoryImpl;
import mz.ac.ucm.feng.ti.vaultkey.data.repository.VaultRepositoryImpl;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.AuthenticatorRepository;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.CredentialRepository;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;
import mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricKeyManager;
import mz.ac.ucm.feng.ti.vaultkey.security.clipboard.ClipboardHelper;
import mz.ac.ucm.feng.ti.vaultkey.security.keystore.KeystoreKeyManager;
import mz.ac.ucm.feng.ti.vaultkey.security.pin.PinVerifier;
import mz.ac.ucm.feng.ti.vaultkey.security.session.SessionManager;

/**
 * Composição manual de dependências (sem Hilt: evita complexidade e mantém
 * o ciclo de vida explícito). Âmbito de aplicação; sem referências a
 * Activities/Fragments.
 */
public final class ServiceLocator {

    private static ServiceLocator instance;

    private final VaultRepositoryImpl vaultRepository;
    private final CredentialRepository credentialRepository;
    private final AuthenticatorRepository authenticatorRepository;
    private final SessionManager sessionManager;
    private final KeystoreKeyManager keystoreKeyManager;
    private final BiometricKeyManager biometricKeyManager;
    private final ClipboardHelper clipboardHelper;

    private ServiceLocator(Context appContext) {
        VaultDatabase db = VaultRepositoryImpl.openDatabase(appContext);
        this.sessionManager = new SessionManager();
        this.keystoreKeyManager = new KeystoreKeyManager();
        this.biometricKeyManager = new BiometricKeyManager();
        this.clipboardHelper = new ClipboardHelper();
        PinVerifier pinVerifier = new PinVerifier();
        this.vaultRepository = new VaultRepositoryImpl(
                appContext, db, sessionManager, keystoreKeyManager, pinVerifier);
        this.credentialRepository = new CredentialRepositoryImpl(vaultRepository);
        this.authenticatorRepository = new AuthenticatorRepositoryImpl(vaultRepository);
    }

    public static synchronized void init(Context appContext) {
        if (instance == null) {
            instance = new ServiceLocator(appContext.getApplicationContext());
        }
    }

    public static ServiceLocator get() {
        if (instance == null) {
            throw new IllegalStateException("ServiceLocator não inicializado.");
        }
        return instance;
    }

    public VaultRepository vaultRepository() {
        return vaultRepository;
    }

    public VaultRepositoryImpl vaultRepositoryImpl() {
        return vaultRepository;
    }

    public CredentialRepository credentials() {
        return credentialRepository;
    }

    public AuthenticatorRepository authenticator() {
        return authenticatorRepository;
    }

    public SessionManager session() {
        return sessionManager;
    }

    public KeystoreKeyManager keystore() {
        return keystoreKeyManager;
    }

    public BiometricKeyManager biometricKeys() {
        return biometricKeyManager;
    }

    public ClipboardHelper clipboard() {
        return clipboardHelper;
    }
}
