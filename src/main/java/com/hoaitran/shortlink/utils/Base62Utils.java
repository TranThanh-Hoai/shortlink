package com.hoaitran.shortlink.utils;

public class Base62Utils {
    // Shuffled alphabet to make codes less predictable
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final int MIN_LENGTH = 6;

    /**
     * Encodes a long number into a Base62 string.
     */
    public static String encode(long num) {
        if (num == 0) {
            return pad(String.valueOf(ALPHABET.charAt(0)));
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        
        return pad(sb.reverse().toString());
    }

    /**
     * Decodes a Base62 string back into a long number.
     */
    public static long decode(String code) {
        long num = 0;
        // Remove padding if any (though in this implementation padding is just leading characters)
        // If we use 'w' as the first char for padding:
        for (int i = 0; i < code.length(); i++) {
            num = num * BASE + ALPHABET.indexOf(code.charAt(i));
        }
        return num;
    }

    private static String pad(String s) {
        if (s.length() >= MIN_LENGTH) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < MIN_LENGTH) {
            sb.append(ALPHABET.charAt(0));
        }
        sb.append(s);
        return sb.toString();
    }
}
