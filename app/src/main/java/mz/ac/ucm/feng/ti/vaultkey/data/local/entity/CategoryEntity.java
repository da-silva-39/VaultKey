package mz.ac.ucm.feng.ti.vaultkey.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Categoria personalizada criada pelo utilizador. */
@Entity(tableName = "categories",
        indices = {@Index(value = "name", unique = true)})
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name = "";
}
