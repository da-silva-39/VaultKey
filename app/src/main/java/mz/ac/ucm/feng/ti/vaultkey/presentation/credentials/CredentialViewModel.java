package mz.ac.ucm.feng.ti.vaultkey.presentation.credentials;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.CredentialUseCases;

/** Estado da lista/pesquisa de credenciais. */
public class CredentialViewModel extends ViewModel {

    private final MutableLiveData<List<Credential>> items = new MutableLiveData<>();
    private final MutableLiveData<List<Credential>> favorites = new MutableLiveData<>();
    private final MutableLiveData<int[]> counts = new MutableLiveData<>();
    private final MutableLiveData<Result<Credential>> detail = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> operation = new MutableLiveData<>();
    private final MutableLiveData<Result<List<String>>> categories = new MutableLiveData<>();

    public LiveData<List<Credential>> getItems() { return items; }
    public LiveData<List<Credential>> getFavorites() { return favorites; }
    public LiveData<int[]> getCounts() { return counts; }
    public LiveData<Result<Credential>> getDetail() { return detail; }
    public LiveData<Result<Void>> getOperation() { return operation; }
    public LiveData<Result<List<String>>> getCategories() { return categories; }

    public void load(boolean orderByName) {
        AppExecutors.diskIo().execute(() -> {
            Result<List<Credential>> r = new CredentialUseCases.GetCredentialsUseCase(
                    ServiceLocator.get().credentials()).execute(orderByName);
            items.postValue(r.isSuccess() ? r.getValue() : null);
        });
    }

    public void search(String query) {
        AppExecutors.diskIo().execute(() -> {
            Result<List<Credential>> r = new CredentialUseCases.SearchCredentialsUseCase(
                    ServiceLocator.get().credentials()).execute(query);
            items.postValue(r.isSuccess() ? r.getValue() : null);
        });
    }

    public void loadFavorites() {
        AppExecutors.diskIo().execute(() ->
                favorites.postValue(ServiceLocator.get().credentials()
                        .listFavorites().getValue()));
    }

    public void loadCounts() {
        AppExecutors.diskIo().execute(() -> {
            int c = orZero(ServiceLocator.get().credentials().count());
            int a = orZero(ServiceLocator.get().authenticator().count());
            counts.postValue(new int[]{c, a});
        });
    }

    private static int orZero(Result<Integer> r) {
        return r != null && r.isSuccess() && r.getValue() != null ? r.getValue() : 0;
    }

    public void loadDetail(long id) {
        AppExecutors.diskIo().execute(() ->
                detail.postValue(ServiceLocator.get().credentials().getById(id)));
    }

    public void save(Credential credential) {
        AppExecutors.diskIo().execute(() -> {
            Result<Long> r = new CredentialUseCases.SaveCredentialUseCase(
                    ServiceLocator.get().credentials()).execute(credential);
            operation.postValue(r.isSuccess() ? Result.ok(null) : Result.fail(r.getError()));
        });
    }

    public void update(Credential credential) {
        AppExecutors.diskIo().execute(() -> {
            Result<Void> r = new CredentialUseCases.UpdateCredentialUseCase(
                    ServiceLocator.get().credentials()).execute(credential);
            operation.postValue(r);
        });
    }

    public void delete(long id) {
        AppExecutors.diskIo().execute(() ->
                operation.postValue(ServiceLocator.get().credentials().delete(id)));
    }

    public void loadCategories() {
        AppExecutors.diskIo().execute(() ->
                categories.postValue(ServiceLocator.get().vaultRepository().listCategories()));
    }
}
