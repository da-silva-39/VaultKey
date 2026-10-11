package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.AppExecutors;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.AuthenticatorUseCases;
import mz.ac.ucm.feng.ti.vaultkey.domain.validation.OtpParamsValidator;

/** Estado das contas 2FA. */
public class AuthenticatorViewModel extends ViewModel {

    public static class CodeState {
        public final String code;
        public final long remaining;

        public CodeState(String code, long remaining) {
            this.code = code;
            this.remaining = remaining;
        }
    }

    private final MutableLiveData<List<AuthenticatorAccount>> items = new MutableLiveData<>();
    private final MutableLiveData<Result<AuthenticatorAccount>> detail = new MutableLiveData<>();
    private final MutableLiveData<Result<Void>> operation = new MutableLiveData<>();
    private final MutableLiveData<CodeState> codeState = new MutableLiveData<>();

    public LiveData<List<AuthenticatorAccount>> getItems() { return items; }
    public LiveData<Result<AuthenticatorAccount>> getDetail() { return detail; }
    public LiveData<Result<Void>> getOperation() { return operation; }
    public LiveData<CodeState> getCodeState() { return codeState; }

    public void load() {
        AppExecutors.diskIo().execute(() -> {
            Result<List<AuthenticatorAccount>> r =
                    ServiceLocator.get().authenticator().listAll();
            items.postValue(r.isSuccess() ? r.getValue() : null);
        });
    }

    public void loadDetail(long id) {
        AppExecutors.diskIo().execute(() ->
                detail.postValue(ServiceLocator.get().authenticator().getById(id)));
    }

    public void save(AuthenticatorAccount account) {
        AppExecutors.diskIo().execute(() -> {
            Result<Long> r = new AuthenticatorUseCases.CreateAuthenticatorAccountUseCase(
                    ServiceLocator.get().authenticator()).execute(account);
            operation.postValue(r.isSuccess() ? Result.ok(null) : Result.fail(r.getError()));
        });
    }

    public void delete(long id) {
        AppExecutors.diskIo().execute(() ->
                operation.postValue(ServiceLocator.get().authenticator().delete(id)));
    }

    public void refreshTotp(long id) {
        AppExecutors.diskIo().execute(() -> {
            long now = System.currentTimeMillis();
            Result<String> code = ServiceLocator.get().authenticator().generateTotp(id, now);
            Result<Long> rem = ServiceLocator.get().authenticator()
                    .totpSecondsRemaining(id, now);
            if (code.isSuccess() && rem.isSuccess()) {
                codeState.postValue(new CodeState(code.getValue(), rem.getValue()));
            } else {
                codeState.postValue(null);
            }
        });
    }

    public void advanceHotp(long id) {
        AppExecutors.diskIo().execute(() -> {
            Result<String> code =
                    ServiceLocator.get().authenticator().generateHotpAndAdvance(id);
            if (code.isSuccess()) {
                codeState.postValue(new CodeState(code.getValue(), -1));
                loadDetail(id);
            } else {
                codeState.postValue(null);
            }
        });
    }

    public String validate(AuthenticatorAccount account) {
        return OtpParamsValidator.validate(account);
    }
}
