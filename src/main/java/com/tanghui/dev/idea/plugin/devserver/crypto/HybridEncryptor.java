package com.tanghui.dev.idea.plugin.devserver.crypto;

import com.alibaba.fastjson.JSONObject;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.crypto
 * @Author: 唐煇
 * @CreateTime: 2025-12-24-16:50
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class HybridEncryptor {

    // 加密
    public static String encrypt(String plainText) throws Exception {

        SecretKey aesKey = AESUtil.generateKey();
        byte[] iv = AESUtil.randomIV();

        String encryptedData = AESUtil.encrypt(plainText, aesKey, iv);
        String encryptedKey = RSAUtil.encrypt(
                aesKey.getEncoded(),
                RSAUtil.loadPublicKey()
        );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key", encryptedKey);
        jsonObject.put("iv", Base64.getEncoder().encodeToString(iv));
        jsonObject.put("data", encryptedData);
        return Base64.getEncoder().encodeToString(jsonObject.toJSONString().getBytes());
    }
}