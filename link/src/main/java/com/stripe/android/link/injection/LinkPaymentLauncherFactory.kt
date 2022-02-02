package com.stripe.android.link.injection

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkPaymentLauncher
import dagger.assisted.AssistedFactory

@AssistedFactory
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LinkPaymentLauncherFactory {
    fun create(
        activityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args>
    ): LinkPaymentLauncher
}
