package mz.ac.ucm.feng.ti.vaultkey.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import mz.ac.ucm.feng.ti.vaultkey.data.local.dao.AuthenticatorAccountDao;
import mz.ac.ucm.feng.ti.vaultkey.data.local.dao.CategoryDao;
import mz.ac.ucm.feng.ti.vaultkey.data.local.dao.CredentialDao;
import mz.ac.ucm.feng.ti.vaultkey.data.local.dao.VaultConfigDao;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.AuthenticatorAccountEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CategoryEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CredentialEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.VaultConfigEntity;

/**
 * Base de dados Room do VaultKey (SQLite local, sem exportação).
 * Alterações de esquema exigem migração explícita — nunca
 * fallbackToDestructiveMigration (não destruir dados silenciosamente).
 */
@Database(
        entities = {
                VaultConfigEntity.class,
                CredentialEntity.class,
                AuthenticatorAccountEntity.class,
                CategoryEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class VaultDatabase extends RoomDatabase {

    public static final String DB_NAME = "vaultkey.db";

    public abstract VaultConfigDao vaultConfigDao();

    public abstract CredentialDao credentialDao();

    public abstract AuthenticatorAccountDao authenticatorAccountDao();

    public abstract CategoryDao categoryDao();
}
