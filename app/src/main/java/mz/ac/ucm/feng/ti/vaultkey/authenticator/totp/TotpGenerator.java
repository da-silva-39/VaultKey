package mz.ac.ucm.feng.ti.vaultkey.authenticator.totp;

import mz.ac.ucm.feng.ti.vaultkey.authenticator.hotp.HotpGenerator;

/**
 * TOTP — Time-Based One-Time Password (RFC 6238).
 * T = (tempoActual - T0) / período; o código é HOTP(segredo, T).
 * Funciona offline com base no relógio local. Classe pura de Java.
 */
public final class TotpGenerator {

    private TotpGenerator() {
    }

    /**
     * Gera o código TOTP para um instante.
     *
     * @param secret      segredo partilhado (bytes)
     * @param timeMillis  instante em milissegundos (ex.: System.currentTimeMillis())
     * @param periodSecs  período em segundos (tipicamente 30; 10–120)
     * @param digits      6 a 10
     * @param hmacAlgo    "HmacSHA1" | "HmacSHA256" | "HmacSHA512"
     * @param t0Seconds   instante inicial (normalmente 0)
     */
    public static String generate(byte[] secret, long timeMillis, int periodSecs,
                                  int digits, String hmacAlgo, long t0Seconds) {
        if (periodSecs < 10 || periodSecs > 120) {
            throw new IllegalArgumentException("Período TOTP fora do intervalo 10–120 s.");
        }
        if (timeMillis < 0) {
            throw new IllegalArgumentException("Instante TOTP inválido.");
        }
        long counter = (timeMillis / 1000L - t0Seconds) / periodSecs;
        return HotpGenerator.generate(secret, counter, digits, hmacAlgo);
    }

    /** Atalho com T0 = 0. */
    public static String generate(byte[] secret, long timeMillis, int periodSecs,
                                  int digits, String hmacAlgo) {
        return generate(secret, timeMillis, periodSecs, digits, hmacAlgo, 0L);
    }

    /** Segundos restantes até à renovação do código actual. */
    public static long secondsRemaining(long timeMillis, int periodSecs, long t0Seconds) {
        long elapsed = timeMillis / 1000L - t0Seconds;
        long r = periodSecs - (elapsed % periodSecs);
        return r == 0 ? periodSecs : r;
    }

    /** Atalho com T0 = 0. */
    public static long secondsRemaining(long timeMillis, int periodSecs) {
        return secondsRemaining(timeMillis, periodSecs, 0L);
    }
}
