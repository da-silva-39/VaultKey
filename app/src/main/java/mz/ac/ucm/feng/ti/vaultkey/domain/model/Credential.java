package mz.ac.ucm.feng.ti.vaultkey.domain.model;

/** Credencial de negócio (senha em memória apenas quando o cofre está desbloqueado). */
public class Credential {

    private long id;
    private String serviceName = "";
    private String url = "";
    private String username = "";
    private String password = "";
    private String notes = "";
    private String category = "";
    private boolean favorite;
    private long createdAt;
    private long updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
