package com.stripe.android.hcaptcha

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal interface HCaptchaCacheStorage : MutableStateFlow<CachedResult>

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal object DefaultHCaptchaCacheStorage :
    HCaptchaCacheStorage,
    MutableStateFlow<CachedResult> by MutableStateFlow(CachedResult.Idle)
