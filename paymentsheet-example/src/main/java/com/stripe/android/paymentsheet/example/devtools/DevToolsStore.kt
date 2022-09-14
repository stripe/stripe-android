package com.stripe.android.paymentsheet.example.devtools

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        get() = url.removePrefix("https://api.stripe.com/").removePrefix("v1/")
}

internal object DevToolsStore {
    val endpoints = mutableStateListOf<Endpoint>()
    val failingEndpoints = mutableStateListOf<Endpoint>()

    var failed by mutableStateOf(false)
        private set

    suspend fun loadEndpoints() = withContext(Dispatchers.Default) {
        val apiEndpoints = stripeApiEndpoints()
        if (apiEndpoints != null) {
            endpoints += apiEndpoints.sortedBy { it.name }
        } else {
            failed = true
        }
    }

    fun toggleFailureFor(endpoint: Endpoint) {
        if (endpoint in failingEndpoints) {
            failingEndpoints.remove(endpoint)
        } else {
            failingEndpoints.add(endpoint)
        }
    }

    fun shouldFailFor(requestUrl: String): Boolean {
        val cleanedUrl = Uri.parse(requestUrl).buildUpon().clearQuery().build().toString()

        val matchingEndpoints = endpoints.filter { endpoint ->
            endpoint.regex.matches(cleanedUrl)
        }

        val matchingEndpoint = if (matchingEndpoints.size > 1) {
            // If multiple endpoints match, find the one that matches exactly
            matchingEndpoints.single { it.url == cleanedUrl }
        } else {
            matchingEndpoints.firstOrNull()
        }

        return matchingEndpoint.takeIf { it in failingEndpoints } != null
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
        Log.d("DevToolsStore", "Failed to load API endpoints", t)
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

    // The results will be:
    // - urlParams: /consumers/paymentdetails/{paymentId}
    // - regexParams: /consumers/paymentdetails/(.+)
    // We use the former in the UI, and the latter to perform the actual matching
    val urlParams = valueParameters.map { "{${it.name}}" }
    val regexParams = valueParameters.map { "(.+)" }

    val url = javaMethod?.invoke(obj, *urlParams.toTypedArray()) as? String ?: return null
    val regex = javaMethod?.invoke(obj, *regexParams.toTypedArray()) as? String ?: return null

    return Endpoint(url, regex.toRegex())
}
