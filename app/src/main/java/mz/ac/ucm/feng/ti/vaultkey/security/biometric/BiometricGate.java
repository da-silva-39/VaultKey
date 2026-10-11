package mz.ac.ucm.feng.ti.vaultkey.security.biometric;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;

import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

import mz.ac.ucm.feng.ti.vaultkey.R;

/**
 * BiometricPrompt do framework (android.hardware.biometrics, API 28+).
 * Optou-se pela API nativa para manter a app 100% offline sem dependências
 * novas; em API &lt; 28 a biometria fica indisponível (fallback para PIN).
 */
public final class BiometricGate {

    public interface Callback {
        void onSucceeded(BiometricPrompt.AuthenticationResult result);
        void onFailed(String message);
    }

    private BiometricGate() {
    }

    /** Forte (classe 3) disponível e com biometria registada? */
    public static boolean canAuthenticate(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BiometricManager manager = context.getSystemService(BiometricManager.class);
            return manager != null
                    && manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        }
        // API 28: sem BiometricManager.canAuthenticate; verifica hardware.
        PackageManager pm = context.getPackageManager();
        return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }

    public static void authenticate(FragmentActivity activity, String title, String subtitle,
                                    BiometricPrompt.CryptoObject cryptoObject, Callback callback) {
        Executor executor = activity.getMainExecutor();
        CancellationSignal cancellation = new CancellationSignal();
        BiometricPrompt.AuthenticationCallback authCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        callback.onSucceeded(result);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        callback.onFailed(activity.getString(R.string.unlock_biometric_failed));
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        callback.onFailed(errString == null ? "" : errString.toString());
                    }
                };
        BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(activity.getString(R.string.action_cancel),
                        executor, (dialog, which) ->
                                callback.onFailed(activity.getString(R.string.action_cancel)))
                .build();
        if (cryptoObject != null) {
            prompt.authenticate(cryptoObject, cancellation, executor, authCallback);
        } else {
            prompt.authenticate(cancellation, executor, authCallback);
        }
    }
}
