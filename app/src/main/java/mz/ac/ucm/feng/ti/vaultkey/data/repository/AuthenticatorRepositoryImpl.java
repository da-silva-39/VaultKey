package mz.ac.ucm.feng.ti.vaultkey.data.repository;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.base32.Base32;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.hotp.HotpGenerator;
import mz.ac.ucm.feng.ti.vaultkey.authenticator.totp.TotpGenerator;
import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.AuthenticatorAccountEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.mapper.EntityMappers;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.AuthenticatorRepository;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.AesGcmService;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.CryptoException;

/** Repositório de contas 2FA: segredos cifrados, contador HOTP transaccional. */
public class AuthenticatorRepositoryImpl implements AuthenticatorRepository {

    private final VaultRepositoryImpl vault;
    private final AesGcmService crypto = new AesGcmService();

    public AuthenticatorRepositoryImpl(VaultRepositoryImpl vault) {
        this.vault = vault;
    }

    private SecretKey key() throws CryptoException {
        SecretKey k = vault.getSession().requireKey();
        if (k == null) {
            throw new CryptoException("Cofre bloqueado.");
        }
        return k;
    }

    @Override
    public Result<Long> save(AuthenticatorAccount account) {
        try {
            SecretKey k = key();
            long now = System.currentTimeMillis();
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
            AuthenticatorAccountEntity e = EntityMappers.toEntity(account,
                    crypto.encryptToString(k, norm(account.getSecretBase32()),
                            VaultRepositoryImpl.aad("2fa", 0, "secret")));
            long id = vault.getDatabase().authenticatorAccountDao().insert(e);
            e.id = id;
            e.secretEncrypted = crypto.encryptToString(k, norm(account.getSecretBase32()),
                    VaultRepositoryImpl.aad("2fa", id, "secret"));
            vault.getDatabase().authenticatorAccountDao().update(e);
            vault.touch();
            return Result.ok(id);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao guardar a conta 2FA.");
        }
    }

    @Override
    public Result<Void> update(AuthenticatorAccount account) {
        try {
            SecretKey k = key();
            account.setUpdatedAt(System.currentTimeMillis());
            AuthenticatorAccountEntity e = EntityMappers.toEntity(account,
                    crypto.encryptToString(k, norm(account.getSecretBase32()),
                            VaultRepositoryImpl.aad("2fa", account.getId(), "secret")));
            vault.getDatabase().authenticatorAccountDao().update(e);
            vault.touch();
            return Result.ok(null);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao actualizar a conta 2FA.");
        }
    }

    @Override
    public Result<Void> delete(long id) {
        try {
            key();
            AuthenticatorAccountEntity e =
                    vault.getDatabase().authenticatorAccountDao().getById(id);
            if (e == null) {
                return Result.fail("Conta inexistente.");
            }
            vault.getDatabase().authenticatorAccountDao().delete(e);
            vault.touch();
            return Result.ok(null);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao eliminar a conta 2FA.");
        }
    }

    @Override
    public Result<AuthenticatorAccount> getById(long id) {
        try {
            SecretKey k = key();
            AuthenticatorAccountEntity e =
                    vault.getDatabase().authenticatorAccountDao().getById(id);
            if (e == null) {
                return Result.fail("Conta inexistente.");
            }
            vault.touch();
            return Result.ok(decrypt(k, e));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao ler a conta 2FA.");
        }
    }

    @Override
    public Result<List<AuthenticatorAccount>> listAll() {
        try {
            SecretKey k = key();
            List<AuthenticatorAccount> out = new ArrayList<>();
            for (AuthenticatorAccountEntity e
                    : vault.getDatabase().authenticatorAccountDao().getAll()) {
                out.add(decrypt(k, e));
            }
            vault.touch();
            return Result.ok(out);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao listar contas 2FA.");
        }
    }

    @Override
    public Result<Integer> count() {
        try {
            key();
            return Result.ok(vault.getDatabase().authenticatorAccountDao().count());
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao contar contas 2FA.");
        }
    }

    @Override
    public Result<String> generateHotpAndAdvance(long accountId) {
        try {
            SecretKey k = key();
            // Leitura do segredo + consumo transaccional do contador.
            AuthenticatorAccountEntity e =
                    vault.getDatabase().authenticatorAccountDao().getById(accountId);
            if (e == null) {
                return Result.fail("Conta inexistente.");
            }
            if (!"HOTP".equals(e.type)) {
                return Result.fail("A conta não é HOTP.");
            }
            String secret = crypto.decryptToString(k, e.secretEncrypted,
                    VaultRepositoryImpl.aad("2fa", e.id, "secret"));
            long counter = vault.getDatabase().authenticatorAccountDao()
                    .consumeHotpCounter(accountId);
            if (counter < 0) {
                return Result.fail("Falha ao avançar o contador.");
            }
            String code = HotpGenerator.generate(Base32.decode(secret),
                    counter, e.digits, e.algorithm);
            vault.touch();
            return Result.ok(code);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail("Parâmetros da conta inválidos.");
        } catch (Exception e) {
            return Result.fail("Falha ao gerar o código HOTP.");
        }
    }

    @Override
    public Result<String> generateTotp(long accountId, long timeMillis) {
        try {
            SecretKey k = key();
            AuthenticatorAccountEntity e =
                    vault.getDatabase().authenticatorAccountDao().getById(accountId);
            if (e == null) {
                return Result.fail("Conta inexistente.");
            }
            if (!"TOTP".equals(e.type)) {
                return Result.fail("A conta não é TOTP.");
            }
            String secret = crypto.decryptToString(k, e.secretEncrypted,
                    VaultRepositoryImpl.aad("2fa", e.id, "secret"));
            String code = TotpGenerator.generate(Base32.decode(secret),
                    timeMillis, e.period, e.digits, e.algorithm);
            vault.touch();
            return Result.ok(code);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail("Parâmetros da conta inválidos.");
        } catch (Exception e) {
            return Result.fail("Falha ao gerar o código TOTP.");
        }
    }

    @Override
    public Result<Long> totpSecondsRemaining(long accountId, long timeMillis) {
        try {
            key();
            AuthenticatorAccountEntity e =
                    vault.getDatabase().authenticatorAccountDao().getById(accountId);
            if (e == null) {
                return Result.fail("Conta inexistente.");
            }
            return Result.ok(TotpGenerator.secondsRemaining(timeMillis, e.period));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha no cálculo do período.");
        }
    }

    @Override
    public Result<Void> setHotpCounter(long accountId, long counter) {
        try {
            key();
            if (counter < 0) {
                return Result.fail("Contador inválido.");
            }
            vault.getDatabase().authenticatorAccountDao()
                    .setCounter(accountId, counter, System.currentTimeMillis());
            vault.touch();
            return Result.ok(null);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao sincronizar o contador.");
        }
    }

    private AuthenticatorAccount decrypt(SecretKey k, AuthenticatorAccountEntity e)
            throws CryptoException {
        String secret = crypto.decryptToString(k, e.secretEncrypted,
                VaultRepositoryImpl.aad("2fa", e.id, "secret"));
        return EntityMappers.toDomain(e, secret);
    }

    private static String norm(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toUpperCase();
    }
}
