package mz.ac.ucm.feng.ti.vaultkey.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CredentialEntity;

@Dao
public interface CredentialDao {

    @Insert
    long insert(CredentialEntity entity);

    @Update
    void update(CredentialEntity entity);

    @Delete
    void delete(CredentialEntity entity);

    @Query("SELECT * FROM credentials WHERE id = :id")
    CredentialEntity getById(long id);

    @Query("SELECT * FROM credentials ORDER BY serviceName COLLATE NOCASE ASC")
    List<CredentialEntity> getAllByName();

    @Query("SELECT * FROM credentials ORDER BY updatedAt DESC")
    List<CredentialEntity> getAllByUpdated();

    @Query("SELECT * FROM credentials WHERE serviceName LIKE '%' || :q || '%' ESCAPE '\\' "
            + "OR username LIKE '%' || :q || '%' ESCAPE '\\' "
            + "OR category LIKE '%' || :q || '%' ESCAPE '\\' "
            + "ORDER BY serviceName COLLATE NOCASE ASC")
    List<CredentialEntity> search(String q);

    @Query("SELECT * FROM credentials WHERE category = :category "
            + "ORDER BY serviceName COLLATE NOCASE ASC")
    List<CredentialEntity> getByCategory(String category);

    @Query("SELECT * FROM credentials WHERE favorite = 1 ORDER BY serviceName COLLATE NOCASE ASC")
    List<CredentialEntity> getFavorites();

    @Query("SELECT COUNT(*) FROM credentials")
    int count();

    @Query("DELETE FROM credentials")
    void deleteAll();
}
