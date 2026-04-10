package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

internal sealed interface ElementsSessionParams : Parcelable {

    val type: String
    val clientSecret: String?
    val customerSessionClientSecret: String?
    val legacyCustomerEphemeralKey: String?
    val mobileSessionId: String?
    val locale: String?
    val expandFields: List<String>
    val savedPaymentMethodSelectionId: String?
    val customPaymentMethods: List<String>
    val externalPaymentMethods: List<String>
    val appId: String
    val sellerDetails: SellerDetails?
    val link: Link
    val countryOverride: String?

    @Parcelize
    data class PaymentIntentType(
        override val clientSecret: String,
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        override val customerSessionClientSecret: String? = null,
        override val legacyCustomerEphemeralKey: String? = null,
        override val savedPaymentMethodSelectionId: String? = null,
        override val mobileSessionId: String? = null,
        override val customPaymentMethods: List<String>,
        override val externalPaymentMethods: List<String>,
        override val appId: String,
        override val link: Link = Link(),
        override val countryOverride: String? = null,
    ) : ElementsSessionParams {

        override val type: String
            get() = "payment_intent"

        override val expandFields: List<String>
            get() = listOf("payment_method_preference.$type.payment_method")

        override val sellerDetails: SellerDetails?
            get() = null
    }

    @Parcelize
    data class SetupIntentType(
        override val clientSecret: String,
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        override val customerSessionClientSecret: String? = null,
        override val legacyCustomerEphemeralKey: String? = null,
        override val savedPaymentMethodSelectionId: String? = null,
        override val mobileSessionId: String? = null,
        override val customPaymentMethods: List<String>,
        override val externalPaymentMethods: List<String>,
        override val appId: String,
        override val link: Link = Link(),
        override val countryOverride: String? = null,
    ) : ElementsSessionParams {

        override val type: String
            get() = "setup_intent"

        override val expandFields: List<String>
            get() = listOf("payment_method_preference.$type.payment_method")

        override val sellerDetails: SellerDetails?
            get() = null
    }

    @Parcelize
    data class DeferredIntentType(
        override val locale: String? = Locale.getDefault().toLanguageTag(),
        val deferredIntentParams: DeferredIntentParams,
        override val customPaymentMethods: List<String>,
        override val externalPaymentMethods: List<String>,
        override val savedPaymentMethodSelectionId: String? = null,
        override val customerSessionClientSecret: String? = null,
        override val legacyCustomerEphemeralKey: String? = null,
        override val mobileSessionId: String? = null,
        override val appId: String,
        override val sellerDetails: SellerDetails? = null,
        override val link: Link = Link(),
        override val countryOverride: String? = null,
    ) : ElementsSessionParams {

        override val clientSecret: String?
            get() = null

        override val type: String
            get() = "deferred_intent"

        override val expandFields: List<String>
            get() = emptyList()
    }

    @Parcelize
    data class SellerDetails(
        val networkId: String? = null,
        val externalId: String? = null,
        val businessName: String? = null,
        val networkBusinessProfile: String? = null,
    ) : Parcelable {
        fun toQueryParams(): Map<String, Any?> {
            return buildMap {
                networkId?.let { put("seller_details[network_id]", it) }
                externalId?.let { put("seller_details[external_id]", it) }
                businessName?.let { put("seller_details[business_name]", it) }
                networkBusinessProfile?.let { put("seller_details[network_business_profile]", it) }
            }
        }
    }

    @Parcelize
    data class Link(
        val disallowFundingSourceCreation: Set<String> = emptySet(),
    ) : Parcelable {
        fun toQueryParams(): Map<String, Any?> {
            return disallowFundingSourceCreation.withIndex().associate { (index, fundingSource) ->
                "link[disallow_funding_source_creation][$index]" to fundingSource
            }
        }
    }
}
