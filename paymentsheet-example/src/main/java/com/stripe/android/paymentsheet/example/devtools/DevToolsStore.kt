package com.stripe.android.paymentsheet.example.devtools

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stripe.android.core.networking.StripeNetworkClientInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

internal data class Endpoint(
    val url: String,
    val regex: Regex
) {
    val name: String
        get() = url.removePrefix("https://api.stripe.com/v1/")
}

internal object DevToolsStore {
    val endpoints = mutableStateListOf<Endpoint>()
    val failingEndpoints = mutableStateListOf<Endpoint>()

    var failed by mutableStateOf(false)
        private set

    suspend fun loadEndpoints() = withContext(Dispatchers.IO) {
        val apiEndpoints = stripeApiEndpoints()
        if (apiEndpoints != null) {
            endpoints += apiEndpoints.sortedBy { it.url }
        } else {
            failed = true
        }
    }

    fun toggle(endpoint: Endpoint) {
        if (endpoint in failingEndpoints) {
            failingEndpoints.remove(endpoint)
        } else {
            failingEndpoints.add(endpoint)
        }

        StripeNetworkClientInterceptor.shouldFailEvaluator = { requestUrl ->
            val cleanedUrl = Uri.parse(requestUrl).buildUpon().clearQuery().build().toString()

            val matchingUrls = failingEndpoints.filter { endpoint ->
                endpoint.regex.matches(cleanedUrl)
            }

            val allMatches = endpoints.filter { endpoint ->
                endpoint.regex.matches(cleanedUrl)
            }

            if (allMatches.size > 1) {
                // We got two matches. This can happen for situations like the following:
                // - /consumers/payment_details/list
                // - /consumers/payment_details/{some_id}
                //
                // To resolve this, we check that the endpoint's URL (and not its regex) matches the
                // matching URL. Only if that's the case, can we be sure that we're intercepting the
                // right one.
                allMatches.single { it.url == cleanedUrl } == matchingUrls.first()
            } else {
                matchingUrls.isNotEmpty()
            }
        }
    }
}

private fun stripeApiEndpoints(): List<Endpoint>? {
    val repoClass = Class.forName(
        "com.stripe.android.networking.StripeApiRepository"
    ).kotlin
    val companionObject = repoClass.companionObjectInstance ?: return emptyList()

    return try {
        val endpointProps = companionObject::class.declaredMemberProperties.mapNotNull { prop ->
            prop.getEndpoint(companionObject)
        }

        val endpointMethods = companionObject::class.declaredMemberFunctions.mapNotNull { func ->
            func.getEndpoint(companionObject)
        }

        endpointProps + endpointMethods
    } catch (t: Throwable) {
        println(t)
        null
    }
}

private fun KProperty1<out Any, *>.getEndpoint(obj: Any): Endpoint? {
    val isEndpointUrl = name.endsWith("url", ignoreCase = true)
    val stringValue = javaGetter?.invoke(obj) as? String ?: return null
    return Endpoint(stringValue, stringValue.toRegex()).takeIf { isEndpointUrl }
}

private fun KFunction<*>.getEndpoint(obj: Any): Endpoint? {
    val isEndpointUrl = name.endsWith("url", ignoreCase = true)
    if (!isEndpointUrl || visibility == KVisibility.PRIVATE) {
        return null
    }

    val urlParams = valueParameters.map { "{${it.name}}" }
    val regexParams = valueParameters.map { "(.+)" }

    val url = javaMethod?.invoke(obj, *urlParams.toTypedArray()) as? String ?: return null
    val regex = javaMethod?.invoke(obj, *regexParams.toTypedArray()) as? String ?: return null

    return Endpoint(url, regex.toRegex())
}
