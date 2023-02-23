package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import java.util.Objects

/**
 * Model for a Stripe Customer object
 */
@Parcelize
class Customer internal constructor(
    val id: String?,
    val defaultSource: String?,
    val shippingInformation: ShippingInformation?,
    val sources: List<CustomerPaymentSource>,
    val hasMore: Boolean,
    val totalCount: Int?,
    val url: String?,
    val description: String?,
    val email: String?,
    val liveMode: Boolean,
) : StripeModel {

    fun getSourceById(sourceId: String): CustomerPaymentSource? {
        return sources.firstOrNull { it.id == sourceId }
    }

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            defaultSource,
            shippingInformation,
            sources,
            hasMore,
            totalCount,
            url,
            description,
            email,
            liveMode,
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is Customer &&
            id == other.id &&
            defaultSource == other.defaultSource &&
            shippingInformation == other.shippingInformation &&
            sources == other.sources &&
            hasMore == other.hasMore &&
            totalCount == other.totalCount &&
            url == other.url &&
            description == other.description &&
            email == other.email &&
            liveMode == other.liveMode
    }

    override fun toString(): String {
        return "Customer(" +
            "id=$id, " +
            "defaultSource=$defaultSource, " +
            "shippingInformation=$shippingInformation, " +
            "sources=$sources, " +
            "hasMore=$hasMore, " +
            "totalCount=$totalCount, " +
            "url=$url, " +
            "description=$description, " +
            "email=$email, " +
            "liveMode=$liveMode)"
    }

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun copy(
        id: String? = this.id,
        defaultSource: String? = this.defaultSource,
        shippingInformation: ShippingInformation? = this.shippingInformation,
        sources: List<CustomerPaymentSource> = this.sources,
        hasMore: Boolean = this.hasMore,
        totalCount: Int? = this.totalCount,
        url: String? = this.url,
        description: String? = this.description,
        email: String? = this.email,
        liveMode: Boolean = this.liveMode,
    ): Customer {
        return Customer(
            id = id,
            defaultSource = defaultSource,
            shippingInformation = shippingInformation,
            sources = sources,
            hasMore = hasMore,
            totalCount = totalCount,
            url = url,
            description = description,
            email = email,
            liveMode = liveMode,
        )
    }

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component1(): String? = id

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component2(): String? = defaultSource

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component3(): ShippingInformation? = shippingInformation

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component4(): List<CustomerPaymentSource> = sources

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component5(): Boolean = hasMore

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component6(): Int? = totalCount

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component7(): String? = url

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component8(): String? = description

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component9(): String? = email

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release",
    )
    fun component10(): Boolean = liveMode
}
