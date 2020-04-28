
package com.hedera.hashgraph.identity.hcs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryptionUtil {

  private static SecretKeySpec secretToKey(String secret) throws NoSuchAlgorithmException {
    MessageDigest sha = MessageDigest.getInstance("SHA-1");
    byte[] key = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
    key = Arrays.copyOf(key, 16);
    return new SecretKeySpec(key, "AES");
  }

  /**
   * Encrypts the given message with AES using a defined secret phrase.
   *
   * @param  messageToEncrypt Message to encrypt.
   * @param  secret           The encryption secret.
   * @return                  The encrypted message.
   */
  public static byte[] encrypt(byte[] messageToEncrypt, String secret) {
    try {
      SecretKeySpec secretKey = secretToKey(secret);
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);

      return cipher.doFinal(messageToEncrypt);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decrypts the given message encrypted with AES and defined secret phrase.
   *
   * @param  messageToDecrypt Message to decrypt.
   * @param  secret           The encryption secret.
   * @return                  The decrypted message.
   */
  public static byte[] decrypt(byte[] messageToDecrypt, String secret) {
    try {
      SecretKeySpec secretKey = secretToKey(secret);
      Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, secretKey);

      return cipher.doFinal(messageToDecrypt);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private AesEncryptionUtil() {
    // Empty on purpose
  }
}
