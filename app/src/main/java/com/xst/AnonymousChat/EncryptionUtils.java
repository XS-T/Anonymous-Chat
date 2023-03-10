package com.xst.AnonymousChat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
    private static final String INIT_VECTOR = "aaaaaaaaaaaaaaaa";
    private static final String ENCRYPTION_KEY = "aaaaaaaaaaaaaaaa";

    public static String encrypt(String plaintext) throws Exception {
        byte[] plaintextBytes = plaintext.getBytes("UTF-8");
        byte[] ivBytes = INIT_VECTOR.getBytes("UTF-8");
        byte[] keyBytes = ENCRYPTION_KEY.getBytes("UTF-8");

        // Apply base64 encoding and zero-padding to the plaintext
        String plaintextB64Padded = toB64Padded(plaintext, 16);

        // Create the cipher object and configure it
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // Encrypt the plaintext and apply base64 encoding to the ciphertext
        byte[] ciphertextBytes = cipher.doFinal(plaintextB64Padded.getBytes("UTF-8"));
        String ciphertextB64 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ciphertextB64 = Base64.getEncoder().encodeToString(ciphertextBytes);
        }

        return ciphertextB64;
    }

    public static String decrypt(String ciphertext) throws Exception {
        byte[] ivBytes = INIT_VECTOR.getBytes("UTF-8");
        byte[] keyBytes = ENCRYPTION_KEY.getBytes("UTF-8");

        // Decode the ciphertext from base64
        byte[] ciphertextBytes = new byte[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ciphertextBytes = Base64.getDecoder().decode(ciphertext);
        }

        // Create the cipher object and configure it
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // Decrypt the ciphertext and remove zero-padding
        byte[] plaintextB64PaddedBytes = cipher.doFinal(ciphertextBytes);
        String plaintextB64Padded = new String(plaintextB64PaddedBytes, "UTF-8");
        String plaintextB64 = unpad(plaintextB64Padded);
        String plaintext = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            plaintext = new String(Base64.getDecoder().decode(plaintextB64), "UTF-8");
        }

        return plaintext;
    }

    private static String toB64Padded(String plaintext, int blocksize) throws UnsupportedEncodingException {
        byte[] plaintextBytes = plaintext.getBytes("UTF-8");
        String plaintextB64 = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            plaintextB64 = Base64.getEncoder().encodeToString(plaintextBytes);
        }
        int paddingLength = blocksize - plaintextB64.length() % blocksize;
        String padding = new String(new char[paddingLength]).replace('\0', (char)paddingLength);
        return plaintextB64 + padding;
    }

    private static String unpad(String data) {
        int pad = (int)data.charAt(data.length() - 1);
        if (data.substring(data.length() - pad).equals(new String(new char[pad]).replace('\0', (char)pad))) {
            return data.substring(0, data.length() - pad);
        }
        return data;
    }

    /*public static void main(String[] args) throws Exception {
        String plaintext = "The quick brown fox jumps over the lazy dog";
        String ciphertext = encrypt(plaintext);
        String decryptedPlaintext = decrypt(ciphertext);

        System.out.println("Enc: "+ciphertext);
        System.out.println("Dec: "+decryptedPlaintext);
    }*/
}





