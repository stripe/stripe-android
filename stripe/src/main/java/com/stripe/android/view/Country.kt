package com.stripe.android.view

internal data class Country(
    val code: CountryCode,
    val name: String
) {
    constructor(twoLetter: String, name: String) : this(CountryCode(twoLetter), name)

    /**
     * @return display value for [CountryTextInputLayout] text view
     */
    override fun toString(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Country

        if (code != other.code) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
