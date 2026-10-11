package mz.ac.ucm.feng.ti.vaultkey.presentation.vault;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.VaultUseCases;

/** Estado da criação/desbloqueio do cofre. */
public class VaultViewModel extends ViewModel {

    private final MutableLiveData<Result<Void>> createResult = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> unlockResult = new MutableLiveData<>();

    public LiveData<Result<Void>> getCreateResult() {
        return createResult;
    }

    public LiveData<Result<Void>> getUnlockResult() {
        return unlockResult;
    }

    public void createVault(String pin, String confirm) {
        AppExecutors.diskIo().execute(() -> {
            try {
                VaultRepository repo = ServiceLocator.get().vaultRepository();
                Result<Void> r = new VaultUseCases.CreateVaultUseCase(repo).execute(pin, confirm);
                createResult.postValue(r);
            } catch (Exception e) {
                createResult.postValue(Result.fail("Não foi possível criar o cofre."));
            }
        });
    }

    public void unlock(String pin) {
        AppExecutors.diskIo().execute(() -> {
            try {
                VaultRepository repo = ServiceLocator.get().vaultRepository();
                Result<Void> r = new VaultUseCases.UnlockVaultUseCase(repo).execute(pin);
                unlockResult.postValue(r);
            } catch (Exception e) {
                unlockResult.postValue(Result.fail("Falha ao desbloquear o cofre."));
            }
        });
    }
}
