package mz.ac.ucm.feng.ti.vaultkey.presentation.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.hardware.biometrics.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.data.repository.VaultRepositoryImpl;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.VaultUseCases;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;
import mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricGate;
import mz.ac.ucm.feng.ti.vaultkey.security.biometric.BiometricKeyManager;

/**
 * Configurações: biometria, bloqueio automático, clipboard, bloqueio
 * imediato e eliminação permanente do cofre. Tudo persiste após reiniciar.
 */
public class SettingsFragment extends Fragment {

    private static final int[] AUTOLOCK_VALUES = {15, 30, 60, 120, 300};
    private static final int[] CLIPBOARD_VALUES = {0, 15, 30, 60, 120};

    private SwitchMaterial swBiometric;
    private SwitchMaterial swLockOnExit;
    private Spinner spAutolock;
    private Spinner spClipboard;
    private boolean userEditingBiometric;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swBiometric = view.findViewById(R.id.sw_biometric);
        swLockOnExit = view.findViewById(R.id.sw_lock_on_exit);
        spAutolock = view.findViewById(R.id.sp_autolock);
        spClipboard = view.findViewById(R.id.sp_clipboard);

        String[] autolockLabels = {
                getString(R.string.time_15s), getString(R.string.time_30s),
                getString(R.string.time_1m), getString(R.string.time_2m),
                getString(R.string.time_5m)};
        String[] clipboardLabels = {
                getString(R.string.time_never), getString(R.string.time_15s),
                getString(R.string.time_30s), getString(R.string.time_1m),
                getString(R.string.time_2m)};
        spAutolock.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, autolockLabels));
        spClipboard.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, clipboardLabels));

        loadCurrent(view);

        swBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!userEditingBiometric) {
                return;
            }
            if (isChecked) {
                enrollBiometric();
            } else {
                AppExecutors.diskIo().execute(() -> {
                    try {
                        VaultRepositoryImpl repo =
                                ServiceLocator.get().vaultRepositoryImpl();
                        repo.setBiometricEnabled(false);
                        ServiceLocator.get().biometricKeys().delete();
                    } catch (Exception ignored) {
                    }
                });
            }
        });

        swLockOnExit.setOnCheckedChangeListener((buttonView, isChecked) ->
                AppExecutors.diskIo().execute(() -> {
                    try {
                        ServiceLocator.get().vaultRepository().setLockOnExit(isChecked);
                    } catch (Exception ignored) {
                    }
                }));

        spAutolock.setOnItemSelectedListener(simpleSelectedListener(position ->
                AppExecutors.diskIo().execute(() -> {
                    try {
                        ServiceLocator.get().vaultRepository()
                                .setAutoLockSeconds(AUTOLOCK_VALUES[position]);
                    } catch (Exception ignored) {
                    }
                })));
        spClipboard.setOnItemSelectedListener(simpleSelectedListener(position ->
                AppExecutors.diskIo().execute(() -> {
                    try {
                        ServiceLocator.get().vaultRepository()
                                .setClipboardClearSeconds(CLIPBOARD_VALUES[position]);
                    } catch (Exception ignored) {
                    }
                })));

        view.findViewById(R.id.btn_lock_now).setOnClickListener(v -> {
            new VaultUseCases.LockVaultUseCase(
                    ServiceLocator.get().vaultRepository()).execute();
            ((MainActivity) requireActivity()).showUnlock();
        });

        view.findViewById(R.id.btn_delete_vault).setOnClickListener(v -> confirmDelete());
    }

    private void loadCurrent(View view) {
        AppExecutors.diskIo().execute(() -> {
            boolean bio = false;
            boolean lockOnExit = true;
            int auto = 60;
            int clip = 30;
            try {
                bio = ServiceLocator.get().vaultRepository().isBiometricEnabled();
                lockOnExit = ServiceLocator.get().vaultRepository().isLockOnExit();
                auto = ServiceLocator.get().vaultRepository().getAutoLockSeconds();
                clip = ServiceLocator.get().vaultRepository().getClipboardClearSeconds();
            } catch (Exception ignored) {
            }
            final boolean fBio = bio;
            final boolean fLock = lockOnExit;
            final int fAuto = indexOf(AUTOLOCK_VALUES, auto);
            final int fClip = indexOf(CLIPBOARD_VALUES, clip);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                userEditingBiometric = false;
                swBiometric.setChecked(fBio);
                swLockOnExit.setChecked(fLock);
                spAutolock.setSelection(Math.max(0, fAuto));
                spClipboard.setSelection(Math.max(0, fClip));
                userEditingBiometric = true;
                if (!BiometricGate.canAuthenticate(requireActivity())) {
                    swBiometric.setEnabled(false);
                }
            });
        });
    }

    private static int indexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private AdapterView.OnItemSelectedListener simpleSelectedListener(
            final java.util.function.Consumer<Integer> onSelect) {
        return new AdapterView.OnItemSelectedListener() {
            private boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (first) {
                    first = false;
                    return;
                }
                onSelect.accept(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    /** Activação biométrica: o prompt autoriza a chave de encapsulamento. */
    private void enrollBiometric() {
        MainActivity activity = (MainActivity) requireActivity();
        if (!BiometricGate.canAuthenticate(activity)) {
            Toast.makeText(activity, R.string.unlock_no_biometric, Toast.LENGTH_SHORT).show();
            swBiometric.setChecked(false);
            return;
        }
        AppExecutors.diskIo().execute(() -> {
            try {
                BiometricKeyManager keys = ServiceLocator.get().biometricKeys();
                keys.ensureKey();
                Cipher cipher = keys.initEncryptCipher();
                BiometricPrompt.CryptoObject crypto = new BiometricPrompt.CryptoObject(cipher);
                activity.runOnUiThread(() -> BiometricGate.authenticate(activity,
                        getString(R.string.set_biometric),
                        getString(R.string.set_biometric_summary),
                        crypto,
                        new BiometricGate.Callback() {
                            @Override
                            public void onSucceeded(BiometricPrompt.AuthenticationResult result) {
                                AppExecutors.diskIo().execute(() -> {
                                    try {
                                        Cipher c = result.getCryptoObject().getCipher();
                                        byte[] token = new byte[32];
                                        new SecureRandom().nextBytes(token);
                                        byte[] ct = c.doFinal(token);
                                        byte[] iv = c.getIV();
                                        byte[] hash = VaultRepositoryImpl.sha256(token);
                                        Arrays.fill(token, (byte) 0);
                                        VaultRepositoryImpl repo = ServiceLocator.get()
                                                .vaultRepositoryImpl();
                                        repo.storeBiometricWrap(iv, ct, hash);
                                        repo.setBiometricEnabled(true);
                                        toast(R.string.set_saved);
                                    } catch (Exception e) {
                                        failEnroll();
                                    }
                                });
                            }

                            @Override
                            public void onFailed(String message) {
                                failEnroll();
                            }
                        }));
            } catch (Exception e) {
                failEnroll();
            }
        });
    }

    private void failEnroll() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            userEditingBiometric = false;
            swBiometric.setChecked(false);
            userEditingBiometric = true;
            Toast.makeText(requireContext(), R.string.unlock_biometric_failed,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void toast(int resId) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show());
    }

    /** Eliminação permanente com confirmação explícita por escrita. */
    private void confirmDelete() {
        final EditText input = new EditText(requireContext());
        input.setHint(getString(R.string.set_delete_type));
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.set_delete_confirm)
                .setView(input)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    String typed = input.getText() == null ? ""
                            : input.getText().toString().trim();
                    if (!"ELIMINAR".equalsIgnoreCase(typed)) {
                        Toast.makeText(requireContext(), R.string.action_cancel,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AppExecutors.diskIo().execute(() -> {
                        Result<Void> r = new VaultUseCases.DeleteVaultUseCase(
                                ServiceLocator.get().vaultRepository()).execute();
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (r.isSuccess()) {
                                Toast.makeText(requireContext(), R.string.set_deleted,
                                        Toast.LENGTH_LONG).show();
                                ((MainActivity) requireActivity()).showSetup();
                            } else {
                                Toast.makeText(requireContext(), r.getError(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
