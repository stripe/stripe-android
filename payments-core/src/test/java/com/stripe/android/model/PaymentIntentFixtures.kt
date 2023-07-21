package com.stripe.android.model

import com.stripe.android.model.parsers.PaymentIntentJsonParser
import org.json.JSONObject

@Suppress("MaxLineLength")
internal object PaymentIntentFixtures {
    private val PARSER = PaymentIntentJsonParser()

    const val KEY_ID = "7c4debe3f4af7f9d1569a2ffea4343c2566826ee"

    private val PI_SUCCEEDED_JSON by lazy {
        JSONObject(
            """
        {
            "id": "pi_1IRg6VCRMbs6F",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IRg6VCRMbs6F_secret_7oH5g4v8GaCrHfsGYS6kiSnwF",
            "confirmation_method": "automatic",
            "created": 1614960135,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": "pm_1IJs3ZCRMbs",
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "succeeded"
        }
            """.trimIndent()
        )
    }

    val PI_SUCCEEDED by lazy {
        requireNotNull(PARSER.parse(PI_SUCCEEDED_JSON))
    }

    val PI_VISA_3DS2_JSON by lazy {
        JSONObject(
            """
            {
              "id": "pi_3LOQYELXscDhLXgF0RBaEFmK",
              "object": "payment_intent",
              "last_payment_error": null,
              "livemode": true,
              "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                  "type": "stripe_3ds2_fingerprint",
                  "merchant": "acct_1G4B0ILXscDhLXgF",
                  "three_d_secure_2_source": "src_1LOQYGLXscDhLXgFfh401OjG",
                  "directory_server_name": "visa",
                  "server_transaction_id": "525ba092-6758-45f2-8155-01078d8985f7",
                  "three_ds_method_url": "",
                  "three_ds_optimizations": "kf",
                  "directory_server_encryption": {
                    "directory_server_id": "A000000003",
                    "algorithm": "RSA",
                    "certificate": "-----BEGIN CERTIFICATE-----\nMIIGAzCCA+ugAwIBAgIQDaAlB1IbPwgx5esGu9tLIjANBgkqhkiG9w0BAQsFADB2\nMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xJzAlBgNVBAMMHlZpc2EgZUNv\nbW1lcmNlIElzc3VpbmcgQ0EgLSBHMjAeFw0yMTA4MjMxNTMyMzNaFw0yNDA4MjIx\nNTMyMzNaMIGhMRgwFgYDVQQHDA9IaWdobGFuZHMgUmFuY2gxETAPBgNVBAgMCENv\nbG9yYWRvMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlz\nYSBJbnRlcm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xJTAjBgNVBAMMHDNk\nczIucnNhLmVuY3J5cHRpb24udmlzYS5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IB\nDwAwggEKAoIBAQCy34cZ88+xfenoccRD1jOi6uVCPXo2xyabXcKntxl7h1kHahac\nmpnuiH+kSgSg4DEHDXHg0WBcpMp0cB67dUE1XDxLAxN0gL5fXpVX7dUjI9tS8lcW\nndChHxZTA8HcXUtv1IwU1L3luhgNkog509bRw/V1GLukW6CwFRkMI/8fecV8EUcw\nIGiBr4/cAcaPnLxFWm/SFL2NoixiNf6LnwHrU4YIHsPQCIAM1km4XPDb7Gk2S3o0\nkkXroU87yoiHzFHbEZUN/tO0Juyz8K6AtGBKoppv1hEHz9MFNzLlvGPo7wcPpovb\nMYtwxj10KhtfEKh0sS0yMl1Uvw36JmuwjaC3AgMBAAGjggFfMIIBWzAMBgNVHRMB\nAf8EAjAAMB8GA1UdIwQYMBaAFL0nYyikrlS3yCO3wTVCF+nGeF+FMGcGCCsGAQUF\nBwEBBFswWTAwBggrBgEFBQcwAoYkaHR0cDovL2Vucm9sbC52aXNhY2EuY29tL2VD\nb21tRzIuY3J0MCUGCCsGAQUFBzABhhlodHRwOi8vb2NzcC52aXNhLmNvbS9vY3Nw\nMEYGA1UdIAQ/MD0wMQYIKwYBBQUHAgEwJTAjBggrBgEFBQcCARYXaHR0cDovL3d3\ndy52aXNhLmNvbS9wa2kwCAYGZ4EDAQEBMBMGA1UdJQQMMAoGCCsGAQUFBwMCMDUG\nA1UdHwQuMCwwKqAooCaGJGh0dHA6Ly9lbnJvbGwudmlzYWNhLmNvbS9lQ29tbUcy\nLmNybDAdBgNVHQ4EFgQU/JtqQ7VLWNd3/9zQjpnsR2rz+cwwDgYDVR0PAQH/BAQD\nAgSwMA0GCSqGSIb3DQEBCwUAA4ICAQBYOGCI/bYG2gmLgh7UXg5qrt4xeDYe4RXe\n5xSjFkTelNvdf+KykB+oQzw8ZobIY+pKsPihM6IrtoJQuzOLXPV5L9U4j1qa/NZB\nGZTXFMwKGN/v0/tAj3h8wefcLPWb15RsXEpZmA87ollezpXeEHXPhFIit7cHoG5P\nfem9yMuDISI97qbnIKNtFENJr+fMkWIykQ0QnkM1rt99Yv2ZE4GWZN7VJ0zXFqOF\nNF2IVwnTIZ21eDiCOjQr6ohq7bChDMelB5XvEuhfe400DqDP+e5pPHo81ecXkjJK\ngS5grYYZIbeDBdQL1Cgs1mGu6On8ecr0rcpRlQh++BySg9MKkzJdLt1vsYmxfrfb\nkUaLglTdYAU2nYaOEDR4NvkRxfzegXyXkOqfPTmfkrg+OB0LeuICITJGJ0cuZD5W\nGUNaT9WruEANBRJNVjSX1UeJUnCpz4nitT1ml069ONjEowyWUcKvTr4/nrargv2R\npOD4RPJMti6kG+bm9OeATiSgVNmO5lkAS4AkOop2IcbRFcVKJUTOhx2Q37L4nuAH\nTCXQ9vwT4yWz6fVaCfL/FTvCGMilLPzXC/00OPA2ZtWvClvFh/uHJBjRUnj6WXp3\nO9p9uHfdV9eKJH37k94GUSMjBKQ6aIru1VUvSOmUPrDz5JbQB7bP+IzUaFHeweZX\nOWumZmyGDw==\n-----END CERTIFICATE-----\n",
                    "root_certificate_authorities": [
                      "-----BEGIN CERTIFICATE-----\nMIIDojCCAoqgAwIBAgIQE4Y1TR0/BvLB+WUF1ZAcYjANBgkqhkiG9w0BAQUFADBr\nMQswCQYDVQQGEwJVUzENMAsGA1UEChMEVklTQTEvMC0GA1UECxMmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xHDAaBgNVBAMTE1Zpc2EgZUNv\nbW1lcmNlIFJvb3QwHhcNMDIwNjI2MDIxODM2WhcNMjIwNjI0MDAxNjEyWjBrMQsw\nCQYDVQQGEwJVUzENMAsGA1UEChMEVklTQTEvMC0GA1UECxMmVmlzYSBJbnRlcm5h\ndGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xHDAaBgNVBAMTE1Zpc2EgZUNvbW1l\ncmNlIFJvb3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCvV95WHm6h\n2mCxlCfLF9sHP4CFT8icttD0b0/Pmdjh28JIXDqsOTPHH2qLJj0rNfVIsZHBAk4E\nlpF7sDPwsRROEW+1QK8bRaVK7362rPKgH1g/EkZgPI2h4H3PVz4zHvtH8aoVlwdV\nZqW1LS7YgFmypw23RuwhY/81q6UCzyr0TP579ZRdhE2o8mCP2w4lPJ9zcc+U30rq\n299yOIzzlr3xF7zSujtFWsan9sYXiwGd/BmoKoMWuDpI/k4+oKsGGelT84ATB+0t\nvz8KPFUgOSwsAGl0lUq8ILKpeeUYiZGo3BxN77t+Nwtd/jmliFKMAGzsGHxBvfaL\ndXe6YJ2E5/4tAgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQD\nAgEGMB0GA1UdDgQWBBQVOIMPPyw/cDMezUb+B4wg4NfDtzANBgkqhkiG9w0BAQUF\nAAOCAQEAX/FBfXxcCLkr4NWSR/pnXKUTwwMhmytMiUbPWU3J/qVAtmPN3XEolWcR\nzCSs00Rsca4BIGsDoo8Ytyk6feUWYFN4PMCvFYP3j1IzJL1kk5fui/fbGKhtcbP3\nLBfQdCVp9/5rPJS+TUtBjE7ic9DjkCJzQ83z7+pzzkWKsKZJ/0x9nXGIxHYdkFsd\n7v3M9+79YKWxehZx0RbQfBI8bGmX265fOZpwLwU8GUYEmSA20GBuYQa7FkKMcPcw\n++DbZqMAAb3mLNqRX6BGi01qnD093QVG/na/oAo85ADmJ7f/hC3euiInlhBx6yLt\n398znM/jra6O1I7mT1GvFpLgXPYHDw==\n-----END CERTIFICATE-----\n",
                      "-----BEGIN CERTIFICATE-----\nMIIFqTCCA5GgAwIBAgIPUT6WAAAA20Qn7qzgvuFIMA0GCSqGSIb3DQEBCwUAMG8x\nCzAJBgNVBAYTAlVTMQ0wCwYDVQQKDARWSVNBMS8wLQYDVQQLDCZWaXNhIEludGVy\nbmF0aW9uYWwgU2VydmljZSBBc3NvY2lhdGlvbjEgMB4GA1UEAwwXVmlzYSBQdWJs\naWMgUlNBIFJvb3QgQ0EwHhcNMjEwMzE2MDAwMDAwWhcNNDEwMzE1MDAwMDAwWjBv\nMQswCQYDVQQGEwJVUzENMAsGA1UECgwEVklTQTEvMC0GA1UECwwmVmlzYSBJbnRl\ncm5hdGlvbmFsIFNlcnZpY2UgQXNzb2NpYXRpb24xIDAeBgNVBAMMF1Zpc2EgUHVi\nbGljIFJTQSBSb290IENBMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA\n2WEbXLS3gI6LOY93bP7Kz6EO9L1QXlr8l+fTkJWZldJ6QuwZ1cv4369tfjeJ8O5w\nSJiDcVw7eNdOP73LfAtwHlTnUnb0e9ILTTipc5bkNnAevocrJACsrpiQ8jBI9ttp\ncqKUeJgzW4Ie25ypirKroVD42b4E0iICK2cZ5QfD4BSzUnftp4Bqh8AfpGvG1lre\nCaD53qrsy5SUadY/NaeUGOkqdPvDSNoDIdrbExwnZaSFUmjQT1svKwMqGo2GFrgJ\n4cULEp4NNj5rga8YTTZ7Xo5MblHrLpSPOmJev30KWi/BcbvtCNYNWBTg7UMzP3cK\nMQ1pGLvG2PgvFTZSRvH3QzngJRgrDYYOJ6kj9ave+6yOOFqj80ZCuH0Nugt2mMS3\nc3+Nksaw+6H3cQPsE/Gv5zjfsKleRhEFtE1gyrdUg1DMgu8o/YhKM7FAqkXUn74z\nwoRFgx3Mi5OaGTQbg+NlwJgR4sVHXCV4s9b8PjneLhzWMn353SFARF9dnO7LDBqq\ntT6WltJu1z9x2Ze0UVNZvxKGcyCkLody29O8j9/MGZ8SOSUu4U6NHrebKuuf9Fht\nn6PqQ4ppkhy6sReXeV5NVGfVpDYY5ZAKEWqTYgMULWpQ2Py4BGpFzBe07jXkyulR\npoKvz14iXeA0oq16c94DrFYX0jmrWLeU4a/TCZQLFIsCAwEAAaNCMEAwHQYDVR0O\nBBYEFEtNpg77oBHorQvi8PMKAC+sixb7MA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0P\nAQH/BAQDAgEGMA0GCSqGSIb3DQEBCwUAA4ICAQC5BU9qQSZYPcgCp2x0Juq59kMm\nXuBly094DaEnPqvtCgwwAirkv8x8/QSOxiWWiu+nveyuR+j6Gz/fJaV4u+J5QEDy\ncfk605Mw3HIcJOeZvDgk1eyOmQwUP6Z/BdQTNJmZ92Z8dcG5yWCxLBrqPH7ro3Ss\njhYq9duIJU7jfizCJCN4W8tp0D2pWBe1/CYNswP4GMs5jQ5+ZQKN/L5JFdwVTu7X\nPt8b5zfgbmmQpVmUn0oFwm3OI++Z6gEpNmW5bd/2oUIZoG96Qff2fauVMAYiWQvN\nnL3y1gkRguTOSMVUCCiGfdvwu5ygowillvV2nHb7+YibQ9N5Z2spP0o9Zlfzoat2\n7WFpyK47TiUdu/4toarLKGZP+hbA/F4xlnM/8EfZkE1DeTTI0lhN3O8yEsHrtRl1\nOuQZ/IexHO8UGU6jvn4TWo10HYeXzrGckL7oIXfGTrjPzfY62T5HDW/BAEZS+9Tk\nijz25YM0fPPz7IdlEG+k4q4YwZ82j73Y9kDEM5423mrWorq/Bq7I5Y8v0LTY9GWH\nYrpElYf0WdOXAbsfwQiT6qnRio+p82VyqlY8Jt6VVA6CDy/iHKwcj1ELEnDQfVv9\nhedoxmnQ6xe/nK8czclu9hQJRv5Lh9gk9Q8DKK2nmgzZ8SSQ+lr3mSSeY8JOMRlE\n+RKdOQIChWthTJKh7w==\n-----END CERTIFICATE-----\n"
                    ]
                  },
                  "one_click_authn": null
                }
              },
              "status": "requires_action",
              "amount": 50,
              "amount_capturable": 0,
              "amount_details": {
                "tip": {
                }
              },
              "amount_received": 0,
              "application": null,
              "application_fee_amount": null,
              "automatic_payment_methods": null,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "manual",
              "charges": {
                "object": "list",
                "data": [
                ],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_3LOQYELXscDhLXgF0RBaEFmK"
              },
              "client_secret": "pi_3LOQYELXscDhLXgF0RBaEFmK_secret_reiwOiZPVTQBOsrwK0wgoH8Iy",
              "confirmation_method": "automatic",
              "created": 1658514374,
              "currency": "gbp",
              "customer": "cus_M6dpL0muuXebnG",
              "description": "Example PaymentIntent",
              "invoice": null,
              "metadata": {
              },
              "on_behalf_of": null,
              "payment_method": {
                "id": "pm_1LGVHaAcAOtNkkj6JyzQM2VM",
                "object": "payment_method",
                "billing_details": {
                  "address": {
                    "city": null,
                    "country": "US",
                    "line1": null,
                    "line2": null,
                    "postal_code": "44444",
                    "state": null
                  },
                  "email": "testmode_2c4403676047567c93eaf3a1b61ac910_brnunes@stripe.com",
                  "name": "",
                  "phone": null
                },
                "card": {
                  "brand": "visa",
                  "checks": {
                    "address_line1_check": null,
                    "address_postal_code_check": null,
                    "cvc_check": null
                  },
                  "country": "IE",
                  "exp_month": 3,
                  "exp_year": 2033,
                  "funding": "credit",
                  "generated_from": null,
                  "last4": "3220",
                  "networks": {
                    "available": [
                      "visa"
                    ],
                    "preferred": null
                  },
                  "three_d_secure_usage": {
                    "supported": true
                  },
                  "wallet": null
                },
                "created": 1656625938,
                "customer": null,
                "livemode": false,
                "type": "card"
              },
              "payment_method_options": {
                "card": {
                  "installments": null,
                  "mandate_options": null,
                  "network": null,
                  "request_three_d_secure": "automatic"
                }
              },
              "payment_method_types": [
                "card"
              ],
              "processing": null,
              "receipt_email": null,
              "review": null,
              "setup_future_usage": null,
              "shipping": {
                "address": {
                  "city": "San Francisco",
                  "country": "US",
                  "line1": "123 Market St",
                  "line2": "#345",
                  "postal_code": "94107",
                  "state": "CA"
                },
                "carrier": "Fedex",
                "name": "Jenny Rosen",
                "phone": null,
                "tracking_number": "12345"
              },
              "source": null,
              "statement_descriptor": null,
              "statement_descriptor_suffix": null,
              "transfer_data": null,
              "transfer_group": null
            }
            """.trimIndent()
        )
    }

