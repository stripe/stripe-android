package com.stripe.android.financialconnections.model

import android.os.Parcelable
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 *
 * @param `data`
 * @param hasMore True if this list has another page of items after this one that can be fetched.
 * @param url The URL where this list can be accessed.
 * @param count
 * @param totalCount
 */
@Parcelize
@Serializable
data class FinancialConnectionsAccountList(
    @SerialName("data")
    val data: List<FinancialConnectionsAccount>,

    /* True if this list has another page of items after this one that can be fetched. */
    @SerialName("has_more")
    val hasMore: Boolean,

    /* The URL where this list can be accessed. */
    @SerialName("url")
    val url: String,

    @SerialName("count")
    val count: Int? = null,

    @SerialName("total_count")
    val totalCount: Int? = null
) : StripeModel, Parcelable
