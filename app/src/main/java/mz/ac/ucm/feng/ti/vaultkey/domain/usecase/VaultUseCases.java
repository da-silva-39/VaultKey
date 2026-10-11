package mz.ac.ucm.feng.ti.vaultkey.domain.usecase;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.VaultRepository;

/** Casos de uso do ciclo de vida do cofre (responsabilidade única cada). */
public final class VaultUseCases {

    private VaultUseCases() {
    }

    public static final class CreateVaultUseCase {
        private final VaultRepository repo;

        public CreateVaultUseCase(VaultRepository repo) {
            this.repo = repo;
        }

        public Result<Void> execute(String pin, String confirm) {
            return repo.createVault(pin, confirm);
        }
    }

    public static final class UnlockVaultUseCase {
        private final VaultRepository repo;

        public UnlockVaultUseCase(VaultRepository repo) {
            this.repo = repo;
        }

        public Result<Void> execute(String pin) {
            return repo.unlock(pin);
        }
    }

    public static final class LockVaultUseCase {
        private final VaultRepository repo;

        public LockVaultUseCase(VaultRepository repo) {
            this.repo = repo;
        }

        public void execute() {
            repo.lock();
        }
    }

    public static final class DeleteVaultUseCase {
        private final VaultRepository repo;

        public DeleteVaultUseCase(VaultRepository repo) {
            this.repo = repo;
        }

        public Result<Void> execute() {
            return repo.deleteVault();
        }
    }
}
