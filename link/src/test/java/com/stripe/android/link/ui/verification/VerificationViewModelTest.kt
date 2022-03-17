package com.stripe.android.link.ui.verification

import androidx.lifecycle.Lifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class VerificationViewModelTest {
    private val linkAccountManager = mock<LinkAccountManager>()
    private val navigator = mock<Navigator>()
    private val logger = Logger.noop()
    private val linkAccount = mock<LinkAccount>()

    @Test
    fun `onResendCodeClicked triggers verification start`() = runTest {
        whenever(linkAccountManager.startVerification())
            .thenReturn(Result.success(mock()))

        val viewModel = createViewModel()
        viewModel.onResendCodeClicked()

        verify(linkAccountManager).startVerification()
    }

    @Test
    fun `When onVerificationCodeEntered succeeds then it navigates to Wallet`() = runTest {
        whenever(linkAccountManager.confirmVerification(any()))
            .thenReturn(Result.success(mock()))

        val viewModel = createViewModel()
        viewModel.onVerificationCodeEntered("code")

        verify(navigator).navigateTo(LinkScreen.Wallet, true)
    }

    @Test
    fun `onChangeEmailClicked triggers logout`() = runTest {
        createViewModel().onChangeEmailClicked()
        verify(linkAccountManager).logout()
    }

    @Test
    fun `onBack triggers logout`() = runTest {
        createViewModel().onBack()
        verify(linkAccountManager).logout()
    }

    @Test
    fun `Factory gets initialized by Injector`() {
        val mockBuilder = mock<SignedInViewModelSubcomponent.Builder>()
        val mockSubComponent = mock<SignedInViewModelSubcomponent>()
        val vmToBeReturned = mock<VerificationViewModel>()

        whenever(mockBuilder.linkAccount(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.build()).thenReturn(mockSubComponent)
        whenever((mockSubComponent.verificationViewModel)).thenReturn(vmToBeReturned)

        val mockSavedStateRegistryOwner = mock<SavedStateRegistryOwner>()
        val mockSavedStateRegistry = mock<SavedStateRegistry>()
        val mockLifeCycle = mock<Lifecycle>()

        whenever(mockSavedStateRegistryOwner.savedStateRegistry).thenReturn(mockSavedStateRegistry)
        whenever(mockSavedStateRegistryOwner.lifecycle).thenReturn(mockLifeCycle)
        whenever(mockLifeCycle.currentState).thenReturn(Lifecycle.State.CREATED)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as VerificationViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }
        WeakMapInjectorRegistry.register(injector, DUMMY_INJECTOR_KEY)
        val factory = VerificationViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(VerificationViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(vmToBeReturned)

        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    private fun createViewModel() = VerificationViewModel(
        linkAccountManager, navigator, logger, linkAccount
    )
}
