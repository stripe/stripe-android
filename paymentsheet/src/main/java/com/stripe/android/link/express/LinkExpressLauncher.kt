package com.stripe.android.link.express

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.model.LinkAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LinkExpressLauncher @Inject constructor(
    private val linkExpressContract: LinkExpressContract
) {
    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkExpressContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (LinkExpressResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultRegistry.register(
            "LinkExpressLauncher",
            linkExpressContract,
        ) { linkExpressResult ->
            handleActivityResult(linkExpressResult, callback)
        }
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (LinkExpressResult) -> Unit,
    ) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            linkExpressContract
        ) { linkExpressContract ->
            handleActivityResult(linkExpressContract, callback)
        }
    }

    private fun handleActivityResult(
        linkExpressResult: LinkExpressResult,
        nextStep: (LinkExpressResult) -> Unit
    ) {
        nextStep(linkExpressResult)
    }

    fun unregister() {
        linkActivityResultLauncher?.unregister()
        linkActivityResultLauncher = null
    }

    /**
     * Launch the Link UI to process a payment.
     *
     * @param configuration The payment and customer settings
     */
    fun present(
        configuration: LinkConfiguration,
        linkAccount: LinkAccount
    ) {
        val args = LinkExpressContract.Args(
            configuration = configuration,
            linkAccount = linkAccount
        )
        linkActivityResultLauncher?.launch(args)
    }
}
