package com.stripe.android.model;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.Objects;

public final class PaymentIntentFixtures {

    @NonNull
    public static final String KEY_ID = "7c4debe3f4af7f9d1569a2ffea4343c2566826ee";

    @NonNull
    public static final PaymentIntent PI_REQUIRES_MASTERCARD_3DS2 = Objects.requireNonNull(
            PaymentIntent.fromString("{\n" +
                    "  id: \"pi_1ExkUeAWhjPjYwPiXph9ouXa\",\n" +
                    "  object: \"payment_intent\",\n" +
                    "  amount: 2000,\n" +
                    "  amount_capturable: 0,\n" +
                    "  amount_received: 0,\n" +
                    "  application: null,\n" +
                    "  application_fee_amount: null,\n" +
                    "  canceled_at: null,\n" +
                    "  cancellation_reason: null,\n" +
                    "  capture_method: \"automatic\",\n" +
                    "  charges: {\n" +
                    "    object: \"list\",\n" +
                    "    data: [],\n" +
                    "    has_more: false,\n" +
                    "    total_count: 0,\n" +
                    "    url: \"/v1/charges?payment_intent=pi_1ExkUeAWhjPjYwPiXph9ouXa\"\n" +
                    "  },\n" +
                    "  client_secret: \"pi_1ExkUeAWhjPjYwPiXph9ouXa_secret_nGTdfGlzL9Uop59wN55LraiC7\",\n" +
                    "  confirmation_method: \"manual\",\n" +
                    "  created: 1563498160,\n" +
                    "  currency: \"usd\",\n" +
                    "  customer: \"cus_FSfpeeEUO3TDOJ\",\n" +
                    "  description: \"Example PaymentIntent\",\n" +
                    "  invoice: null,\n" +
                    "  last_payment_error: null,\n" +
                    "  livemode: true,\n" +
                    "  metadata: {\n" +
                    "    order_id: \"5278735C-1F40-407D-933A-286E463E72D8\"\n" +
                    "  },\n" +
                    "  next_action: {\n" +
                    "    type: \"use_stripe_sdk\",\n" +
                    "    use_stripe_sdk: {\n" +
                    "      type: \"stripe_3ds2_fingerprint\",\n" +
                    "      three_d_secure_2_source: \"src_1ExkUeAWhjPjYwPiLWUvXrSA\",\n" +
                    "      directory_server_name: \"mastercard\",\n" +
                    "      server_transaction_id: \"34b16ea1-1206-4ee8-84d2-d292bc73c2ae\",\n" +
                    "      three_ds_method_url: \"https://secure5.arcot.com/content-server/api/tds2/txn/browser/v1/tds-method\",\n" +
                    "      three_ds_optimizations: \"\",\n" +
                    "      directory_server_encryption: {\n" +
                    "        directory_server_id: \"A000000004\",\n" +
                    "        key_id: \"7c4debe3f4af7f9d1569a2ffea4343c2566826ee\",\n" +
                    "        algorithm: \"RSA\",\n" +
                    "        certificate: \"-----BEGIN CERTIFICATE-----\\nMIIFtTCCA52gAwIBAgIQJqSRaPua/6cpablmVDHWUDANBgkqhkiG9w0BAQsFADB6\\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEsMCoGA1UEAxMjUFJEIE1hc3Rl\\nckNhcmQgM0RTMiBBY3F1aXJlciBTdWIgQ0EwHhcNMTgxMTIwMTQ1MzIzWhcNMjEx\\nMTIwMTQ1MzIzWjBxMQswCQYDVQQGEwJVUzEdMBsGA1UEChMUTWFzdGVyQ2FyZCBX\\nb3JsZHdpZGUxGzAZBgNVBAsTEmdhdGV3YXktZW5jcnlwdGlvbjEmMCQGA1UEAxMd\\nM2RzMi5kaXJlY3RvcnkubWFzdGVyY2FyZC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\\nA4IBDwAwggEKAoIBAQCFlZjqbbL9bDKOzZFawdbyfQcezVEUSDCWWsYKw/V6co9A\\nGaPBUsGgzxF6+EDgVj3vYytgSl8xFvVPsb4ZJ6BJGvimda8QiIyrX7WUxQMB3hyS\\nBOPf4OB72CP+UkaFNR6hdlO5ofzTmB2oj1FdLGZmTN/sj6ZoHkn2Zzums8QAHFjv\\nFjspKUYCmms91gpNpJPUUztn0N1YMWVFpFMytahHIlpiGqTDt4314F7sFABLxzFr\\nDmcqhf623SPV3kwQiLVWOvewO62ItYUFgHwle2dq76YiKrUv1C7vADSk2Am4gqwv\\n7dcCnFeM2AHbBFBa1ZBRQXosuXVw8ZcQqfY8m4iNAgMBAAGjggE+MIIBOjAOBgNV\\nHQ8BAf8EBAMCAygwCQYDVR0TBAIwADAfBgNVHSMEGDAWgBSakqJUx4CN/s5W4wMU\\n/17uSLhFuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9vY3Nw\\nLnBraS5pZGVudGl0eWNoZWNrLm1hc3RlcmNhcmQuY29tMCgGA1UdEQQhMB+CHTNk\\nczIuZGlyZWN0b3J5Lm1hc3RlcmNhcmQuY29tMGkGA1UdHwRiMGAwXqBcoFqGWGh0\\ndHA6Ly9jcmwucGtpLmlkZW50aXR5Y2hlY2subWFzdGVyY2FyZC5jb20vOWE5MmEy\\nNTRjNzgwOGRmZWNlNTZlMzAzMTRmZjVlZWU0OGI4NDViYi5jcmwwHQYDVR0OBBYE\\nFHxN6+P0r3+dFWmi/+pDQ8JWaCbuMA0GCSqGSIb3DQEBCwUAA4ICAQAtwW8siyCi\\nmhon1WUAUmufZ7bbegf3cTOafQh77NvA0xgVeloELUNCwsSSZgcOIa4Zgpsa0xi5\\nfYxXsPLgVPLM0mBhTOD1DnPu1AAm32QVelHe6oB98XxbkQlHGXeOLs62PLtDZd94\\n7pm08QMVb+MoCnHLaBLV6eKhKK+SNrfcxr33m0h3v2EMoiJ6zCvp8HgIHEhVpleU\\n8H2Uo5YObatb/KUHgtp2z0vEfyGhZR7hrr48vUQpfVGBABsCV0aqUkPxtAXWfQo9\\n1N9B7H3EIcSjbiUz5vkj9YeDSyJIi0Y/IZbzuNMsz2cRi1CWLl37w2fe128qWxYq\\nY/k+Y4HX7uYchB8xPaZR4JczCvg1FV2JrkOcFvElVXWSMpBbe2PS6OMr3XxrHjzp\\nDyM9qvzge0Ai9+rq8AyGoG1dP2Ay83Ndlgi42X3yl1uEUW2feGojCQQCFFArazEj\\nLUkSlrB2kA12SWAhsqqQwnBLGSTp7PqPZeWkluQVXS0sbj0878kTra6TjG3U+KqO\\nJCj8v6G380qIkAXe1xMHHNQ6GS59HZMeBPYkK2y5hmh/JVo4bRfK7Ya3blBSBfB8\\nAVWQ5GqVWklvXZsQLN7FH/fMIT3y8iE1W19Ua4whlhvn7o/aYWOkHr1G2xyh8BHj\\n7H63A2hjcPlW/ZAJSTuBZUClAhsNohH2Jg==\\n-----END CERTIFICATE-----\\n\",\n" +
                    "        root_certificate_authorities: [\n" +
                    "          \"-----BEGIN CERTIFICATE-----\\nMIIFxzCCA6+gAwIBAgIQFsjyIuqhw80wNMjXU47lfjANBgkqhkiG9w0BAQsFADB8\\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEuMCwGA1UEAxMlUFJEIE1hc3Rl\\nckNhcmQgSWRlbnRpdHkgQ2hlY2sgUm9vdCBDQTAeFw0xNjA3MTQwNzI0MDBaFw0z\\nMDA3MTUwODEwMDBaMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpNYXN0ZXJDYXJk\\nMSgwJgYDVQQLEx9NYXN0ZXJDYXJkIElkZW50aXR5IENoZWNrIEdlbiAzMS4wLAYD\\nVQQDEyVQUkQgTWFzdGVyQ2FyZCBJZGVudGl0eSBDaGVjayBSb290IENBMIICIjAN\\nBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxZF3nCEiT8XFFaq+3BPT0cMDlWE7\\n6IBsdx27w3hLxwVLog42UTasIgzmysTKpBc17HEZyNAqk9GrCHo0Oyk4JZuXHoW8\\n0goZaR2sMnn49ytt7aGsE1PsfVup8gqAorfm3IFab2/CniJJNXaWPgn94+U/nsoa\\nqTQ6j+6JBoIwnFklhbXHfKrqlkUZJCYaWbZRiQ7nkANYYM2Td3N87FmRanmDXj5B\\nG6lc9o1clTC7UvRQmNIL9OdDDZ8qlqY2Fi0eztBnuo2DUS5tGdVy8SgqPM3E12ft\\nk4EdlKyrWmBqFcYwGx4AcSJ88O3rQmRBMxtk0r5vhgr6hDCGq7FHK/hQFP9LhUO9\\n1qxWEtMn76Sa7DPCLas+tfNRVwG12FBuEZFhdS/qKMdIYUE5Q6uwGTEvTzg2kmgJ\\nT3sNa6dbhlYnYn9iIjTh0dPGgiXap1Bhi8B9aaPFcHEHSqW8nZUINcrwf5AUi+7D\\n+q/AG5ItiBtQTCaaFm74gv51yutzwgKnH9Q+x3mtuK/uwlLCslj9DeXgOzMWFxFg\\nuuwLGX39ktDnetxNw3PLabjHkDlGDIfx0MCQakM74sTcuW8ICiHvNA7fxXCnbtjs\\ny7at/yXYwAd+IDS51MA/g3OYVN4M+0pG843Re6Z53oODp0Ymugx0FNO1NxT3HO1h\\nd7dXyjAV/tN/GGcCAwEAAaNFMEMwDgYDVR0PAQH/BAQDAgGGMBIGA1UdEwEB/wQI\\nMAYBAf8CAQEwHQYDVR0OBBYEFNSlUaqS2hGLFMT/EXrhHeEx+UqxMA0GCSqGSIb3\\nDQEBCwUAA4ICAQBLqIYorrtVz56F6WOoLX9CcRjSFim7gO873a3p7+62I6joXMsM\\nr0nd9nRPcEwduEloZXwFgErVUQWaUZWNpue0mGvU7BUAgV9Tu0J0yA+9srizVoMv\\nx+o4zTJ3Vu5p5aTf1aYoH1xYVo5ooFgl/hI/EXD2lo/xOUfPKXBY7twfiqOziQmT\\nGBuqPRq8h3dQRlXYxX/rzGf80SecIT6wo9KavDkjOmJWGzzHsn6Ryo6MEClMaPn0\\nte87ukNN740AdPhTvNeZdWlwyqWAJpsv24caEckjSpgpoIZOjc7PAcEVQOWFSxUe\\nsMk4Jz5bVZa/ABjzcp+rsq1QLSJ5quqHwWFTewChwpw5gpw+E5SpKY6FIHPlTdl+\\nqHThvN8lsKNAQg0qTdEbIFZCUQC0Cl3Ti3q/cXv8tguLJNWvdGzB600Y32QHclMp\\neyabT4/QeOesqpx6Da70J2KvLT1j6Ch2BsKSzeVLahrjnoPrdgiIYYBOgeA3T8SE\\n1pgagt56R7nIkRQbtesoRKi+NfC7pPb/G1VUsj/cREAHH1i1UKa0aCsIiANfEdQN\\n5Ok6wtFJJhp3apAvnVkrZDfOG5we9bYzvGoI7SUnleURBJ+N3ihjARfL4hDeeRHh\\nYyLkM3kEyEkrJBL5r0GDjicxM+aFcR2fCBAkv3grT5kz4kLcvsmHX+9DBw==\\n-----END CERTIFICATE-----\\n\\n\"\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  },\n" +
                    "  on_behalf_of: null,\n" +
                    "  payment_method: \"pm_1ExkUWAWhjPjYwPiBMVId8xT\",\n" +
                    "  payment_method_options: {\n" +
                    "    card: {\n" +
                    "      request_three_d_secure: \"automatic\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  payment_method_types: [\n" +
                    "    \"card\"\n" +
                    "  ],\n" +
                    "  receipt_email: \"jenny@example.com\",\n" +
                    "  review: null,\n" +
                    "  setup_future_usage: null,\n" +
                    "  shipping: {\n" +
                    "    address: {\n" +
                    "      city: \"San Francisco\",\n" +
                    "      country: \"US\",\n" +
                    "      line1: \"123 Market St\",\n" +
                    "      line2: \"#345\",\n" +
                    "      postal_code: \"94107\",\n" +
                    "      state: \"CA\"\n" +
                    "    },\n" +
                    "    carrier: null,\n" +
                    "    name: \"Fake Name\",\n" +
                    "    phone: \"(555) 555-5555\",\n" +
                    "    tracking_number: null\n" +
                    "  },\n" +
                    "  source: null,\n" +
                    "  statement_descriptor: null,\n" +
                    "  status: \"requires_action\",\n" +
                    "  transfer_data: null,\n" +
                    "  transfer_group: null\n" +
                    "}"));

