package com.dynamicduo.proto.codegen;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * Crypto helper utilities for generated protocol code.
 *
 * Uses:
 *  - AES/GCM/NoPadding for symmetric encryption (shared keys)
 *  - ElGamal for public-key encryption (via BouncyCastle JCE)
 *  - HMAC-SHA256 for MAC
 *  - SHA-256 for hashing
 *  - SHA256withRSA for signatures
 *
 * NOTE: Make sure BouncyCastle is on the classpath and registered as a provider.
 */
public final class CryptoUtil {

    private static final String PROVIDER = "BC";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    // utility class
    private CryptoUtil() {}

    // --------------------------------------------------------------------
    // AES-GCM (shared-key encryption)
    // --------------------------------------------------------------------

    /**
     * Generate a random AES key (default 128-bit).
     */
    public static SecretKey generateAesKey(int bits) throws GeneralSecurityException {
        KeyGenerator kg = KeyGenerator.getInstance("AES", PROVIDER);
        kg.init(bits, SecureRandom.getInstanceStrong());
        return kg.generateKey();
    }

    public static SecretKey generateAesKey() throws GeneralSecurityException {
        return generateAesKey(128);
    }

    /**
     * Encrypt using AES-GCM.
     *
     * Returns iv || ciphertextAndTag as a single byte[].
     */
    public static byte[] encryptAESGCM(SecretKey key, byte[] plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_BYTES];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", PROVIDER);
        AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] result = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
        return result;
    }

    /**
     * Decrypt AES-GCM where input is iv || ciphertextAndTag.
     */
    public static byte[] decryptAESGCM(SecretKey key, byte[] ivAndCiphertext) throws GeneralSecurityException {
        if (ivAndCiphertext.length < GCM_IV_BYTES + 1) {
            throw new IllegalArgumentException("Ciphertext too short for AES-GCM");
        }

        byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_BYTES, ivAndCiphertext.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", PROVIDER);
        AlgorithmParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        return cipher.doFinal(ciphertext);
    }

    /**
     * Rebuild an AES key from raw bytes.
     * Useful if keys are stored / transmitted as bytes.
     */
    public static SecretKey aesKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    // --------------------------------------------------------------------
    // ElGamal (public-key encryption)
    // --------------------------------------------------------------------

    /**
     * Generate an ElGamal key pair.
     *
     * NOTE: This relies on BouncyCastle's "ELGAMAL" support via JCE.
     * Key size 2048 is a reasonable default for demos.
     */
    public static KeyPair generateElGamalKeyPair(int keySize) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ELGAMAL", PROVIDER);
        kpg.initialize(keySize, SecureRandom.getInstanceStrong());
        return kpg.generateKeyPair();
    }

    /**
     * Encrypt using ElGamal public key.
     */
    public static byte[] elGamalEncrypt(PublicKey pk, byte[] plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("ELGAMAL/None/NoPadding", PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, pk, SecureRandom.getInstanceStrong());
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypt using ElGamal private key.
     */
    public static byte[] elGamalDecrypt(PrivateKey sk, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("ELGAMAL/None/NoPadding", PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, sk);
        return cipher.doFinal(ciphertext);
    }

    // --------------------------------------------------------------------
    // HMAC-SHA256 (MAC)
    // --------------------------------------------------------------------

    public static byte[] hmacSha256(byte[] keyBytes, byte[] data) throws GeneralSecurityException {
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256", PROVIDER);
        mac.init(key);
        return mac.doFinal(data);
    }

    // --------------------------------------------------------------------
    // Hash (SHA-256)
    // --------------------------------------------------------------------

    public static byte[] sha256(byte[] data) throws GeneralSecurityException {
        MessageDigest md = MessageDigest.getInstance("SHA-256", PROVIDER);
        return md.digest(data);
    }

    // --------------------------------------------------------------------
    // Sign / Verify (RSA)
    // --------------------------------------------------------------------

    /**
     * Generate an RSA key pair for signatures.
     */
    public static KeyPair generateRsaKeyPair(int bits) throws GeneralSecurityException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", PROVIDER);
        kpg.initialize(bits, SecureRandom.getInstanceStrong());
        return kpg.generateKeyPair();
    }

    /**
     * Sign data using RSA + SHA-256.
     */
    public static byte[] sign(PrivateKey sk, byte[] data) throws GeneralSecurityException {
        Signature sig = Signature.getInstance("SHA256withRSA", PROVIDER);
        sig.initSign(sk, SecureRandom.getInstanceStrong());
        sig.update(data);
        return sig.sign();
    }

    /**
     * Verify RSA signature on data.
     */
    public static boolean verify(PublicKey pk, byte[] data, byte[] signature) throws GeneralSecurityException {
        Signature sig = Signature.getInstance("SHA256withRSA", PROVIDER);
        sig.initVerify(pk);
        sig.update(data);
        return sig.verify(signature);
    }
    
    // --------------------------------------------------------------------
    // Utility: concatenate byte arrays (for "||" in the protocol)
    // --------------------------------------------------------------------

    public static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            if (p != null) {
                total += p.length;
            }
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            if (p != null) {
                System.arraycopy(p, 0, out, pos, p.length);
                pos += p.length;
            }
        }
        return out;
    }
}
