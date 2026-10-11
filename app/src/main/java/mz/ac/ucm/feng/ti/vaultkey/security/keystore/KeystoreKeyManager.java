package mz.ac.ucm.feng.ti.vaultkey.security.keystore;

import android.content.pm.PackageManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Gestão da chave de dados no Android Keystore (nunca sai do hardware/TEE
 * quando suportado, nunca é guardada no código, recursos ou base de dados).
 *
 * <p>Modelo: uma chave AES-256-GCM {@code vaultkey_data_key} protegida pelo
 * Keystore. O PIN apenas autentica o utilizador (verificador PBKDF2); a chave
 * é libertada para a sessão após autenticação bem-sucedida. A biometria
 * autoriza o uso de uma chave de encapsulamento separada
 * (ver BiometricKeyManager).</p>
 */
public final class KeystoreKeyManager {

    public static final String DATA_KEY_ALIAS = "vaultkey_data_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    /** Obtém a chave de dados, criando-a se necessário. */
    public SecretKey getOrCreateDataKey() throws KeystoreException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(DATA_KEY_ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    DATA_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            kg.init(spec);
            return kg.generateKey();
        } catch (Exception e) {
            throw new KeystoreException("Chave de dados indisponível.", e);
        }
    }

    public boolean dataKeyExists() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            return ks.containsAlias(DATA_KEY_ALIAS);
        } catch (Exception e) {
            return false;
        }
    }

    /** Elimina permanentemente a chave (parte da eliminação do cofre). */
    public void deleteDataKey() throws KeystoreException {
        deleteAlias(DATA_KEY_ALIAS);
    }

    public void deleteAlias(String alias) throws KeystoreException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias);
            }
        } catch (Exception e) {
            throw new KeystoreException("Falha ao eliminar a chave.", e);
        }
    }

    /** Indica se o dispositivo suporta Keystore com o nível exigido. */
    public static boolean isSupported(PackageManager pm) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}
