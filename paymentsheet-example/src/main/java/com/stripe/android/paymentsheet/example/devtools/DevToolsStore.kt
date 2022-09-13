package com.stripe.android.paymentsheet.example.devtools

import androidx.compose.runtime.mutableStateListOf
import com.stripe.android.core.networking.StripeNetworkClientInterceptor
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter

object DevToolsStore {
    val endpoints = stripeApiEndpoints()
    val failingEndpoints = mutableStateListOf<String>()

    fun toggle(endpoint: String) {
        if (endpoint in failingEndpoints) {
            failingEndpoints.remove(endpoint)
        } else {
            failingEndpoints.add(endpoint)
        }

        StripeNetworkClientInterceptor.evaluator = { requestUrl ->
            failingEndpoints.any { requestUrl.startsWith(it) }
        }
    }
}

private fun stripeApiEndpoints(): List<String> {
    val repoClass = Class.forName("com.stripe.android.networking.StripeApiRepository").kotlin
    val companionObject = repoClass.companionObjectInstance ?: return emptyList()

    return try {
        companionObject::class.declaredMemberProperties.mapNotNull { property ->
            property.getEndpointUrl(companionObject)
        }
    } catch (t: Throwable) {
        println(t)
        emptyList()
    }
}

private fun KProperty1<out Any, *>.getEndpointUrl(obj: Any): String? {
    val isEndpointUrl = name.endsWith("url", ignoreCase = true)
    val stringValue = javaGetter?.invoke(obj) as? String ?: return null
    return stringValue.takeIf { isEndpointUrl }
}
