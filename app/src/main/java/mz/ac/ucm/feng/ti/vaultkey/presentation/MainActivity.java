package mz.ac.ucm.feng.ti.vaultkey.presentation;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;
import mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator.AuthenticatorListFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialListFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.dashboard.DashboardFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.settings.SettingsFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.splash.SplashFragment;

/**
 * Activity única. Gere o encaminhamento (splash/setup/desbloqueio),
 * a barra de navegação inferior e a política de sessão:
 * toque renova a actividade, pausa bloqueia (se configurado).
 */
public class MainActivity extends AppCompatActivity {

    private View bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_nav);

        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showTab(new DashboardFragment(), "home");
                return true;
            } else if (id == R.id.nav_vault) {
                showTab(new CredentialListFragment(), "vault");
                return true;
            } else if (id == R.id.nav_auth) {
                showTab(new AuthenticatorListFragment(), "auth");
                return true;
            } else if (id == R.id.nav_settings) {
                showTab(new SettingsFragment(), "settings");
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            showFragment(new SplashFragment(), "splash", false, false, false);
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        try {
            ServiceLocator.get().session().touch();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            VaultRepository vault = ServiceLocator.get().vaultRepository();
            if (vault.isUnlocked() && vault.isLockOnExit() && !isChangingConfigurations()) {
                vault.lock();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enforceSessionGate();
    }

    /** Se o cofre bloqueou por baixo da UI, volta ao desbloqueio. */
    public void enforceSessionGate() {
        try {
            VaultRepository vault = ServiceLocator.get().vaultRepository();
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
            boolean needsUnlock = current != null
                    && !(current instanceof SplashFragment)
                    && !(current instanceof mz.ac.ucm.feng.ti.vaultkey.presentation.vault.SetupPinFragment)
                    && !(current instanceof mz.ac.ucm.feng.ti.vaultkey.presentation.vault.UnlockFragment);
            if (!vault.isInitialized() && needsUnlock) {
                showSetup();
            } else if (vault.isInitialized() && !vault.isUnlocked() && needsUnlock) {
                showUnlock();
            }
        } catch (Exception ignored) {
        }
    }

    public void showFragment(Fragment fragment, String tag, boolean addToBackStack,
                             boolean showBottomNav, boolean secure) {
        setSecure(secure);
        bottomNav.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.container, fragment, tag)
                    .addToBackStack(tag)
                    .commit();
        } else {
            getSupportFragmentManager().popBackStack(null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.container, fragment, tag)
                    .commit();
        }
    }

    private void showTab(Fragment fragment, String tag) {
        if (!ServiceLocator.get().vaultRepository().isUnlocked()) {
            showUnlock();
            return;
        }
        showFragment(fragment, tag, false, true, false);
    }

    public void showSetup() {
        showFragment(new mz.ac.ucm.feng.ti.vaultkey.presentation.vault.SetupPinFragment(),
                "setup", false, false, false);
    }

    public void showUnlock() {
        showFragment(new mz.ac.ucm.feng.ti.vaultkey.presentation.vault.UnlockFragment(),
                "unlock", false, false, false);
    }

    public void showHome() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_home);
        showFragment(new DashboardFragment(), "home", false, true, false);
    }

    public void setSecure(boolean secure) {
        if (secure) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }
}
