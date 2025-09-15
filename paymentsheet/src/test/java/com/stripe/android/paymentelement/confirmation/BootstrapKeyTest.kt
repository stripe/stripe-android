package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Rule
import org.junit.Test

internal class BootstrapKeyTest {

    @get:Rule
    val testRule = CoroutineTestRule()

    @Test
    fun `PassiveCaptcha toValue should return value when key exists and type matches`() {
        val passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams()
        val metadata = mapOf<BootstrapKey<*>, Parcelable>(
            BootstrapKey.PassiveCaptcha to passiveCaptchaParams
        )

        val result = BootstrapKey.PassiveCaptcha.toValue(metadata)

        assertThat(result).isEqualTo(passiveCaptchaParams)
    }

    @Test
    fun `PassiveCaptcha toValue should return null when key is missing`() {
        val metadata = emptyMap<BootstrapKey<*>, Parcelable>()

        val result = BootstrapKey.PassiveCaptcha.toValue(metadata)

        assertThat(result).isNull()
    }

    @Test
    fun `PassiveCaptcha toValue should throw exception when value type is invalid`() {
        val metadata = mapOf<BootstrapKey<*>, Parcelable>(
            BootstrapKey.PassiveCaptcha to TestInvalidParcelable
        )

        val exception = runCatching {
            BootstrapKey.PassiveCaptcha.toValue(metadata)
        }.exceptionOrNull()

        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(exception?.message).isEqualTo("invalid value for PassiveCaptcha")
    }

    @Test
    fun `bootstrapHelper with PassiveCaptchaParams should call bootstrap with correct metadata`() = runTest {
        val fakeConfirmationHandler = FakeConfirmationHandler()
        val lifecycleOwner = TestLifecycleOwner()
        val passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams()

        fakeConfirmationHandler.bootstrapHelper(passiveCaptchaParams, lifecycleOwner)

        val bootstrapCall = fakeConfirmationHandler.bootstrapTurbine.awaitItem()

        assertThat(bootstrapCall.metadata).containsExactly(BootstrapKey.PassiveCaptcha, passiveCaptchaParams)
        assertThat(bootstrapCall.lifecycleOwner).isEqualTo(lifecycleOwner)
    }

    @Test
    fun `bootstrapHelper with Flow should call bootstrap when flow emits value`() = runTest {
        val fakeConfirmationHandler = FakeConfirmationHandler()
        val lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        val passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams()
        val flow = flowOf(
            null,
            passiveCaptchaParams,
            PassiveCaptchaParamsFactory.passiveCaptchaParams(siteKey = "another-site-key")
        )

        fakeConfirmationHandler.bootstrapHelper(flow, lifecycleOwner)

        // Should only take the first non-null value
        val bootstrapCall = fakeConfirmationHandler.bootstrapTurbine.awaitItem()

        assertThat(bootstrapCall.metadata).containsExactly(BootstrapKey.PassiveCaptcha, passiveCaptchaParams)
        assertThat(bootstrapCall.lifecycleOwner).isEqualTo(lifecycleOwner)

        // Should not emit more than once
        fakeConfirmationHandler.bootstrapTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `bootstrapHelper with Flow should not call bootstrap when flow only emits null`() = runTest {
        val fakeConfirmationHandler = FakeConfirmationHandler()
        val lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        val flow = flowOf<PassiveCaptchaParams?>(null, null)

        fakeConfirmationHandler.bootstrapHelper(flow, lifecycleOwner)

        // Should not call bootstrap when no non-null values are emitted
        fakeConfirmationHandler.bootstrapTurbine.ensureAllEventsConsumed()
    }

    @Parcelize
    private object TestInvalidParcelable : Parcelable
}
