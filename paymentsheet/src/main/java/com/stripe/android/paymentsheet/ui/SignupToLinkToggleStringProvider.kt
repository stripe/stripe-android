package com.stripe.android.paymentsheet.ui

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import com.stripe.android.link.ui.replaceHyperlinks

internal interface SignupToLinkToggleStringProvider {
    val title: String
    val description: String
    val termsAndConditions: AnnotatedString
}

internal class DefaultSignupToLinkToggleStringProvider(
    private val application: Application
) : SignupToLinkToggleStringProvider {

    override val title: String by lazy {
        application.getString(com.stripe.android.paymentsheet.R.string.stripe_link_signup_toggle_title)
    }

    override val description: String by lazy {
        application.getString(com.stripe.android.paymentsheet.R.string.stripe_link_signup_toggle_description)
    }

    override val termsAndConditions: AnnotatedString by lazy {
        AnnotatedString.fromHtml(
            application.getString(com.stripe.android.paymentsheet.R.string.stripe_link_sign_up_terms)
                .replaceHyperlinks()
        )
    }
}
