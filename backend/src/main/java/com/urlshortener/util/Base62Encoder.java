package com.urlshortener.util;

public final class Base62Encoder {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private Base62Encoder() {
    }

    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(ALPHABET[0]);
        }
        StringBuilder builder = new StringBuilder();
        long current = value;
        while (current > 0) {
            builder.append(ALPHABET[(int) (current % 62)]);
            current /= 62;
        }
        return builder.reverse().toString();
    }
}
