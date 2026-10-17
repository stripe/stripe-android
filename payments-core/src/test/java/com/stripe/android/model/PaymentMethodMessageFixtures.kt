package com.stripe.android.model

import org.json.JSONObject

@Suppress("LargeClass")
internal object PaymentMethodMessageFixtures {
    val NO_CONTENT_JSON = JSONObject(
        """
        {
            "country" : "US",
            "merchant_id" : "acct_1HvTI7Lu5o3P18Zp",
            "payment_methods" : [ ],
            "partner_configs" : { },
            "payment_plan_groups" : [ ],
            "api_feature_flags" : [ {
                "key" : "enable_pmme_api_content",
                "result" : true
            } ],
            "experiments" : {
                "experiment_assignments" : { },
                "event_id" : ""
            },
            "content" : {
                "images" : [ ],
                "inline_partner_promotion" : null,
                "learn_more" : {
                    "message" : "Learn more",
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=0&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en"
                },
                "promotion" : null,
                "summary" : {
                    "message" : "Buy now pay later",
                    "url" : null
                }
            }
        }
        """.trimIndent()
    )

    val SINGLE_PARTNER_JSON = JSONObject(
        """
            {
              "country": "US",
              "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
              "payment_methods": [
                "klarna"
              ],
              "partner_configs": {},
              "payment_plan_groups": [
                {
                  "content": {
                    "images": [
                      {
                        "dark_theme_png": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.png",
                          "width": 78
                        },
                        "dark_theme_svg": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.svg",
                          "width": 78
                        },
                        "flat_theme_png": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.png",
                          "width": 78
                        },
                        "flat_theme_svg": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.svg",
                          "width": 78
                        },
                        "light_theme_png": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.png",
                          "width": 98
                        },
                        "light_theme_svg": {
                          "height": 40,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.svg",
                          "width": 98
                        },
                        "payment_method_type": "klarna",
                        "role": "logo",
                        "text": "Klarna"
                      },
                      {
                        "dark_theme_png": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                          "width": 16
                        },
                        "dark_theme_svg": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                          "width": 16
                        },
                        "flat_theme_png": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.png",
                          "width": 16
                        },
                        "flat_theme_svg": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.svg",
                          "width": 16
                        },
                        "light_theme_png": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                          "width": 16
                        },
                        "light_theme_svg": {
                          "height": 16,
                          "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                          "width": 16
                        },
                        "payment_method_type": "klarna",
                        "role": "icon",
                        "text": "Klarna"
                      }
                    ],
                    "inline_partner_promotion": {
                      "message": "4 interest-free payments of ${'$'}25.00 with {partner}",
                      "url": null
                    },
                    "learn_more": {
                      "message": "Learn more",
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=10000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=klarna"
                    },
                    "promotion": {
                      "message": "4 interest-free payments of ${'$'}25.00",
                      "url": null
                    },
                    "summary": {
                      "message": "Pay now, or in 4 interest-free payments of ${'$'}25.00.",
                      "url": null
                    }
                  },
                  "payment_plans": [
                    {
                      "id": "106bbe62-f77d-4ee6-85b2-bbbc071c4a0a",
                      "due_days": null,
                      "installment_amount": 2500,
                      "interest_rate": 0,
                      "interval": {
                        "frequency": 2,
                        "unit": "week"
                      },
                      "number_of_installments": 4,
                      "terms": "Pay in 4 is offered by Klarna, Inc. It’s available to eligible US residents in most states. Initial payments may be higher. Missed payments are subject to late fees. For CA residents, loans made or arranged by Klarna, Inc. pursuant to a California Financing Law license. [Review the Pay in 4 terms](https://cdn.klarna.com/1.0/shared/content/legal/terms/0/en_us/sliceitinx).",
                      "total_amount": 10000,
                      "type": "INSTALLMENTS"
                    },
                    {
                      "id": "f95acff9-0473-40db-982a-d81284e67438",
                      "due_days": null,
                      "installment_amount": null,
                      "interest_rate": 0,
                      "interval": null,
                      "number_of_installments": null,
                      "terms": "[Terms](https://www.klarna.com/us/legal/).",
                      "total_amount": 10000,
                      "type": "PAY_NOW"
                    },
                    {
                      "id": "4af3d10b-22b8-494c-ace3-97ce2b479253",
                      "due_days": null,
                      "installment_amount": null,
                      "interest_rate": 0,
                      "interval": null,
                      "number_of_installments": null,
                      "terms": "[Terms](https://www.klarna.com/us/legal/).",
                      "total_amount": 10000,
                      "type": "PAY_NOW"
                    }
                  ],
                  "type": "KLARNA"
                }
              ],
              "api_feature_flags": [
                {
                  "key": "enable_pmme_api_content",
                  "result": true
                }
              ],
              "experiments": {
                "experiment_assignments": {},
                "event_id": ""
              },
              "content": {
                "images": [
                  {
                    "dark_theme_png": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.png",
                      "width": 78
                    },
                    "dark_theme_svg": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.svg",
                      "width": 78
                    },
                    "flat_theme_png": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.png",
                      "width": 78
                    },
                    "flat_theme_svg": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.svg",
                      "width": 78
                    },
                    "light_theme_png": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.png",
                      "width": 98
                    },
                    "light_theme_svg": {
                      "height": 40,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.svg",
                      "width": 98
                    },
                    "payment_method_type": "klarna",
                    "role": "logo",
                    "text": "Klarna"
                  },
                  {
                    "dark_theme_png": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                      "width": 16
                    },
                    "dark_theme_svg": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                      "width": 16
                    },
                    "flat_theme_png": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.png",
                      "width": 16
                    },
                    "flat_theme_svg": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.svg",
                      "width": 16
                    },
                    "light_theme_png": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                      "width": 16
                    },
                    "light_theme_svg": {
                      "height": 16,
                      "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                      "width": 16
                    },
                    "payment_method_type": "klarna",
                    "role": "icon",
                    "text": "Klarna"
                  }
                ],
                "inline_partner_promotion": null,
                "learn_more": {
                  "message": "Learn more",
                  "url": "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=10000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=klarna"
                },
                "promotion": {
                  "message": "4 interest-free payments of ${'$'}25.00",
                  "url": null
                },
                "summary": {
                  "message": "Pay now, or in 4 interest-free payments of ${'$'}25.00.",
                  "url": null
                }
              }
            }
        """.trimIndent()
    )

