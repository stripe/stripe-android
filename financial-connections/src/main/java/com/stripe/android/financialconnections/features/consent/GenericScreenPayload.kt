package com.stripe.android.financialconnections.features.consent

internal val streamlinedConsentPayload = """
    {
        "data_access_notice": {
            "body": {
                "bullets": [
                    {
                        "icon": {
                            "default": "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--bank-primary-3x.png"
                        },
                        "title": "Account details"
                    }
                ]
            },
            "cta": "OK",
            "disclaimer": "Learn about [data shared with Stripe](https://support.stripe.com/user/questions/what-data-does-stripe-access-from-my-linked-financial-account?eventName=click.data_access.learn_more) and [how to disconnect](https://support.stripe.com/user/how-do-i-disconnect-my-linked-financial-account?eventName=click.disconnect_link)",
            "icon": {
                "default": "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--platform-stripeBrand-3x.png"
            },
            "subtitle": "watchshop will have access to the following data and related insights:",
            "title": "Data sharing"
        },
        "legal_details_notice": {
            "body": {
                "links": [
                    {
                        "title": "[Terms](https://stripe.com/legal/consumer/financial-connections?eventName=click.legal.terms)"
                    },
                    {
                        "title": "[Privacy Policy](https://stripe.com/privacy?eventName=click.legal.privacy_policy)"
                    }
                ]
            },
            "cta": "OK",
            "icon": {
                "default": "https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--document-stripeBrand-3x.png"
            },
            "subtitle": "Stripe only uses your data and credentials as described in the Terms, such as to improve its services, manage loss, and mitigate fraud.",
            "title": "Terms and Privacy Policy"
        },
        "more_info_notice": {
            "id": "more_info_notice",
            "footer": {
                "primary_cta": {
                    "id": "ok",
                    "label": "OK"
                }
            },
            "header": {
                "icon": {
                    "default": "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--stripe-4x.png"
                },
                "subtitle": "Stripe safely connects your bank. Your data is encrypted for your protection, and you can [disconnect](https://support.stripe.com/user/how-do-i-disconnect-my-linked-financial-account?eventName=click.disconnect_link) anytime.",
                "title": "About Stripe"
            }
        },
        "screen": {
            "id": "streamlined_consent",
            "body": {
                "entries": [
                    {
                        "id": "streamlined_consent_body_text",
                        "alignment": "center",
                        "size": "small",
                        "text": "Connect your account in seconds. watchshop may retrieve [account data.](stripe://data-access-notice?eventName=click.data_requested) Your data is encrypted.",
                        "type": "text"
                    }
                ]
            },
            "footer": {
                "below_cta": "[Manually verify instead](stripe://manual-entry?eventName=click.manual_entry&testId=manual-verification-button) (takes 1-2 business days)",
                "disclaimer": "By connecting via Stripe, you agree to the [Terms and Privacy Policy](stripe://legal-details-notice?eventName=click.legal_details.open&testId=legal-details-button)",
                "primary_cta": {
                    "id": "continue",
                    "label": "Agree and continue",
                    "test_id": "agree-button"
                }
            },
            "header": {
                "alignment": "center",
                "icon": {
                    "default": "https://b.stripecdn.com/connections-statics-srv/assets/BrandIcon--testmodeGreenBank-4x.png"
                },
                "title": "Log in with Test Institution"
            },
            "options": {
                "vertical_alignment": "centered"
            }
        }
    }
""".trimIndent()