    @NonNull
    public static final PaymentIntent PI_REQUIRES_AMEX_3DS2 = Objects.requireNonNull(
            PaymentIntent.fromString("{\n" +
                    "    \"id\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx\",\n" +
                    "    \"object\": \"payment_intent\",\n" +
                    "    \"amount\": 1000,\n" +
                    "    \"amount_capturable\": 0,\n" +
                    "    \"amount_received\": 0,\n" +
                    "    \"application\": null,\n" +
                    "    \"application_fee_amount\": null,\n" +
                    "    \"canceled_at\": null,\n" +
                    "    \"cancellation_reason\": null,\n" +
                    "    \"capture_method\": \"automatic\",\n" +
                    "    \"charges\": {\n" +
                    "        \"object\": \"list\",\n" +
                    "        \"data\": [],\n" +
                    "        \"has_more\": false,\n" +
                    "        \"total_count\": 0,\n" +
                    "        \"url\": \"/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx\"\n" +
                    "    },\n" +
                    "    \"client_secret\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k\",\n" +
                    "    \"confirmation_method\": \"automatic\",\n" +
                    "    \"created\": 1558469721,\n" +
                    "    \"currency\": \"usd\",\n" +
                    "    \"customer\": null,\n" +
                    "    \"description\": null,\n" +
                    "    \"invoice\": null,\n" +
                    "    \"last_payment_error\": null,\n" +
                    "    \"livemode\": false,\n" +
                    "    \"metadata\": {},\n" +
                    "    \"next_action\": {\n" +
                    "        \"type\": \"use_stripe_sdk\",\n" +
                    "        \"use_stripe_sdk\": {\n" +
                    "            \"type\": \"stripe_3ds2_fingerprint\",\n" +
                    "            \"three_d_secure_2_source\": \"src_1EceOlCRMbs6FrXf2hqrI1g5\",\n" +
                    "            \"directory_server_name\": \"american_express\",\n" +
                    "            \"server_transaction_id\": \"e64bb72f-60ac-4845-b8b6-47cfdb0f73aa\",\n" +
                    "            \"three_ds_method_url\": \"\",\n" +
                    "            \"directory_server_encryption\": {\n" +
                    "               \"directory_server_id\": \"A000000025\",\n" +
                    "               \"certificate\": " + JSONObject.quote(Stripe3ds2FingerprintTest.DS_CERT_DATA_RSA) + ",\n" +
                    "               \"key_id\": \"" + KEY_ID + "\"\n" +
                    "            }\n" +
                    "        }\n" +
                    "    },\n" +
                    "    \"on_behalf_of\": null,\n" +
                    "    \"payment_method\": \"pm_1EceOkCRMbs6FrXft9sFxCTG\",\n" +
                    "    \"payment_method_types\": [\n" +
                    "        \"card\"\n" +
                    "    ],\n" +
                    "    \"receipt_email\": null,\n" +
                    "    \"review\": null,\n" +
                    "    \"shipping\": null,\n" +
                    "    \"source\": null,\n" +
                    "    \"statement_descriptor\": null,\n" +
                    "    \"status\": \"requires_action\",\n" +
                    "    \"transfer_data\": null,\n" +
                    "    \"transfer_group\": null\n" +
                    "}"));

