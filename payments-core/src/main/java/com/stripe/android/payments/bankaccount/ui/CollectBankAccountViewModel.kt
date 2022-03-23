package com.stripe.android.payments.bankaccount.ui

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.payments.bankaccount.di.DaggerCollectBankAccountComponent
import com.stripe.android.payments.bankaccount.domain.AttachLinkAccountSession
import com.stripe.android.payments.bankaccount.domain.CreateLinkAccountSession
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
internal class CollectBankAccountViewModel @Inject constructor(
    private val args: CollectBankAccountContract.Args,
    private val createLinkAccountSession: CreateLinkAccountSession,
    private val attachLinkAccountSession: AttachLinkAccountSession,
    private val logger: Logger
) : ViewModel() {

    private val _viewEffect = MutableSharedFlow<CollectBankAccountViewEffect>()
    val viewEffect: SharedFlow<CollectBankAccountViewEffect> = _viewEffect

    init {
        viewModelScope.launch {
            createLinkAccountSession()
        }
    }

    private suspend fun createLinkAccountSession() {
        // TODO@Carlos call createLinkAccountSession
    }

    class Factory(
        private val applicationSupplier: () -> Application,
        private val argsSupplier: () -> CollectBankAccountContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            return DaggerCollectBankAccountComponent.builder()
                .application(applicationSupplier())
                .configuration(argsSupplier()).build()
                .viewModel as T
        }
    }
}
