package com.stripe.android.paymentelement.callbacks

import android.os.Bundle
import android.os.Parcel
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class LifecyclePaymentElementCallbackReferencesRestoreTest {
    @get:Rule
    val activityRule = createTestActivityRule<TestActivity>()

    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `A key held by an existing consumer resolves callbacks after activity recreation`() = runScenario {
        val previousActivity = controller.get()
        val previousReferences = references(previousActivity)
        previousReferences[KEY] = createCallbacks("before recreation")
        val existingKey = previousReferences.key(KEY)

        controller.recreate()

        assertThat(controller.get()).isNotSameInstanceAs(previousActivity)
        val currentReferences = references(controller.get())
        val currentCallbacks = createCallbacks("after recreation")
        currentReferences[KEY] = currentCallbacks
        previousReferences[KEY] = createCallbacks("late update")

        assertThat(currentReferences.key(KEY)).isEqualTo(existingKey)
        assertThat(PaymentElementCallbackReferences[existingKey]).isSameInstanceAs(currentCallbacks)
    }

    @Test
    fun `Saved state restores the UUID for a new host and ViewModel store`() = runScenario {
        val previousReferences = references(controller.get())
        previousReferences[KEY] = createCallbacks("before restoration")
        val existingKey = previousReferences.key(KEY)
        val savedState = Bundle()
        controller.pause().saveInstanceState(savedState).stop().destroy()

        val restored = Robolectric.buildActivity(TestActivity::class.java)
            .create(parcelSavedState(savedState)).start().resume()
        try {
            val restoredReferences = references(restored.get())
            val restoredCallbacks = createCallbacks("after restoration")
            restoredReferences[KEY] = restoredCallbacks

            assertThat(restoredReferences.key(KEY)).isEqualTo(existingKey)
            assertThat(PaymentElementCallbackReferences[existingKey]).isSameInstanceAs(restoredCallbacks)
        } finally {
            restored.pause().stop().destroy()
        }
    }

    @Test
    fun `An old lifecycle cannot clear callbacks after its scope is rebound`() = runScenario {
        val firstOwner = TestLifecycleOwner()
        val nextOwner = TestLifecycleOwner()
        val oldReferences = LifecyclePaymentElementCallbackReferences.get(firstOwner.lifecycle, controller.get(), KEY)
        val existingKey = oldReferences.key(KEY)
        oldReferences[KEY] = createCallbacks("old")

        val currentReferences = LifecyclePaymentElementCallbackReferences.get(
            nextOwner.lifecycle,
            controller.get(),
            KEY,
        )
        val callbacks = createCallbacks("current")
        currentReferences[KEY] = callbacks
        firstOwner.currentState = Lifecycle.State.DESTROYED
        oldReferences[KEY] = createCallbacks("stale")

        assertThat(currentReferences.key(KEY)).isEqualTo(existingKey)
        assertThat(PaymentElementCallbackReferences[existingKey]).isSameInstanceAs(callbacks)
        nextOwner.currentState = Lifecycle.State.DESTROYED
        assertThat(PaymentElementCallbackReferences[existingKey]).isNull()
    }

    @Test
    fun `A parcelled key retains its UUID and resolves the same callbacks`() = runScenario {
        val references = references(controller.get())
        val callbacks = createCallbacks("parcelled")
        references[KEY] = callbacks
        val key = references.key(KEY)
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(key, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val restored = requireNotNull(parcel.readParcelable<CallbacksKey>(CallbacksKey::class.java.classLoader))

            assertThat(restored).isEqualTo(key)
            assertThat(PaymentElementCallbackReferences[restored]).isSameInstanceAs(callbacks)
        } finally {
            parcel.recycle()
        }
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val controller = Robolectric.buildActivity(TestActivity::class.java).setup()
        try {
            Scenario(controller).block()
        } finally {
            if (controller.get().lifecycle.currentState != Lifecycle.State.DESTROYED) {
                controller.pause().stop().destroy()
            }
        }
    }

    private class Scenario(val controller: ActivityController<TestActivity>)

    private fun references(activity: TestActivity): LifecyclePaymentElementCallbackReferences {
        return LifecyclePaymentElementCallbackReferences.get(activity.lifecycle, activity, KEY)
    }

    private fun createCallbacks(name: String): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder().createIntentCallback { _, _ -> error(name) }.build()
    }

    private fun parcelSavedState(state: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(state)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    class TestActivity : AppCompatActivity()

    private companion object {
        const val KEY = "PaymentSheet"
    }
}
