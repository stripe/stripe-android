package com.stripe.android.stripe3ds2.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import com.stripe.android.stripe3ds2.databinding.StripeChallengeSubmitDialogBinding
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import com.stripe.android.stripe3ds2.utils.Factory0

class ChallengeSubmitDialogFactory(
    private val context: Context,
    private val uiCustomization: UiCustomization
) : Factory0<Dialog> {

    override fun create(): Dialog = ChallengeSubmitDialog(context, uiCustomization)

    private class ChallengeSubmitDialog(
        context: Context,
        private val uiCustomization: UiCustomization
    ) : Dialog(context) {
        private val viewBinding: StripeChallengeSubmitDialogBinding by lazy {
            StripeChallengeSubmitDialogBinding.inflate(layoutInflater)
        }

        init {
            setCancelable(false)

            window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        override fun onStart() {
            super.onStart()
            setContentView(viewBinding.root)
            CustomizeUtils.applyProgressBarColor(viewBinding.progressBar, uiCustomization)
        }
    }
}
