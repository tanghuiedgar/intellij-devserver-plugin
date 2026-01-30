package com.tanghui.dev.idea.plugin.devserver;

import com.intellij.testFramework.LightPlatformTestCase;
import com.tanghui.dev.idea.plugin.devserver.crypto.HybridDecryptor;
import com.tanghui.dev.idea.plugin.devserver.crypto.HybridEncryptor;

/**
 * 不要直接运行测试类，执行 intellij-devserver-plugin [test]
 * */
public class CryptoTest extends LightPlatformTestCase {

    /*public void testLogic() {
        // 可以拿到 Project, Editor, PsiFile 等
        assertNotNull(getProject());
    }*/

    public void testHybridEncryptor() throws Exception {
        String encrypt = HybridEncryptor.encrypt("123456");
        System.out.println("encrypt: " + encrypt);
    }

}