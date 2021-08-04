package com.stripe.android.googlepaylauncher

import dagger.assisted.AssistedFactory

@AssistedFactory
interface GooglePayRepositoryFactory {
    fun create(googlePayEnvironment: GooglePayEnvironment) : GooglePayRepository
}
