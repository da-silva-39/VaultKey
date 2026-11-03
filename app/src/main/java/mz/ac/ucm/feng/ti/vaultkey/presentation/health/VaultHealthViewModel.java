package mz.ac.ucm.feng.ti.vaultkey.presentation.health;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.health.VaultHealthAnalyzer;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.CredentialUseCases;

/** Estado da análise de saúde do cofre (executa apenas no dispositivo). */
public class VaultHealthViewModel extends ViewModel {

    private final MutableLiveData<VaultHealthAnalyzer.Report> report = new MutableLiveData<>();

    public LiveData<VaultHealthAnalyzer.Report> getReport() {
        return report;
    }

    public void load() {
        AppExecutors.diskIo().execute(() -> {
            Result<List<Credential>> result =
                    new CredentialUseCases.GetCredentialsUseCase(
                            ServiceLocator.get().credentials()).execute(false);
            if (!result.isSuccess()) {
                return;
            }
            report.postValue(VaultHealthAnalyzer.analyze(
                    result.getValue(), System.currentTimeMillis()));
        });
    }
}
