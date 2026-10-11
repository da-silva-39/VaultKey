package mz.ac.ucm.feng.ti.vaultkey.data.repository;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.data.mapper.EntityMappers;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CredentialEntity;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.repository.CredentialRepository;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.AesGcmService;
import mz.ac.ucm.feng.ti.vaultkey.security.crypto.CryptoException;

/**
 * Repositório de credenciais: coordena Room + AES-GCM. O DAO nunca vê
 * texto simples sensível; a sessão tem de estar válida em cada operação.
 */
public class CredentialRepositoryImpl implements CredentialRepository {

    private final VaultRepositoryImpl vault;
    private final AesGcmService crypto = new AesGcmService();

    public CredentialRepositoryImpl(VaultRepositoryImpl vault) {
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
    public Result<Long> save(Credential credential) {
        try {
            SecretKey k = key();
            long now = System.currentTimeMillis();
            credential.setCreatedAt(now);
            credential.setUpdatedAt(now);
            // Cifra com AAD provisório (id 0); após o insert, re-cifra com o id real.
            CredentialEntity e = EntityMappers.toEntity(credential,
                    crypto.encryptToString(k, credential.getPassword(), VaultRepositoryImpl.aad("cred", 0, "password")),
                    crypto.encryptToString(k, nz(credential.getNotes()), VaultRepositoryImpl.aad("cred", 0, "notes")));
            long id = vault.getDatabase().credentialDao().insert(e);
            e.id = id;
            e.passwordEncrypted = crypto.encryptToString(k, credential.getPassword(), VaultRepositoryImpl.aad("cred", id, "password"));
            e.notesEncrypted = crypto.encryptToString(k, nz(credential.getNotes()), VaultRepositoryImpl.aad("cred", id, "notes"));
            vault.getDatabase().credentialDao().update(e);
            vault.touch();
            return Result.ok(id);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao guardar a credencial.");
        }
    }

    @Override
    public Result<Void> update(Credential credential) {
        try {
            SecretKey k = key();
            credential.setUpdatedAt(System.currentTimeMillis());
            CredentialEntity e = EntityMappers.toEntity(credential,
                    crypto.encryptToString(k, credential.getPassword(), VaultRepositoryImpl.aad("cred", credential.getId(), "password")),
                    crypto.encryptToString(k, nz(credential.getNotes()), VaultRepositoryImpl.aad("cred", credential.getId(), "notes")));
            vault.getDatabase().credentialDao().update(e);
            vault.touch();
            return Result.ok(null);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao actualizar a credencial.");
        }
    }

    @Override
    public Result<Void> delete(long id) {
        try {
            key();
            CredentialEntity e = vault.getDatabase().credentialDao().getById(id);
            if (e == null) {
                return Result.fail("Credencial inexistente.");
            }
            vault.getDatabase().credentialDao().delete(e);
            vault.touch();
            return Result.ok(null);
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao eliminar a credencial.");
        }
    }

    @Override
    public Result<Credential> getById(long id) {
        try {
            SecretKey k = key();
            CredentialEntity e = vault.getDatabase().credentialDao().getById(id);
            if (e == null) {
                return Result.fail("Credencial inexistente.");
            }
            vault.touch();
            return Result.ok(decrypt(k, e));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao ler a credencial.");
        }
    }

    @Override
    public Result<List<Credential>> listAll(boolean orderByName) {
        try {
            SecretKey k = key();
            List<CredentialEntity> entities = orderByName
                    ? vault.getDatabase().credentialDao().getAllByName()
                    : vault.getDatabase().credentialDao().getAllByUpdated();
            vault.touch();
            return Result.ok(decryptAll(k, entities));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao listar credenciais.");
        }
    }

    @Override
    public Result<List<Credential>> search(String query) {
        try {
            SecretKey k = key();
            List<CredentialEntity> entities =
                    vault.getDatabase().credentialDao().search(query);
            vault.touch();
            return Result.ok(decryptAll(k, entities));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha na pesquisa.");
        }
    }

    @Override
    public Result<List<Credential>> listByCategory(String category) {
        try {
            SecretKey k = key();
            vault.touch();
            return Result.ok(decryptAll(k,
                    vault.getDatabase().credentialDao().getByCategory(category)));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao filtrar por categoria.");
        }
    }

    @Override
    public Result<List<Credential>> listFavorites() {
        try {
            SecretKey k = key();
            vault.touch();
            return Result.ok(decryptAll(k,
                    vault.getDatabase().credentialDao().getFavorites()));
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao listar favoritos.");
        }
    }

    @Override
    public Result<Integer> count() {
        try {
            key();
            return Result.ok(vault.getDatabase().credentialDao().count());
        } catch (CryptoException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Falha ao contar credenciais.");
        }
    }

    private List<Credential> decryptAll(SecretKey k, List<CredentialEntity> entities)
            throws CryptoException {
        List<Credential> out = new ArrayList<>(entities.size());
        for (CredentialEntity e : entities) {
            out.add(decrypt(k, e));
        }
        return out;
    }

    private Credential decrypt(SecretKey k, CredentialEntity e) throws CryptoException {
        String password = crypto.decryptToString(k, e.passwordEncrypted,
                VaultRepositoryImpl.aad("cred", e.id, "password"));
        String notes = "";
        if (e.notesEncrypted != null && !e.notesEncrypted.isEmpty()) {
            notes = crypto.decryptToString(k, e.notesEncrypted,
                    VaultRepositoryImpl.aad("cred", e.id, "notes"));
        }
        return EntityMappers.toDomain(e, password, notes);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
