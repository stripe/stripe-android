package com.stripe.android.connect

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.stripe.android.connect.appearance.Appearance
import kotlin.math.roundToInt

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeComponentDialogFragmentView<ComponentView : StripeComponentView<*, *>>(
    layoutInflater: LayoutInflater
) : LinearLayout(layoutInflater.context) {

    constructor(context: Context) : this(LayoutInflater.from(context))

    val toolbar: Toolbar
    val divider: View
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

    init {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        val tv = TypedValue()
        toolbar = Toolbar(context).apply {
            context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            val height = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            elevation = 0f
            navigationIcon = ContextCompat.getDrawable(context, R.drawable.stripe_connect_close)
        }
        addView(toolbar)

        divider = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).roundToInt()
            )
        }
        addView(divider)
    }

    fun bindAppearance(appearance: Appearance) {
        val context = toolbar.context

        val backgroundColor =
            appearance.colors.background
                ?: ContextCompat.getColor(context, R.color.stripe_connect_background)
        toolbar.setBackgroundColor(backgroundColor)

        val textColor = appearance.colors.text
            ?: ContextCompat.getColor(context, R.color.stripe_connect_text)
        toolbar.setTitleTextColor(textColor)
        toolbar.navigationIcon?.setTint(textColor)

        divider.setBackgroundColor(
            appearance.colors.border
                ?: ContextCompat.getColor(context, R.color.stripe_connect_border)
        )
    }
}
