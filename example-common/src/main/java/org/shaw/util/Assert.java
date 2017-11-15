package org.shaw.util;

import com.sun.istack.internal.Nullable;
import org.springframework.util.StringUtils;

/**
 * @create: 2017-06-30
 * @description:
 */
public abstract class Assert {

    /**
     * 如果对象为空,则抛出异常
     *
     * @param object  对象
     * @param message 异常信息
     */
    public static void notNull(@Nullable Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言表达试,如果为false则抛出异常
     *
     * @param expression 表达试
     * @param message    异常信息
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 如果字符串长度为空,则抛出异常信息
     *
     * @param text    字符串
     * @param message 异常信息
     */
    public static void hasLength(@Nullable String text, String message) {
        if (!StringUtils.hasLength(text)) {
            throw new IllegalArgumentException(message);
        }
    }
}
