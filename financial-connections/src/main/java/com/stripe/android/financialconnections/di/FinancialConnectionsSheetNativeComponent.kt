package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerViewModel
import com.stripe.android.financialconnections.features.accountupdate.AccountUpdateRequiredViewModel
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentViewModel
import com.stripe.android.financialconnections.features.consent.ConsentViewModel
import com.stripe.android.financialconnections.features.error.ErrorViewModel
import com.stripe.android.financialconnections.features.exit.ExitViewModel
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerViewModel
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerViewModel
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationViewModel
import com.stripe.android.financialconnections.features.manualentry.ManualEntryViewModel
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessViewModel
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupViewModel
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupViewModel
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationViewModel
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationViewModel
import com.stripe.android.financialconnections.features.notice.NoticeSheetViewModel
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthViewModel
import com.stripe.android.financialconnections.features.reset.ResetViewModel
import com.stripe.android.financialconnections.features.success.SuccessViewModel
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named

@ActivityRetainedScope
@Component(
    dependencies = [FinancialConnectionsSingletonSharedComponent::class],
    modules = [
        FinancialConnectionsSheetNativeModule::class,
        FinancialConnectionsSheetSharedModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeComponent {
    fun inject(financialConnectionsSheetNativeActivity: FinancialConnectionsSheetNativeActivity)

    val viewModel: FinancialConnectionsSheetNativeViewModel

    val consentViewModelFactory: ConsentViewModel.Factory
    val institutionPickerViewModelFactory: InstitutionPickerViewModel.Factory
    val accountPickerViewModelFactory: AccountPickerViewModel.Factory
    val manualEntryViewModelFactory: ManualEntryViewModel.Factory
    val manualEntrySuccessViewModelFactory: ManualEntrySuccessViewModel.Factory
    val partnerAuthViewModelFactory: PartnerAuthViewModel.Factory
    val successViewModelFactory: SuccessViewModel.Factory
    val attachPaymentViewModelFactory: AttachPaymentViewModel.Factory
    val resetViewModelFactory: ResetViewModel.Factory
    val errorViewModelFactory: ErrorViewModel.Factory
    val exitViewModelFactory: ExitViewModel.Factory
    val noticeSheetViewModelFactory: NoticeSheetViewModel.Factory
    val networkingLinkSignupViewModelFactory: NetworkingLinkSignupViewModel.Factory
    val networkingLinkLoginWarmupViewModelFactory: NetworkingLinkLoginWarmupViewModel.Factory
    val networkingLinkVerificationViewModelFactory: NetworkingLinkVerificationViewModel.Factory
    val networkingSaveToLinkVerificationViewModelFactory: NetworkingSaveToLinkVerificationViewModel.Factory
    val linkAccountPickerViewModelFactory: LinkAccountPickerViewModel.Factory
    val linkStepUpVerificationViewModelFactory: LinkStepUpVerificationViewModel.Factory
    val accountUpdateRequiredViewModelFactory: AccountUpdateRequiredViewModel.Factory

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun initialSyncResponse(
            @Named(INITIAL_SYNC_RESPONSE) initialSyncResponse: SynchronizeSessionResponse?
        ): Builder

        @BindsInstance
        fun savedStateHandle(
            savedStateHandle: SavedStateHandle
        ): Builder

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun sharedComponent(component: FinancialConnectionsSingletonSharedComponent): Builder

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
