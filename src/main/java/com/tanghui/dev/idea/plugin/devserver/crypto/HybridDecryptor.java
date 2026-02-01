package com.tanghui.dev.idea.plugin.devserver.crypto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.crypto
 * @Author: 唐煇
 * @CreateTime: 2025-12-24-16:51
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class HybridDecryptor {

    // 解密
    public static String decrypt(String encryptedJson) throws Exception {
        JSONObject obj = JSON.parseObject(new String(Base64.getDecoder().decode(encryptedJson)));
        byte[] aesKeyBytes = RSAUtil.decrypt(
                obj.getString("key"),
                RSAUtil.loadPrivateKey()
        );
        SecretKey aesKey = AESUtil.restoreKey(aesKeyBytes);
        return AESUtil.decrypt(
                obj.getString("data"),
                aesKey,
                Base64.getDecoder().decode(obj.getString("iv"))
        );
    }
}