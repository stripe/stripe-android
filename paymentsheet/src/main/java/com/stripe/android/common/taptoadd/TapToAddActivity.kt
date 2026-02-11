package com.stripe.android.common.taptoadd

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import javax.inject.Inject

internal class TapToAddActivity : AppCompatActivity() {
    private val args: TapToAddContract.Args? by lazy {
        TapToAddContract.Args.fromIntent(intent)
    }

    private val viewModel: TapToAddViewModel by viewModels {
        TapToAddViewModel.Factory {
            requireNotNull(args)
        }
    }

    @Inject
    lateinit var tapToAddRegistrar: TapToAddRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tapToAddArguments = args

        if (tapToAddArguments == null || !tapToAddArguments.paymentMethodMetadata.isTapToAddSupported) {
            finish()
            return
        }

        renderEdgeToEdge()

        viewModel.component.subcomponentFactory.build(
            activityResultCaller = this,
            lifecycleOwner = this,
        ).inject(this)
    }
}
