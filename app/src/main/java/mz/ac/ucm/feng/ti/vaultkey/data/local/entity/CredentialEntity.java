package mz.ac.ucm.feng.ti.vaultkey.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Credencial. Campos sensíveis (senha, notas) persistidos CIFRADOS
 * (formato "v1.iv.ct" do AesGcmService). Serviço/utilizador/URL/categoria
 * ficam em texto simples para permitir pesquisa e ordenação.
 */
@Entity(tableName = "credentials",
        indices = {@Index("serviceName"), @Index("category"), @Index("favorite")})
public class CredentialEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String serviceName = "";
    public String url = "";
    public String username = "";
    /** Senha cifrada (contentor v1). */
    public String passwordEncrypted = "";
    /** Notas cifradas (contentor v1, pode ser vazio). */
    public String notesEncrypted = "";
    public String category = "";
    public boolean favorite;
    public long createdAt;
    public long updatedAt;
}
