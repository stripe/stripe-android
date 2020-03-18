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

    var merchantName: String by Delegates.observable(
        ""
    ) { _, _, merchantName ->
        text = merchantName.takeIf { it.isNotBlank() }?.let { factory.create(it) } ?: ""
    }
}
