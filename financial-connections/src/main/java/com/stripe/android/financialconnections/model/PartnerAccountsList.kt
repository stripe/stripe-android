package com.stripe.android.financialconnections.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 * @param `data`
 * @param hasMore True if this list has another page of items after this one that can be fetched.
 * @param nextPane
 * @param url The URL where this list can be accessed.
 * @param count
 * @param repairAuthorizationEnabled
 * @param totalCount
 **/
@Serializable
internal data class PartnerAccountsList(

    @SerialName(value = "data") @Required val data: List<PartnerAccount>,

    @SerialName(value = "has_more") @Required val hasMore: Boolean,

    @SerialName(value = "next_pane") @Required val nextPane: FinancialConnectionsSessionManifest.Pane,

    @SerialName(value = "url") @Required val url: String,

    @SerialName(value = "count") val count: Int? = null,

    @SerialName(value = "repair_authorization_enabled") val repairAuthorizationEnabled: Boolean? = null,

    @SerialName(value = "skip_account_selection") val skipAccountSelection: Boolean? = null,

    @SerialName(value = "total_count") val totalCount: Int? = null

)

/**
 *
 *
 * @param authorization
 * @param category
 * @param id
 * @param name
 * @param subcategory
 * @param supportedPaymentMethodTypes
 * @param balanceAmount
 * @param currency
 * @param displayableAccountNumbers
 * @param initialBalanceAmount
 * @param institutionName
 * @param institutionUrl
 * @param linkedAccountId
 * @param routingNumber
 * @param status
 */
@Serializable
@Suppress("ConstructorParameterNaming")
internal data class PartnerAccount(

    @SerialName(value = "authorization") @Required val authorization: String,

    @SerialName(value = "category") @Required val category: FinancialConnectionsAccount.Category,

    @SerialName(value = "id") @Required val id: String,

    @SerialName(value = "name") @Required val name: String,

    @SerialName(value = "subcategory") @Required val subcategory: FinancialConnectionsAccount.Subcategory,

    @SerialName(value = "supported_payment_method_types")
    @Required
    val supportedPaymentMethodTypes: List<FinancialConnectionsAccount.SupportedPaymentMethodTypes>,

    @SerialName(value = "balance_amount") val balanceAmount: Int? = null,

    @SerialName(value = "currency") val currency: String? = null,

    @SerialName(value = "institution") val institution: FinancialConnectionsInstitution? = null,

    @SerialName(value = "displayable_account_numbers") val displayableAccountNumbers: String? = null,

    @SerialName(value = "initial_balance_amount") val initialBalanceAmount: Int? = null,

    @SerialName(value = "institution_name") val institutionName: String? = null,

    @SerialName(value = "allow_selection") private val _allowSelection: Boolean? = null,

    @SerialName(value = "allow_selection_message") val allowSelectionMessage: String? = null,

    @SerialName(value = "institution_url") val institutionUrl: String? = null,

    @SerialName(value = "linked_account_id") val linkedAccountId: String? = null,

    @SerialName(value = "routing_number") val routingNumber: String? = null,

    @SerialName(value = "status") val status: FinancialConnectionsAccount.Status? = null

) {

    internal val allowSelection: Boolean
        get() = _allowSelection ?: true

    internal val encryptedNumbers get() = displayableAccountNumbers?.let { "••••$it" } ?: ""

    internal val fullName get() = "${this.name} $encryptedNumbers"
}
