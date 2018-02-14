package com.distelli.europa.util;

import java.util.regex.Pattern;

public class Tag {
    private static final Pattern SHA256_PATTERN = Pattern.compile("sha256:[0-9a-fA-F]{64}");
    private static final Pattern TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_][a-zA-Z0-9_\\-\\.]{0,127}$");

    public static boolean isDigest(String tag) {
        if ( null == tag ) return false;
        return SHA256_PATTERN.matcher(tag).matches();
    }

    public static boolean isValid(String tag) {
        if (null == tag) {
            return false;
        }
        if (isDigest(tag)) {
            return true;
        }
        return TAG_PATTERN.matcher(tag).matches();
    }
}
