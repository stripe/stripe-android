package com.stripe.android.common.taptoadd

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import com.stripe.android.uicore.image.StripeImageLoader

internal object TapToAddStripeImageLoaderFactory {
    @VisibleForTesting
    @Volatile
    var override: StripeImageLoader? = null

    fun create(context: Context): StripeImageLoader {
        return override ?: DefaultStripeImageLoader(context)
    }
}
