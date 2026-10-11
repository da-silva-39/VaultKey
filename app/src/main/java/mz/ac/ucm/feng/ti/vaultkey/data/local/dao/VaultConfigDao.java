package mz.ac.ucm.feng.ti.vaultkey.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.VaultConfigEntity;

@Dao
public interface VaultConfigDao {

    @Query("SELECT * FROM vault_config WHERE id = 1")
    VaultConfigEntity get();

    @Insert
    void insert(VaultConfigEntity entity);

    @Update
    void update(VaultConfigEntity entity);

    @Query("DELETE FROM vault_config")
    void deleteAll();
}