    val PI_VISA_3DS2 by lazy {
        requireNotNull(PARSER.parse(PI_VISA_3DS2_JSON))
    }

    val PI_REQUIRES_MASTERCARD_3DS2_JSON by lazy {
        JSONObject(

            """
        {
            "id": "pi_1ExkUeAWhjPjYwPiXph9ouXa",
            "object": "payment_intent",
            "amount": 2000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1ExkUeAWhjPjYwPiXph9ouXa"
            },
            "client_secret": "pi_1ExkUeAWhjPjYwPiXph9ouXa_secret_nGTdfGlzL9Uop59wN55LraiC7",
            "confirmation_method": "manual",
            "created": 1563498160,
            "currency": "usd",
            "customer": "cus_FSfpeeEUO3TDOJ",
            "description": "Example PaymentIntent",
            "invoice": null,
            "last_payment_error": null,
            "livemode": true,
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "stripe_3ds2_fingerprint",
                    "three_d_secure_2_source": "src_1ExkUeAWhjPjYwPiLWUvXrSA",
                    "three_d_secure_2_intent": "pi_1ExkUeAWhjPjYwPiLWUvXrSA",
                    "directory_server_name": "mastercard",
                    "server_transaction_id": "34b16ea1-1206-4ee8-84d2-d292bc73c2ae",
                    "three_ds_method_url": "https://secure5.arcot.com/content-server/api/tds2/txn/browser/v1/tds-method",
                    "three_ds_optimizations": "",
                    "directory_server_encryption": {
                        "directory_server_id": "A000000004",
                        "key_id": "7c4debe3f4af7f9d1569a2ffea4343c2566826ee",
                        "algorithm": "RSA",
                        "certificate": "-----BEGIN CERTIFICATE-----\nMIIFtTCCA52gAwIBAgIQJqSRaPua/6cpablmVDHWUDANBgkqhkiG9w0BAQsFADB6\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEsMCoGA1UEAxMjUFJEIE1hc3Rl\nckNhcmQgM0RTMiBBY3F1aXJlciBTdWIgQ0EwHhcNMTgxMTIwMTQ1MzIzWhcNMjEx\nMTIwMTQ1MzIzWjBxMQswCQYDVQQGEwJVUzEdMBsGA1UEChMUTWFzdGVyQ2FyZCBX\nb3JsZHdpZGUxGzAZBgNVBAsTEmdhdGV3YXktZW5jcnlwdGlvbjEmMCQGA1UEAxMd\nM2RzMi5kaXJlY3RvcnkubWFzdGVyY2FyZC5jb20wggEiMA0GCSqGSIb3DQEBAQUA\nA4IBDwAwggEKAoIBAQCFlZjqbbL9bDKOzZFawdbyfQcezVEUSDCWWsYKw/V6co9A\nGaPBUsGgzxF6+EDgVj3vYytgSl8xFvVPsb4ZJ6BJGvimda8QiIyrX7WUxQMB3hyS\nBOPf4OB72CP+UkaFNR6hdlO5ofzTmB2oj1FdLGZmTN/sj6ZoHkn2Zzums8QAHFjv\nFjspKUYCmms91gpNpJPUUztn0N1YMWVFpFMytahHIlpiGqTDt4314F7sFABLxzFr\nDmcqhf623SPV3kwQiLVWOvewO62ItYUFgHwle2dq76YiKrUv1C7vADSk2Am4gqwv\n7dcCnFeM2AHbBFBa1ZBRQXosuXVw8ZcQqfY8m4iNAgMBAAGjggE+MIIBOjAOBgNV\nHQ8BAf8EBAMCAygwCQYDVR0TBAIwADAfBgNVHSMEGDAWgBSakqJUx4CN/s5W4wMU\n/17uSLhFuzBIBggrBgEFBQcBAQQ8MDowOAYIKwYBBQUHMAGGLGh0dHA6Ly9vY3Nw\nLnBraS5pZGVudGl0eWNoZWNrLm1hc3RlcmNhcmQuY29tMCgGA1UdEQQhMB+CHTNk\nczIuZGlyZWN0b3J5Lm1hc3RlcmNhcmQuY29tMGkGA1UdHwRiMGAwXqBcoFqGWGh0\ndHA6Ly9jcmwucGtpLmlkZW50aXR5Y2hlY2subWFzdGVyY2FyZC5jb20vOWE5MmEy\nNTRjNzgwOGRmZWNlNTZlMzAzMTRmZjVlZWU0OGI4NDViYi5jcmwwHQYDVR0OBBYE\nFHxN6+P0r3+dFWmi/+pDQ8JWaCbuMA0GCSqGSIb3DQEBCwUAA4ICAQAtwW8siyCi\nmhon1WUAUmufZ7bbegf3cTOafQh77NvA0xgVeloELUNCwsSSZgcOIa4Zgpsa0xi5\nfYxXsPLgVPLM0mBhTOD1DnPu1AAm32QVelHe6oB98XxbkQlHGXeOLs62PLtDZd94\n7pm08QMVb+MoCnHLaBLV6eKhKK+SNrfcxr33m0h3v2EMoiJ6zCvp8HgIHEhVpleU\n8H2Uo5YObatb/KUHgtp2z0vEfyGhZR7hrr48vUQpfVGBABsCV0aqUkPxtAXWfQo9\n1N9B7H3EIcSjbiUz5vkj9YeDSyJIi0Y/IZbzuNMsz2cRi1CWLl37w2fe128qWxYq\nY/k+Y4HX7uYchB8xPaZR4JczCvg1FV2JrkOcFvElVXWSMpBbe2PS6OMr3XxrHjzp\nDyM9qvzge0Ai9+rq8AyGoG1dP2Ay83Ndlgi42X3yl1uEUW2feGojCQQCFFArazEj\nLUkSlrB2kA12SWAhsqqQwnBLGSTp7PqPZeWkluQVXS0sbj0878kTra6TjG3U+KqO\nJCj8v6G380qIkAXe1xMHHNQ6GS59HZMeBPYkK2y5hmh/JVo4bRfK7Ya3blBSBfB8\nAVWQ5GqVWklvXZsQLN7FH/fMIT3y8iE1W19Ua4whlhvn7o/aYWOkHr1G2xyh8BHj\n7H63A2hjcPlW/ZAJSTuBZUClAhsNohH2Jg==\n-----END CERTIFICATE-----\n",
                        "root_certificate_authorities": ["-----BEGIN CERTIFICATE-----\nMIIFxzCCA6+gAwIBAgIQFsjyIuqhw80wNMjXU47lfjANBgkqhkiG9w0BAQsFADB8\nMQswCQYDVQQGEwJVUzETMBEGA1UEChMKTWFzdGVyQ2FyZDEoMCYGA1UECxMfTWFz\ndGVyQ2FyZCBJZGVudGl0eSBDaGVjayBHZW4gMzEuMCwGA1UEAxMlUFJEIE1hc3Rl\nckNhcmQgSWRlbnRpdHkgQ2hlY2sgUm9vdCBDQTAeFw0xNjA3MTQwNzI0MDBaFw0z\nMDA3MTUwODEwMDBaMHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpNYXN0ZXJDYXJk\nMSgwJgYDVQQLEx9NYXN0ZXJDYXJkIElkZW50aXR5IENoZWNrIEdlbiAzMS4wLAYD\nVQQDEyVQUkQgTWFzdGVyQ2FyZCBJZGVudGl0eSBDaGVjayBSb290IENBMIICIjAN\nBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAxZF3nCEiT8XFFaq+3BPT0cMDlWE7\n6IBsdx27w3hLxwVLog42UTasIgzmysTKpBc17HEZyNAqk9GrCHo0Oyk4JZuXHoW8\n0goZaR2sMnn49ytt7aGsE1PsfVup8gqAorfm3IFab2/CniJJNXaWPgn94+U/nsoa\nqTQ6j+6JBoIwnFklhbXHfKrqlkUZJCYaWbZRiQ7nkANYYM2Td3N87FmRanmDXj5B\nG6lc9o1clTC7UvRQmNIL9OdDDZ8qlqY2Fi0eztBnuo2DUS5tGdVy8SgqPM3E12ft\nk4EdlKyrWmBqFcYwGx4AcSJ88O3rQmRBMxtk0r5vhgr6hDCGq7FHK/hQFP9LhUO9\n1qxWEtMn76Sa7DPCLas+tfNRVwG12FBuEZFhdS/qKMdIYUE5Q6uwGTEvTzg2kmgJ\nT3sNa6dbhlYnYn9iIjTh0dPGgiXap1Bhi8B9aaPFcHEHSqW8nZUINcrwf5AUi+7D\n+q/AG5ItiBtQTCaaFm74gv51yutzwgKnH9Q+x3mtuK/uwlLCslj9DeXgOzMWFxFg\nuuwLGX39ktDnetxNw3PLabjHkDlGDIfx0MCQakM74sTcuW8ICiHvNA7fxXCnbtjs\ny7at/yXYwAd+IDS51MA/g3OYVN4M+0pG843Re6Z53oODp0Ymugx0FNO1NxT3HO1h\nd7dXyjAV/tN/GGcCAwEAAaNFMEMwDgYDVR0PAQH/BAQDAgGGMBIGA1UdEwEB/wQI\nMAYBAf8CAQEwHQYDVR0OBBYEFNSlUaqS2hGLFMT/EXrhHeEx+UqxMA0GCSqGSIb3\nDQEBCwUAA4ICAQBLqIYorrtVz56F6WOoLX9CcRjSFim7gO873a3p7+62I6joXMsM\nr0nd9nRPcEwduEloZXwFgErVUQWaUZWNpue0mGvU7BUAgV9Tu0J0yA+9srizVoMv\nx+o4zTJ3Vu5p5aTf1aYoH1xYVo5ooFgl/hI/EXD2lo/xOUfPKXBY7twfiqOziQmT\nGBuqPRq8h3dQRlXYxX/rzGf80SecIT6wo9KavDkjOmJWGzzHsn6Ryo6MEClMaPn0\nte87ukNN740AdPhTvNeZdWlwyqWAJpsv24caEckjSpgpoIZOjc7PAcEVQOWFSxUe\nsMk4Jz5bVZa/ABjzcp+rsq1QLSJ5quqHwWFTewChwpw5gpw+E5SpKY6FIHPlTdl+\nqHThvN8lsKNAQg0qTdEbIFZCUQC0Cl3Ti3q/cXv8tguLJNWvdGzB600Y32QHclMp\neyabT4/QeOesqpx6Da70J2KvLT1j6Ch2BsKSzeVLahrjnoPrdgiIYYBOgeA3T8SE\n1pgagt56R7nIkRQbtesoRKi+NfC7pPb/G1VUsj/cREAHH1i1UKa0aCsIiANfEdQN\n5Ok6wtFJJhp3apAvnVkrZDfOG5we9bYzvGoI7SUnleURBJ+N3ihjARfL4hDeeRHh\nYyLkM3kEyEkrJBL5r0GDjicxM+aFcR2fCBAkv3grT5kz4kLcvsmHX+9DBw==\n-----END CERTIFICATE-----\n\n"]
                    },
                    "publishable_key": "pk_test_nextActionData"
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1ExkUWAWhjPjYwPiBMVId8xT",
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": ["card"],
            "receipt_email": "jenny@example.com",
            "review": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": null,
                "name": "Fake Name",
                "phone": "(555) 555-5555",
                "tracking_number": null
            },
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
            """.trimIndent()
        )
    }

