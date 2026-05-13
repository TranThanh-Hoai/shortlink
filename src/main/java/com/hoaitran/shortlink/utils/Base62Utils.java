package com.hoaitran.shortlink.utils;

public class Base62Utils {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final long SECRET = 781923451L;
    private static final int BASE = ALPHABET.length();

    // Chuyển từ số Long (ID Database) sang chuỗi Base62    
    public static String encode(long num) {
        if (num == 0)
            return String.valueOf(ALPHABET.charAt(0));

        num = num ^ SECRET;

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }
}
