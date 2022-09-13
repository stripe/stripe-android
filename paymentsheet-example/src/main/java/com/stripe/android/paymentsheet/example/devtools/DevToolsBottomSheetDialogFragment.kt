package com.stripe.android.paymentsheet.example.devtools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stripe.android.core.networking.StripeNetworkClientInterceptor
import java.time.format.TextStyle
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter

class DevToolsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DevTools()
            }
        }
    }

    companion object {
        fun newInstance(): DevToolsBottomSheetDialogFragment = DevToolsBottomSheetDialogFragment()
    }
}

@Composable
private fun DevTools() {
    val endpoints = remember { stripeApiEndpoints() }

    Column {
        Text(
            text = "https://api.stripe.com/v1",
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        for (endpoint in endpoints) {
            val text = endpoint.removePrefix("https://api.stripe.com/v1/")
            val isTurnedOn = endpoint in ApiStore.failingEndpoints
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { ApiStore.toggle(endpoint) }
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isTurnedOn) {
                    Image(imageVector = Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}

object ApiStore {
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

fun stripeApiEndpoints(): List<String> {
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
