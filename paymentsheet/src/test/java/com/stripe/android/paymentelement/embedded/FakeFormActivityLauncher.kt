package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

internal class FakeFormActivityLauncher : ActivityResultLauncher<FormContract.Args>() {
    var didLaunch = false
        private set
    var launchArgs: FormContract.Args? = null
        private set
    var didUnregister = false
        private set

    override fun launch(input: FormContract.Args?, options: ActivityOptionsCompat?) {
        didLaunch = true
        launchArgs = input
    }

    override fun unregister() {
        didUnregister = true
    }

    override fun getContract(): ActivityResultContract<FormContract.Args, FormResult> {
        return FormContract()
    }
}

@Suppress("UNCHECKED_CAST")
internal class FakeActivityResultCaller(private val fakeLauncher: FakeFormActivityLauncher) : ActivityResultCaller {
    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return fakeLauncher as ActivityResultLauncher<I>
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>
    ): ActivityResultLauncher<I> {
        return fakeLauncher as ActivityResultLauncher<I>
    }
}
