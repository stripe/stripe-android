package com.stripe.android.paymentsheet.injection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.ViewModelScope
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Singleton

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelScopeTest {
    @Test
    fun `cancelling the ViewModel scope cancels dependency jobs`() {
        val component = DaggerPaymentSheetViewModelScopeTestComponent.create()
        val ownerScope = component.scope
        val dependencyScope = component.scope
        try {
            val connectionJob = Job(dependencyScope.coroutineContext[Job])
            assertThat(connectionJob.isActive).isTrue()

            ownerScope.cancel()

            assertThat(connectionJob.isCancelled).isTrue()
        } finally {
            ownerScope.cancel()
            dependencyScope.cancel()
        }
    }
}

@Singleton
@Component(modules = [PaymentSheetViewModelModule::class])
internal interface PaymentSheetViewModelScopeTestComponent {
    @get:ViewModelScope
    val scope: CoroutineScope
}
