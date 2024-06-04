package com.stripe.android.testing

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras

fun HasDefaultViewModelProviderFactory.fakeCreationExtras(): CreationExtras {
    return MutableCreationExtras(
        initialExtras = defaultViewModelCreationExtras,
    ).apply {
        set(ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY, "some_key")
    }
}