    @NonNull
    public static final PaymentIntent PI_REQUIRES_3DS1 = Objects.requireNonNull(
            PaymentIntent.fromString("{\n" +
                    "    \"id\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx\",\n" +
                    "    \"object\": \"payment_intent\",\n" +
                    "    \"amount\": 1000,\n" +
                    "    \"amount_capturable\": 0,\n" +
                    "    \"amount_received\": 0,\n" +
                    "    \"application\": null,\n" +
                    "    \"application_fee_amount\": null,\n" +
                    "    \"canceled_at\": null,\n" +
                    "    \"cancellation_reason\": null,\n" +
                    "    \"capture_method\": \"automatic\",\n" +
                    "    \"charges\": {\n" +
                    "        \"object\": \"list\",\n" +
                    "        \"data\": [],\n" +
                    "        \"has_more\": false,\n" +
                    "        \"total_count\": 0,\n" +
                    "        \"url\": \"/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx\"\n" +
                    "    },\n" +
                    "    \"client_secret\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k\",\n" +
                    "    \"confirmation_method\": \"automatic\",\n" +
                    "    \"created\": 1558469721,\n" +
                    "    \"currency\": \"usd\",\n" +
                    "    \"customer\": null,\n" +
                    "    \"description\": null,\n" +
                    "    \"invoice\": null,\n" +
                    "    \"last_payment_error\": null,\n" +
                    "    \"livemode\": false,\n" +
                    "    \"metadata\": {},\n" +
                    "    \"next_action\": {\n" +
                    "        \"type\": \"use_stripe_sdk\",\n" +
                    "        \"use_stripe_sdk\": {\n" +
                    "            \"type\": \"three_d_secure_redirect\",\n" +
                    "            \"stripe_js\": \"https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW\"\n" +
                    "        }\n" +
                    "    },\n" +
                    "    \"on_behalf_of\": null,\n" +
                    "    \"payment_method\": \"pm_1Ecve6CRMbs6FrXf08xsGeHv\",\n" +
                    "    \"payment_method_types\": [\n" +
                    "        \"card\"\n" +
                    "    ],\n" +
                    "    \"receipt_email\": null,\n" +
                    "    \"review\": null,\n" +
                    "    \"shipping\": null,\n" +
                    "    \"source\": null,\n" +
                    "    \"statement_descriptor\": null,\n" +
                    "    \"status\": \"requires_action\",\n" +
                    "    \"transfer_data\": null,\n" +
                    "    \"transfer_group\": null\n" +
                    "}")
    );

