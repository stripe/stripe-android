package com.stripe.android.link

import android.app.Activity
import androidx.annotation.RestrictTo
import com.stripe.android.view.ActivityStarter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkActivityStarter(
    activity: Activity
) : ActivityStarter<LinkActivity, LinkActivityContract.Args>(
    activity,
    LinkActivity::class.java,
    REQUEST_CODE
) {
    internal companion object {
        const val REQUEST_CODE: Int = 6005
    }
}
