package com.stripe.android.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.properties.Delegates

class BecsDebitMandateAcceptanceTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val factory = BecsDebitMandateAcceptanceTextFactory(context)

    init {
        movementMethod = LinkMovementMethod.getInstance()
    }

    var companyName: String by Delegates.observable(
        ""
    ) { _, _, companyName ->
        text = companyName.takeIf {
            it.isNotBlank()
        }?.let {
            factory.create(it)
        } ?: ""
    }

    internal val isValid: Boolean
        get() {
            return !text.isNullOrBlank()
        }
}
