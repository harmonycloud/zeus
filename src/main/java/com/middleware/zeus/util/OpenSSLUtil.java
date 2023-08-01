package com.middleware.zeus.util;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * @author xutianhong
 * @Date 2023/7/24 5:09 下午
 */
public class OpenSSLUtil {

    public static String getPrivateKeyPem(KeyPair keyPair) throws Exception{
        PemObject pem = new PemObject("RSA PRIVATE KEY", keyPair.getPrivate().getEncoded());
        StringWriter str = new StringWriter();
        PemWriter pemWriter = new PemWriter(str);
        pemWriter.writeObject(pem);
        pemWriter.close();
        str.close();

        return Base64.getEncoder().encodeToString(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String getCertificationRequestPem(KeyPair keyPair, String username) throws Exception{
        PKCS10CertificationRequest csr = generateCsr(keyPair, username);
        PemObject pem = new PemObject("CERTIFICATE REQUEST", csr.getEncoded());
        StringWriter str = new StringWriter();
        PemWriter pemWriter = new PemWriter(str);
        pemWriter.writeObject(pem);
        pemWriter.close();
        str.close();

        return Base64.getEncoder().encodeToString(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public static PKCS10CertificationRequest generateCsr(KeyPair keyPair, String username) throws Exception {
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

        String info = "C=CN";
        if (StringUtils.isNotEmpty(username)){
            info = info + ",CN=" + username;
        }

        X500Name subject = new X500Name(info);

        JcaPKCS10CertificationRequestBuilder csrBuilder =
            new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        return csrBuilder.build(contentSigner);
    }
}
