package com.stripe.android.checkout.analytics

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.UIContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class CheckoutConfirmationContextHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @UIContext private val uiContext: CoroutineContext,
) {
    @get:MainThread
    @set:MainThread
    var context: CheckoutConfirmationContext?
        get() = savedStateHandle[CONFIRMATION_CONTEXT_KEY]
        set(value) {
            if (value == null) {
                savedStateHandle.remove<CheckoutConfirmationContext>(CONFIRMATION_CONTEXT_KEY)
            } else {
                savedStateHandle[CONFIRMATION_CONTEXT_KEY] = value
            }
        }

    suspend fun setRequestId(requestId: String?) = withContext(uiContext) {
        context = context?.copy(requestId = requestId)
    }

    private companion object {
        const val CONFIRMATION_CONTEXT_KEY = "CheckoutAnalyticsPerformer.confirmationContext"
    }
}
