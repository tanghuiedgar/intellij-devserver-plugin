package com.tanghui.dev.idea.plugin.devserver.utils;

import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ResourceUtil;
import com.tanghui.dev.idea.plugin.devserver.ui.server.BrowserPanel;
import com.tanghui.dev.idea.plugin.devserver.utils.file.FileUtil;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils
 * @Author: 唐煇
 * @CreateTime: 2025-06-05-15:21
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class MarkdownToHtmlTool {

    public static final String LOCAL_RENDER_BASE = "http://127.0.0.1/markdown";

    /**
     * Markdown 在浏览器渲染
     * */
    public static JBCefBrowser getJBCefBrowser(String markdownContent) {
        // 初始化 JCEF 浏览器，用于加载 HTML 内容
        JBCefBrowser browser = new JBCefBrowser();
        // 设置启用的扩展
        String htmlContent = BrowserPanel.browserLoadHTML(markdownContent);
        browser.loadHTML(renderMarkdownToHtml(htmlContent), LOCAL_RENDER_BASE);
        return browser;
    }

    public static String renderMarkdownToHtml(String markdownContent) {
        File file = FileUtil.getInstance().getResourceFile("/template/html", "template.html.vm");
        String templateContent = FileUtil.getInstance().getFileContents(file);
        List<String> jsPathList = getPathList("template/html", "js", "prism.js");
        List<String> cssPathList = getPathList("template/html", "css", "index.css");
        Map<String, Object> param = new HashMap<>();
        param.put("jsPathList", jsPathList);
        param.put("cssPathList", cssPathList);
        param.put("content", markdownContent);
        return VelocityUtils.generate(templateContent, param);
    }

    private static List<String> getPathList(String basePath, String fileName, String first) {
        URL dirUrl = ResourceUtil.getResource(
                MarkdownToHtmlTool.class.getClassLoader(),
                basePath,
                fileName
        );
        List<String> jsPathList = new ArrayList<>();
        if (dirUrl != null && "jar".equals(dirUrl.getProtocol())) {
            try {
                JarURLConnection conn = (JarURLConnection) dirUrl.openConnection();
                JarFile jarFile = conn.getJarFile();
                jarFile.stream()
                        .filter(e -> e.getName().startsWith(basePath + "/" + fileName +"/"))
                        .filter(e -> e.getName().endsWith("." + fileName))
                        .forEach(e -> {
                            if (StringUtils.isNotBlank(e.getName())) {
                                String[] split = e.getName().split("/");
                                String name = split[split.length - 1];
                                File resourceFile = FileUtil.getInstance().getResourceFile("/" + basePath +"/" + fileName, name);
                                String replace = resourceFile.getPath().replace("\\", "/");
                                if (StringUtils.isNotBlank(first) && replace.endsWith(first)) {
                                    jsPathList.addFirst(replace);
                                } else {
                                    jsPathList.add(replace);
                                }
                            }
                        });
            } catch (Exception ignored) {
            }
        }
        return jsPathList;
    }

}
