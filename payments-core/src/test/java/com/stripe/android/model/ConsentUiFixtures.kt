package com.stripe.android.model

object ConsentUiFixtures {
    const val HAS_CONSENT_PANE =
        """
        {
          "consent_pane": {
            "title": "Connect Example with Link",
            "scopes_section": {
              "header": "Example will have access to:",
              "scopes": [
                {
                  "icon": {
                    "default": "https://asset.stripe.com/account_scope.png"
                  },
                  "header": "Account info",
                  "description": "Name, email, and **profile picture**"
                }
              ]
            },
            "disclaimer": "By allowing, you agree to **Example**'s [Terms of Service](https://www.stripe.com).",
            "deny_button_label": "Cancel",
            "allow_button_label": "Allow"
          }
        }
        """

    const val HAS_CONSENT_SECTION =
        """
        {
          "consent_section": {
            "disclaimer": "By allowing, you agree to **Example**'s [Terms of Service](https://www.stripe.com)."
          }
        }
        """
}
