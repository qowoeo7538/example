package org.lucas.example.foundation.basic.kata.enums;

/**
 * 常量的定义
 */
public enum ConstantKata {
    SUCCESS("Y"),
    FAIL("N"),
    AUDIT_SUCCESS("1"),
    AUDIT_FAIL("0");

    private final String state;

    ConstantKata(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
