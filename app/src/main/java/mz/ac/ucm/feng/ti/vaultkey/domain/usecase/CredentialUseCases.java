package mz.ac.ucm.feng.ti.vaultkey.domain.usecase;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.CredentialRepository;
import mz.ac.ucm.feng.ti.vaultkey.domain.validation.CredentialValidator;

/** Casos de uso de credenciais, com validação de negócio antes de persistir. */
public final class CredentialUseCases {

    private CredentialUseCases() {
    }

    public static final class SaveCredentialUseCase {
        private final CredentialRepository repo;

        public SaveCredentialUseCase(CredentialRepository repo) {
            this.repo = repo;
        }

        public Result<Long> execute(Credential credential) {
            String error = CredentialValidator.validate(credential);
            if (error != null) {
                return Result.fail(error);
            }
            return repo.save(credential);
        }
    }

    public static final class UpdateCredentialUseCase {
        private final CredentialRepository repo;

        public UpdateCredentialUseCase(CredentialRepository repo) {
            this.repo = repo;
        }

        public Result<Void> execute(Credential credential) {
            String error = CredentialValidator.validate(credential);
            if (error != null) {
                return Result.fail(error);
            }
            return repo.update(credential);
        }
    }

    public static final class DeleteCredentialUseCase {
        private final CredentialRepository repo;

        public DeleteCredentialUseCase(CredentialRepository repo) {
            this.repo = repo;
        }

        public Result<Void> execute(long id) {
            return repo.delete(id);
        }
    }

    public static final class GetCredentialsUseCase {
        private final CredentialRepository repo;

        public GetCredentialsUseCase(CredentialRepository repo) {
            this.repo = repo;
        }

        public Result<List<Credential>> execute(boolean orderByName) {
            return repo.listAll(orderByName);
        }
    }

    public static final class SearchCredentialsUseCase {
        private final CredentialRepository repo;

        public SearchCredentialsUseCase(CredentialRepository repo) {
            this.repo = repo;
        }

        public Result<List<Credential>> execute(String query) {
            if (query == null || query.trim().isEmpty()) {
                return repo.listAll(true);
            }
            return repo.search(query.trim());
        }
    }
}
