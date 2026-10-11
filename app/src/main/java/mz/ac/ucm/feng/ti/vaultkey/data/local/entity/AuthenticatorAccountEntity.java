package mz.ac.ucm.feng.ti.vaultkey.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Conta 2FA. O segredo é persistido CIFRADO. O contador HOTP é persistente
 * e só avança na operação explícita de geração (transaccional).
 */
@Entity(tableName = "authenticator_accounts",
        indices = {@Index("issuer"), @Index("type")})
public class AuthenticatorAccountEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String issuer = "";
    public String accountName = "";
    /** Segredo Base32 cifrado (contentor v1). */
    public String secretEncrypted = "";
    /** "TOTP" ou "HOTP". */
    public String type = "TOTP";
    /** "HmacSHA1" | "HmacSHA256" | "HmacSHA512". */
    public String algorithm = "HmacSHA1";
    public int digits = 6;
    public int period = 30;
    public long counter = 0;
    public long createdAt;
    public long updatedAt;
}
