package mz.ac.ucm.feng.ti.vaultkey.presentation.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Splash: decide entre configuração inicial e desbloqueio. */
public class SplashFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppExecutors.diskIo().execute(() -> {
            VaultRepository vault;
            boolean initialized = false;
            try {
                vault = ServiceLocator.get().vaultRepository();
                initialized = vault.isInitialized();
            } catch (Exception e) {
                initialized = false;
            }
            final boolean goSetup = !initialized;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) {
                    return;
                }
                MainActivity activity = (MainActivity) requireActivity();
                if (goSetup) {
                    activity.showSetup();
                } else {
                    activity.showUnlock();
                }
            }, 600);
        });
    }
}
