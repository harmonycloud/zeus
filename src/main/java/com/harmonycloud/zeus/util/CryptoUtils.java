package com.harmonycloud.zeus.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import java.util.Base64;

/**
 * 支持AES、DES、RSA加密、数字签名以及生成对称密钥和非对称密钥对
 */
public class CryptoUtils {

	private static final String key = "HcABCDEFGHIJKLMN";
	private static final String initVector = "HcABCDEFGHIJKLMN";

	public static String encrypt(String value) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
			Security.addProvider(new BouncyCastleProvider());
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

			int blockSize = cipher.getBlockSize();
			byte[] byteContent = value.getBytes();
			int plaintextLength = byteContent.length;
			if (plaintextLength % blockSize != 0) {
				plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
			}
			byte[] plaintext = new byte[plaintextLength];
			System.arraycopy(byteContent, 0, plaintext, 0, byteContent.length);

			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			byte[] encrypted = cipher.doFinal(plaintext);
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}

	public static void main(String[] args) {
		encrypt("Ab123456");
	}

}

