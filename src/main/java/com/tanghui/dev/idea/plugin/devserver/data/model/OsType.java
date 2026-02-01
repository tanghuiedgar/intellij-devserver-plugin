package com.tanghui.dev.idea.plugin.devserver.data.model;

import com.tanghui.dev.idea.plugin.devserver.icons.DevServerIcons;
import lombok.Getter;

import javax.swing.*;

@Getter
public enum OsType {
    LINUX("Linux", DevServerIcons.DevServer_Linux),
    CENTOS("CentOS", DevServerIcons.DevServer_CentOS),
    DEBIAN("Debian", DevServerIcons.DevServer_Debian),
    UBUNTU("Ubuntu", DevServerIcons.DevServer_Ubuntu),
    KYLIN("Kylin", DevServerIcons.DevServer_Kylin),
    ALMA_LINUX("AlmaLinux", DevServerIcons.DevServer_AlmaLinux),
    AMAZON("Amazon", DevServerIcons.DevServer_Amazon),
    OPENSUSE("OpenSUSE", DevServerIcons.DevServer_OpenSUSE),
    ROCKY("Rocky", DevServerIcons.DevServer_Rocky);

    private final String displayName;
    private final Icon icon;

    OsType(String displayName, Icon icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /** ⭐ 核心方法 */
    public static OsType fromDisplayName(String name) {
        if (name == null) {
            return LINUX;
        }
        for (OsType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return LINUX; // 默认兜底
    }
}
