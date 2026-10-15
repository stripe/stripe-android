package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
internal class FraudDetectionDataRepositoryInjectionTest {
    @Test
    fun `repository is shared inside a component and distinct across components`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dispatcher = StandardTestDispatcher()
        val factory = DaggerFraudDetectionTestComponent.factory()
        val first = factory.create(context, dispatcher)
        val second = factory.create(context, dispatcher)

        assertThat(first.consumer.repository).isSameInstanceAs(first.repository)
        assertThat(second.repository).isNotSameInstanceAs(first.repository)
    }
}

internal class FraudDetectionTestConsumer @Inject constructor(val repository: FraudDetectionDataRepository)

@Singleton
@Component(modules = [StripeRepositoryModule::class])
internal interface FraudDetectionTestComponent {
    val repository: FraudDetectionDataRepository
    val consumer: FraudDetectionTestConsumer

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
            @BindsInstance @IOContext workContext: CoroutineContext,
        ): FraudDetectionTestComponent
    }
}