    val PI_REQUIRES_MASTERCARD_3DS2 by lazy {
        PARSER.parse(PI_REQUIRES_MASTERCARD_3DS2_JSON)!!
    }

    val PI_REQUIRES_AMEX_3DS2 by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            "object": "payment_intent",
            "amount": 1000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx"
            },
            "client_secret": "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            "confirmation_method": "automatic",
            "created": 1558469721,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "stripe_3ds2_fingerprint",
                    "three_d_secure_2_source": "src_1EceOlCRMbs6FrXf2hqrI1g5",
                    "directory_server_name": "american_express",
                    "server_transaction_id": "e64bb72f-60ac-4845-b8b6-47cfdb0f73aa",
                    "three_ds_method_url": "",
                    "directory_server_encryption": {
                        "directory_server_id": "A000000025",
                        "certificate": "-----BEGIN CERTIFICATE-----\nMIIE0TCCA7mgAwIBAgIUXbeqM1duFcHk4dDBwT8o7Ln5wX8wDQYJKoZIhvcNAQEL\nBQAwXjELMAkGA1UEBhMCVVMxITAfBgNVBAoTGEFtZXJpY2FuIEV4cHJlc3MgQ29t\ncGFueTEsMCoGA1UEAxMjQW1lcmljYW4gRXhwcmVzcyBTYWZla2V5IElzc3Vpbmcg\nQ0EwHhcNMTgwMjIxMjM0OTMxWhcNMjAwMjIxMjM0OTMwWjCB0DELMAkGA1UEBhMC\nVVMxETAPBgNVBAgTCE5ldyBZb3JrMREwDwYDVQQHEwhOZXcgWW9yazE\/MD0GA1UE\nChM2QW1lcmljYW4gRXhwcmVzcyBUcmF2ZWwgUmVsYXRlZCBTZXJ2aWNlcyBDb21w\nYW55LCBJbmMuMTkwNwYDVQQLEzBHbG9iYWwgTmV0d29yayBUZWNobm9sb2d5IC0g\nTmV0d29yayBBUEkgUGxhdGZvcm0xHzAdBgNVBAMTFlNESy5TYWZlS2V5LkVuY3J5\ncHRLZXkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFF9kTYbwRrxX\nC6WcJJYio5TZDM62+CnjQRfggV3GMI+xIDtMIN8LL\/jbWBTycu97vrNjNNv+UPhI\nWzhFDdUqyRfrY337A39uE8k1xhdDI3dNeZz6xgq8r9hn2NBou78YPBKidpN5oiHn\nTxcFq1zudut2fmaldaa9a4ZKgIQo+02heiJfJ8XNWkoWJ17GcjJ59UU8C1KF\/y1G\nymYO5ha2QRsVZYI17+ZFsqnpcXwK4Mr6RQKV6UimmO0nr5++CgvXfekcWAlLV6Xq\njuACWi3kw0haepaX\/9qHRu1OSyjzWNcSVZ0On6plB5Lq6Y9ylgmxDrv+zltz3MrT\nK7txIAFFAgMBAAGjggESMIIBDjAMBgNVHRMBAf8EAjAAMCEGA1UdEQQaMBiCFlNE\nSy5TYWZlS2V5LkVuY3J5cHRLZXkwRQYJKwYBBAGCNxQCBDgeNgBBAE0ARQBYAF8A\nUwBBAEYARQBLAEUAWQAyAF8ARABTAF8ARQBOAEMAUgBZAFAAVABJAE8ATjAOBgNV\nHQ8BAf8EBAMCBJAwHwYDVR0jBBgwFoAU7k\/rXuVMhTBxB1zSftPgmLFuDIgwRAYD\nVR0fBD0wOzA5oDegNYYzaHR0cDovL2FtZXhzay5jcmwuY29tLXN0cm9uZy1pZC5u\nZXQvYW1leHNhZmVrZXkuY3JsMB0GA1UdDgQWBBQHclVTo5nwZGH8labJ2F2P45xi\nfDANBgkqhkiG9w0BAQsFAAOCAQEAWY6b77VBoGLs3k5vOqSU7QRqT+4v6y77T8LA\nBKrSZ58DiVZWVyDSxyftQUiRRgFHt2gTN0yfJTP50Fyp84nCEWC0tugZ4iIhgPss\nHzL+4\/u4eG\/MTzK2ESxvPgr6YHajyuU+GXA89u8+bsFrFmojOjhTgFKli7YUeV\/0\nxoiYZf2utlns800ofJrcrfiFoqE6PvK4Od0jpeMgfSKv71nK5ihA1+wTk76ge1fs\nPxL23hEdRpWW11ofaLfJGkLFXMM3\/LHSXWy7HhsBgDELdzLSHU4VkSv8yTOZxsRO\nByxdC5v3tXGcK56iQdtKVPhFGOOEBugw7AcuRzv3f1GhvzAQZg==\n-----END CERTIFICATE-----",
                        "key_id": "7c4debe3f4af7f9d1569a2ffea4343c2566826ee"
                    }
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1EceOkCRMbs6FrXft9sFxCTG",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
                """.trimIndent()
            )
        )!!
    }

    val PI_REQUIRES_3DS1 by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1EceMnCRMbs6FrXfCXdF8dnx",
            "object": "payment_intent",
            "amount": 1000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx"
            },
            "client_secret": "pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k",
            "confirmation_method": "automatic",
            "created": 1558469721,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "type": "use_stripe_sdk",
                "use_stripe_sdk": {
                    "type": "three_d_secure_redirect",
                    "stripe_js": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecve7CRMbs6FrXfm8AxXMIh/src_client_secret_F79yszOBAiuaZTuIhbn3LPUW"
                }
            },
            "on_behalf_of": null,
            "payment_method": "pm_1Ecve6CRMbs6FrXf08xsGeHv",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
                """.trimIndent()
            )
        )!!
    }

    val PI_REQUIRES_REDIRECT by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1EZlvVCRMbs6FrXfKpq2xMmy",
            "object": "payment_intent",
            "amount": 1000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1EZlvVCRMbs6FrXfKpq2xMmy"
            },
            "client_secret": "pi_1EZlvVCRMbs6FrXfKpq2xMmy_secret_cmhLfbSA54n4",
            "confirmation_method": "automatic",
            "created": 1557783797,
            "currency": "usd",
            "customer": null,
            "description": null,
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": {},
            "next_action": {
                "redirect_to_url": {
                    "return_url": "stripe://deeplink",
                    "url": "https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv"
                },
                "type": "redirect_to_url"
            },
            "on_behalf_of": null,
            "payment_method": "pm_1Ecaz6CRMbs6FrXfQs3wNGwB",
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "shipping": null,
            "source": null,
            "statement_descriptor": null,
            "status": "requires_action",
            "transfer_data": null,
            "transfer_group": null
        }
                """.trimIndent()
            )
        )!!
    }

    val PI_REQUIRES_PAYMENT_METHOD by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
                """.trimIndent()
            )
        )!!
    }

    val PI_WITH_LAST_PAYMENT_ERROR by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
            "object": "payment_intent",
            "amount": 1000,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
            "confirmation_method": "automatic",
            "created": 1565775850,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": {
                "code": "payment_intent_authentication_failure",
                "doc_url": "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
                "message": "The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.",
                "payment_method": {
                    "id": "pm_1F7J1bCRMbs6FrXfQKsYwO3U",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": null,
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": null,
                            "cvc_check": null
                        },
                        "country": null,
                        "exp_month": 8,
                        "exp_year": 2020,
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3220",
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1565775851,
                    "customer": null,
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "type": "invalid_request_error"
            },
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
                """.trimIndent()
            )
        )!!
    }

    val CANCELLED by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1FCpMECRMbs6FrXfVulorSf5",
            "object": "payment_intent",
            "amount": 4000,
            "amount_capturable": 0,
            "amount_received": 0,
            "application": null,
            "application_fee_amount": null,
            "canceled_at": 1567091866,
            "cancellation_reason": "abandoned",
            "capture_method": "automatic",
            "charges": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/charges?payment_intent=pi_1FCpMECRMbs6FrXfVulorSf5"
            },
            "client_secret": "pi_1FCpMECRMbs6FrXfVulorSf5_secret_oSppt5A",
            "confirmation_method": "manual",
            "created": 1567091778,
            "currency": "usd",
            "customer": "cus_FWhpaTLIPWLhpJ",
            "description": "Example PaymentIntent",
            "invoice": null,
            "last_payment_error": null,
            "livemode": false,
            "metadata": null,
            "next_action": null,
            "on_behalf_of": null,
            "payment_method": null,
            "payment_method_options": {
                "card": {
                    "request_three_d_secure": "automatic"
                }
            },
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "review": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": null,
                "name": "Fake Name",
                "phone": "(555) 555-5555",
                "tracking_number": null
            },
            "source": null,
            "statement_descriptor": null,
            "statement_descriptor_suffix": null,
            "status": "canceled",
            "transfer_data": null,
            "transfer_group": null
        }
                """.trimIndent()
            )
        )!!
    }

    val PAYMENT_INTENT_WITH_CANCELED_3DS1_SOURCE by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
            "id": "pi_1FeqH9CRMbs6FrXfcqqpoC2H",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "manual",
            "client_secret": "pi_1FeqH9CRMbs6FrXfcqqpoC2H_secret_DHYkuHTGq",
            "confirmation_method": "automatic",
            "created": 1573768491,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": {
                "message": "The PaymentMethod on this PaymentIntent was previously used without being attached to a Customer or was detached from a Customer, and may not be used again. You can try confirming again with a new PaymentMethod.",
                "payment_method": {
                    "id": "pm_1FeqHBCRMbs6FrXfsDE5NFJH",
                    "object": "payment_method",
                    "billing_details": {
                        "address": {
                            "city": null,
                            "country": null,
                            "line1": null,
                            "line2": null,
                            "postal_code": null,
                            "state": null
                        },
                        "email": null,
                        "name": null,
                        "phone": null
                    },
                    "card": {
                        "brand": "visa",
                        "checks": {
                            "address_line1_check": null,
                            "address_postal_code_check": null,
                            "cvc_check": null
                        },
                        "country": "US",
                        "exp_month": 1,
                        "exp_year": 2025,
                        "funding": "credit",
                        "generated_from": null,
                        "last4": "3063",
                        "three_d_secure_usage": {
                            "supported": true
                        },
                        "wallet": null
                    },
                    "created": 1573768493,
                    "customer": null,
                    "livemode": false,
                    "metadata": {},
                    "type": "card"
                },
                "type": "invalid_request_error"
            },
            "livemode": false,
            "next_action": null,
            "payment_method": null,
            "payment_method_types": [
                "card"
            ],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_payment_method"
        }
                """.trimIndent()
            )
        )
    }

    val PAYMENT_INTENT_WITH_CANCELED_3DS2_SOURCE by lazy {
        PARSER.parse(
            JSONObject(
                """
        {
          "id": "pi_3LGVHaAcAOtNkkj60zU6p2DQ",
          "object": "payment_intent",
          "amount": 5099,
          "amount_details": {
            "tip": {
            }
          },
          "automatic_payment_methods": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3LGVHaAcAOtNkkj60zU6p2DQ_secret_vun1jSWKn4aqRWGvpcB9mL03E",
          "confirmation_method": "automatic",
          "created": 1656625938,
          "currency": "usd",
          "description": null,
          "last_payment_error": {
            "code": "payment_intent_authentication_failure",
            "doc_url": "https://stripe.com/docs/error-codes/payment-intent-authentication-failure",
            "message": "The latest payment attempt of this PaymentIntent has failed or been canceled, and the attached payment method has been removed. You can try confirming again with a new payment method.",
            "payment_method": {
              "id": "pm_1LGVHaAcAOtNkkj6JyzQM2VM",
              "object": "payment_method",
              "billing_details": {
                "address": {
                  "city": null,
                  "country": "US",
                  "line1": null,
                  "line2": null,
                  "postal_code": "44444",
                  "state": null
                },
                "email": "testmode_2c4403676047567c93eaf3a1b61ac910_brnunes@stripe.com",
                "name": "",
                "phone": null
              },
              "card": {
                "brand": "visa",
                "checks": {
                  "address_line1_check": null,
                  "address_postal_code_check": null,
                  "cvc_check": null
                },
                "country": "IE",
                "exp_month": 3,
                "exp_year": 2033,
                "funding": "credit",
                "generated_from": null,
                "last4": "3220",
                "networks": {
                  "available": [
                    "visa"
                  ],
                  "preferred": null
                },
                "three_d_secure_usage": {
                  "supported": true
                },
                "wallet": null
              },
              "created": 1656625938,
              "customer": null,
              "livemode": false,
              "type": "card"
            },
            "type": "invalid_request_error"
          },
          "livemode": false,
          "next_action": null,
          "payment_method": null,
          "payment_method_types": [
            "card"
          ],
          "processing": null,
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": {
            "address": {
              "city": "San Francisco",
              "country": "US",
              "line1": "510 Townsend St",
              "line2": null,
              "postal_code": "94102",
              "state": "California"
            },
            "carrier": null,
            "name": "John Doe",
            "phone": null,
            "tracking_number": null
          },
          "source": null,
          "status": "requires_payment_method"
        }
                """.trimIndent()
            )
        )!!
    }

    val EXPANDED_PAYMENT_METHOD_JSON by lazy {
        JSONObject(
            """
        {
            "id": "pi_1GSTxJCRMbs",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1GSTxJCRMbs_secret_NqmhRfE9f",
            "confirmation_method": "automatic",
            "created": 1585599093,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "payment_method": {
                "id": "pm_1GSTxOCRMbs6FrXfYCosDqyr",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": null,
                        "country": null,
                        "line1": null,
                        "line2": null,
                        "postal_code": null,
                        "state": null
                    },
                    "email": null,
                    "name": null,
                    "phone": null
                },
                "card": {
                    "brand": "visa",
                    "checks": {
                        "address_line1_check": null,
                        "address_postal_code_check": null,
                        "cvc_check": null
                    },
                    "country": "IE",
                    "exp_month": 1,
                    "exp_year": 2025,
                    "funding": "credit",
                    "generated_from": null,
                    "last4": "3238",
                    "three_d_secure_usage": {
                        "supported": true
                    },
                    "wallet": null
                },
                "created": 1585599098,
                "customer": null,
                "livemode": false,
                "metadata": {},
                "type": "card"
            },
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": "Fedex",
                "name": "Jenny Rosen",
                "phone": null,
                "tracking_number": "12345"
            },
            "source": null,
            "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val PI_WITH_SHIPPING_JSON by lazy {
        JSONObject(
            """
        {
            "id": "pi_1GYda2CRMbs",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1GYda2CRMbs_secret_Z2zduomY0",
            "confirmation_method": "automatic",
            "created": 1587066058,
            "currency": "usd",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": null,
            "payment_method": {
                "id": "pm_1GYda7CRMbs6FrX",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": "San Francisco",
                        "country": "US",
                        "line1": "123 Market St",
                        "line2": "#345",
                        "postal_code": "94107",
                        "state": "CA"
                    },
                    "email": null,
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "card": {
                    "brand": "visa",
                    "checks": {
                        "address_line1_check": null,
                        "address_postal_code_check": null,
                        "cvc_check": null
                    },
                    "country": "US",
                    "exp_month": 12,
                    "exp_year": 2025,
                    "funding": "credit",
                    "generated_from": null,
                    "last4": "4242",
                    "three_d_secure_usage": {
                        "supported": true
                    },
                    "wallet": {
                        "dynamic_last4": "4242",
                        "google_pay": {},
                        "type": "google_pay"
                    }
                },
                "created": 1587066063,
                "customer": null,
                "livemode": false,
                "metadata": {},
                "type": "card"
            },
            "payment_method_types": ["card"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": {
                "address": {
                    "city": "San Francisco",
                    "country": "US",
                    "line1": "123 Market St",
                    "line2": "#345",
                    "postal_code": "94107",
                    "state": "CA"
                },
                "carrier": "UPS",
                "name": "Jenny Rosen",
                "phone": "1-800-555-1234",
                "tracking_number": "12345"
            },
            "source": null,
            "status": "succeeded"
        }
            """.trimIndent()
        )
    }

    val PI_WITH_SHIPPING by lazy {
        PARSER.parse(PI_WITH_SHIPPING_JSON)!!
    }
    val PI_OFF_SESSION by lazy {
        PARSER.parse(
            PI_WITH_SHIPPING_JSON
        )!!.copy(
            setupFutureUsage = StripeIntent.Usage.OffSession
        )
    }

    val AFTERPAY_REQUIRES_ACTION_JSON_STRING = """
        {
          "id": "pi_3LNLYmLu5o3P18Zp04dCLrZf",
          "object": "payment_intent",
          "amount": 5099,
          "amount_details": {
            "tip": {}
          },
          "automatic_payment_methods": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3LNLYmLu5o3P18Zp04dCLrZf_secret_hbuNP7A9J5qsyxOVdTSUwbOGa",
          "confirmation_method": "automatic",
          "created": 1658256860,
          "currency": "usd",
          "description": null,
          "last_payment_error": null,
          "livemode": false,
          "next_action": {
            "redirect_to_url": {
              "return_url": "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
              "url": "https://hooks.stripe.com/afterpay_clearpay/acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
            },
            "type": "redirect_to_url"
          },
          "payment_method": {
            "id": "pm_1LNLZQLu5o3P18ZpXBHpDiPF",
            "object": "payment_method",
            "afterpay_clearpay": {},
            "billing_details": {
              "address": {
                "city": "Blackrock",
                "country": "IE",
                "line1": "123 Main Street",
                "line2": "",
                "postal_code": "T37 F8HK",
                "state": "Co. Dublin"
              },
              "email": "email@email.com",
              "name": "Jenny Rosen",
              "phone": null
            },
            "created": 1658256900,
            "customer": null,
            "livemode": false,
            "type": "afterpay_clearpay"
          },
          "payment_method_options": {
            "us_bank_account": {
              "verification_method": "automatic"
            }
          },
          "payment_method_types": [
            "card",
            "afterpay_clearpay",
            "klarna",
            "us_bank_account",
            "affirm"
          ],
          "processing": null,
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": {
            "address": {
              "city": "San Francisco",
              "country": "US",
              "line1": "510 Townsend St",
              "line2": null,
              "postal_code": "94102",
              "state": "California"
            },
            "carrier": null,
            "name": "John Doe",
            "phone": null,
            "tracking_number": null
          },
          "source": null,
          "status": "requires_action"
        }
    """.trimIndent()

    val AFTERPAY_REQUIRES_ACTION_JSON by lazy {
        JSONObject(
            AFTERPAY_REQUIRES_ACTION_JSON_STRING
        )
    }
    val AFTERPAY_REQUIRES_ACTION by lazy {
        requireNotNull(PARSER.parse(AFTERPAY_REQUIRES_ACTION_JSON))
    }

    val AFTERPAY_REQUIRES_ACTION_JSON_NO_RETURN_URL by lazy {
        JSONObject(
            """
        {
          "id": "pi_3LNLYmLu5o3P18Zp04dCLrZf",
          "object": "payment_intent",
          "amount": 5099,
          "amount_details": {
            "tip": {}
          },
          "automatic_payment_methods": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3LNLYmLu5o3P18Zp04dCLrZf_secret_hbuNP7A9J5qsyxOVdTSUwbOGa",
          "confirmation_method": "automatic",
          "created": 1658256860,
          "currency": "usd",
          "description": null,
          "last_payment_error": null,
          "livemode": false,
          "next_action": {
            "redirect_to_url": {
              "url": "https://hooks.stripe.com/afterpay_clearpay/acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
            },
            "type": "redirect_to_url"
          },
          "payment_method": {
            "id": "pm_1LNLZQLu5o3P18ZpXBHpDiPF",
            "object": "payment_method",
            "afterpay_clearpay": {},
            "billing_details": {
              "address": {
                "city": "Blackrock",
                "country": "IE",
                "line1": "123 Main Street",
                "line2": "",
                "postal_code": "T37 F8HK",
                "state": "Co. Dublin"
              },
              "email": "email@email.com",
              "name": "Jenny Rosen",
              "phone": null
            },
            "created": 1658256900,
            "customer": null,
            "livemode": false,
            "type": "afterpay_clearpay"
          },
          "payment_method_options": {
            "us_bank_account": {
              "verification_method": "automatic"
            }
          },
          "payment_method_types": [
            "card",
            "afterpay_clearpay",
            "klarna",
            "us_bank_account",
            "affirm"
          ],
          "processing": null,
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": {
            "address": {
              "city": "San Francisco",
              "country": "US",
              "line1": "510 Townsend St",
              "line2": null,
              "postal_code": "94102",
              "state": "California"
            },
            "carrier": null,
            "name": "John Doe",
            "phone": null,
            "tracking_number": null
          },
          "source": null,
          "status": "requires_action"
        }
            """.trimIndent()
        )
    }
    val AFTERPAY_REQUIRES_ACTION_NO_RETURN_URL by lazy {
        requireNotNull(PARSER.parse(AFTERPAY_REQUIRES_ACTION_JSON_NO_RETURN_URL))
    }

    val OXXO_REQUIRES_ACTION_JSON by lazy {
        JSONObject(
            """
        {
            "id": "pi_1IcuwoL32KlRo",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IcuwoL32KlRo_secret_KC0YoHfna465TDVW",
            "confirmation_method": "automatic",
            "created": 1617638802,
            "currency": "mxn",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": {
                "oxxo_display_details": {
                    "expires_after": 1617944399,
                    "hosted_voucher_url": "https:\/\/payments.stripe.com\/oxxo\/voucher\/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb",
                    "number": "12345678901234657890123456789012"
                },
                "type": "oxxo_display_details"
            },
            "payment_method": {
                "id": "pm_1IcuwoL32KlRot01",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": null,
                        "country": null,
                        "line1": null,
                        "line2": null,
                        "postal_code": null,
                        "state": null
                    },
                    "email": "jrosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "created": 1617638802,
                "customer": null,
                "livemode": false,
                "oxxo": {},
                "type": "oxxo"
            },
            "payment_method_types": ["card", "oxxo"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val OXXO_REQUIRES_ACTION by lazy {
        requireNotNull(PARSER.parse(OXXO_REQUIRES_ACTION_JSON))
    }

    private val CASH_APP_PAY_REQUIRES_ACTION_JSON by lazy {
        JSONObject(
            """
            {
              "id": "pi_3MRdj6Lu5o3P18Zp0LlnrKVz",
              "object": "payment_intent",
              "amount": 100,
              "amount_details": {
                "tip": {
                }
              },
              "automatic_payment_methods": null,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "client_secret": "pi_3MRdj6Lu5o3P18Zp0LlnrKVz_secret_OcTe88IFTH9VkFQKLtfX4vV1a",
              "confirmation_method": "automatic",
              "created": 1674056340,
              "currency": "usd",
              "description": "Example PaymentIntent",
              "last_payment_error": null,
              "livemode": false,
              "next_action": {
                "cashapp_handle_redirect_or_display_qr_code": {
                  "hosted_instructions_url": "https://payments.stripe.com/cashapp/instructions/CCUaFwoVYWNjdF8xSHZUSTdMdTVvM1AxOFpwKJaloJ4GMgZcsN7zB3I6L2wvyPfW8B6gy0_BsHb7Q21FYoKjIGxNvVsVYjJ6pbAIw_28VE2MVWcJQHMaEObM",
                  "mobile_auth_url": "https://pm-redirects.stripe.com/authorize/acct_1HvTI7Lu5o3P18Zp/pa_nonce_NC1mezV544wpYmFaXyJpnleeurKO3TZ",
                  "qr_code": {
                    "expires_at": 1674056362,
                    "image_url_png": "https://qr.stripe.com/test_YWNjdF8xSHZUSTdMdTVvM1AxOFpwLF9OQzFtSUpKYkMwaVBZTldRMW5BelF0OWNiWkk3a25o0100dYmSjOJt.png",
                    "image_url_svg": "https://qr.stripe.com/test_YWNjdF8xSHZUSTdMdTVvM1AxOFpwLF9OQzFtSUpKYkMwaVBZTldRMW5BelF0OWNiWkk3a25o0100dYmSjOJt.svg"
                  }
                },
                "type": "cashapp_handle_redirect_or_display_qr_code"
              },
              "payment_method": "pm_1MRdj7Lu5o3P18Zp41wd191i",
              "payment_method_types": [
                "cashapp"
              ],
              "processing": null,
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": null,
              "source": null,
              "status": "requires_action"
            }
            """.trimIndent()
        )
    }

    val CASH_APP_PAY_REQUIRES_ACTION by lazy {
        requireNotNull(PARSER.parse(CASH_APP_PAY_REQUIRES_ACTION_JSON))
    }

    val LLAMAPAY_REQUIRES_ACTION_JSON by lazy {
        JSONObject(

            """
        {
            "id": "pi_1IcuwoL32KlRo",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IcuwoL32KlRo_secret_KC0YoHfna465TDVW",
            "confirmation_method": "automatic",
            "created": 1617638802,
            "currency": "mxn",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
              "next_action": {
                "llamapay_redirect_to_url": {
                  "return_url": "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
                  "url": "https://hooks.stripe.com/llamapay/acct_1HvTI7Lu5o3P18Zp/pa_nonce_M5WcnAEWqB7mMANvtyWuxOWAXIHw9T9/redirect"
                },
                "type": "redirect_to_url"
              },
            "payment_method": {
                "id": "pm_1IcuwoL32KlRot01",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": null,
                        "country": null,
                        "line1": null,
                        "line2": null,
                        "postal_code": null,
                        "state": null
                    },
                    "email": "jrosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "created": 1617638802,
                "customer": null,
                "livemode": false,
                "konbini": {},
                "type": "llamapay"
            },
            "payment_method_types": ["card", "llamapay"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val KONBINI_REQUIRES_ACTION_JSON by lazy {
        JSONObject(

            """
        {
            "id": "pi_1IcuwoL32KlRo",
            "object": "payment_intent",
            "amount": 1099,
            "canceled_at": null,
            "cancellation_reason": null,
            "capture_method": "automatic",
            "client_secret": "pi_1IcuwoL32KlRo_secret_KC0YoHfna465TDVW",
            "confirmation_method": "automatic",
            "created": 1617638802,
            "currency": "mxn",
            "description": "Example PaymentIntent",
            "last_payment_error": null,
            "livemode": false,
            "next_action": {
                "konbini_display_details": {
                    "expires_after": 1617944399,
                    "hosted_voucher_url": "https:\/\/payments.stripe.com\/konbini\/voucher\/test_YWNjdF8xSWN1c1VMMzJLbFJvdDAxLF9KRlBtckVBMERWM0lBZEUyb",
                    "number": "12345678901234657890123456789012"
                },
                "type": "konbini_display_details"
            },
            "payment_method": {
                "id": "pm_1IcuwoL32KlRot01",
                "object": "payment_method",
                "billing_details": {
                    "address": {
                        "city": null,
                        "country": null,
                        "line1": null,
                        "line2": null,
                        "postal_code": null,
                        "state": null
                    },
                    "email": "jrosen@example.com",
                    "name": "Jenny Rosen",
                    "phone": null
                },
                "created": 1617638802,
                "customer": null,
                "livemode": false,
                "konbini": {},
                "type": "konbini"
            },
            "payment_method_types": ["card", "konbini"],
            "receipt_email": null,
            "setup_future_usage": null,
            "shipping": null,
            "source": null,
            "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val KONBINI_REQUIRES_ACTION by lazy {
        requireNotNull(PARSER.parse(KONBINI_REQUIRES_ACTION_JSON))
    }

    val ALIPAY_REQUIRES_ACTION_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_1HDEFVKlwPmebFhpCobFP55H",
          "object": "payment_intent",
          "amount": 100,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_1HDEFVKlwPmebFhpCobFP55H_secret_XW8sADccCxtusewAwn5z9kAiw",
          "confirmation_method": "automatic",
          "created": 1596740133,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "alipay_handle_redirect": {
              "native_data": "_input_charset=utf-8&app_pay=Y&currency=USD&forex_biz=FP&notify_url=https%3A%2F%2Fhooks.stripe.com%2Falipay%2Falipay%2Fhook%2F6255d30b067c8f7a162c79c654483646%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T&out_trade_no=src_1HDEFWKlwPmebFhp6tcpln8T&partner=2088621828244481&payment_type=1&product_code=NEW_WAP_OVERSEAS_SELLER&return_url=https%3A%2F%2Fhooks.stripe.com%2Fadapter%2Falipay%2Fredirect%2Fcomplete%2Fsrc_1HDEFWKlwPmebFhp6tcpln8T%2Fsrc_client_secret_S6H9mVMKK6qxk9YxsUvbH55K&secondary_merchant_id=acct_1EqOyCKlwPmebFhp&secondary_merchant_industry=5734&secondary_merchant_name=Yuki-Test&sendFormat=normal&service=create_forex_trade_wap&sign=b691876a7f0bd889530f54a271d314d5&sign_type=MD5&subject=Yuki-Test&supplier=Yuki-Test&timeout_rule=20m&total_fee=1.00",
              "native_url": null,
              "return_url": "example://return_url",
              "url": "https://hooks.stripe.com/redirect/authenticate/src_1HDEFWKlwPmebFhp6tcpln8T?client_secret=src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K"
            },
            "type": "alipay_handle_redirect"
          },
          "payment_method": {
            "id": "pm_1HDEFVKlwPmebFhpKYYkSm8H",
            "object": "payment_method",
            "alipay": {},
            "billing_details": {
              "address": {
                "city": null,
                "country": null,
                "line1": null,
                "line2": null,
                "postal_code": null,
                "state": null
              },
              "email": null,
              "name": null,
              "phone": null
            },
            "created": 1596740133,
            "customer": null,
            "livemode": true,
            "type": "alipay"
          },
          "payment_method_types": [
            "alipay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val ALIPAY_REQUIRES_ACTION by lazy {
        PARSER.parse(ALIPAY_REQUIRES_ACTION_JSON)!!
    }

    val ALIPAY_TEST_MODE_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_1HDEFVKlwPmebFhpCobFP55H",
          "object": "payment_intent",
          "amount": 100,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_1HDEFVKlwPmebFhpCobFP55H_secret_XW8sADccCxtusewAwn5z9kAiw",
          "confirmation_method": "automatic",
          "created": 1596740133,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "alipay_handle_redirect": {
              "native_data": null,
              "native_url": null,
              "return_url": "example://return_url",
              "url": "https://hooks.stripe.com/redirect/authenticate/src_1HDEFWKlwPmebFhp6tcpln8T?client_secret=src_client_secret_S6H9mVMKK6qxk9YxsUvbH55K"
            },
            "type": "alipay_handle_redirect"
          },
          "payment_method": {
            "id": "pm_1HDEFVKlwPmebFhpKYYkSm8H",
            "object": "payment_method",
            "alipay": {},
            "billing_details": {
              "address": {
                "city": null,
                "country": null,
                "line1": null,
                "line2": null,
                "postal_code": null,
                "state": null
              },
              "email": null,
              "name": null,
              "phone": null
            },
            "created": 1596740133,
            "customer": null,
            "livemode": false,
            "type": "alipay"
          },
          "payment_method_types": [
            "alipay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val ALIPAY_TEST_MODE by lazy {
        PARSER.parse(ALIPAY_TEST_MODE_JSON)!!
    }

    val PI_REQUIRES_BLIK_AUTHORIZE_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_1IVmwXFY0qyl6XeWwxGWA04D",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "amount_subtotal": 1099,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IVmwXFY0qyl6XeWwxGWA04D"
          },
          "client_secret": "pi_1IVmwXFY0qyl6XeWwxGWA04D_secret_4U8cSCdPefr8LHtPsKvA3mcQz",
          "confirmation_method": "automatic",
          "created": 1615939737,
          "currency": "pln",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "blik_authorize"
          },
          "on_behalf_of": null,
          "payment_method": "pm_1IVnI3FY0qyl6XeWxJFdBh2g",
          "payment_method_options": {
            "blik": {
            }
          },
          "payment_method_types": [
            "blik"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "total_details": {
            "amount_discount": 0,
            "amount_tax": 0
          },
          "transfer_data": null,
          "transfer_group": null
        }
            """.trimIndent()
        )
    }

    val PI_REQUIRES_BLIK_AUTHORIZE by lazy {
        PARSER.parse(PI_REQUIRES_BLIK_AUTHORIZE_JSON)!!
    }

    private val PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_1IlJH7BNJ02ErVOjm37T3OUt",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_1IlJH7BNJ02ErVOjm37T3OUt"
          },
          "client_secret": "pi_1IlJH7BNJ02ErVOjm37T3OUt_secret_vgMExmjvESdtPqddHOSSSDip2",
          "confirmation_method": "automatic",
          "created": 1619638941,
          "currency": "usd",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "some_random_string",
              "package": "Sign=WXPay",
              "partner_id": "wx65997d6307c3827d",
              "prepay_id": "test_transaction",
              "sign": "8B26124BABC816D7140034DDDC7D3B2F1036CCB2D910E52592687F6A44790D5E",
              "timestamp": "1619638941"
            }
          },
          "on_behalf_of": null,
          "payment_method": {
            "id": "pm_1IlJH7BNJ02ErVOjxKQu1wfH",
            "object": "payment_method",
            "billing_details": {
              "address": {
                "city": null,
                "country": null,
                "line1": null,
                "line2": null,
                "postal_code": null,
                "state": null
              },
              "email": null,
              "name": null,
              "phone": null
            },
            "created": 1634242405,
            "customer": null,
            "livemode": false,
            "type": "wechat_pay",
            "wechat_pay": {
            }
          },
          "payment_method_options": {
            "wechat_pay": {
            }
          },
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_action",
          "transfer_data": null,
          "transfer_group": null
        }
            """.trimIndent()
        )
    }

    val PI_REQUIRES_WECHAT_PAY_AUTHORIZE by lazy {
        PARSER.parse(PI_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON)!!
    }

    /**
     * A sample response of PI from refresh endpoint that has status 'requires_action'
     */
    val PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ",
          "object": "payment_intent",
          "allowed_source_types": [
            "wechat_pay"
          ],
          "amount": 50,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq",
          "confirmation_method": "automatic",
          "created": 1634152658,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "next_source_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "payment_method": "pm_1JkEhjBNJ02ErVOjUZ3ekLUL",
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "requires_action"
        }
            """.trimIndent()
        )
    }

    val PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE by lazy {
        PARSER.parse(PI_REFRESH_RESPONSE_REQUIRES_WECHAT_PAY_AUTHORIZE_JSON)!!
    }

    /**
     * A sample response of PI from refresh endpoint that has status 'succeeded'
     */
    val PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ",
          "object": "payment_intent",
          "allowed_source_types": [
            "wechat_pay"
          ],
          "amount": 50,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "client_secret": "pi_3JkCxKBNJ02ErVOj0kNqBMAZ_secret_bC6oXqo976LFM06Z9rlhmzUQq",
          "confirmation_method": "automatic",
          "created": 1634152658,
          "currency": "usd",
          "description": "Example PaymentIntent",
          "last_payment_error": null,
          "livemode": true,
          "next_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "next_source_action": {
            "type": "wechat_pay_redirect_to_android_app",
            "wechat_pay_redirect_to_android_app": {
              "app_id": "wx65997d6307c3827d",
              "nonce_str": "osR3zctjDetBvsBN",
              "package": "Sign=WXPay",
              "partner_id": "268716457",
              "prepay_id": "wx140509412586868522820b12f205690000",
              "sign": "7A5981D3347941F4F9AECACF36E467EAE4B7734E47CE4B058E8B2299DAF19E8A",
              "timestamp": "1634159381"
            }
          },
          "payment_method": "pm_1JkEhjBNJ02ErVOjUZ3ekLUL",
          "payment_method_types": [
            "wechat_pay"
          ],
          "receipt_email": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "status": "succeeded"
        }
            """.trimIndent()
        )
    }

    val PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS by lazy {
        PARSER.parse(PI_REFRESH_RESPONSE_WECHAT_PAY_SUCCESS_JSON)!!
    }

    val PI_WITH_KLARNA_IN_PAYMENT_METHODS_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_3JoznDLu5o3P18Zp0lRzng7p",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "application": null,
          "application_fee_amount": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [

            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_3JoznDLu5o3P18Zp0lRzng7p"
          },
          "client_secret": "pi_3JoznDLu5o3P18Zp0lRzng7p_secret_2wQkxNRVYQOzBcG6XGbw2tyBv",
          "confirmation_method": "automatic",
          "created": 1635293699,
          "currency": "eur",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": null,
          "on_behalf_of": null,
          "payment_method": null,
          "payment_method_options": {
            "klarna": {
              "preferred_locale": null
            }
          },
          "payment_method_types": [
            "klarna"
          ],
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": null,
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_payment_method",
          "transfer_data": null,
          "transfer_group": null
        }
            """.trimIndent()
        )
    }

    val PI_WITH_KLARNA_IN_PAYMENT_METHODS by lazy {
        PARSER.parse(PI_WITH_KLARNA_IN_PAYMENT_METHODS_JSON)!!
    }

    val PI_WITH_AFFIRM_IN_PAYMENT_METHODS_JSON by lazy {
        JSONObject(
            """
        {
          "id": "pi_3KTyNsLu5o3P18Zp1aPD6iIX",
          "object": "payment_intent",
          "amount": 1099,
          "amount_capturable": 0,
          "amount_received": 0,
          "application": null,
          "application_fee_amount": null,
          "automatic_payment_methods": null,
          "canceled_at": null,
          "cancellation_reason": null,
          "capture_method": "automatic",
          "charges": {
            "object": "list",
            "data": [
        
            ],
            "has_more": false,
            "total_count": 0,
            "url": "/v1/charges?payment_intent=pi_3KTyNsLu5o3P18Zp1aPD6iIX"
          },
          "client_secret": "pi_3KTyNsLu5o3P18Zp1aPD6iIX_secret_jDzWndA5ybPCzqBKJTf6Hn6Zp",
          "confirmation_method": "automatic",
          "created": 1645059732,
          "currency": "usd",
          "customer": null,
          "description": null,
          "invoice": null,
          "last_payment_error": null,
          "livemode": false,
          "metadata": {
          },
          "next_action": null,
          "on_behalf_of": null,
          "payment_method": null,
          "payment_method_options": {
          },
          "payment_method_types": [
            "affirm"
          ],
          "processing": null,
          "receipt_email": null,
          "review": null,
          "setup_future_usage": null,
          "shipping": {
            "address": {
              "city": "San Francisco",
              "country": "US",
              "line1": "1234 Main Street",
              "line2": null,
              "postal_code": "94111",
              "state": "CA"
            },
            "carrier": null,
            "name": "Jenny Rosen",
            "phone": null,
            "tracking_number": null
          },
          "source": null,
          "statement_descriptor": null,
          "statement_descriptor_suffix": null,
          "status": "requires_payment_method",
          "transfer_data": null,
          "transfer_group": null
        }
            """.trimIndent()
        )
    }

    val PI_WITH_AFFIRM_IN_PAYMENT_METHODS by lazy {
        PARSER.parse(PI_WITH_AFFIRM_IN_PAYMENT_METHODS_JSON)!!
    }
    val PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS_JSON by lazy {
        JSONObject(
            """
            {
              "id": "pi_3KcDp1FnkumiFUFx1d5DwGIq",
              "object": "payment_intent",
              "client_secret": "pi_3KcDp1FnkumiFUFx1d5DwGIq_secret_hdutSWBUMuc8ON6jXPgyCsPba",
              "last_payment_error": null,
              "livemode": false,
              "next_action": {
                "type": "verify_with_microdeposits",
                "verify_with_microdeposits": {
                  "arrival_date": 1647241200,
                  "hosted_verification_url": "https://payments.stripe.com/microdeposit/pacs_test_YWNjdF8xS2J1SjlGbmt1bWlGVUZ4LHBhX25vbmNlX0xJcFVEaERaU0JOVVR3akhxMXc5eklOQkl3UTlwNWo0000v3GS1Jej",
                  "microdeposit_type": "amounts"
                }
              },
              "status": "requires_action",
              "amount": 6099,
              "automatic_payment_methods": null,
              "canceled_at": null,
              "cancellation_reason": null,
              "capture_method": "automatic",
              "confirmation_method": "automatic",
              "created": 1647025699,
              "currency": "usd",
              "description": "Example PaymentIntent",
              "payment_method": {
                "id": "pm_1KcDp2FnkumiFUFxkQjAzVS8",
                "object": "payment_method",
                "billing_details": {
                  "address": {
                    "city": null,
                    "country": null,
                    "line1": null,
                    "line2": null,
                    "postal_code": null,
                    "state": null
                  },
                  "email": "johnny@lawrence.com",
                  "name": "Johnny Lawrence",
                  "phone": null
                },
                "created": 1647025700,
                "customer": null,
                "livemode": false,
                "type": "us_bank_account",
                "us_bank_account": {
                  "account_holder_type": "individual",
                  "account_type": "checking",
                  "bank_name": "STRIPE TEST BANK",
                  "fingerprint": "8Rm3lfhEbg8vMEmj",
                  "last4": "6789",
                  "networks": {
                    "preferred": "ach",
                    "supported": [
                      "ach"
                    ]
                  },
                  "routing_number": "110000000"
                }
              },
              "payment_method_options": {
                "us_bank_account": {
                  "verification_method": "automatic"
                }
              },
              "payment_method_types": [
                "us_bank_account"
              ],
              "processing": null,
              "receipt_email": null,
              "setup_future_usage": null,
              "shipping": null,
              "source": null
            }
            """.trimIndent()
        )
    }

    val PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS by lazy {
        PARSER.parse(PI_WITH_US_BANK_ACCOUNT_IN_PAYMENT_METHODS_JSON)!!
    }

    val PI_LINK_ACCOUNT_SESSION_JSON by lazy {
        JSONObject(
            """
            {
              "client_secret": "test_client_secret",
              "id": "random_id"
            }
            """.trimIndent()
        )
    }

    val SI_LINK_ACCOUNT_SESSION_JSON by lazy {
        JSONObject(
            """
            {
              "client_secret": "test_client_secret",
              "id": "random_id"
            }
            """.trimIndent()
        )
    }

    val PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON by lazy {
        JSONObject(
            """
            {
                "id": "pi_3KbV27Lu5o3P18Zp1e7NOonG",
                "object": "payment_intent",
                "client_secret": "pi_3KbV27Lu5o3P18Zp1e7NOonG_secret_9PqgvdhyLNub0UvzjFf9bQKqd",
                "last_payment_error": null,
                "livemode": false,
                "next_action": null,
                "status": "processing",
                "amount": 6099,
                "automatic_payment_methods": null,
                "canceled_at": null,
                "cancellation_reason": null,
                "capture_method": "automatic",
                "confirmation_method": "automatic",
                "created": 1646853531,
                "currency": "usd",
                "description": "Example PaymentIntent",
                "payment_method": "pm_1KbV27Lu5o3P18ZpAEIGorq8",
                "payment_method_options": { },
                "payment_method_types": [
                    "us_bank_account"
                ],
                "processing": null,
                "receipt_email": null,
                "setup_future_usage": "off_session",
                "shipping": null,
                "source": null
            }
            """.trimIndent()
        )
    }

    val PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED by lazy {
        PARSER.parse(PI_WITH_US_BANK_ACCOUNT_VERIFY_COMPLETED_JSON)!!
    }

    val PI_WITH_LINK_FUNDING_SOURCES_JSON by lazy {
        JSONObject(
            """
            {
                "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
                "object": "payment_intent",
                "amount": 1099,
                "canceled_at": null,
                "cancellation_reason": null,
                "capture_method": "manual",
                "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
                "confirmation_method": "automatic",
                "created": 1565775850,
                "currency": "usd",
                "description": "Example PaymentIntent",
                "livemode": false,
                "next_action": null,
                "payment_method": null,
                "payment_method_types": [
                    "card"
                ],
                "receipt_email": null,
                "setup_future_usage": null,
                "shipping": null,
                "source": null,
                "status": "requires_payment_method",
                "link_funding_sources": [
                  "CARD", "BANK_ACCOUNT"
                ]
            }
            """.trimIndent()
        )
    }

    val PI_WITH_LINK_FUNDING_SOURCES by lazy {
        PARSER.parse(PI_WITH_LINK_FUNDING_SOURCES_JSON)!!
    }

    private val PI_WITH_COUNTRY_CODE_JSON by lazy {
        JSONObject(
            """
            {
                "id": "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
                "object": "payment_intent",
                "amount": 1099,
                "canceled_at": null,
                "cancellation_reason": null,
                "capture_method": "manual",
                "client_secret": "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
                "confirmation_method": "automatic",
                "created": 1565775850,
                "currency": "usd",
                "description": "Example PaymentIntent",
                "livemode": false,
                "next_action": null,
                "payment_method": null,
                "payment_method_types": [
                    "card"
                ],
                "receipt_email": null,
                "setup_future_usage": null,
                "shipping": null,
                "source": null,
                "status": "requires_payment_method",
                "link_funding_sources": [
                  "CARD", "BANK_ACCOUNT"
                ],
                "country_code": "US"
            }
            """.trimIndent()
        )
    }

    internal val PI_WITH_COUNTRY_CODE by lazy {
        PARSER.parse(PI_WITH_COUNTRY_CODE_JSON)!!
    }
}
