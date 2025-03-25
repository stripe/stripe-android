@file:OptIn(PrivateBetaConnectSDK::class)

package com.stripe.android.connect.test

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.EmptyProps
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.StripeComponentController
import com.stripe.android.connect.StripeComponentDialogFragment
import com.stripe.android.connect.StripeComponentView
import com.stripe.android.connect.StripeEmbeddedComponent
import com.stripe.android.connect.StripeEmbeddedComponentListener
import com.stripe.android.connect.manager.EmbeddedComponentCoordinator
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class TestActivity : FragmentActivity() {
    lateinit var controller: StripeComponentController<TestComponentListener, *>
    lateinit var embeddedComponentManager: EmbeddedComponentManager
    val embeddedComponentCoordinator: EmbeddedComponentCoordinator = mock()
    val onDismissListener = StripeComponentController.OnDismissListener { }
    val listener = object : TestComponentListener {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        embeddedComponentManager = mock<EmbeddedComponentManager> {
            on { this.coordinator }.doReturn(embeddedComponentCoordinator)
        }
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
