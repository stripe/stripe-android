package com.stripe.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLUtils {

    public static final String UTF8 = "UTF-8";

    public static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, UTF8);
        } catch (UnsupportedEncodingException e) {
            StripeLog.e(e);
            throw new RuntimeException(e);
        }
    }
}
