package com.stripe.android;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AppInfoTest {
    static final AppInfo APP_INFO = AppInfo.create(
            "MyAwesomePlugin",
            "1.2.34",
            "https://myawesomeplugin.info",
            "pp_partner_1234"
    );

    @Test
    public void toUserAgent() {
        assertEquals("MyAwesomePlugin/1.2.34 (https://myawesomeplugin.info)",
                APP_INFO.toUserAgent());
    }

    @Test
    public void createClientHeaders() {
        final Map<String, String> header = new HashMap<>();
        header.put("application",
                "{\"name\":\"MyAwesomePlugin\",\"partnerId\":\"pp_partner_1234\"," +
                        "\"version\":\"1.2.34\",\"url\":\"https://myawesomeplugin.info\"}");
        assertEquals(header, APP_INFO.createClientHeaders());
    }

    @Test
    public void equals() {
        assertEquals(APP_INFO,
                AppInfo.create(
                        "MyAwesomePlugin",
                        "1.2.34",
                        "https://myawesomeplugin.info",
                        "pp_partner_1234"
                ));
    }
}
