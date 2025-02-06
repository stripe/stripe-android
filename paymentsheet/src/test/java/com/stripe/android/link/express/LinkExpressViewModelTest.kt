package com.stripe.android.link.express

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.FakeNativeLinkComponent
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LinkExpressViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun test() {
        var result: LinkExpressResult? = null
        val viewModel = viewModel {
            result = it
        }

        viewModel.onVerificationSucceeded(TestFactory.LINK_ACCOUNT)

        assertThat(result).isEqualTo(
            LinkExpressResult.Authenticated(
                linkAccountUpdate = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
            )
        )
    }

    @Test
    fun test2() {
        var result: LinkExpressResult? = null
        val viewModel = viewModel {
            result = it
        }

        viewModel.onDismissClicked()

        assertThat(result).isEqualTo(
            LinkExpressResult.Authenticated(
                linkAccountUpdate = LinkAccountUpdate.None
            )
        )
    }

    @Test
    fun test3() {
        var result: LinkExpressResult? = null
        val viewModel = viewModel {
            result = it
        }

        viewModel.onChangeEmailClicked()

        assertThat(result).isNull()
    }

    private fun viewModel(
        activityRetainedComponent: NativeLinkComponent = FakeNativeLinkComponent(),
        linkAccount: LinkAccount = TestFactory.LINK_ACCOUNT,
        dismissWithResult: (LinkExpressResult) -> Unit = {}
    ): LinkExpressViewModel {
        return LinkExpressViewModel(
            activityRetainedComponent = activityRetainedComponent,
            linkAccount = linkAccount
        ).apply {
            this.dismissWithResult = dismissWithResult
        }
    }
}
