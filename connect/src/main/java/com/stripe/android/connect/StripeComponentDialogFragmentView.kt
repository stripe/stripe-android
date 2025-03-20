package com.stripe.android.connect

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.TypefaceCompat
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.webview.MobileInput

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentDialogFragmentView<ComponentView : StripeComponentView<*, *>>(
    layoutInflater: LayoutInflater
) : LinearLayout(layoutInflater.context) {

    constructor(context: Context) : this(LayoutInflater.from(context))

    private val toolbar: ViewGroup
    private val closeButton: AppCompatImageButton
    private val titleText: TextView
    private val divider: View
    var componentView: ComponentView? = null
        set(value) {
            field?.let { removeView(it) }
            field = value
            if (value != null) {
                addView(
                    value.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                )
            }
        }

    var title: CharSequence?
        get() = titleText.text
        set(value) {
            titleText.text = value
        }

    var listener: Listener? = null

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        layoutInflater.inflate(R.layout.stripe_full_screen_component, this, true)
        toolbar = findViewById(R.id.toolbar)
        closeButton = findViewById(R.id.close_button)
        titleText = findViewById(R.id.title_text)
        divider = findViewById(R.id.divider)

        closeButton.setOnClickListener {
            // Defer to the component view if it's available.
            val componentView = this.componentView
            if (componentView == null) {
                listener?.onCloseButtonClickError()
                return@setOnClickListener
            }
            componentView.mobileInputReceived(
                input = MobileInput.CLOSE_BUTTON_PRESSED,
                resultCallback = { result ->
                    if (result.isFailure) {
                        listener?.onCloseButtonClickError()
                    }
                }
            )
        }
    }

    fun bindAppearance(appearance: Appearance) {
        val context = titleText.context

        val backgroundColor =
            appearance.colors.background
                ?: ContextCompat.getColor(context, R.color.stripe_connect_background)
        setBackgroundColor(backgroundColor)
        toolbar.setBackgroundColor(backgroundColor)

        val textColor = appearance.colors.text
            ?: ContextCompat.getColor(context, R.color.stripe_connect_text)
        titleText.setTextColor(textColor)
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        titleText.typeface = TypefaceCompat.create(context, null, 700, false)
        closeButton.imageTintList = ColorStateList.valueOf(textColor)

        divider.setBackgroundColor(
            appearance.colors.border
                ?: ContextCompat.getColor(context, R.color.stripe_connect_border)
        )
    }

    interface Listener {
        fun onCloseButtonClickError()
    }
}
