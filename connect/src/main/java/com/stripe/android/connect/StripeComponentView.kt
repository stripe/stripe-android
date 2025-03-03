package com.stripe.android.connect

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import com.stripe.android.connect.webview.StripeConnectWebViewContainer

@PrivateBetaConnectSDK
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class StripeComponentView<Listener, Props>(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : FrameLayout(context, attrs, defStyleAttr),
    StripeConnectWebViewContainer<Listener, Props>
    where Listener : StripeEmbeddedComponentListener,
          Props : ComponentProps
