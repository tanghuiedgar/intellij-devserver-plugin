package com.tanghui.dev.idea.plugin.devserver.utils;

import java.util.concurrent.TimeUnit;

public final class DurationFormatUtils {

    private DurationFormatUtils() {
    }

    // ================= 公共拆分工具 =================

    private static class Parts {
        final long h, m, s, ms;

        Parts(long millis) {
            long abs = Math.abs(millis);

            long totalSeconds = abs / 1000;
            this.ms = abs % 1000;
            this.h = totalSeconds / 3600;
            this.m = (totalSeconds % 3600) / 60;
            this.s = totalSeconds % 60;
        }
    }

    // ====================================================
    // 自适应格式
    // ====================================================

    /**
     * 根据时长大小自适应格式：
     * < 1 s: X ms
     * < 1 min: Ss MMMms
     * < 1h: Min Ss
     * ≥ 1h: Hh Min Ss
     */
    public static String autoFormat(long millis) {
        if (millis < 0) return "-" + autoFormat(-millis);

        Parts p = new Parts(millis);

        if (millis < 1000) {
            return p.ms + " ms";
        }
        if (millis < TimeUnit.MINUTES.toMillis(1)) {
            return p.ms == 0
                    ? p.s + " s"
                    : p.s + " s " + p.ms + " ms";
        }
        if (millis < TimeUnit.HOURS.toMillis(1)) {
            return pad2(p.m) + " min " + pad2(p.s) + " s";
        }

        // >= 1h
        StringBuilder sb = new StringBuilder();
        sb.append(p.h).append(" h");
        if (p.m > 0) sb.append(" ").append(pad2(p.m)).append(" min");
        if (p.s > 0) sb.append(" ").append(pad2(p.s)).append(" s");
        return sb.toString();
    }

    // ====================================================
    // 固定格式 HH:mm:ss.SSS
    // ====================================================

    /**
     * 固定格式 HH:mm:ss.SSS（>24h 不取模）
     */
    public static String formatHmsMillis(long millis) {
        boolean neg = millis < 0;
        Parts p = new Parts(millis);

        String s = pad2(p.h) + ":" + pad2(p.m) + ":" + pad2(p.s) + "." + pad3(p.ms);
        return neg ? "-" + s : s;
    }

    // ====================================================
    // 简洁可读格式
    // ====================================================

    /**
     * 简洁可读格式，例如：
     * 0ms
     * 950ms
     * 12s
     * 1m 2s
     * 2h 3m（去除尾部 0 单位）
     */
    public static String humanize(long millis) {
        if (millis == 0) return "0ms";
        boolean neg = millis < 0;

        Parts p = new Parts(millis);
        StringBuilder sb = new StringBuilder();

        if (p.h > 0) sb.append(p.h).append("h ");
        if (p.m > 0) sb.append(p.m).append("m ");
        if (p.s > 0) sb.append(p.s).append("s ");
        if (p.h == 0 && p.m == 0 && p.s == 0 && p.ms > 0) sb.append(p.ms).append("ms");

        String out = sb.toString().trim();
        return neg ? "-" + out : out;
    }

    // ================= 工具方法 =================

    private static String pad2(long v) {
        // return v < 10 ? "0" + v : String.valueOf(v);
        return String.valueOf(v);
    }

    private static String pad3(long v) {
        if (v < 10) return "00" + v;
        if (v < 100) return "0" + v;
        return String.valueOf(v);
    }
}
