package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultLauncher
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
        return FormContract
    }
}
