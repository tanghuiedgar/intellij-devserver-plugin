package com.tanghui.dev.idea.plugin.devserver.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class SizeFormatUtils {

    private SizeFormatUtils() {}

    // 十进制单位（1000）
    private static final String[] DEC_UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};
    private static final long DEC_STEP = 1000L;

    // 二进制单位（1024）
    private static final String[] BIN_UNITS = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
    private static final long BIN_STEP = 1024L;

    // ===========================
    //   公共格式化逻辑
    // ===========================
    private static String format(long bytes, String[] units, long step) {
        if (bytes < 0) return "0 B";

        double val = bytes;
        int idx = 0;

        while (val >= step && idx < units.length - 1) {
            val /= step;
            idx++;
        }

        return String.format(Locale.ROOT, "%.2f %s", val, units[idx]);
    }

    // ===========================
    //    对外暴露的接口
    // ===========================

    /** 十进制（1 KB = 1000 B） */
    public static String formatDecimal(long bytes) {
        return format(bytes, DEC_UNITS, DEC_STEP);
    }

    /** 二进制（1 KiB = 1024 B） */
    public static String formatBinary(long bytes) {
        return format(bytes, BIN_UNITS, BIN_STEP);
    }

    /** 从 InputStream 读取全部内容并返回十进制大小字符串 */
    public static String readAllToBytesAndMeasure(InputStream in) throws IOException {
        if (in == null) {
            return "0 B";
        }

        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            long total = 0;

            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                total += n;
            }

            return formatDecimal(total);
        }
    }
}
