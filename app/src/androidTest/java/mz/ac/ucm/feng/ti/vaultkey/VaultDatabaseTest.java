package mz.ac.ucm.feng.ti.vaultkey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.data.local.VaultDatabase;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.AuthenticatorAccountEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CredentialEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.VaultConfigEntity;

/**
 * Testes instrumentados: CRUD real sobre SQLite (base em memória),
 * pesquisa, favoritos e avanço transaccional do contador HOTP.
 */
@RunWith(AndroidJUnit4.class)
public class VaultDatabaseTest {

    private VaultDatabase db;

    @Before
    public void open() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, VaultDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void close() {
        db.close();
    }

    @Test
    public void vaultConfigCrud() {
        VaultConfigEntity config = new VaultConfigEntity();
        config.pinVerifier = "pbkdf2-sha256$210000$c2FsdA$aGFzaA";
        config.autoLockSeconds = 60;
        db.vaultConfigDao().insert(config);

        VaultConfigEntity loaded = db.vaultConfigDao().get();
        assertNotNull(loaded);
        assertEquals(60, loaded.autoLockSeconds);

        loaded.autoLockSeconds = 120;
        db.vaultConfigDao().update(loaded);
        assertEquals(120, db.vaultConfigDao().get().autoLockSeconds);
    }

    @Test
    public void credentialCrudSearchAndFavorites() {
        CredentialEntity a = credential("GitHub", "alice", "Banco");
        CredentialEntity b = credential("Banco X", "bob", "Banco");
        b.favorite = true;
        long idA = db.credentialDao().insert(a);
        db.credentialDao().insert(b);

        assertNotNull(db.credentialDao().getById(idA));
        assertEquals(2, db.credentialDao().count());

        List<CredentialEntity> search = db.credentialDao().search("git");
        assertEquals(1, search.size());
        assertEquals("GitHub", search.get(0).serviceName);

        List<CredentialEntity> favs = db.credentialDao().getFavorites();
        assertEquals(1, favs.size());
        assertEquals("Banco X", favs.get(0).serviceName);

        List<CredentialEntity> byCat = db.credentialDao().getByCategory("Banco");
        assertEquals(2, byCat.size());

        CredentialEntity loaded = db.credentialDao().getById(idA);
        loaded.serviceName = "GitLab";
        db.credentialDao().update(loaded);
        assertEquals("GitLab", db.credentialDao().getById(idA).serviceName);

        db.credentialDao().delete(loaded);
        assertNull(db.credentialDao().getById(idA));
        assertEquals(1, db.credentialDao().count());
    }

    @Test
    public void hotpCounterAdvancesTransactionally() {
        AuthenticatorAccountEntity e = new AuthenticatorAccountEntity();
        e.issuer = "Banco";
        e.type = "HOTP";
        e.counter = 5;
        long id = db.authenticatorAccountDao().insert(e);

        long used = db.authenticatorAccountDao().consumeHotpCounter(id);
        assertEquals(5, used);
        assertEquals(6, db.authenticatorAccountDao().getById(id).counter);

        long used2 = db.authenticatorAccountDao().consumeHotpCounter(id);
        assertEquals(6, used2);

        // Conta TOTP não deve consumir contador.
        AuthenticatorAccountEntity t = new AuthenticatorAccountEntity();
        t.issuer = "Site";
        t.type = "TOTP";
        long totpId = db.authenticatorAccountDao().insert(t);
        assertEquals(-1, db.authenticatorAccountDao().consumeHotpCounter(totpId));

        // Sincronização manual do contador.
        db.authenticatorAccountDao().setCounter(id, 41, System.currentTimeMillis());
        assertEquals(41, db.authenticatorAccountDao().getById(id).counter);
    }

    @Test
    public void searchWithoutMatchesReturnsEmpty() {
        db.credentialDao().insert(credential("Email", "carol", "Email"));
        assertTrue(db.credentialDao().search("zzz-inexistente").isEmpty());
    }

    private static CredentialEntity credential(String service, String user, String category) {
        CredentialEntity e = new CredentialEntity();
        e.serviceName = service;
        e.username = user;
        e.category = category;
        e.passwordEncrypted = "v1.iv.ct";
        e.createdAt = System.currentTimeMillis();
        e.updatedAt = e.createdAt;
        return e;
    }
}
