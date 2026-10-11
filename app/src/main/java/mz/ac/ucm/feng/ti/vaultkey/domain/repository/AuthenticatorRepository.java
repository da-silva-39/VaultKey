package mz.ac.ucm.feng.ti.vaultkey.domain.repository;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;

/** Contrato de persistência de contas 2FA. */
public interface AuthenticatorRepository {

    Result<Long> save(AuthenticatorAccount account);

    Result<Void> update(AuthenticatorAccount account);

    Result<Void> delete(long id);

    Result<AuthenticatorAccount> getById(long id);

    Result<List<AuthenticatorAccount>> listAll();

    Result<Integer> count();

    /**
     * Gera um código HOTP e avança o contador atomicamente.
     *
     * @return o código gerado para o contador anterior
     */
    Result<String> generateHotpAndAdvance(long accountId);

    /** Gera o código TOTP actual (não altera estado). */
    Result<String> generateTotp(long accountId, long timeMillis);

    /** Segundos restantes do período TOTP actual. */
    Result<Long> totpSecondsRemaining(long accountId, long timeMillis);

    Result<Void> setHotpCounter(long accountId, long counter);
}