    val MULTI_PARTNER_JSON = JSONObject(
        """
            {
              "country" : "US",
              "merchant_id" : "acct_1HvTI7Lu5o3P18Zp",
              "payment_methods" : [ "affirm", "klarna", "afterpay_clearpay" ],
              "partner_configs" : {
                "affirm" : {
                  "summary" : {
                    "public_key" : "JHTPBPHS018OII2S"
                  }
                }
              },
              "payment_plan_groups" : [ {
                "content" : {
                  "images" : [ {
                    "dark_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-dark.png",
                      "width" : 144
                    },
                    "dark_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-dark.svg",
                      "width" : 144
                    },
                    "flat_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-flat.png",
                      "width" : 144
                    },
                    "flat_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-flat.svg",
                      "width" : 144
                    },
                    "light_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo.png",
                      "width" : 144
                    },
                    "light_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo.svg",
                      "width" : 144
                    },
                    "payment_method_type" : "afterpay_clearpay",
                    "role" : "logo",
                    "text" : "Cash App Afterpay"
                  }, {
                    "dark_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.png",
                      "width" : 16
                    },
                    "dark_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.svg",
                      "width" : 16
                    },
                    "flat_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon-flat.png",
                      "width" : 16
                    },
                    "flat_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon-flat.svg",
                      "width" : 16
                    },
                    "light_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.png",
                      "width" : 16
                    },
                    "light_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.svg",
                      "width" : 16
                    },
                    "payment_method_type" : "afterpay_clearpay",
                    "role" : "icon",
                    "text" : "Cash App Afterpay"
                  } ],
                  "inline_partner_promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50 with {partner}",
                    "url" : null
                  },
                  "learn_more" : {
                    "message" : "Learn more",
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=afterpay_clearpay"
                  },
                  "promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50",
                    "url" : null
                  },
                  "summary" : {
                    "message" : "Pay in 4 interest-free payments of ${'$'}22.50.",
                    "url" : null
                  }
                },
                "payment_plans" : [ {
                  "id" : "9e4ebba3-60b0-40f0-9778-f23e90ee7e99",
                  "due_days" : null,
                  "installment_amount" : 2250,
                  "interest_rate" : 0,
                  "interval" : {
                    "frequency" : 2,
                    "unit" : "week"
                  },
                  "number_of_installments" : 4,
                  "terms" : "For Cash App Afterpay in 4 users, you must be over 18, a resident of the U.S. and meet additional eligibility criteria to qualify. Late fees may apply. Estimated payment amounts shown on product pages exclude taxes and shipping charges, which are added at checkout. Click [here](https://www.afterpay.com/en-US/installment-agreement) for complete terms. Loans to California residents made or arranged pursuant to a California Finance Lenders Law license.",
                  "total_amount" : 9000,
                  "type" : "INSTALLMENTS"
                } ],
                "type" : "AFTERPAY_CLEARPAY"
              }, {
                "content" : {
                  "images" : [ {
                    "dark_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-dark.png",
                      "width" : 75
                    },
                    "dark_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-dark.svg",
                      "width" : 75
                    },
                    "flat_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-flat.png",
                      "width" : 75
                    },
                    "flat_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-flat.svg",
                      "width" : 75
                    },
                    "light_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo.png",
                      "width" : 75
                    },
                    "light_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo.svg",
                      "width" : 75
                    },
                    "payment_method_type" : "affirm",
                    "role" : "logo",
                    "text" : "Affirm"
                  }, {
                    "dark_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-dark.png",
                      "width" : 16
                    },
                    "dark_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-dark.svg",
                      "width" : 16
                    },
                    "flat_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-flat.png",
                      "width" : 16
                    },
                    "flat_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-flat.svg",
                      "width" : 16
                    },
                    "light_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon.png",
                      "width" : 16
                    },
                    "light_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon.svg",
                      "width" : 16
                    },
                    "payment_method_type" : "affirm",
                    "role" : "icon",
                    "text" : "Affirm"
                  } ],
                  "inline_partner_promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50 with {partner}",
                    "url" : null
                  },
                  "learn_more" : {
                    "message" : "Learn more",
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=affirm"
                  },
                  "promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50",
                    "url" : null
                  },
                  "summary" : {
                    "message" : "Pay in 4 interest-free payments of ${'$'}22.50.",
                    "url" : null
                  }
                },
                "payment_plans" : [ {
                  "id" : "e2864a04-dfc5-4646-80c6-04f5a3b20e60",
                  "due_days" : null,
                  "installment_amount" : 2250,
                  "interest_rate" : 0,
                  "interval" : {
                    "frequency" : 2,
                    "unit" : "week"
                  },
                  "number_of_installments" : 4,
                  "terms" : "Rates from 0-36% APR. Payment options may be subject to an eligibility check and may not be available in all states. While Affirm doesn’t charge late fees, other payment methods may do so. Options depend on your purchase amount, and a down payment may be required. Estimated payment amounts exclude taxes and shipping charges, which are added at checkout. Loans to California residents made or arranged pursuant to a California Finance Lenders Law license. Financing options through Affirm are provided by these lending partners: affirm.com/lenders. CA residents: Loans by Affirm Loan Services, LLC are made or arranged pursuant to a California Finance Lender license. For licenses and disclosures, see affirm.com/licenses.",
                  "total_amount" : 9000,
                  "type" : "INSTALLMENTS"
                } ],
                "type" : "AFFIRM"
              }, {
                "content" : {
                  "images" : [ {
                    "dark_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.png",
                      "width" : 78
                    },
                    "dark_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.svg",
                      "width" : 78
                    },
                    "flat_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.png",
                      "width" : 78
                    },
                    "flat_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.svg",
                      "width" : 78
                    },
                    "light_theme_png" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.png",
                      "width" : 98
                    },
                    "light_theme_svg" : {
                      "height" : 40,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.svg",
                      "width" : 98
                    },
                    "payment_method_type" : "klarna",
                    "role" : "logo",
                    "text" : "Klarna"
                  }, {
                    "dark_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                      "width" : 16
                    },
                    "dark_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                      "width" : 16
                    },
                    "flat_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.png",
                      "width" : 16
                    },
                    "flat_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.svg",
                      "width" : 16
                    },
                    "light_theme_png" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                      "width" : 16
                    },
                    "light_theme_svg" : {
                      "height" : 16,
                      "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                      "width" : 16
                    },
                    "payment_method_type" : "klarna",
                    "role" : "icon",
                    "text" : "Klarna"
                  } ],
                  "inline_partner_promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50 with {partner}",
                    "url" : null
                  },
                  "learn_more" : {
                    "message" : "Learn more",
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=klarna"
                  },
                  "promotion" : {
                    "message" : "4 interest-free payments of ${'$'}22.50",
                    "url" : null
                  },
                  "summary" : {
                    "message" : "Pay now, or in 4 interest-free payments of ${'$'}22.50.",
                    "url" : null
                  }
                },
                "payment_plans" : [ {
                  "id" : "4c7096ad-f906-4a2e-a298-9c0d286dc7f9",
                  "due_days" : null,
                  "installment_amount" : 2250,
                  "interest_rate" : 0,
                  "interval" : {
                    "frequency" : 2,
                    "unit" : "week"
                  },
                  "number_of_installments" : 4,
                  "terms" : "Pay in 4 is offered by Klarna, Inc. It’s available to eligible US residents in most states. Initial payments may be higher. Missed payments are subject to late fees. For CA residents, loans made or arranged by Klarna, Inc. pursuant to a California Financing Law license. [Review the Pay in 4 terms](https://cdn.klarna.com/1.0/shared/content/legal/terms/0/en_us/sliceitinx).",
                  "total_amount" : 9000,
                  "type" : "INSTALLMENTS"
                }, {
                  "id" : "b7185417-6c03-4033-b74f-6745ab3fbcb9",
                  "due_days" : null,
                  "installment_amount" : null,
                  "interest_rate" : 0,
                  "interval" : null,
                  "number_of_installments" : null,
                  "terms" : "[Terms](https://www.klarna.com/us/legal/).",
                  "total_amount" : 9000,
                  "type" : "PAY_NOW"
                }, {
                  "id" : "4946b6d4-00e2-489d-8ba6-2595e437799b",
                  "due_days" : null,
                  "installment_amount" : null,
                  "interest_rate" : 0,
                  "interval" : null,
                  "number_of_installments" : null,
                  "terms" : "[Terms](https://www.klarna.com/us/legal/).",
                  "total_amount" : 9000,
                  "type" : "PAY_NOW"
                } ],
                "type" : "KLARNA"
              } ],
              "api_feature_flags" : [ {
                "key" : "enable_pmme_api_content",
                "result" : true
              } ],
              "experiments" : {
                "experiment_assignments" : {
                  "ocs_buyer_xp_pmme_affirm_solo_prequal" : "control",
                  "ocs_buyer_xp_pmme_affirm_multi_prequal" : "control"
                },
                "event_id" : "42e17fef-6b66-43d8-a45d-53cf2bd651b4"
              },
              "content" : {
                "images" : [ {
                  "dark_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-dark.png",
                    "width" : 144
                  },
                  "dark_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-dark.svg",
                    "width" : 144
                  },
                  "flat_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-flat.png",
                    "width" : 144
                  },
                  "flat_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo-flat.svg",
                    "width" : 144
                  },
                  "light_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo.png",
                    "width" : 144
                  },
                  "light_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-logo.svg",
                    "width" : 144
                  },
                  "payment_method_type" : "afterpay_clearpay",
                  "role" : "logo",
                  "text" : "Cash App Afterpay"
                }, {
                  "dark_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.png",
                    "width" : 16
                  },
                  "dark_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.svg",
                    "width" : 16
                  },
                  "flat_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon-flat.png",
                    "width" : 16
                  },
                  "flat_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon-flat.svg",
                    "width" : 16
                  },
                  "light_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.png",
                    "width" : 16
                  },
                  "light_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/cashapp-afterpay-icon.svg",
                    "width" : 16
                  },
                  "payment_method_type" : "afterpay_clearpay",
                  "role" : "icon",
                  "text" : "Cash App Afterpay"
                }, {
                  "dark_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-dark.png",
                    "width" : 75
                  },
                  "dark_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-dark.svg",
                    "width" : 75
                  },
                  "flat_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-flat.png",
                    "width" : 75
                  },
                  "flat_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo-flat.svg",
                    "width" : 75
                  },
                  "light_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo.png",
                    "width" : 75
                  },
                  "light_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-logo.svg",
                    "width" : 75
                  },
                  "payment_method_type" : "affirm",
                  "role" : "logo",
                  "text" : "Affirm"
                }, {
                  "dark_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-dark.png",
                    "width" : 16
                  },
                  "dark_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-dark.svg",
                    "width" : 16
                  },
                  "flat_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-flat.png",
                    "width" : 16
                  },
                  "flat_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon-flat.svg",
                    "width" : 16
                  },
                  "light_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon.png",
                    "width" : 16
                  },
                  "light_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/affirm-icon.svg",
                    "width" : 16
                  },
                  "payment_method_type" : "affirm",
                  "role" : "icon",
                  "text" : "Affirm"
                }, {
                  "dark_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.png",
                    "width" : 78
                  },
                  "dark_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-dark.svg",
                    "width" : 78
                  },
                  "flat_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.png",
                    "width" : 78
                  },
                  "flat_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo-flat.svg",
                    "width" : 78
                  },
                  "light_theme_png" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.png",
                    "width" : 98
                  },
                  "light_theme_svg" : {
                    "height" : 40,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-logo.svg",
                    "width" : 98
                  },
                  "payment_method_type" : "klarna",
                  "role" : "logo",
                  "text" : "Klarna"
                }, {
                  "dark_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                    "width" : 16
                  },
                  "dark_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                    "width" : 16
                  },
                  "flat_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.png",
                    "width" : 16
                  },
                  "flat_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon-flat.svg",
                    "width" : 16
                  },
                  "light_theme_png" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.png",
                    "width" : 16
                  },
                  "light_theme_svg" : {
                    "height" : 16,
                    "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/images/klarna-icon.svg",
                    "width" : 16
                  },
                  "payment_method_type" : "klarna",
                  "role" : "icon",
                  "text" : "Klarna"
                } ],
                "inline_partner_promotion" : null,
                "learn_more" : {
                  "message" : "Learn more",
                  "url" : "https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFfB7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=afterpay_clearpay&payment_methods%5B1%5D=affirm&payment_methods%5B2%5D=klarna"
                },
                "promotion" : {
                  "message" : "4 interest-free payments of ${'$'}22.50",
                  "url" : null
                },
                "summary" : {
                  "message" : "Pay now, or in 4 interest-free payments of ${'$'}22.50.",
                  "url" : null
                }
              }
            }
        """.trimIndent()
    )
}
