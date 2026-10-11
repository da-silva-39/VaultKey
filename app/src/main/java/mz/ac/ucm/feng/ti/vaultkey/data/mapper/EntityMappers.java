package mz.ac.ucm.feng.ti.vaultkey.data.mapper;

import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.AuthenticatorAccountEntity;
import mz.ac.ucm.feng.ti.vaultkey.data.local.entity.CredentialEntity;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;

/**
 * Mapeadores entidade <-> domínio. Os campos cifrados são tratados
 * como opacos aqui; cifragem/decifragem vive nos repositórios.
 */
public final class EntityMappers {

    private EntityMappers() {
    }

    public static CredentialEntity toEntity(Credential c, String passwordEnc, String notesEnc) {
        CredentialEntity e = new CredentialEntity();
        e.id = c.getId();
        e.serviceName = nz(c.getServiceName());
        e.url = nz(c.getUrl());
        e.username = nz(c.getUsername());
        e.passwordEncrypted = passwordEnc;
        e.notesEncrypted = notesEnc == null ? "" : notesEnc;
        e.category = nz(c.getCategory());
        e.favorite = c.isFavorite();
        e.createdAt = c.getCreatedAt();
        e.updatedAt = c.getUpdatedAt();
        return e;
    }

    public static Credential toDomain(CredentialEntity e, String password, String notes) {
        Credential c = new Credential();
        c.setId(e.id);
        c.setServiceName(e.serviceName);
        c.setUrl(e.url);
        c.setUsername(e.username);
        c.setPassword(password);
        c.setNotes(notes);
        c.setCategory(e.category);
        c.setFavorite(e.favorite);
        c.setCreatedAt(e.createdAt);
        c.setUpdatedAt(e.updatedAt);
        return c;
    }

    public static AuthenticatorAccountEntity toEntity(AuthenticatorAccount a, String secretEnc) {
        AuthenticatorAccountEntity e = new AuthenticatorAccountEntity();
        e.id = a.getId();
        e.issuer = nz(a.getIssuer());
        e.accountName = nz(a.getAccountName());
        e.secretEncrypted = secretEnc;
        e.type = a.getType() == AuthenticatorAccount.OtpType.HOTP ? "HOTP" : "TOTP";
        e.algorithm = a.getHmacAlgorithm();
        e.digits = a.getDigits();
        e.period = a.getPeriod();
        e.counter = a.getCounter();
        e.createdAt = a.getCreatedAt();
        e.updatedAt = a.getUpdatedAt();
        return e;
    }

    public static AuthenticatorAccount toDomain(AuthenticatorAccountEntity e, String secretBase32) {
        AuthenticatorAccount a = new AuthenticatorAccount();
        a.setId(e.id);
        a.setIssuer(e.issuer);
        a.setAccountName(e.accountName);
        a.setSecretBase32(secretBase32);
        a.setType("HOTP".equals(e.type)
                ? AuthenticatorAccount.OtpType.HOTP
                : AuthenticatorAccount.OtpType.TOTP);
        a.setHmacAlgorithm(e.algorithm);
        a.setDigits(e.digits);
        a.setPeriod(e.period);
        a.setCounter(e.counter);
        a.setCreatedAt(e.createdAt);
        a.setUpdatedAt(e.updatedAt);
        return a;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
