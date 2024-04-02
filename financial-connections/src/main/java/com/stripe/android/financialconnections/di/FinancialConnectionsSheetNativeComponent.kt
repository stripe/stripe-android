package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentSubcomponent
import com.stripe.android.financialconnections.features.bankauthrepair.BankAuthRepairSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.error.ErrorSubcomponent
import com.stripe.android.financialconnections.features.exit.ExitSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerSubcomponent
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationSubcomponent
import com.stripe.android.financialconnections.features.manualentry.ManualEntrySubcomponent
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessSubcomponent
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupSubcomponent
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupSubcomponent
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationSubcomponent
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationSubcomponent
import com.stripe.android.financialconnections.features.notice.NoticeSheetSubcomponent
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

    // Exposed subcomponent factories.
    val consentSubcomponent: ConsentSubcomponent.Factory
    val institutionPickerSubcomponent: InstitutionPickerSubcomponent.Factory
    val accountPickerSubcomponent: AccountPickerSubcomponent.Factory
    val manualEntrySubcomponent: ManualEntrySubcomponent.Factory
    val manualEntrySuccessSubcomponent: ManualEntrySuccessSubcomponent.Factory
    val partnerAuthSubcomponent: PartnerAuthSubcomponent.Factory
    val bankAuthRepairSubcomponent: BankAuthRepairSubcomponent.Factory
    val successSubcomponent: SuccessSubcomponent.Factory
    val attachPaymentSubcomponent: AttachPaymentSubcomponent.Factory
    val resetSubcomponent: ResetSubcomponent.Factory
    val errorSubcomponent: ErrorSubcomponent.Factory
    val exitSubcomponent: ExitSubcomponent.Factory
    val noticeSheetSubcomponent: NoticeSheetSubcomponent.Factory
    val networkingLinkSignupSubcomponent: NetworkingLinkSignupSubcomponent.Factory
    val networkingLinkLoginWarmupSubcomponent: NetworkingLinkLoginWarmupSubcomponent.Factory
    val networkingLinkVerificationSubcomponent: NetworkingLinkVerificationSubcomponent.Factory
    val networkingSaveToLinkVerificationSubcomponent: NetworkingSaveToLinkVerificationSubcomponent.Factory
    val linkAccountPickerSubcomponent: LinkAccountPickerSubcomponent.Factory
    val linkStepUpVerificationSubcomponent: LinkStepUpVerificationSubcomponent.Factory

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

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
