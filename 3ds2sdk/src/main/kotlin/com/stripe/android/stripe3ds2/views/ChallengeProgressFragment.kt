package com.stripe.android.stripe3ds2.views

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.databinding.StripeProgressViewLayoutBinding
import com.stripe.android.stripe3ds2.observability.DefaultErrorReporter
import com.stripe.android.stripe3ds2.observability.Stripe3ds2ErrorReporterConfig
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId

class ChallengeProgressFragment(
    private val directoryServerName: String,
    private val sdkTransactionId: SdkTransactionId,
    private val accentColor: Int?
) : Fragment(R.layout.stripe_progress_view_layout) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewBinding = StripeProgressViewLayoutBinding.bind(view)

        val errorReporter = DefaultErrorReporter(
            requireContext(),
            config = Stripe3ds2ErrorReporterConfig(sdkTransactionId)
        )

        val brand = Brand.lookup(directoryServerName, errorReporter)

        viewBinding.brandLogo.let { brandLogo ->
            brandLogo.setImageDrawable(
                activity?.let {
                    ContextCompat.getDrawable(it, brand.drawableResId)
                }
            )
            brandLogo.contentDescription = brand.nameResId?.let { getString(it) }

            if (brand.shouldStretch) {
                brandLogo.updateLayoutParams {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }

            brandLogo.isVisible = true
        }

        accentColor?.let {
            viewBinding.progressBar.setIndicatorColor(it)
        }
    }
}
