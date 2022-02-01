package com.stripe.android.stripe3ds2.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import com.stripe.android.stripe3ds2.databinding.StripeBrandZoneViewBinding

/**
 * A view to display brand images (issuer and payment system) for 3DS2 Challenge Brand Zone
 */
internal class BrandZoneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    internal val issuerImageView: ImageView
    internal val paymentSystemImageView: ImageView

    init {
        val viewBinding = StripeBrandZoneViewBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        issuerImageView = viewBinding.issuerImage
        paymentSystemImageView = viewBinding.paymentSystemImage
    }
}
