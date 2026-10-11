package mz.ac.ucm.feng.ti.vaultkey.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Configuração do cofre (linha única, id = 1).
 * O PIN é guardado apenas como verificador PBKDF2 — nunca em texto simples.
 * Nenhum segredo (senha, TOTPs) vive aqui.
 */
@Entity(tableName = "vault_config")
public class VaultConfigEntity {

    @PrimaryKey
    public int id = 1;

    public String pinVerifier;
    public boolean biometricEnabled;
    public int autoLockSeconds = 60;
    public int clipboardClearSeconds = 30;
    public boolean lockOnExit = true;
    public long createdAt;
}
