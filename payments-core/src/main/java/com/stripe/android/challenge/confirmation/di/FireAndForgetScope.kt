package com.stripe.android.challenge.confirmation.di

import javax.inject.Qualifier

/**
 * [Qualifier] for coroutine scope used for a fire-and-forget operation.
 */
@Qualifier
internal annotation class FireAndForgetScope
