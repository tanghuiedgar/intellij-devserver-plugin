package com.tanghui.dev.idea.plugin.devserver.utils.file;

import com.intellij.openapi.application.PathManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils.file
 * @Author: 唐煇
 * @CreateTime: 2026-01-22-15:48
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class FileUtil {
    private FileUtil() {
    }

    private static class SingletonHelper {
        private static final FileUtil INSTANCE = new FileUtil();
    }

    /**
     * 单例模式
     */
    public static FileUtil getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * 读取文件内容
     * @param file 文件
     * @return 文件内容
     */
    public String getFileContents(File file) {
        StringBuilder result = new StringBuilder();
        try {
            // 构造一个 BufferedReader类来读取文件
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String s;
            // 使用readLine方法，一次读一行
            while ((s = br.readLine()) != null) {
                result.append(System.lineSeparator()).append(s);
            }
            br.close();
        } catch (Exception ignored) {}
        return result.toString();
    }

    /**
     * 获取resource文件
     *
     * @param path     文件夹路径
     * @param fileName 文件名称
     * @return 文件
     */
    public File getResourceFile(String path, String fileName) {
        // 获取系统临时目录
        String tempDir = System.getProperty("java.io.tmpdir");
        URL url = Objects.requireNonNull(FileUtil.class.getResource(path + "/" + fileName));
        File file = new File(tempDir + "/" + fileName);
        OutputStream output;
        try {
            if (file.exists()) {
                file.createNewFile();
            }
            InputStream input = url.openStream();
            output = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
            input.close();
            output.close();
        } catch (IOException ignored) {
        }
        return file;
    }

    /**
     * 计算文件md5值
     * @param file 文件
     * @return md5值
     * */
    public String getFileChecksum(File file) throws IOException, NoSuchAlgorithmException {
        // 获取 MD5 的 MessageDigest 实例
        MessageDigest digest = MessageDigest.getInstance("MD5");
        // 创建一个文件输入流
        FileInputStream fis = new FileInputStream(file);
        // 创建一个字节数组，用于存储读取到的数据
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        // 读取文件的字节并更新到 MessageDigest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        // 关闭文件输入流
        fis.close();
        // 获取文件的哈希值
        byte[] bytes = digest.digest();
        // 将字节数组转换为十六进制格式的字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取配置保存文件
     * @param path 相对文件路径
     * @param fileName 文件名称
     * @return 文件
     */
    public File getConfigPathExample(String path, String fileName) {
        // 当前idea配置路径
        String configPath = PathManager.getConfigPath();
        if (StringUtils.isNotBlank(path)) {
            configPath = configPath + File.separator + path + File.separator + fileName;
        } else {
            configPath = configPath + File.separator + fileName;
        }
        File file = new File(configPath);
        if (file.exists()) {
            return file;
        }
        return null;
    }


    /**
     * 保存配置保存文件
     * @param path 相对文件路径
     * @param fileName 文件名称
     * @param content 文件内容
     * @return 文件
     */
    public File saveConfigPathExample(@NotNull String path, String fileName, String content) {
        String configPath = PathManager.getConfigPath();
        if (StringUtils.isNotBlank(path)) {
            configPath = configPath + File.separator + path + File.separator + fileName;
        } else {
            configPath = configPath + File.separator + fileName;
        }
        File file = new File(configPath);
        // 检查目录是否存在，如果不存在则创建
        File parentDir = file.getParentFile();  // 获取文件的父目录
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();  // 创建目录
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        } catch (IOException ignored) {
        }
        if (file.exists()) {
            return file;
        }
        return null;
    }

}
