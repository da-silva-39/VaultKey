package mz.ac.ucm.feng.ti.vaultkey.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CategoryEntity;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CategoryEntity entity);

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    List<CategoryEntity> getAll();

    @Query("DELETE FROM categories")
    void deleteAll();
}
