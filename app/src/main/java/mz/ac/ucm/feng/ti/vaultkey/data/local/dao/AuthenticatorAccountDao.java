package mz.ac.ucm.feng.ti.vaultkey.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.AuthenticatorAccountEntity;

@Dao
public abstract class AuthenticatorAccountDao {

    @Insert
    public abstract long insert(AuthenticatorAccountEntity entity);

    @Update
    public abstract void update(AuthenticatorAccountEntity entity);

    @Delete
    public abstract void delete(AuthenticatorAccountEntity entity);

    @Query("SELECT * FROM authenticator_accounts WHERE id = :id")
    public abstract AuthenticatorAccountEntity getById(long id);

    @Query("SELECT * FROM authenticator_accounts ORDER BY issuer COLLATE NOCASE ASC, accountName COLLATE NOCASE ASC")
    public abstract List<AuthenticatorAccountEntity> getAll();

    @Query("SELECT COUNT(*) FROM authenticator_accounts")
    public abstract int count();

    @Query("DELETE FROM authenticator_accounts")
    public abstract void deleteAll();

    /**
     * Avanço transaccional do contador HOTP: lê, incrementa e persiste
     * atomicamente. Uma falha de gravação não produz estado inconsistente.
     *
     * @return o contador a usar na geração, ou -1 se a conta não existir
     */
    @Transaction
    public long consumeHotpCounter(long id) {
        AuthenticatorAccountEntity e = getById(id);
        if (e == null || !"HOTP".equals(e.type)) {
            return -1;
        }
        long current = e.counter;
        e.counter = current + 1;
        e.updatedAt = System.currentTimeMillis();
        update(e);
        return current;
    }

    @Transaction
    @Query("UPDATE authenticator_accounts SET counter = :counter, updatedAt = :now WHERE id = :id")
    public abstract void setCounter(long id, long counter, long now);
}
