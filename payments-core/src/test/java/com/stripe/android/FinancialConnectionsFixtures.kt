package com.stripe.android

import org.json.JSONObject

internal object FinancialConnectionsFixtures {
    val SESSION = JSONObject(
        """
            {
              "id": "fcsess_1LYLUOHINT0kwo6sqtuuuB",
              "object": "financial_connections.session",
              "accounts": {
                "object": "list",
                "data": [],
                "has_more": false,
                "total_count": 0,
                "url": "/v1/financial_connections/accounts"
              },
              "client_secret": "fcsess_client_secret_CMfC84LV5ZA3v6v8aWigz3",
              "livemode": false,
              "permissions": [
                "balances",
                "ownership",
                "payment_method",
                "transactions"
              ]
            }
        """.trimIndent()
    )
}
