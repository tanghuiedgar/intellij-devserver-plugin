package com.tanghui.dev.idea.plugin.devserver.crypto;


import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static com.tanghui.dev.idea.plugin.devserver.crypto.RSAKeyGenerator.PRIVATE_KEY;
import static com.tanghui.dev.idea.plugin.devserver.crypto.RSAKeyGenerator.PUBLIC_KEY;


/**
 * @BelongsPackage: com.tanghuidev.idea.plugin.common.utils.crypto
 * @Author: 唐煇
 * @CreateTime: 2025-12-24-16:50
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class RSAUtil {

    public static PublicKey loadPublicKey() throws Exception {

        byte[] bytes = Base64.getDecoder().decode(PUBLIC_KEY.getBytes());
        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static PrivateKey loadPrivateKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(PRIVATE_KEY.getBytes());
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public static String encrypt(byte[] data, PublicKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data));
    }

    public static byte[] decrypt(String encrypted, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(Base64.getDecoder().decode(encrypted));
    }
}