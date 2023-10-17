package com.stripe.android.customersheet

import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCustomerSheetApi::class)
internal interface CustomerSheetLoader {
    suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState>
}

@OptIn(ExperimentalCustomerSheetApi::class)
@Suppress("unused")
internal class DefaultCustomerSheetLoader @Inject constructor(
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val lpmRepository: LpmRepository,
    private val customerAdapter: CustomerAdapter,
) : CustomerSheetLoader {
    override suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState> {
        TODO("Not yet implemented")
    }
}
