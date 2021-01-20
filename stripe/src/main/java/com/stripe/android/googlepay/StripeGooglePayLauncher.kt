package com.stripe.android.googlepay

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.Fragment
import com.stripe.android.view.ActivityStarter

internal class StripeGooglePayLauncher : ActivityStarter<StripeGooglePayActivity, StripeGooglePayContract.Args> {
    constructor(activity: Activity) : super(
        activity,
        StripeGooglePayActivity::class.java,
        REQUEST_CODE,
        intentFlags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    )

    constructor(fragment: Fragment) : super(
        fragment,
        StripeGooglePayActivity::class.java,
        REQUEST_CODE,
        intentFlags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    )

    companion object {
        const val REQUEST_CODE: Int = 9004
    }
}
