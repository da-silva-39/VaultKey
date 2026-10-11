package mz.ac.ucm.feng.ti.vaultkey;

import android.app.Application;

import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;

/**
 * Application do VaultKey. Inicializa a composição manual de dependências
 * (ServiceLocator) uma única vez por processo.
 */
public class VaultKeyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceLocator.init(this);
    }
}
