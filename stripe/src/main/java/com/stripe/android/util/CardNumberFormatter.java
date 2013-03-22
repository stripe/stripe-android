package com.stripe.android.util;

import com.stripe.android.model.Card;

public abstract class CardNumberFormatter {
    public static String format(String number, boolean trailing) {
        Card card = new Card(number, null, null, null);
        String type = card.getType();
        if (type == null || "Unknown".equals(type)) {
            return number;
        }

        int grouping[] = new int[] { 4, 4, 4, 4 };

        if ("American Express".equals(type)) {
            grouping = new int[] { 4, 6, 5 };
        }

        return formatHelper(number, grouping, trailing);
    }

    private static String formatHelper(String number, int[] grouping, boolean trailing) {
        String rawNumber = number.trim().replaceAll("[^0-9]", "");

        StringBuffer buf = new StringBuffer();

        int start = 0;
        for (int i = 0; i < grouping.length && start < rawNumber.length(); ++i) {
            if (i != 0) {
                buf.append(" ");
            }
            int end = Math.min(start + grouping[i], rawNumber.length());
            buf.append(rawNumber.substring(start, end));
            start = end;
        }

        int n = rawNumber.length();
        if (trailing) {
            int sum = 0;
            for (int i = 0; i < grouping.length - 1; ++i) {
                sum += grouping[i];
                if (n == sum) {
                    buf.append(" ");
                    break;
                }
            }
        }

        return buf.toString();
    }
}
