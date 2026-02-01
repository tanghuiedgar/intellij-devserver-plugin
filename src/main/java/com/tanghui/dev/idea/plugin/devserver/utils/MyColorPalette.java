package com.tanghui.dev.idea.plugin.devserver.utils;

import com.intellij.execution.process.ConsoleHighlighter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.terminal.TerminalColorPalette;
import com.intellij.ui.JBColor;
import com.jediterm.core.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @BelongsPackage: com.tanghui.dev.idea.plugin.devserver.utils
 * @Author: 唐煇
 * @CreateTime: 2025-11-17-17:02
 * @Description: 描述类的主要功能和用途。
 * @Version: v1.0
 */
public class MyColorPalette extends TerminalColorPalette {

    private static final TextAttributesKey[] myAnsiColorKeys = new TextAttributesKey[]{
            ConsoleHighlighter.BLACK,
            ConsoleHighlighter.RED,
            ConsoleHighlighter.GREEN,
            ConsoleHighlighter.YELLOW,
            ConsoleHighlighter.BLUE,
            ConsoleHighlighter.MAGENTA,
            ConsoleHighlighter.CYAN,
            ConsoleHighlighter.GRAY,
            ConsoleHighlighter.DARKGRAY,
            ConsoleHighlighter.RED_BRIGHT,
            ConsoleHighlighter.GREEN_BRIGHT,
            ConsoleHighlighter.YELLOW_BRIGHT,
            ConsoleHighlighter.BLUE_BRIGHT,
            ConsoleHighlighter.MAGENTA_BRIGHT,
            ConsoleHighlighter.CYAN_BRIGHT,
            ConsoleHighlighter.WHITE,
    };

    private final Color defaultFg = new Color(220,220,220);
    private final Color defaultBg = new Color(25,25,25);

    // 持有一个配色方案（可传入或用全局方案）
    private final EditorColorsScheme colorsScheme =
            EditorColorsManager.getInstance().getGlobalScheme();

    public MyColorPalette() {
    }

    @NotNull
    @Override
    public Color getDefaultForeground() { return defaultFg; }

    @NotNull
    @Override
    public Color getDefaultBackground() { return defaultBg; }

    @Nullable
    @Override
    protected TextAttributes getAttributesByColorIndex(int index) {
        // 目录常用 ANSI：4(蓝)、12(亮蓝)
        if (index == 1) {
            // 自定义更高对比度的目录前景色（示例：亮青蓝/亮蓝紫）
            // 普通蓝替换为更亮的天蓝
            java.awt.Color dirColor = new JBColor(new java.awt.Color(0xFF7300), new java.awt.Color(0xFF7300)); // 亮蓝替换为更亮的蓝紫
            TextAttributes attrs = new TextAttributes();
            attrs.setForegroundColor(dirColor);
            // 可选：保持粗体，提升可读性（有的主题目录会加粗）
            attrs.setFontType(Font.BOLD);
            return attrs;
        } else if (index == 2) {
            // 自定义更高对比度的目录前景色（示例：亮青蓝/亮蓝紫）
            // 普通蓝替换为更亮的天蓝
            java.awt.Color dirColor = new JBColor(new java.awt.Color(0x1CEE18), new java.awt.Color(0x1CEE18)); // 亮蓝替换为更亮的蓝紫
            TextAttributes attrs = new TextAttributes();
            attrs.setForegroundColor(dirColor);
            // 可选：保持粗体，提升可读性（有的主题目录会加粗）
            attrs.setFontType(Font.BOLD);
            return attrs;
        } else if (index == 4) {
            // 自定义更高对比度的目录前景色（示例：亮青蓝/亮蓝紫）
            // 普通蓝替换为更亮的天蓝
            java.awt.Color dirColor = new JBColor(new java.awt.Color(0x00E6FF), new java.awt.Color(0x00E6FF)); // 亮蓝替换为更亮的蓝紫
            TextAttributes attrs = new TextAttributes();
            attrs.setForegroundColor(dirColor);
            // 可选：保持粗体，提升可读性（有的主题目录会加粗）
            attrs.setFontType(Font.BOLD);
            return attrs;
        }
        // 将 ANSI 索引映射为 IDE 的 TextAttributes
        var key = getAnsiColorKey(index);
        return colorsScheme.getAttributes(key);
    }

    public static TextAttributesKey getAnsiColorKey(int value) {
        if (value >= 16) {
            return ConsoleViewContentType.NORMAL_OUTPUT_KEY;
        }
        return myAnsiColorKeys[value];
    }

}
