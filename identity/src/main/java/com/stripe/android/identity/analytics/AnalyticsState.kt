package com.stripe.android.identity.analytics

import android.os.Parcelable
import com.stripe.android.identity.networking.models.DocumentUploadParam
import com.stripe.android.identity.states.IdentityScanState
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AnalyticsState(
    val scanType: IdentityScanState.ScanType? = null,
    val requireSelfie: Boolean? = null,
    val docFrontRetryTimes: Int? = null,
    val docBackRetryTimes: Int? = null,
    val selfieRetryTimes: Int? = null,
    val docFrontUploadType: DocumentUploadParam.UploadMethod? = null,
    val docBackUploadType: DocumentUploadParam.UploadMethod? = null,
    val docFrontModelScore: Float? = null,
    val docBackModelScore: Float? = null,
    val selfieModelScore: Float? = null,
    val docFrontBlurScore: Float? = null,
    val docBackBlurScore: Float? = null
) : Parcelable
