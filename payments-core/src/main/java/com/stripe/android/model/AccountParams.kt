package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * [Create an account token](https://stripe.com/docs/api/tokens/create_account)
 *
 * Creates a single-use token that wraps a user’s legal entity information. Use this when creating
 * or updating a Connect account. See the
 * [account tokens documentation](https://stripe.com/docs/connect/account-tokens) to learn more.
 */
@Parcelize
data class AccountParams internal constructor(
    /**
     * Whether the user described by the data in the token has been shown the
     * [Stripe Connected Account Agreement](https://stripe.com/docs/connect/account-tokens#stripe-connected-account-agreement).
     * When creating an account token to create a new Connect account, this value must be `true`.
     *
     * [account.tos_shown_and_accepted](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted)
     */
    private val tosShownAndAccepted: Boolean,

    private val businessTypeParams: BusinessTypeParams? = null
) : TokenParams(Token.Type.Account) {
    override val typeDataParams: Map<String, Any>
        get() = mapOf(PARAM_TOS_SHOWN_AND_ACCEPTED to tosShownAndAccepted)
            .plus(
                businessTypeParams?.let { params ->
                    mapOf(
                        PARAM_BUSINESS_TYPE to params.type.code,
                        params.type.code to params.toParamMap()
                    )
                }.orEmpty()
            )

    /**
     * The business type.
     *
     * [account.business_type](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-business_type)
     */
    enum class BusinessType constructor(val code: String) {
        Individual("individual"),
        Company("company")
    }

    sealed class BusinessTypeParams(
        internal val type: BusinessType
    ) : StripeParamsModel, Parcelable {

        abstract val paramsList: List<Pair<String, Any?>>

        override fun toParamMap(): Map<String, Any> {
            return paramsList.fold(emptyMap()) { acc, (key, value) ->
                acc.plus(
                    value?.let { mapOf(key to it) }.orEmpty()
                )
            }
        }

        /**
         * Information about the company or business.
         *
         * [account.company](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company)
         */
        @Parcelize
        data class Company(
            /**
             * The company’s primary address.
             *
             * [account.company.address](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address)
             */
            var address: Address? = null,

            /**
             * The Kana variation of the company’s primary address (Japan only).
             *
             * [account.company.address_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address_kana)
             */
            var addressKana: AddressJapanParams? = null,

            /**
             * The Kanji variation of the company’s primary address (Japan only).
             *
             * [account.company.address_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address_kanji)
             */
            var addressKanji: AddressJapanParams? = null,

            /**
             * Whether the company’s directors have been provided. Set this Boolean to `true` after
             * creating all the company’s directors with the
             * [Persons API](https://stripe.com/docs/api/persons) for accounts with a
             * `relationship.director` requirement. This value is not automatically set to `true`
             * after creating directors, so it needs to be updated to indicate all directors have
             * been provided.
             *
             * [account.company.directors_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-directors_provided)
             */
            var directorsProvided: Boolean? = null,

            /**
             * Whether the company’s executives have been provided. Set this Boolean to `true` after
             * creating all the company’s executives with the
             * [Persons API](https://stripe.com/docs/api/persons) for accounts with a
             * `relationship.executive` requirement.
             *
             * [account.company.executives_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-executives_provided)
             */
            var executivesProvided: Boolean? = null,

            /**
             * The company’s legal name.
             *
             * [account.company.name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name)
             */
            var name: String? = null,

            /**
             * The Kana variation of the company’s legal name (Japan only).
             *
             * [account.company.name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name_kana)
             */
            var nameKana: String? = null,

            /**
             * The Kanji variation of the company’s legal name (Japan only).
             *
             * [account.company.name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name_kanji)
             */
            var nameKanji: String? = null,

            /**
             * Whether the company’s owners have been provided. Set this Boolean to `true` after
             * creating all the company’s owners with the
             * [Persons API](https://stripe.com/docs/api/persons)
             * for accounts with a `relationship.owner` requirement.
             *
             * [account.company.owners_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-owners_provided)
             */
            var ownersProvided: Boolean? = false,

            /**
             * The company’s phone number (used for verification).
             *
             * [account.company.phone](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-phone)
             */
            var phone: String? = null,

            /**
             * The business ID number of the company, as appropriate for the company’s country.
             * (Examples are an Employer ID Number in the U.S., a Business Number in Canada, or a
             * Company Number in the UK.)
             *
             * [account.company.tax_id](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-tax_id)
             */
            var taxId: String? = null,

            /**
             * The jurisdiction in which the `tax_id` is registered (Germany-based companies only).
             *
             * [account.company.tax_id_registrar](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-tax_id_registrar)
             */
            var taxIdRegistrar: String? = null,

            /**
             * The VAT number of the company.
             *
             * [account.company.vat_id](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-vat_id)
             */
            var vatId: String? = null,

            /**
             * Information on the verification state of the company.
             *
             * [account.company.verification](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-verification)
             */
            var verification: Verification? = null
        ) : BusinessTypeParams(BusinessType.Company) {

            override val paramsList: List<Pair<String, Any?>>
                get() = listOf(
                    PARAM_ADDRESS to address?.toParamMap(),
                    PARAM_ADDRESS_KANA to addressKana?.toParamMap(),
                    PARAM_ADDRESS_KANJI to addressKanji?.toParamMap(),
                    PARAM_DIRECTORS_PROVIDED to directorsProvided,
                    PARAM_EXECUTIVES_PROVIDED to executivesProvided,
                    PARAM_NAME to name,
                    PARAM_NAME_KANA to nameKana,
                    PARAM_NAME_KANJI to nameKanji,
                    PARAM_OWNERS_PROVIDED to ownersProvided,
                    PARAM_PHONE to phone,
                    PARAM_TAX_ID to taxId,
                    PARAM_TAX_ID_REGISTRAR to taxIdRegistrar,
                    PARAM_VAT_ID to vatId,
                    PARAM_VERIFICATION to verification?.toParamMap()
                )

            @Parcelize
            data class Verification(
                /**
                 * A document verifying the business.
                 */
                var document: Document? = null
            ) : StripeParamsModel, Parcelable {
                override fun toParamMap(): Map<String, Any> {
                    return document?.let {
                        mapOf(PARAM_DOCUMENT to it.toParamMap())
                    }.orEmpty()
                }

                private companion object {
                    private const val PARAM_DOCUMENT = "document"
                }
            }

            @Parcelize
            data class Document @JvmOverloads constructor(
                /**
                 * The front of a document returned by a
                 * [file upload](https://stripe.com/docs/api/tokens/create_account#create_file)
                 * with a `purpose` value of `additional_verification`. The uploaded file needs to
                 * be a color image (smaller than 8,000px by 8,000px), in JPG or PNG format, and
                 * less than 10 MB in size.
                 *
                 * [account.company.verification.document.front](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-verification-document-front)
                 */
                private val front: String? = null,

                /**
                 * The back of a document returned by a
                 * [file upload](https://stripe.com/docs/api/tokens/create_account#create_file)
                 * with a `purpose` value of `additional_verification`. The uploaded file needs to
                 * be a color image (smaller than 8,000px by 8,000px), in JPG or PNG format, and
                 * less than 10 MB in size.
                 *
                 * [account.company.verification.document.back](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-verification-document-back)
                 */
                private val back: String? = null
            ) : StripeParamsModel, Parcelable {
                override fun toParamMap(): Map<String, Any> {
                    return listOf(
                        PARAM_FRONT to front,
                        PARAM_BACK to back
                    ).fold(emptyMap()) { acc, (key, value) ->
                        acc.plus(
                            value?.let { mapOf(key to it) }.orEmpty()
                        )
                    }
                }

                private companion object {
                    private const val PARAM_BACK = "back"
                    private const val PARAM_FRONT = "front"
                }
            }

            class Builder {
                private var address: Address? = null
                private var addressKana: AddressJapanParams? = null
                private var addressKanji: AddressJapanParams? = null
                private var directorsProvided: Boolean? = null
                private var executivesProvided: Boolean? = null
                private var name: String? = null
                private var nameKana: String? = null
                private var nameKanji: String? = null
                private var ownersProvided: Boolean? = null
                private var phone: String? = null
                private var taxId: String? = null
                private var taxIdRegistrar: String? = null
                private var vatId: String? = null
                private var verification: Verification? = null

                /**
                 * @param address The company’s primary address.
                 *
                 * [account.company.address](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address)
                 */
                fun setAddress(address: Address?): Builder = apply {
                    this.address = address
                }

                /**
                 * @param addressKana The Kana variation of the company’s primary address (Japan only).
                 *
                 * [account.company.address_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address_kana)
                 */
                fun setAddressKana(addressKana: AddressJapanParams?): Builder = apply {
                    this.addressKana = addressKana
                }

                /**
                 * @param addressKanji The Kanji variation of the company’s primary address (Japan only).
                 *
                 * [account.company.address_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-address_kanji)
                 */
                fun setAddressKanji(addressKanji: AddressJapanParams?): Builder = apply {
                    this.addressKanji = addressKanji
                }

                /**
                 * @param directorsProvided Whether the company’s directors have been provided. Set
                 * this Boolean to `true` after creating all the company’s directors with the
                 * [Persons API](https://stripe.com/docs/api/persons) for accounts with a
                 * `relationship.director` requirement. This value is not automatically set to
                 * `true` after creating directors, so it needs to be updated to indicate all
                 * directors have been provided.
                 *
                 * [account.company.directors_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-directors_provided)
                 */
                fun setDirectorsProvided(directorsProvided: Boolean?): Builder = apply {
                    this.directorsProvided = directorsProvided
                }

                /**
                 * @param executivesProvided Whether the company’s executives have been provided.
                 * Set this Boolean to `true` after creating all the company’s executives with the
                 * [Persons API](https://stripe.com/docs/api/persons) for accounts with a
                 * `relationship.executive` requirement.
                 *
                 * [account.company.executives_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-executives_provided)
                 */
                fun setExecutivesProvided(executivesProvided: Boolean?): Builder = apply {
                    this.executivesProvided = executivesProvided
                }

                /**
                 * @param name The company’s legal name.
                 *
                 * [account.company.name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name)
                 */
                fun setName(name: String?): Builder = apply {
                    this.name = name
                }

                /**
                 * @param nameKana The Kana variation of the company’s legal name (Japan only).
                 *
                 * [account.company.name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name_kana)
                 */
                fun setNameKana(nameKana: String?): Builder = apply {
                    this.nameKana = nameKana
                }

                /**
                 * @param nameKanji The Kanji variation of the company’s legal name (Japan only).
                 *
                 * [account.company.name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-name_kanji)
                 */
                fun setNameKanji(nameKanji: String?): Builder = apply {
                    this.nameKanji = nameKanji
                }

                /**
                 * @param ownersProvided Whether the company’s owners have been provided. Set this
                 * Boolean to `true` after creating all the company’s owners with the
                 * [Persons API](https://stripe.com/docs/api/persons) for accounts with a
                 * `relationship.owner` requirement.
                 *
                 * [account.company.owners_provided](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-owners_provided)
                 */
                fun setOwnersProvided(ownersProvided: Boolean?): Builder = apply {
                    this.ownersProvided = ownersProvided
                }

                /**
                 * @param phone The company’s phone number (used for verification).
                 *
                 * [account.company.phone](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-phone)
                 */
                fun setPhone(phone: String?): Builder = apply {
                    this.phone = phone
                }

                /**
                 * @param taxId The business ID number of the company, as appropriate for the
                 * company’s country. (Examples are an Employer ID Number in the U.S.,
                 * a Business Number in Canada, or a Company Number in the UK.)
                 *
                 * [account.company.tax_id](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-tax_id)
                 */
                fun setTaxId(taxId: String?): Builder = apply {
                    this.taxId = taxId
                }

                /**
                 * @param taxIdRegistrar The jurisdiction in which the `tax_id` is registered
                 * (Germany-based companies only).
                 *
                 * [account.company.tax_id_registrar](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-tax_id_registrar)
                 */
                fun setTaxIdRegistrar(taxIdRegistrar: String?): Builder = apply {
                    this.taxIdRegistrar = taxIdRegistrar
                }

                /**
                 * @param vatId The VAT number of the company.
                 *
                 * [account.company.vat_id](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-vat_id)
                 */
                fun setVatId(vatId: String?): Builder = apply {
                    this.vatId = vatId
                }

                /**
                 * @param verification Information on the verification state of the company.
                 *
                 * [account.company.verification](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-company-verification)
                 */
                fun setVerification(verification: Verification?): Builder = apply {
                    this.verification = verification
                }

                fun build(): Company {
                    return Company(
                        address = address,
                        addressKana = addressKana,
                        addressKanji = addressKanji,
                        directorsProvided = directorsProvided,
                        executivesProvided = executivesProvided,
                        name = name,
                        nameKana = nameKana,
                        nameKanji = nameKanji,
                        ownersProvided = ownersProvided,
                        phone = phone,
                        taxId = taxId,
                        taxIdRegistrar = taxIdRegistrar,
                        vatId = vatId,
                        verification = verification
                    )
                }
            }

            private companion object {
                private const val PARAM_ADDRESS = "address"
                private const val PARAM_ADDRESS_KANA = "address_kana"
                private const val PARAM_ADDRESS_KANJI = "address_kanji"
                private const val PARAM_DIRECTORS_PROVIDED = "directors_provided"
                private const val PARAM_EXECUTIVES_PROVIDED = "executives_provided"
                private const val PARAM_NAME = "name"
                private const val PARAM_NAME_KANA = "name_kana"
                private const val PARAM_NAME_KANJI = "name_kanji"
                private const val PARAM_OWNERS_PROVIDED = "owners_provided"
                private const val PARAM_PHONE = "phone"
                private const val PARAM_TAX_ID = "tax_id"
                private const val PARAM_TAX_ID_REGISTRAR = "tax_id_registrar"
                private const val PARAM_VAT_ID = "vat_id"
                private const val PARAM_VERIFICATION = "verification"
            }
        }

        /**
         * Information about the person represented by the account.
         *
         * [account.individual](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual)
         */
        @Parcelize
        data class Individual(
            /**
             * The individual’s primary address.
             *
             * [account.individual.address](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address)
             */
            var address: Address? = null,

            /**
             * The Kana variation of the the individual’s primary address (Japan only).
             *
             * [account.individual.address_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address_kana)
             */
            var addressKana: AddressJapanParams? = null,

            /**
             * The Kanji variation of the the individual’s primary address (Japan only).
             *
             * [account.individual.address_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address_kanji)
             */
            var addressKanji: AddressJapanParams? = null,

            /**
             * The individual’s date of birth.
             *
             * [account.individual.dob](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-dob)
             */
            var dateOfBirth: DateOfBirth? = null,

            /**
             * The individual’s email.
             *
             * [account.individual.email](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-email)
             */
            var email: String? = null,

            /**
             * The individual’s first name.
             *
             * [account.individual.first_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name)
             */
            var firstName: String? = null,

            /**
             * The Kana variation of the the individual’s first name (Japan only).
             *
             * [account.individual.first_name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name_kana)
             */
            var firstNameKana: String? = null,

            /**
             * The Kanji variation of the individual’s first name (Japan only).
             *
             * [account.individual.first_name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name_kanji)
             */
            var firstNameKanji: String? = null,

            /**
             * The individual’s gender (International regulations require either “male” or “female”).
             *
             * [account.individual.gender](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-gender)
             */
            var gender: String? = null,

            /**
             * The government-issued ID number of the individual, as appropriate for the
             * representative’s country. (Examples are a Social Security Number in the U.S., or a
             * Social Insurance Number in Canada). Instead of the number itself, you can also
             * provide a PII token.
             *
             * [account.individual.id_number](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-id_number)
             */
            var idNumber: String? = null,

            /**
             * The individual’s last name.
             *
             * [account.individual.last_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name)
             */
            var lastName: String? = null,

            /**
             * The Kana varation of the individual’s last name (Japan only).
             *
             * [account.individual.last_name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name_kana)
             */
            var lastNameKana: String? = null,

            /**
             * The Kanji varation of the individual’s last name (Japan only).
             *
             * [account.individual.last_name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name_kanji)
             */
            var lastNameKanji: String? = null,

            /**
             * The individual’s maiden name.
             *
             * [account.individual.maiden_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-maiden_name)
             */
            var maidenName: String? = null,

            /**
             * Set of key-value pairs that you can attach to an object. This can be useful for
             * storing additional information about the object in a structured format. Individual keys
             * can be unset by posting an empty value to them. All keys can be unset by posting an
             * empty value to `metadata`.
             *
             * [account.individual.metadata](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-metadata)
             */
            var metadata: Map<String, String>? = null,

            /**
             * The individual’s phone number.
             *
             * [account.individual.phone](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-phone)
             */
            var phone: String? = null,

            /**
             * The last four digits of the individual’s Social Security Number (U.S. only).
             *
             * [account.individual.ssn_last_4](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-ssn_last_4)
             */
            var ssnLast4: String? = null,

            /**
             * The individual’s verification document information.
             *
             * [account.individual.verification](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-verification)
             */
            var verification: Verification? = null
        ) : BusinessTypeParams(BusinessType.Individual) {
            override val paramsList: List<Pair<String, Any?>>
                get() = listOf(
                    PARAM_ADDRESS to address?.toParamMap(),
                    PARAM_ADDRESS_KANA to addressKana?.toParamMap(),
                    PARAM_ADDRESS_KANJI to addressKanji?.toParamMap(),
                    PARAM_DOB to dateOfBirth?.toParamMap(),
                    PARAM_EMAIL to email,
                    PARAM_FIRST_NAME to firstName,
                    PARAM_FIRST_NAME_KANA to firstNameKana,
                    PARAM_FIRST_NAME_KANJI to firstNameKanji,
                    PARAM_GENDER to gender,
                    PARAM_ID_NUMBER to idNumber,
                    PARAM_LAST_NAME to lastName,
                    PARAM_LAST_NAME_KANA to lastNameKana,
                    PARAM_LAST_NAME_KANJI to lastNameKanji,
                    PARAM_MAIDEN_NAME to maidenName,
                    PARAM_METADATA to metadata,
                    PARAM_PHONE to phone,
                    PARAM_SSN_LAST_4 to ssnLast4,
                    PARAM_VERIFICATION to verification?.toParamMap()
                )

            @Parcelize
            data class Verification @JvmOverloads constructor(
                /**
                 * An identifying document, either a passport or local ID card.
                 */
                var document: Document? = null,

                /**
                 * A document showing address, either a passport, local ID card, or utility bill from
                 * a well-known utility company.
                 */
                var additionalDocument: Document? = null
            ) : StripeParamsModel, Parcelable {
                override fun toParamMap(): Map<String, Any> {
                    return listOf(
                        PARAM_ADDITIONAL_DOCUMENT to additionalDocument?.toParamMap(),
                        PARAM_DOCUMENT to document?.toParamMap()
                    ).fold(emptyMap()) { acc, (key, value) ->
                        acc.plus(
                            value?.let { mapOf(key to it) }.orEmpty()
                        )
                    }
                }

                private companion object {
                    private const val PARAM_ADDITIONAL_DOCUMENT = "additional_document"
                    private const val PARAM_DOCUMENT = "document"
                }
            }

            @Parcelize
            data class Document @JvmOverloads constructor(
                /**
                 * The front of an ID returned by a
                 * [file upload](https://stripe.com/docs/api/tokens/create_account#create_file) with
                 * a `purpose` value of `identity_document`. The uploaded file needs to be a color
                 * image (smaller than 8,000px by 8,000px), in JPG or PNG format, and less than
                 * 10 MB in size.
                 */
                private var front: String? = null,

                /**
                 * The back of an ID returned by a
                 * [file upload](https://stripe.com/docs/api/tokens/create_account#create_file)
                 * with a `purpose` value of `identity_document`. The uploaded file needs to be a
                 * color image (smaller than 8,000px by 8,000px), in JPG or PNG format, and less
                 * than 10 MB in size.
                 */
                private var back: String? = null
            ) : StripeParamsModel, Parcelable {
                override fun toParamMap(): Map<String, Any> {
                    return listOf(
                        PARAM_FRONT to front,
                        PARAM_BACK to back
                    ).fold(emptyMap()) { acc, (key, value) ->
                        acc.plus(
                            value?.let { mapOf(key to it) }.orEmpty()
                        )
                    }
                }

                private companion object {
                    private const val PARAM_BACK = "back"
                    private const val PARAM_FRONT = "front"
                }
            }

            class Builder {
                private var address: Address? = null
                private var addressKana: AddressJapanParams? = null
                private var addressKanji: AddressJapanParams? = null
                private var dateOfBirth: DateOfBirth? = null
                private var email: String? = null
                private var firstName: String? = null
                private var firstNameKana: String? = null
                private var firstNameKanji: String? = null
                private var gender: String? = null
                private var idNumber: String? = null
                private var lastName: String? = null
                private var lastNameKana: String? = null
                private var lastNameKanji: String? = null
                private var maidenName: String? = null
                private var metadata: Map<String, String>? = null
                private var phone: String? = null
                private var ssnLast4: String? = null
                private var verification: Verification? = null

                /**
                 * @param address The individual’s primary address.
                 *
                 * [account.individual.address](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address)
                 */
                fun setAddress(address: Address?): Builder = apply {
                    this.address = address
                }

                /**
                 * @param addressKana The Kana variation of the the individual’s primary address (Japan only).
                 *
                 * [account.individual.address_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address_kana)
                 */
                fun setAddressKana(addressKana: AddressJapanParams?): Builder = apply {
                    this.addressKana = addressKana
                }

                /**
                 * @param addressKanji The Kanji variation of the the individual’s primary address (Japan only).
                 *
                 * [account.individual.address_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-address_kanji)
                 */
                fun setAddressKanji(addressKanji: AddressJapanParams?): Builder = apply {
                    this.addressKanji = addressKanji
                }

                /**
                 * @param dateOfBirth The individual’s date of birth.
                 *
                 * [account.individual.dob](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-dob)
                 */
                fun setDateOfBirth(dateOfBirth: DateOfBirth?): Builder = apply {
                    this.dateOfBirth = dateOfBirth
                }

                /**
                 * @param email The individual’s email.
                 *
                 * [account.individual.email](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-email)
                 */
                fun setEmail(email: String?): Builder = apply {
                    this.email = email
                }

                /**
                 * @param firstName The individual’s first name.
                 *
                 * [account.individual.first_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name)
                 */
                fun setFirstName(firstName: String?): Builder = apply {
                    this.firstName = firstName
                }

                /**
                 * @param firstNameKana The Kana variation of the the individual’s first name (Japan only).
                 *
                 * [account.individual.first_name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name_kana)
                 */
                fun setFirstNameKana(firstNameKana: String?): Builder = apply {
                    this.firstNameKana = firstNameKana
                }

                /**
                 * @param firstNameKanji The Kanji variation of the individual’s first name (Japan only).
                 *
                 * [account.individual.first_name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-first_name_kanji)
                 */
                fun setFirstNameKanji(firstNameKanji: String?): Builder = apply {
                    this.firstNameKanji = firstNameKanji
                }

                /**
                 * @param gender The individual’s gender (International regulations require either “male” or “female”).
                 *
                 * [account.individual.gender](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-gender)
                 */
                fun setGender(gender: String?): Builder = apply {
                    this.gender = gender
                }

                /**
                 * @param idNumber The government-issued ID number of the individual, as appropriate
                 * for the representative’s country. (Examples are a Social Security Number in the
                 * U.S., or a Social Insurance Number in Canada). Instead of the number itself, you
                 * can also provide a PII token.
                 *
                 * [account.individual.id_number](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-id_number)
                 */
                fun setIdNumber(idNumber: String?): Builder = apply {
                    this.idNumber = idNumber
                }

                /**
                 * @param lastName The individual’s last name.
                 *
                 * [account.individual.last_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name)
                 */
                fun setLastName(lastName: String?): Builder = apply {
                    this.lastName = lastName
                }

                /**
                 * @param lastNameKana The Kana varation of the individual’s last name (Japan only).
                 *
                 * [account.individual.last_name_kana](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name_kana)
                 */
                fun setLastNameKana(lastNameKana: String?): Builder = apply {
                    this.lastNameKana = lastNameKana
                }

                /**
                 * @param lastNameKanji The Kanji varation of the individual’s last name (Japan only).
                 *
                 * [account.individual.last_name_kanji](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-last_name_kanji)
                 */
                fun setLastNameKanji(lastNameKanji: String?): Builder = apply {
                    this.lastNameKanji = lastNameKanji
                }

                /**
                 * @param maidenName The individual’s maiden name.
                 *
                 * [account.individual.maiden_name](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-maiden_name)
                 */
                fun setMaidenName(maidenName: String?): Builder = apply {
                    this.maidenName = maidenName
                }

                /**
                 * @param metadata Set of key-value pairs that you can attach to an object. This
                 * can be useful for storing additional information about the object in a
                 * structured format. Individual keys can be unset by posting an empty value to
                 * them. All keys can be unset by posting an empty value to `metadata`.
                 *
                 * [account.individual.metadata](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-metadata)
                 */
                fun setMetadata(metadata: Map<String, String>?): Builder = apply {
                    this.metadata = metadata
                }

                /**
                 * @param phone The individual’s phone number.
                 *
                 * [account.individual.phone](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-phone)
                 */
                fun setPhone(phone: String?): Builder = apply {
                    this.phone = phone
                }

                /**
                 * @param ssnLast4 The last four digits of the individual’s Social Security Number (U.S. only).
                 *
                 * [account.individual.ssn_last_4](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-ssn_last_4)
                 */
                fun setSsnLast4(ssnLast4: String?): Builder = apply {
                    this.ssnLast4 = ssnLast4
                }

                /**
                 * @param verification The individual’s verification document information.
                 *
                 * [account.individual.verification](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-individual-verification)
                 */
                fun setVerification(verification: Verification?): Builder = apply {
                    this.verification = verification
                }

                fun build(): Individual {
                    return Individual(
                        address = address,
                        addressKana = addressKana,
                        addressKanji = addressKanji,
                        dateOfBirth = dateOfBirth,
                        email = email,
                        firstName = firstName,
                        firstNameKana = firstNameKana,
                        firstNameKanji = firstNameKanji,
                        gender = gender,
                        idNumber = idNumber,
                        lastName = lastName,
                        lastNameKana = lastNameKana,
                        lastNameKanji = lastNameKanji,
                        maidenName = maidenName,
                        metadata = metadata,
                        phone = phone,
                        ssnLast4 = ssnLast4,
                        verification = verification
                    )
                }
            }

            private companion object {
                private const val PARAM_ADDRESS = "address"
                private const val PARAM_ADDRESS_KANA = "address_kana"
                private const val PARAM_ADDRESS_KANJI = "address_kanji"
                private const val PARAM_DOB = "dob"
                private const val PARAM_EMAIL = "email"
                private const val PARAM_FIRST_NAME = "first_name"
                private const val PARAM_FIRST_NAME_KANA = "first_name_kana"
                private const val PARAM_FIRST_NAME_KANJI = "first_name_kanji"
                private const val PARAM_GENDER = "gender"
                private const val PARAM_ID_NUMBER = "id_number"
                private const val PARAM_LAST_NAME = "last_name"
                private const val PARAM_LAST_NAME_KANA = "last_name_kana"
                private const val PARAM_LAST_NAME_KANJI = "last_name_kanji"
                private const val PARAM_MAIDEN_NAME = "maiden_name"
                private const val PARAM_METADATA = "metadata"
                private const val PARAM_PHONE = "phone"
                private const val PARAM_SSN_LAST_4 = "ssn_last_4"
                private const val PARAM_VERIFICATION = "verification"
            }
        }
    }

    companion object {
        private const val PARAM_BUSINESS_TYPE = "business_type"
        private const val PARAM_TOS_SHOWN_AND_ACCEPTED = "tos_shown_and_accepted"

        /**
         * Create an [AccountParams] instance with information about the person represented by the account.
         *
         * @param tosShownAndAccepted Whether the user described by the data in the token has been
         * shown the
         * [Stripe Connected Account Agreement](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted).
         * When creating an account token to create a new Connect account, this value must be `true`.
         * @param individual Information about the person represented by the account.
         *
         * @return [AccountParams]
         */
        @JvmStatic
        fun create(
            tosShownAndAccepted: Boolean,
            individual: BusinessTypeParams.Individual
        ): AccountParams {
            return AccountParams(
                tosShownAndAccepted,
                individual
            )
        }

        /**
         * Create an [AccountParams] instance with information about the company or business.
         *
         * @param tosShownAndAccepted Whether the user described by the data in the token has been
         * shown the
         * [Stripe Connected Account Agreement](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted).
         * When creating an account token to create a new Connect account, this value must be `true`.
         * @param company Information about the company or business.
         *
         * @return [AccountParams]
         */
        @JvmStatic
        fun create(
            tosShownAndAccepted: Boolean,
            company: BusinessTypeParams.Company
        ): AccountParams {
            return AccountParams(
                tosShownAndAccepted,
                company
            )
        }

        /**
         * Create an [AccountParams] instance.
         *
         * @param tosShownAndAccepted Whether the user described by the data in the token has been
         * shown the
         * [Stripe Connected Account Agreement](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted).
         * When creating an account token to create a new Connect account, this value must be `true`.
         * @param businessType The business type.
         *
         * @return [AccountParams]
         */
        @JvmStatic
        fun create(
            tosShownAndAccepted: Boolean,
            businessType: BusinessType
        ): AccountParams {
            return AccountParams(
                tosShownAndAccepted,
                when (businessType) {
                    BusinessType.Individual -> BusinessTypeParams.Individual()
                    BusinessType.Company -> BusinessTypeParams.Company()
                }
            )
        }

        /**
         * Create an [AccountParams] instance.
         *
         * @param tosShownAndAccepted Whether the user described by the data in the token has been
         * shown the
         * [Stripe Connected Account Agreement](https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted).
         * When creating an account token to create a new Connect account, this value must be `true`.
         *
         * @return [AccountParams]
         */
        @JvmStatic
        fun create(
            tosShownAndAccepted: Boolean
        ): AccountParams {
            return AccountParams(tosShownAndAccepted)
        }
    }
}
