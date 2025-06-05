package com.stripe.android.iapexample

import androidx.lifecycle.ViewModel
import com.stripe.android.iap.InAppPurchasePlugin

internal class MainActivityViewModel : ViewModel() {
    val plugins = listOf(
        InAppPurchasePlugin.stripeCheckout { "https://checkout.stripe.com/c/pay/cs_test_a1HBtfleCxMwy8n2EVIVG0GWBan1iL5UneGLlSQZfXk9WntapuOf22Zu7e#fid2cGd2ZndsdXFsamtQa2x0cGBrYHZ2QGtkZ2lgYSc%2FY2RpdmApJ2R1bE5gfCc%2FJ3VuWnFgdnFaMDRNc1FMMklwMGo2VTQ9X3UzcTBEYkdWbkhzUmpRcUQ1a3xEMnVTXEF0dWNJbldxUnBrMnRfUVxGSk1GV2B1d2NJSDEzMXxkR2BDMjdQQ2NHMmZcPFJCMWQ1NV9rQXFsRjdGJyknY3dqaFZgd3Ngdyc%2FcXdwYCknaWR8anBxUXx1YCc%2FJ3Zsa2JpYFpscWBoJyknYGtkZ2lgVWlkZmBtamlhYHd2Jz9xd3BgeCUl" },
        InAppPurchasePlugin.googlePlay { "cscs_1234" },
    )
}
