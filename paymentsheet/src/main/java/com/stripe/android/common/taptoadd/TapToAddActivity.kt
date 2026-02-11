package com.stripe.android.common.taptoadd

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge

internal class TapToAddActivity : AppCompatActivity() {
    private val args: TapToAddContract.Args? by lazy {
        TapToAddContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tapToAddArguments = args

        if (tapToAddArguments == null || !tapToAddArguments.paymentMethodMetadata.isTapToAddSupported) {
            finish()
            return
        }

        renderEdgeToEdge()
    }
}
