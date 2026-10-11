package mz.ac.ucm.feng.ti.vaultkey.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Cifragem autenticada AES-GCM (256 bits, tag de 128 bits, nonce de 96 bits
 * único por operação via SecureRandom).
 *
 * <p>Formato persistido: {@code v1.<ivBase64>.<ctBase64>}. O AAD (dados
 * associados autenticados) vincula o texto cifrado ao registo/contexto;
 * qualquer alteração (texto, nonce ou AAD) faz a decifragem falhar e nenhum
 * dado parcial é devolvido.</p>
 */
public final class AesGcmService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    /** Cifra texto e devolve o contentor serializado. */
    public String encryptToString(SecretKey key, String plainText, byte[] aad) throws CryptoException {
        if (key == null || plainText == null) {
            throw new CryptoException("Parâmetros de cifragem inválidos.");
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "v1." + Base64.getEncoder().encodeToString(iv)
                    + "." + Base64.getEncoder().encodeToString(ct);
        } catch (Exception e) {
            throw new CryptoException("Falha ao cifrar os dados.", e);
        }
    }

    /** Decifra o contentor; falha (excepção) se a autenticação GCM falhar. */
    public String decryptToString(SecretKey key, String payload, byte[] aad) throws CryptoException {
        if (key == null || payload == null) {
            throw new CryptoException("Parâmetros de decifragem inválidos.");
        }
        try {
            String[] parts = payload.split("\\.");
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new CryptoException("Formato de dados cifrados inválido.");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ct = Base64.getDecoder().decode(parts[2]);
            if (iv.length != IV_BYTES || ct.length == 0) {
                throw new CryptoException("Parâmetros cifrados inválidos.");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            // AEADBadTagException e afins: dados alterados ou chave errada.
            throw new CryptoException("Falha de integridade ao decifrar.", e);
        }
    }
}
