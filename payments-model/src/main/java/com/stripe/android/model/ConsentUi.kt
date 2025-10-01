package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.networking.MarkdownToHtmlSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class ConsentUi(
    @SerialName("consent_pane")
    val consentPane: ConsentPane?,
    @SerialName("consent_section")
    val consentSection: ConsentSection?,
) : StripeModel {

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    data class ConsentPane(
        @SerialName("title")
        val title: String,
        @SerialName("scopes_section")
        val scopesSection: ScopesSection,
        @SerialName("disclaimer")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val disclaimer: String?,
        @SerialName("deny_button_label")
        val denyButtonLabel: String?,
        @SerialName("allow_button_label")
        val allowButtonLabel: String,
    ) : StripeModel {

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Serializable
        data class ScopesSection(
            @SerialName("header")
            val header: String,
            @SerialName("scopes")
            val scopes: List<Scope>,
        ) : StripeModel {

            @Parcelize
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @Serializable
            data class Scope(
                @SerialName("icon")
                val icon: Icon,
                @SerialName("header")
                val header: String?,
                @SerialName("description")
                @Serializable(with = MarkdownToHtmlSerializer::class)
                val description: String,
            ) : StripeModel
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    data class ConsentSection(
        @SerialName("disclaimer")
        @Serializable(with = MarkdownToHtmlSerializer::class)
        val disclaimer: String,
    ) : StripeModel

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Serializable
    data class Icon(
        @SerialName("default")
        val default: String,
    ) : StripeModel
}
