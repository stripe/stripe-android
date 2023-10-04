package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentSubcomponent
import com.stripe.android.financialconnections.features.bankauthrepair.BankAuthRepairSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerSubcomponent
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationSubcomponent
import com.stripe.android.financialconnections.features.manualentry.ManualEntrySubcomponent
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessSubcomponent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupSubcomponent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupSubcomponent
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationSubcomponent
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationSubcomponent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthSubcomponent
import com.stripe.android.financialconnections.features.reset.ResetSubcomponent
import com.stripe.android.financialconnections.features.success.SuccessSubcomponent
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetNativeModule::class,
        FinancialConnectionsSheetSharedModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeComponent {
    fun inject(financialConnectionsSheetNativeActivity: FinancialConnectionsSheetNativeActivity)

    val viewModel: FinancialConnectionsSheetNativeViewModel

    // Exposed subcomponent builders.
    val consentBuilder: ConsentSubcomponent.Builder
    val institutionPickerBuilder: InstitutionPickerSubcomponent.Builder
    val accountPickerBuilder: AccountPickerSubcomponent.Builder
    val manualEntryBuilder: ManualEntrySubcomponent.Builder
    val manualEntrySuccessBuilder: ManualEntrySuccessSubcomponent.Builder
    val partnerAuthSubcomponent: PartnerAuthSubcomponent.Builder
    val bankAuthRepairSubcomponent: BankAuthRepairSubcomponent.Builder
    val successSubcomponent: SuccessSubcomponent.Builder
    val attachPaymentSubcomponent: AttachPaymentSubcomponent.Builder
    val resetSubcomponent: ResetSubcomponent.Builder
    val networkingLinkSignupSubcomponent: NetworkingLinkSignupSubcomponent.Builder
    val networkingLinkLoginWarmupSubcomponent: NetworkingLinkLoginWarmupSubcomponent.Builder
    val networkingLinkVerificationSubcomponent: NetworkingLinkVerificationSubcomponent.Builder
    val networkingSaveToLinkVerificationSubcomponent: NetworkingSaveToLinkVerificationSubcomponent.Builder
    val linkAccountPickerSubcomponent: LinkAccountPickerSubcomponent.Builder
    val linkStepUpVerificationSubcomponent: LinkStepUpVerificationSubcomponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun initialSyncResponse(
            @Named(INITIAL_SYNC_RESPONSE) initialSyncResponse: SynchronizeSessionResponse?
        ): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
