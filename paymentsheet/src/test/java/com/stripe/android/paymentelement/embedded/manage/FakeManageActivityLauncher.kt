package com.stripe.android.paymentelement.embedded.manage

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat

internal class FakeManageActivityLauncher : ActivityResultLauncher<ManageContract.Args>() {
    var didLaunch = false
        private set
    var launchArgs: ManageContract.Args? = null
        private set
    var didUnregister = false
        private set

    override fun launch(input: ManageContract.Args?, options: ActivityOptionsCompat?) {
        didLaunch = true
        launchArgs = input
    }

    override fun unregister() {
        didUnregister = true
    }

    override fun getContract(): ActivityResultContract<ManageContract.Args, ManageResult> {
        return ManageContract
    }
}
