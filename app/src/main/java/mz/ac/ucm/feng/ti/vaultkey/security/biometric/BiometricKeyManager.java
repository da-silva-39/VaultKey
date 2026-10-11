package mz.ac.ucm.feng.ti.vaultkey.security.biometric;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import mz.ac.ucm.feng.ti.vaultkey.security.keystore.KeystoreException;

/**
 * Chave de encapsulamento biométrico: uma chave AES no Keystore com
 * {@code setUserAuthenticationRequired(true)}, cujo uso só é autorizado
 * após autenticação biométrica (via CryptoObject do BiometricPrompt).
 *
 * <p>Na activação, um token aleatório é cifrado com esta chave; no desbloqueio
 * biométrico, o token é decifrado e o seu hash comparado. Se o Keystore
 * invalidar a chave (ex.: nova biometria registada), é lançada
 * {@link BiometricKeyInvalidatedException} e a app deve exigir o PIN.</p>
 */
public final class BiometricKeyManager {

    public static final String BIO_KEY_ALIAS = "vaultkey_biometric_wrap";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_BITS = 128;

    public void ensureKey() throws KeystoreException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(BIO_KEY_ALIAS)) {
                return;
            }
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    BIO_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                        30, KeyProperties.AUTH_BIOMETRIC_STRONG);
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(30);
            }
            kg.init(builder.build());
            kg.generateKey();
        } catch (Exception e) {
            throw new KeystoreException("Chave biométrica indisponível.", e);
        }
    }

    public boolean exists() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            return ks.containsAlias(BIO_KEY_ALIAS);
        } catch (Exception e) {
            return false;
        }
    }

    public void delete() throws KeystoreException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(BIO_KEY_ALIAS)) {
                ks.deleteEntry(BIO_KEY_ALIAS);
            }
        } catch (Exception e) {
            throw new KeystoreException("Falha ao eliminar a chave biométrica.", e);
        }
    }

    private SecretKey load() throws KeystoreException, BiometricKeyInvalidatedException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(BIO_KEY_ALIAS, null);
            if (!(entry instanceof KeyStore.SecretKeyEntry)) {
                throw new KeystoreException("Chave biométrica ausente.");
            }
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        } catch (KeystoreException e) {
            throw e;
        } catch (Exception e) {
            if (isInvalidated(e)) {
                throw new BiometricKeyInvalidatedException();
            }
            throw new KeystoreException("Chave biométrica ilegível.", e);
        }
    }

    /** Cipher para ENCRYPT_MODE a usar no CryptoObject do prompt de activação. */
    public Cipher initEncryptCipher() throws KeystoreException, BiometricKeyInvalidatedException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, load());
            return cipher;
        } catch (KeystoreException | BiometricKeyInvalidatedException e) {
            throw e;
        } catch (Exception e) {
            if (isInvalidated(e)) {
                throw new BiometricKeyInvalidatedException();
            }
            throw new KeystoreException("Cipher biométrico indisponível.", e);
        }
    }

    /** Cipher para DECRYPT_MODE (com o IV guardado) a usar no prompt de desbloqueio. */
    public Cipher initDecryptCipher(byte[] iv) throws KeystoreException, BiometricKeyInvalidatedException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, load(), new GCMParameterSpec(TAG_BITS, iv));
            return cipher;
        } catch (KeystoreException | BiometricKeyInvalidatedException e) {
            throw e;
        } catch (Exception e) {
            if (isInvalidated(e)) {
                throw new BiometricKeyInvalidatedException();
            }
            throw new KeystoreException("Cipher biométrico indisponível.", e);
        }
    }

    private static boolean isInvalidated(Throwable t) {
        while (t != null) {
            String name = t.getClass().getSimpleName();
            if (name.contains("KeyPermanentlyInvalidatedException")
                    || name.contains("UserNotAuthenticatedException")
                    || name.contains("KeyInvalidatedException")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** Excepção específica: a chave foi invalidada pelo sistema. */
    public static final class BiometricKeyInvalidatedException extends Exception {
        public BiometricKeyInvalidatedException() {
            super("Chave biométrica invalidada pelo sistema.");
        }
    }
}
