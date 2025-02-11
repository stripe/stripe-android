package com.stripe.android.paymentelement.confirmation

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.DefaultLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.injection.ExtendedPaymentElementConfirmationModule
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.FakeDurationProvider
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal class ExtendedPaymentElementConfirmationTestActivity : AppCompatActivity() {
    val viewModel: TestViewModel by viewModels {
        TestViewModel.Factory
    }

    val confirmationHandler by lazy {
        viewModel.confirmationHandler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmationHandler.register(activityResultCaller = this, lifecycleOwner = this)
    }

    class TestViewModel @Inject constructor(
        confirmationHandlerFactory: ConfirmationHandler.Factory
    ) : ViewModel() {
        val confirmationHandler = confirmationHandlerFactory.create(viewModelScope)

        object Factory : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val component = DaggerExtendedPaymentElementConfirmationTestComponent.builder()
                    .application(extras.requireApplication())
                    .allowsManualConfirmation(allowsManualConfirmation = false)
                    .statusBarColor(null)
                    .savedStateHandle(extras.createSavedStateHandle())
                    .build()

                @Suppress("UNCHECKED_CAST")
                return component.viewModel as T
            }
        }
    }
}

@Component(
    modules = [
        ExtendedPaymentElementConfirmationModule::class,
        CoroutineContextModule::class,
        ExtendedPaymentElementConfirmationTestModule::class,
    ]
)
@Singleton
internal interface ExtendedPaymentElementConfirmationTestComponent {
    val viewModel: ExtendedPaymentElementConfirmationTestActivity.TestViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun statusBarColor(@Named(STATUS_BAR_COLOR) statusBarColor: Int?): Builder

        @BindsInstance
        fun allowsManualConfirmation(@Named(ALLOWS_MANUAL_CONFIRMATION) allowsManualConfirmation: Boolean): Builder

        fun build(): ExtendedPaymentElementConfirmationTestComponent
    }
}

@Module
internal interface ExtendedPaymentElementConfirmationTestModule {
    @Binds
    fun bindsStripeRepository(repository: StripeApiRepository): StripeRepository

    @Binds
    fun bindLinkGateFactory(linkGateFactory: DefaultLinkGate.Factory): LinkGate.Factory

    companion object {
        @Provides
        fun providesContext(application: Application): Context = application

        @Provides
        fun providesErrorReporter(): ErrorReporter = FakeErrorReporter()

        @Provides
        fun provideDurationProvider(): DurationProvider = FakeDurationProvider()

        @Provides
        fun providesUserFacingLogger(): UserFacingLogger = FakeUserFacingLogger()

        @Provides
        fun providesAnalyticsRequestExecutor(): AnalyticsRequestExecutor = FakeAnalyticsRequestExecutor()

        @Provides
        fun providesGooglePayRepositoryFactory(): (GooglePayEnvironment) -> GooglePayRepository = { _ ->
            GooglePayRepository { flowOf(true) }
        }

        @Provides
        fun providesLogger(): Logger = FakeLogger()

        @Provides
        @Named(ENABLE_LOGGING)
        fun providesEnableLogging(): Boolean = false

        @Provides
        @Named(PRODUCT_USAGE)
        fun providesProductUsage(): Set<String> = setOf()

        @Provides
        fun providesPaymentConfiguration(): PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_123",
            stripeAccountId = null,
        )

        @Provides
        @Named(PUBLISHABLE_KEY)
        fun providesPublishableKey(config: PaymentConfiguration): () -> String = { config.publishableKey }

        @Provides
        @Named(STRIPE_ACCOUNT_ID)
        fun providesStripeAccountId(config: PaymentConfiguration): () -> String? = { config.stripeAccountId }

        @Provides
        @Singleton
        fun providesFakeLinkConfigurationCoordinator(): LinkConfigurationCoordinator =
            FakeLinkConfigurationCoordinator()

        @Provides
        @Singleton
        fun providesLinkAccountHolder(savedStateHandle: SavedStateHandle): LinkAccountHolder {
            return LinkAccountHolder(savedStateHandle)
        }
    }
}
