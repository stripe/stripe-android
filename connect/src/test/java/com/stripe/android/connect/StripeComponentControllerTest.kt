@file:OptIn(PrivateBetaConnectSDK::class)

package com.stripe.android.connect

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class StripeComponentControllerTest {

    private val activityController = Robolectric.buildActivity(TestActivity::class.java)

    @Test
    fun `init initializes the dialog fragment with listeners`() {
        val activity = activityController.create().get()
        val controller = activity.controller
        val df = controller.dialogFragment
        assertThat(df.listener).isEqualTo(controller.listener)
        assertThat(df.onDismissListener).isEqualTo(controller.onDismissListener)
    }

    @Test
    fun `show and dismiss shows and dismisses the dialog fragment respectively`() {
        val controller = activityController.create().start().resume().get().controller
        assertThat(controller.dialogFragment.isAdded).isFalse()

        controller.show()
        awaitMainLooper()
        assertThat(controller.dialogFragment.isAdded).isTrue()

        controller.dismiss()
        awaitMainLooper()
        assertThat(controller.dialogFragment.isAdded).isFalse()
    }

    @Test
    fun `existing dialog fragment is reused`() {
        val activity = activityController.create().start().resume().get()

        // Use first controller to show the DF.
        val controller = TestComponentController(activity, mock())
        val dialogFragment = controller.dialogFragment
        controller.show()
        awaitMainLooper()

        // Second controller should obtain the DF that was added.
        val newController = TestComponentController(activity, mock())
        assertThat(newController.dialogFragment).isSameInstanceAs(dialogFragment)
    }

    @Test
    fun `dialog fragment is able to create component view on recreate`() {
        // Show the DF
        val firstActivity = activityController.create().start().resume().get()
        firstActivity.controller.show()
        awaitMainLooper()
        val firstDialogFragment = firstActivity.controller.dialogFragment

        // Recreate activity to simulate a config change.
        val secondActivity = activityController.recreate().get()
        val secondDialogFragment = secondActivity.controller.dialogFragment

        // New DF should have been recreated by the OS and still added.
        assertThat(secondDialogFragment).isNotSameInstanceAs(firstDialogFragment)
        assertThat(secondDialogFragment.isAdded).isTrue()

        // The component view should be able to be recreated from the ECM cached in the VM.
        val componentView =
            (secondDialogFragment.view as ViewGroup)
                .children.filterIsInstance<TestComponentView>()
                .firstOrNull()
        assertThat(componentView).isNotNull()
        assertThat(componentView?.listener).isEqualTo(secondActivity.listener)
    }

    private fun awaitMainLooper() {
        ShadowLooper.shadowMainLooper().idle()
    }

    internal class TestActivity : FragmentActivity() {
        lateinit var controller: StripeComponentController<TestComponentListener, *>
        lateinit var embeddedComponentManager: EmbeddedComponentManager
        val onDismissListener = StripeComponentController.OnDismissListener { }
        val listener = object : TestComponentListener {}

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            embeddedComponentManager = mock()
            controller = TestComponentController(this, embeddedComponentManager)
            controller.onDismissListener = onDismissListener
            controller.listener = listener
        }
    }

    internal class TestComponentController(
        activity: FragmentActivity,
        embeddedComponentManager: EmbeddedComponentManager
    ) : StripeComponentController<TestComponentListener, EmptyProps>(
        dfClass = TestComponentDialogFragment::class.java,
        activity = activity,
        embeddedComponentManager = embeddedComponentManager,
    )

    internal class TestComponentDialogFragment :
        StripeComponentDialogFragment<TestComponentView, TestComponentListener, EmptyProps>() {

        override fun createComponentView(
            embeddedComponentManager: EmbeddedComponentManager
        ): TestComponentView {
            return TestComponentView(requireContext())
        }
    }

    internal class TestComponentView(
        context: Context
    ) : StripeComponentView<TestComponentListener, EmptyProps>(
        context = context,
        attrs = null,
        defStyleAttr = 0,
        embeddedComponent = StripeEmbeddedComponent.ACCOUNT_ONBOARDING,
        embeddedComponentManager = null,
        listener = null,
        props = null,
    ) {
        override var listener: TestComponentListener? = null

        override fun initialize(
            embeddedComponentManager: EmbeddedComponentManager,
            listener: TestComponentListener?,
            props: EmptyProps
        ) {
            // Nothing.
        }
    }

    internal interface TestComponentListener : StripeEmbeddedComponentListener
}
