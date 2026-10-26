package com.sennecools.tablist.config;

import java.util.Locale;

public enum NameFormattingProvider {
    NONE,
    FTB,
    LP;

    public static NameFormattingProvider fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return NONE;
        }
    }
}