    @NonNull
    public static final PaymentIntent PI_REQUIRES_REDIRECT =
            Objects.requireNonNull(PaymentIntent.fromString("{\n" +
                    "\t\"id\": \"pi_1EZlvVCRMbs6FrXfKpq2xMmy\",\n" +
                    "\t\"object\": \"payment_intent\",\n" +
                    "\t\"amount\": 1000,\n" +
                    "\t\"amount_capturable\": 0,\n" +
                    "\t\"amount_received\": 0,\n" +
                    "\t\"application\": null,\n" +
                    "\t\"application_fee_amount\": null,\n" +
                    "\t\"canceled_at\": null,\n" +
                    "\t\"cancellation_reason\": null,\n" +
                    "\t\"capture_method\": \"automatic\",\n" +
                    "\t\"charges\": {\n" +
                    "\t\t\"object\": \"list\",\n" +
                    "\t\t\"data\": [],\n" +
                    "\t\t\"has_more\": false,\n" +
                    "\t\t\"total_count\": 0,\n" +
                    "\t\t\"url\": \"/v1/charges?payment_intent=pi_1EZlvVCRMbs6FrXfKpq2xMmy\"\n" +
                    "\t},\n" +
                    "\t\"client_secret\": \"pi_1EZlvVCRMbs6FrXfKpq2xMmy_secret_cmhLfbSA54n4\",\n" +
                    "\t\"confirmation_method\": \"automatic\",\n" +
                    "\t\"created\": 1557783797,\n" +
                    "\t\"currency\": \"usd\",\n" +
                    "\t\"customer\": null,\n" +
                    "\t\"description\": null,\n" +
                    "\t\"invoice\": null,\n" +
                    "\t\"last_payment_error\": null,\n" +
                    "\t\"livemode\": false,\n" +
                    "\t\"metadata\": {},\n" +
                    "\t\"next_action\": {\n" +
                    "\t\t\"redirect_to_url\": {\n" +
                    "\t\t\t\"return_url\": \"stripe://deeplink\",\n" +
                    "\t\t\t\"url\": \"https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv\"\n" +
                    "\t\t},\n" +
                    "\t\t\"type\": \"redirect_to_url\"\n" +
                    "\t},\n" +
                    "\t\"on_behalf_of\": null,\n" +
                    "\t\"payment_method\": \"pm_1Ecaz6CRMbs6FrXfQs3wNGwB\",\n" +
                    "\t\"payment_method_types\": [\n" +
                    "\t\t\"card\"\n" +
                    "\t],\n" +
                    "\t\"receipt_email\": null,\n" +
                    "\t\"review\": null,\n" +
                    "\t\"shipping\": null,\n" +
                    "\t\"source\": null,\n" +
                    "\t\"statement_descriptor\": null,\n" +
                    "\t\"status\": \"requires_action\",\n" +
                    "\t\"transfer_data\": null,\n" +
                    "\t\"transfer_group\": null\n" +
                    "}"
            ));

