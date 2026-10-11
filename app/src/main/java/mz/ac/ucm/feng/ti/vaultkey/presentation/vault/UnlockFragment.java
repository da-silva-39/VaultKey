package mz.ac.ucm.feng.ti.vaultkey.presentation.vault;

import android.os.Bundle;
import android.hardware.biometrics.BiometricPrompt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.data.repository.VaultRepositoryImpl;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;
import mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricGate;
import mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricKeyManager;

/**
 * Desbloqueio do cofre: PIN com bloqueio progressivo + biometria
 * (autoriza o uso da chave de encapsulamento do Keystore).
 */
public class UnlockFragment extends Fragment {

    private VaultViewModel viewModel;
    private TextInputEditText etPin;
    private TextView tvError;
    private MaterialButton btnUnlock;
    private MaterialButton btnBiometric;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unlock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VaultViewModel.class);
        etPin = view.findViewById(R.id.et_pin);
        tvError = view.findViewById(R.id.tv_error);
        btnUnlock = view.findViewById(R.id.btn_unlock);
        btnBiometric = view.findViewById(R.id.btn_biometric);

        btnUnlock.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            btnUnlock.setEnabled(false);
            String pin = etPin.getText() == null ? "" : etPin.getText().toString();
            viewModel.unlock(pin);
        });

        viewModel.getUnlockResult().observe(getViewLifecycleOwner(), result -> {
            btnUnlock.setEnabled(true);
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                etPin.setText("");
                ((MainActivity) requireActivity()).showHome();
            } else {
                showUnlockError(result.getError());
            }
        });

        updateBiometricButton();
    }

    private void showUnlockError(String error) {
        if (error != null && error.startsWith("wrong:")) {
            int remaining = 0;
            try {
                remaining = Integer.parseInt(error.substring(6));
            } catch (NumberFormatException ignored) {
            }
            tvError.setText(getString(R.string.unlock_wrong,
                    ServiceLocator.get().session().getFailedAttempts(),
                    ServiceLocator.get().session().MAX_ATTEMPTS_BEFORE_LOCKOUT));
            tvError.setVisibility(View.VISIBLE);
        } else if (error != null && error.startsWith("lockout:")) {
            long secs = 0;
            try {
                secs = Long.parseLong(error.substring(8));
            } catch (NumberFormatException ignored) {
            }
            tvError.setText(getString(R.string.unlock_locked_out, secs));
            tvError.setVisibility(View.VISIBLE);
        } else {
            tvError.setText(error == null ? getString(R.string.error_generic) : error);
            tvError.setVisibility(View.VISIBLE);
        }
        updateBiometricButton();
    }

    private void updateBiometricButton() {
        AppExecutors.diskIo().execute(() -> {
            boolean show = false;
            try {
                VaultRepositoryImpl repo = ServiceLocator.get().vaultRepositoryImpl();
                show = repo.isBiometricEnabled()
                        && repo.hasBiometricWrap()
                        && BiometricGate.canAuthenticate(requireActivity());
            } catch (Exception ignored) {
            }
            final boolean visible = show;
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        btnBiometric.setVisibility(visible ? View.VISIBLE : View.GONE));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        btnBiometric.setOnClickListener(v -> startBiometricUnlock());
    }

    private void startBiometricUnlock() {
        MainActivity activity = (MainActivity) requireActivity();
        if (!BiometricGate.canAuthenticate(activity)) {
            Toast.makeText(activity, R.string.unlock_no_biometric, Toast.LENGTH_SHORT).show();
            return;
        }
        AppExecutors.diskIo().execute(() -> {
            try {
                VaultRepositoryImpl repo = ServiceLocator.get().vaultRepositoryImpl();
                BiometricKeyManager keys = ServiceLocator.get().biometricKeys();
                byte[] iv = repo.getBioWrapIv();
                if (iv == null) {
                    postError(getString(R.string.error_generic));
                    return;
                }
                Cipher cipher;
                try {
                    cipher = keys.initDecryptCipher(iv);
                } catch (BiometricKeyManager.BiometricKeyInvalidatedException e) {
                    repo.setBiometricEnabled(false);
                    postError(getString(R.string.unlock_biometric_failed));
                    return;
                }
                BiometricPrompt.CryptoObject crypto = new BiometricPrompt.CryptoObject(cipher);
                activity.runOnUiThread(() -> BiometricGate.authenticate(activity,
                        getString(R.string.unlock_title),
                        getString(R.string.unlock_biometric),
                        crypto,
                        new BiometricGate.Callback() {
                            @Override
                            public void onSucceeded(BiometricPrompt.AuthenticationResult result) {
                                AppExecutors.diskIo().execute(() -> {
                                    try {
                                        Cipher c = result.getCryptoObject().getCipher();
                                        byte[] ct = repo.getBioWrapCt();
                                        byte[] token = c.doFinal(ct);
                                        byte[] hash = VaultRepositoryImpl.sha256(token);
                                        Arrays.fill(token, (byte) 0);
                                        byte[] expected = repo.getBioTokenHash();
                                        if (expected == null
                                                || !MessageDigest.isEqual(hash, expected)) {
                                            postError(getString(R.string.unlock_biometric_failed));
                                            return;
                                        }
                                        Result<Void> r = repo.unlockWithBiometricToken(
                                                repo.getBioWrapIv(), ct);
                                        activity.runOnUiThread(() -> {
                                            if (!isAdded()) {
                                                return;
                                            }
                                            if (r.isSuccess()) {
                                                activity.showHome();
                                            } else {
                                                postError(r.getError());
                                            }
                                        });
                                    } catch (Exception e) {
                                        postError(getString(R.string.unlock_biometric_failed));
                                    }
                                });
                            }

                            @Override
                            public void onFailed(String message) {
                                postError(message);
                            }
                        }));
            } catch (Exception e) {
                postError(getString(R.string.error_generic));
            }
        });
    }

    private void postError(String message) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        });
    }
}
