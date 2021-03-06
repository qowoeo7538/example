package org.lucas.example.foundation.design.pattern.observer.method2.support;

/**
 * @create: 2017-11-07
 * @description:
 */
public interface Observer {
    /**
     * 更新状态
     *
     * @param subject 更新对象
     */
    void update(Subject subject);
}
