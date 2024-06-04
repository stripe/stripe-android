package com.stripe.android.financialconnections.utils

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.serialization.json.Json

@Suppress("UNCHECKED_CAST")
internal object TestUtils {
    @Suppress("UNCHECKED_CAST")
    fun viewModelFactoryFor(viewModel: ViewModel) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return viewModel as T
        }
    }
}

internal fun testJson() = Json {
    coerceInputValues = true
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

internal class FakeActivityResultRegistry<T>(
    private val result: T
) : ActivityResultRegistry() {
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        dispatchResult(
            requestCode,
            result
        )
    }
}

internal class TestFragment : Fragment()
