package com.stripe.android.connect.example.data

import android.os.Build
import com.github.kittinunf.fuel.core.FoldableRequestInterceptor
import com.github.kittinunf.fuel.core.FoldableResponseInterceptor
import com.github.kittinunf.fuel.core.RequestTransformer
import com.github.kittinunf.fuel.core.ResponseTransformer
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion

object ApplicationJsonHeaderInterceptor : FoldableRequestInterceptor {
    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            next(request.header("content-type", "application/json"))
        }
    }
}

object UserAgentHeader : FoldableRequestInterceptor {
    private val userAgent: String
        get() {
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
            next(request.header("User-Agent", userAgent))
        }
    }
}

class RequestLogger(
    private val tag: String,
    private val logger: Logger,
) : FoldableRequestInterceptor {
    override fun invoke(next: RequestTransformer): RequestTransformer {
        return { request ->
            logger.info("($tag) Request: ${request.cUrlString()}")
            next(request)
        }
    }
}

class ResponseLogger(
    private val tag: String,
    private val logger: Logger,
) : FoldableResponseInterceptor {
    override fun invoke(next: ResponseTransformer): ResponseTransformer {
        return { request, response ->
            logger.info("($tag) Response: $response")
            next(request, response)
        }
    }
}
