package com.stripe.android.connectsdk.example.networking

import android.os.Build
import com.github.kittinunf.fuel.core.FoldableRequestInterceptor
import com.github.kittinunf.fuel.core.FoldableResponseInterceptor
import com.github.kittinunf.fuel.core.RequestTransformer
import com.github.kittinunf.fuel.core.ResponseTransformer
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.stripe.android.core.version.StripeSdkVersion
import timber.log.Timber

object ApplicationJsonHeaderInterceptor : FoldableRequestInterceptor {
    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            next(request.header("content-type", "application/json"))
        }
    }
}

object UserAgentHeader : FoldableRequestInterceptor {
    fun getUserAgent(): String {
        val androidBrand = Build.BRAND
        val androidDevice = Build.MODEL
        val osVersion = Build.VERSION.SDK_INT
        return buildString {
            append("Stripe/ConnectSDKExample")
            append(" (Android $androidBrand $androidDevice; (OS Version $osVersion))+")
            append(" Version/${StripeSdkVersion.VERSION_NAME}")
        }
    }

    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            next(request.header("User-Agent", getUserAgent()))
        }
    }
}

class TimberRequestLogger(private val tag: String) : FoldableRequestInterceptor {
    private val timber get() = Timber.tag(tag)

    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            timber.i("Request: ${request.cUrlString()}")
            next(request)
        }
    }
}

class TimberResponseLogger(private val tag: String) : FoldableResponseInterceptor {
    private val timber get() = Timber.tag(tag)

    override fun invoke(next: ResponseTransformer): ResponseTransformer {
        return { request, response ->
            timber.i("Response: $response")
            next(request, response)
        }
    }
}