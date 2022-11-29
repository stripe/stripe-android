@file:Suppress("MaxLineLength", "MaximumLineLength")

package com.stripe.android.financialconnections.presentation

internal object FinancialConnectionsUrls {
    object Disconnect {
        const val email = "ttps://support.stripe.com/contact"
        const val supportEndUser = "https://support.stripe.com/user/how-do-i-disconnect-my-linked-financial-account"
        const val supportMerchantUser = "https://support.stripe.com/how-to-disconnect-a-linked-financial-account"
        const val dashboard = "https://dashboard.stripe.com/settings/linked-accounts"
        const val link = "https://support.link.co/questions/connecting-your-bank-account#how-do-i-disconnect-my-connected-bank-account"
    }

    object FAQ {
        const val stripe = "https://stripe.com/docs/linked-accounts/faqs"
        const val merchant = "https://support.stripe.com/user/topics/linked-financial-accounts"
    }

    object StripeToS {
        const val endUser = "https://stripe.com/legal/end-users#linked-financial-account-terms"
        const val merchantUser = "https://stripe.com/legal/linked-financial-accounts-merchant"
    }

    object Link {
        const val ToS = "https://link.co/terms"
        const val privacyPolicy = "https://link.co/privacy"
    }

    object Finicity {
        const val ToS = "https://connect.finicity.com/assets/html/connect-eula.html"
        const val privacyPolicy = "https://www.finicity.com/privacy/"
    }

    object PrivacyCenter {
        const val merchant = "https://stripe.com/privacy-center/legal#linking-financial-accounts"
        const val stripe = "https://stripe.com/docs/linked-accounts/faqs"
    }

    const val StripePrivacyPolicy = "https://stripe.com/privacy"

    object PartnerNotice {
        const val stripe = "https://stripe.com/docs/linked-accounts/faqs"
        const val merchant = "https://support.stripe.com/user/questions/what-is-the-relationship-between-stripe-and-stripes-service-providers"
    }

    object DataPolicy {
        const val stripe = "https://stripe.com/docs/linked-accounts/faqs"
        const val merchant = "https://support.stripe.com/user/questions/what-data-does-stripe-access-from-my-linked-financial-account"
    }
}
