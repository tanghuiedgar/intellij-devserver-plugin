package com.tanghui.dev.idea.plugin.devserver.utils;

import java.util.Collection;
import java.util.Map;

/**
 * @Author: 唐煇
 * @Date: 2023/09/27/22:13
 */
public class CollectionUtil {
    /**
     * 判断集合是否为空的
     *
     * @param collection 集合对象
     * @return 是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空的
     *
     * @param collection 集合对象
     * @return 是否为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断map是否为空的
     *
     * @param map map对象
     * @return 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
