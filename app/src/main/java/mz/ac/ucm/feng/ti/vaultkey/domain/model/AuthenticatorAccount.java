package mz.ac.ucm.feng.ti.vaultkey.domain.model;

/** Conta 2FA de negócio (segredo em memória apenas com sessão válida). */
public class AuthenticatorAccount {

    public enum OtpType { TOTP, HOTP }

    private long id;
    private String issuer = "";
    private String accountName = "";
    private String secretBase32 = "";
    private OtpType type = OtpType.TOTP;
    private String hmacAlgorithm = "HmacSHA1";
    private int digits = 6;
    private int period = 30;
    private long counter = 0;
    private long createdAt;
    private long updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getSecretBase32() { return secretBase32; }
    public void setSecretBase32(String secretBase32) { this.secretBase32 = secretBase32; }
    public OtpType getType() { return type; }
    public void setType(OtpType type) { this.type = type; }
    public String getHmacAlgorithm() { return hmacAlgorithm; }
    public void setHmacAlgorithm(String hmacAlgorithm) { this.hmacAlgorithm = hmacAlgorithm; }
    public int getDigits() { return digits; }
    public void setDigits(int digits) { this.digits = digits; }
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
    public long getCounter() { return counter; }
    public void setCounter(long counter) { this.counter = counter; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String displayName() {
        if (!issuer.isEmpty() && !accountName.isEmpty()) {
            return issuer + " (" + accountName + ")";
        }
        if (!issuer.isEmpty()) {
            return issuer;
        }
        return accountName.isEmpty() ? "Conta 2FA" : accountName;
    }
}
