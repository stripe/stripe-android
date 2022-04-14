package com.stripe.android.link

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.StripeIntentFixtures
import com.stripe.android.link.utils.FakeAndroidKeyStore
import com.stripe.android.link.utils.InjectableActivityScenario
import com.stripe.android.link.utils.injectableActivityScenario
import com.stripe.android.link.utils.viewModelFactoryFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class LinkActivityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val args = LinkActivityContract.Args(
        StripeIntentFixtures.PI_SUCCEEDED,
        "Example, Inc."
    )
    private val intent = LinkActivityContract().createIntent(context, args)

    private val linkAccountManager = mock<LinkAccountManager>().apply {
        whenever(linkAccount).thenReturn(MutableStateFlow(mock()))
    }
    private val confirmationManager = mock<ConfirmationManager>()
    private val navigator = mock<Navigator>()

    private val viewModel = LinkActivityViewModel(
        args,
        linkAccountManager,
        navigator,
        confirmationManager
    )

    init {
        FakeAndroidKeyStore.setup()
        PaymentConfiguration.init(context, "publishable_key")
    }

    @Test
    fun `When consumer does not exist then it navigates to SignUp screen`() {
        whenever(linkAccountManager.accountStatus).thenReturn(flowOf(AccountStatus.SignedOut))

        activityScenario().launch(intent).onActivity {
            verify(navigator).navigateTo(
                argWhere {
                    it.route.startsWith(LinkScreen.SignUp.route.substringBefore('?'))
                },
                eq(true)
            )
        }
    }

    @Test
    fun `When consumer is verified then it navigates to Wallet screen`() = runTest {
        whenever(linkAccountManager.accountStatus).thenReturn(flowOf(AccountStatus.Verified))

        activityScenario().launch(intent).onActivity {
            verify(navigator).navigateTo(
                argWhere {
                    it.route == LinkScreen.Wallet.route
                },
                eq(true)
            )
        }
    }

    @Test
    fun `When consumer is not verified then it navigates to Verification screen`() = runTest {
        whenever(linkAccountManager.accountStatus).thenReturn(flowOf(AccountStatus.VerificationStarted))

        activityScenario().launch(intent).onActivity {
            verify(navigator).navigateTo(
                argWhere {
                    it.route == LinkScreen.Verification.route
                },
                eq(true)
            )
        }
    }

    private fun activityScenario(): InjectableActivityScenario<LinkActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }
}
