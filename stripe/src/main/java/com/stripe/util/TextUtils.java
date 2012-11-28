package com.stripe.util;

import java.util.List;

public class TextUtils {


    public static String toCamelCase(String s) {
        if (s == null) {
            return null;
        }
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return parts[0] + camelCaseString;
    }

    public static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }

    public static boolean hasAnyPrefix(String number, String... prefixes) {
        for (String prefix : prefixes) {
            if (number.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWholePositiveNumber(String value) {
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public static String nullIfBlank(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static String join(List<String> array, String delimiter) {
        if (array == null) {
            return null;
        }

        int arraySize = array.size();

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < arraySize; i++) {
            if (i > 0) {
                buf.append(delimiter);
            }
            if (array.get(i) != null) {
                buf.append(array.get(i));
            }
        }
        return buf.toString();


    }


}
