package mz.ac.ucm.feng.ti.vaultkey.authenticator.base32;

import java.util.Arrays;

/**
 * Codificação/descodificação Base32 (RFC 4648, alfabeto A–Z2–7).
 * Classe pura de Java — sem dependências Android, totalmente testável.
 */
public final class Base32 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] REVERSE = new int[128];

    static {
        Arrays.fill(REVERSE, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            REVERSE[ALPHABET[i]] = i;
            REVERSE[Character.toLowerCase(ALPHABET[i])] = i;
        }
    }

    private Base32() {
    }

    /**
     * Descodifica Base32 tolerando minúsculas, espaços e ausência de padding.
     *
     * @throws IllegalArgumentException se o texto for inválido
     */
    public static byte[] decode(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Segredo Base32 nulo.");
        }
        String s = input.trim().replaceAll("[\\s-]", "").replaceFirst("=+$", "");
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Segredo Base32 vazio.");
        }
        if (s.length() % 8 == 1) {
            throw new IllegalArgumentException("Comprimento Base32 inválido.");
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= REVERSE.length || REVERSE[c] < 0) {
                throw new IllegalArgumentException("Carácter Base32 inválido: '" + c + "'.");
            }
        }
        int outLen = s.length() * 5 / 8;
        byte[] out = new byte[outLen];
        int buffer = 0;
        int bits = 0;
        int pos = 0;
        for (int i = 0; i < s.length(); i++) {
            buffer = (buffer << 5) | REVERSE[s.charAt(i)];
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out[pos++] = (byte) ((buffer >> bits) & 0xFF);
            }
        }
        return out;
    }

    /** Codifica bytes em Base32 com padding. */
    public static String encode(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Dados vazios para codificar.");
        }
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                sb.append(ALPHABET[(buffer >> bits) & 0x1F]);
            }
        }
        if (bits > 0) {
            sb.append(ALPHABET[(buffer << (5 - bits)) & 0x1F]);
        }
        while (sb.length() % 8 != 0) {
            sb.append('=');
        }
        return sb.toString();
    }

    /** Verifica se o texto é um segredo Base32 aceitável (mín. 16 chars úteis = 80 bits). */
    public static boolean isValidSecret(String input) {
        if (input == null) {
            return false;
        }
        String s = input.trim().replaceAll("[\\s-]", "").replaceFirst("=+$", "");
        if (s.length() < 16) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= REVERSE.length || REVERSE[c] < 0) {
                return false;
            }
        }
        return s.length() % 8 != 1;
    }
}
