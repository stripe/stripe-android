package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.util.Patterns
import com.stripe.android.R

internal class EmailEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : StripeEditText(context, attrs, defStyleAttr) {

    val email: String?
        get() {
            errorMessage = when {
                fieldText.isBlank() -> {
                    resources.getString(R.string.becs_widget_email_required)
                }
                !Patterns.EMAIL_ADDRESS.matcher(fieldText).matches() -> {
                    resources.getString(R.string.becs_widget_email_invalid)
                }
                else -> null
            }

            return fieldText.takeIf { errorMessage == null }
        }
}
