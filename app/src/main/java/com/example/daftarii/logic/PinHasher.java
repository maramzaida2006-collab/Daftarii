package com.example.daftarii.logic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * الـ PIN ما بينحفظ أبداً كنص. بنحفظ "بصمة" إله (hash) مع ملح (salt) عشوائي،
 * ولما المستخدم يكتب PIN بنحسب البصمة كمان مرة ونقارن.
 *
 * PBKDF2WithHmacSHA1 لأنه موجود من أندرويد 7 (minSdk 24)، أما SHA256 بس من أندرويد 8.
 * بدون Android، فبينفحص بـ JUnit.
 */
public final class PinHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 10_000;
    private static final int KEY_BITS = 256;
    public static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PinHasher() {}

    /** ملح عشوائي جديد كنص hex (لكل PIN جديد). */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** بصمة الـ PIN كنص hex. الأرقام العربية (١٢٣٤) بتتحوّل لإنجليزي أول. */
    public static String hash(String pin, String saltHex) {
        String normalized = Money.normalizeDigits(pin == null ? "" : pin);
        PBEKeySpec spec = new PBEKeySpec(normalized.toCharArray(), fromHex(saltHex), ITERATIONS, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return toHex(key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** بيقارن بوقت ثابت، عشان ما يبيّن من سرعة الجواب قديش الـ PIN قريب. */
    public static boolean matches(String pin, String saltHex, String expectedHashHex) {
        if (saltHex == null || expectedHashHex == null) return false;
        byte[] actual = fromHex(hash(pin, saltHex));
        byte[] expected = fromHex(expectedHashHex);
        return MessageDigest.isEqual(actual, expected);
    }

    static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    static byte[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0) return new byte[0];
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) return new byte[0];
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