    public static final PaymentIntent PI_WITH_LAST_PAYMENT_ERROR =
            Objects.requireNonNull(PaymentIntent.fromString("{\n" +
                    "\t\"id\": \"pi_1F7J1aCRMbs6FrXfaJcvbxF6\",\n" +
                    "\t\"object\": \"payment_intent\",\n" +
                    "\t\"amount\": 1000,\n" +
                    "\t\"canceled_at\": null,\n" +
                    "\t\"cancellation_reason\": null,\n" +
                    "\t\"capture_method\": \"manual\",\n" +
                    "\t\"client_secret\": \"pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s\",\n" +
                    "\t\"confirmation_method\": \"automatic\",\n" +
                    "\t\"created\": 1565775850,\n" +
                    "\t\"currency\": \"usd\",\n" +
                    "\t\"description\": \"Example PaymentIntent\",\n" +
                    "\t\"last_payment_error\": {\n" +
                    "\t\t\"code\": \"payment_intent_authentication_failure\",\n" +
                    "\t\t\"doc_url\": \"https://stripe.com/docs/error-codes/payment-intent-authentication-failure\",\n" +
                    "\t\t\"message\": \"The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.\",\n" +
                    "\t\t\"payment_method\": {\n" +
                    "\t\t\t\"id\": \"pm_1F7J1bCRMbs6FrXfQKsYwO3U\",\n" +
                    "\t\t\t\"object\": \"payment_method\",\n" +
                    "\t\t\t\"billing_details\": {\n" +
                    "\t\t\t\t\"address\": {\n" +
                    "\t\t\t\t\t\"city\": null,\n" +
                    "\t\t\t\t\t\"country\": null,\n" +
                    "\t\t\t\t\t\"line1\": null,\n" +
                    "\t\t\t\t\t\"line2\": null,\n" +
                    "\t\t\t\t\t\"postal_code\": null,\n" +
                    "\t\t\t\t\t\"state\": null\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"email\": null,\n" +
                    "\t\t\t\t\"name\": null,\n" +
                    "\t\t\t\t\"phone\": null\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"card\": {\n" +
                    "\t\t\t\t\"brand\": \"visa\",\n" +
                    "\t\t\t\t\"checks\": {\n" +
                    "\t\t\t\t\t\"address_line1_check\": null,\n" +
                    "\t\t\t\t\t\"address_postal_code_check\": null,\n" +
                    "\t\t\t\t\t\"cvc_check\": null\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"country\": null,\n" +
                    "\t\t\t\t\"exp_month\": 8,\n" +
                    "\t\t\t\t\"exp_year\": 2020,\n" +
                    "\t\t\t\t\"funding\": \"credit\",\n" +
                    "\t\t\t\t\"generated_from\": null,\n" +
                    "\t\t\t\t\"last4\": \"3220\",\n" +
                    "\t\t\t\t\"three_d_secure_usage\": {\n" +
                    "\t\t\t\t\t\"supported\": true\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"wallet\": null\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"created\": 1565775851,\n" +
                    "\t\t\t\"customer\": null,\n" +
                    "\t\t\t\"livemode\": false,\n" +
                    "\t\t\t\"metadata\": {},\n" +
                    "\t\t\t\"type\": \"card\"\n" +
                    "\t\t},\n" +
                    "\t\t\"type\": \"invalid_request_error\"\n" +
                    "\t},\n" +
                    "\t\"livemode\": false,\n" +
                    "\t\"next_action\": null,\n" +
                    "\t\"payment_method\": null,\n" +
                    "\t\"payment_method_types\": [\n" +
                    "\t\t\"card\"\n" +
                    "\t],\n" +
                    "\t\"receipt_email\": null,\n" +
                    "\t\"setup_future_usage\": null,\n" +
                    "\t\"shipping\": null,\n" +
                    "\t\"source\": null,\n" +
                    "\t\"status\": \"requires_payment_method\"\n" +
                    "}"));

    public static final PaymentIntent.RedirectData REDIRECT_DATA =
            new PaymentIntent.RedirectData("https://example.com",
                    "yourapp://post-authentication-return-url");
}
