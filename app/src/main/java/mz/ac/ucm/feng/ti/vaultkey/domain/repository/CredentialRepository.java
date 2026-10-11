package mz.ac.ucm.feng.ti.vaultkey.domain.repository;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.common.Result;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;

/** Contrato de persistência de credenciais (implementado na camada data). */
public interface CredentialRepository {

    Result<Long> save(Credential credential);

    Result<Void> update(Credential credential);

    Result<Void> delete(long id);

    Result<Credential> getById(long id);

    Result<List<Credential>> listAll(boolean orderByName);

    Result<List<Credential>> search(String query);

    Result<List<Credential>> listByCategory(String category);

    Result<List<Credential>> listFavorites();

    Result<Integer> count();
}
