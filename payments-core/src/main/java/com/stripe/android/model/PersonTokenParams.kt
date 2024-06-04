package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Creates a single-use token that represents the details for a person. Use this when creating or
 * updating persons associated with a Connect account.
 * See [the documentation](https://stripe.com/docs/connect/account-tokens) to learn more.
 *
 * See [Create a person token](https://stripe.com/docs/api/tokens/create_person)
 */
@Parcelize
data class PersonTokenParams(
    /**
     * The person’s address.
     *
     * [person.address](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-address)
     */
    val address: Address? = null,

    /**
     * The Kana variation of the person’s address (Japan only).
     *
     * [person.address_kana](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-address_kana)
     */
    val addressKana: AddressJapanParams? = null,

    /**
     * The Kanji variation of the person’s address (Japan only).
     *
     * [person.address_kanji](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-address_kanji)
     */
    val addressKanji: AddressJapanParams? = null,

    /**
     * The person’s date of birth.
     *
     * [person.dob](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-dob)
     */
    val dateOfBirth: DateOfBirth? = null,

    /**
     * The person’s email address.
     *
     * [person.email](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-email)
     */
    val email: String? = null,

    /**
     * The person’s first name.
     *
     * [person.first_name](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-first_name)
     */
    val firstName: String? = null,

    /**
     * The Kana variation of the person’s first name (Japan only).
     *
     * [person.first_name_kana](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-first_name_kana)
     */
    val firstNameKana: String? = null,

    /**
     * The Kanji variation of the person’s first name (Japan only).
     *
     * [person.first_name_kanji](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-first_name_kanji)
     */
    val firstNameKanji: String? = null,

    /**
     * The person’s gender (International regulations require either “male” or “female”).
     *
     * [person.gender](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-gender)
     */
    val gender: String? = null,

    /**
     * The person’s ID number, as appropriate for their country. For example, a social security
     * number in the U.S., social insurance number in Canada, etc. Instead of the number itself,
     * you can also provide a PII token.
     *
     * [person.id_number](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-id_number)
     */
    val idNumber: String? = null,

    /**
     * The person’s last name.
     *
     * [person.last_name](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-last_name)
     */
    val lastName: String? = null,

    /**
     * The Kana variation of the person’s last name (Japan only).
     *
     * [person.last_name_kana](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-last_name_kana)
     */
    val lastNameKana: String? = null,

    /**
     * The Kanji variation of the person’s last name (Japan only).
     *
     * [person.last_name_kanji](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-last_name_kanji)
     */
    val lastNameKanji: String? = null,

    /**
     * The person’s maiden name.
     *
     * [person.maiden_name](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-maiden_name)
     */
    val maidenName: String? = null,

    /**
     * Set of key-value pairs that you can attach to an object. This can be useful for storing
     * additional information about the object in a structured format. Individual keys can be unset
     * by posting an empty value to them. All keys can be unset by posting an empty value
     * to `metadata`.
     *
     * [person.metadata](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-metadata)
     */
    val metadata: Map<String, String>? = null,

    /**
     * The person’s phone number.
     *
     * [person.phone](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-phone)
     */
    val phone: String? = null,

    /**
     * The relationship that this person has with the account’s legal entity.
     *
     * [person.relationship](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship)
     */
    val relationship: Relationship? = null,

    /**
     * The last 4 digits of the person’s social security number.
     *
     * [person.ssn_last_4](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-ssn_last_4)
     */
    val ssnLast4: String? = null,

    /**
     * The person’s verification status.
     *
     * [person.verification](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification)
     */
    val verification: Verification? = null
) : TokenParams(Token.Type.Person) {
    override val typeDataParams: Map<String, Any>
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
            PARAM_RELATIONSHIP to relationship?.toParamMap(),
            PARAM_SSN_LAST_4 to ssnLast4,
            PARAM_VERIFICATION to verification?.toParamMap()
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }

    /**
     * The relationship that this person has with the account’s legal entity.
     *
     * [person.relationship](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship)
     */
    @Parcelize
    data class Relationship(
        /**
         * Whether the person is a director of the account’s legal entity. Currently only required
         * for accounts in the EU. Directors are typically members of the governing board of the
         * company, or responsible for ensuring the company meets its regulatory obligations.
         *
         * [person.relationship.directory](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-directory)
         */
        val director: Boolean? = null,

        /**
         * Whether the person has significant responsibility to control, manage, or direct the
         * organization.
         *
         * [person.relationship.executive](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-executive)
         */
        val executive: Boolean? = null,

        /**
         * Whether the person is an owner of the account’s legal entity.
         *
         * [person.relationship.owner](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-owner)
         */
        val owner: Boolean? = null,

        /**
         * The percent owned by the person of the account’s legal entity.
         *
         * [person.relationship.representative](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-representative)
         */
        val percentOwnership: Int? = null,

        /**
         * Whether the person is authorized as the primary representative of the account. This is
         * the person nominated by the business to provide information about themselves, and general
         * information about the account. There can only be one representative at any given time.
         * At the time the account is created, this person should be set to the person responsible
         * for opening the account.
         *
         * [person.relationship.percent_ownership](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-percent_ownership)
         */
        val representative: Boolean? = null,

        /**
         * The person’s title (e.g., CEO, Support Engineer).
         *
         * [person.relationship.title](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-relationship-title)
         */
        val title: String? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_DIRECTOR to director,
                PARAM_EXECUTIVE to executive,
                PARAM_OWNER to owner,
                PARAM_PERCENT_OWNERSHIP to percentOwnership,
                PARAM_REPRESENTATIVE to representative,
                PARAM_TITLE to title
            ).fold(emptyMap()) { acc, (key, value) ->
                acc.plus(
                    value?.let { mapOf(key to it) }.orEmpty()
                )
            }
        }

        class Builder {
            private var director: Boolean? = null
            private var executive: Boolean? = null
            private var owner: Boolean? = null
            private var percentOwnership: Int? = null
            private var representative: Boolean? = null
            private var title: String? = null

            fun setDirector(director: Boolean?): Builder = apply {
                this.director = director
            }

            fun setExecutive(executive: Boolean?): Builder = apply {
                this.executive = executive
            }

            fun setOwner(owner: Boolean?): Builder = apply {
                this.owner = owner
            }

            fun setPercentOwnership(percentOwnership: Int?): Builder = apply {
                this.percentOwnership = percentOwnership
            }

            fun setRepresentative(representative: Boolean?): Builder = apply {
                this.representative = representative
            }

            fun setTitle(title: String?): Builder = apply {
                this.title = title
            }

            fun build(): Relationship {
                return Relationship(
                    director = director,
                    executive = executive,
                    owner = owner,
                    percentOwnership = percentOwnership,
                    representative = representative,
                    title = title
                )
            }
        }

        private companion object {
            private const val PARAM_DIRECTOR = "director"
            private const val PARAM_EXECUTIVE = "executive"
            private const val PARAM_OWNER = "owner"
            private const val PARAM_PERCENT_OWNERSHIP = "percent_ownership"
            private const val PARAM_REPRESENTATIVE = "representative"
            private const val PARAM_TITLE = "title"
        }
    }

    /**
     * The person’s verification status.
     *
     * [person.verification](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification)
     */
    @Parcelize
    data class Verification @JvmOverloads constructor(
        /**
         * An identifying document, either a passport or local ID card.
         *
         * [person.verification.document](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification-document)
         */
        val document: Document? = null,

        /**
         * A document showing address, either a passport, local ID card, or utility bill from a well-known utility company.
         *
         * [person.verification.additional_document](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification-additional_document)
         */
        val additionalDocument: Document? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_ADDITIONAL_DOCUMENT to document?.toParamMap(),
                PARAM_DOCUMENT to additionalDocument?.toParamMap()
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
         * [file upload](https://stripe.com/docs/api/tokens/create_person#create_file) with a
         * `purpose` value of `identity_document`. The uploaded file needs to be a color image
         * (smaller than 8,000px by 8,000px), in JPG or PNG format, and less than 10 MB in size.
         *
         * [person.verification.document.front](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification-document-front)
         */
        val front: String? = null,

        /**
         * The back of an ID returned by a
         * [file upload](https://stripe.com/docs/api/tokens/create_person#create_file) with a
         * `purpose` value of `identity_document`. The uploaded file needs to be a color image
         * (smaller than 8,000px by 8,000px), in JPG or PNG format, and less than 10 MB in size.
         *
         * [person.verification.document.back](https://stripe.com/docs/api/tokens/create_person#create_person_token-person-verification-document-back)
         */
        val back: String? = null
    ) : StripeParamsModel, Parcelable {
        override fun toParamMap(): Map<String, Any> {
            return listOf(
                PARAM_BACK to back,
                PARAM_FRONT to front
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
        private var relationship: Relationship? = null
        private var ssnLast4: String? = null
        private var verification: Verification? = null

        fun setAddress(address: Address?): Builder = apply {
            this.address = address
        }

        fun setAddressKana(addressKana: AddressJapanParams?): Builder = apply {
            this.addressKana = addressKana
        }

        fun setAddressKanji(addressKanji: AddressJapanParams?): Builder = apply {
            this.addressKanji = addressKanji
        }

        fun setDateOfBirth(dateOfBirth: DateOfBirth?): Builder = apply {
            this.dateOfBirth = dateOfBirth
        }

        fun setEmail(email: String?): Builder = apply {
            this.email = email
        }

        fun setFirstName(firstName: String?): Builder = apply {
            this.firstName = firstName
        }

        fun setFirstNameKana(firstNameKana: String?): Builder = apply {
            this.firstNameKana = firstNameKana
        }

        fun setFirstNameKanji(firstNameKanji: String?): Builder = apply {
            this.firstNameKanji = firstNameKanji
        }

        fun setGender(gender: String?): Builder = apply {
            this.gender = gender
        }

        fun setIdNumber(idNumber: String?): Builder = apply {
            this.idNumber = idNumber
        }

        fun setLastName(lastName: String?): Builder = apply {
            this.lastName = lastName
        }

        fun setLastNameKana(lastNameKana: String?): Builder = apply {
            this.lastNameKana = lastNameKana
        }

        fun setLastNameKanji(lastNameKanji: String?): Builder = apply {
            this.lastNameKanji = lastNameKanji
        }

        fun setMaidenName(maidenName: String?): Builder = apply {
            this.maidenName = maidenName
        }

        fun setMetadata(metadata: Map<String, String>?): Builder = apply {
            this.metadata = metadata
        }

        fun setPhone(phone: String?): Builder = apply {
            this.phone = phone
        }

        fun setRelationship(relationship: Relationship?): Builder = apply {
            this.relationship = relationship
        }

        fun setSsnLast4(ssnLast4: String?): Builder = apply {
            this.ssnLast4 = ssnLast4
        }

        fun setVerification(verification: Verification?): Builder = apply {
            this.verification = verification
        }

        fun build(): PersonTokenParams {
            return PersonTokenParams(
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
                relationship = relationship,
                ssnLast4 = ssnLast4,
                verification = verification
            )
        }
    }

    private companion object {
        // top level param
        private const val PARAM_PERSON = "person"

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
        private const val PARAM_RELATIONSHIP = "relationship"
        private const val PARAM_SSN_LAST_4 = "ssn_last_4"
        private const val PARAM_VERIFICATION = "verification"
    }
}
