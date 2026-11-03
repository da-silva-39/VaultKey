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

/**
 * Splash: decide entre configuração inicial e desbloqueio. A navegação só
 * acontece com o estado do FragmentManager válido; se a app for para segundo
 * plano durante a verificação, tenta de novo no onResume (sem crash).
 */
public class SplashFragment extends Fragment {

    private boolean decisionReady;
    private boolean goSetup;

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
            boolean initialized;
            try {
                VaultRepository vault = ServiceLocator.get().vaultRepository();
                initialized = vault.isInitialized();
            } catch (Exception e) {
                initialized = false;
            }
            final boolean setup = !initialized;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isAdded()) {
                    return;
                }
                goSetup = setup;
                decisionReady = true;
                navigate();
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        navigate();
    }

    private void navigate() {
        if (!decisionReady || !isAdded()) {
            return;
        }
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.isFinishing() || activity.isDestroyed()
                || activity.getSupportFragmentManager().isStateSaved()) {
            return;
        }
        if (goSetup) {
            activity.showSetup();
        } else {
            activity.showUnlock();
        }
    }
}
