package com.stripe.android.view

/**
 * A class for tracking the placement and layout of fields in [CardInputWidget].
 */
internal data class CardInputWidgetPlacement(
    internal var totalLengthInPixels: Int = 0,

    internal var cardWidth: Int = 0,
    internal var hiddenCardWidth: Int = 0,
    internal var peekCardWidth: Int = 0,
    internal var cardDateSeparation: Int = 0,
    internal var dateWidth: Int = 0,
    internal var dateCvcSeparation: Int = 0,
    internal var cvcWidth: Int = 0,
    internal var cvcPostalCodeSeparation: Int = 0,
    internal var postalCodeWidth: Int = 0,

    internal var cardTouchBufferLimit: Int = 0,
    internal var dateStartPosition: Int = 0,
    internal var dateRightTouchBufferLimit: Int = 0,
    internal var cvcStartPosition: Int = 0,
    internal var cvcRightTouchBufferLimit: Int = 0,
    internal var postalCodeStartPosition: Int = 0
) {
    private val cardPeekDateLeftMargin: Int
        @JvmSynthetic
        get() {
            return peekCardWidth + cardDateSeparation
        }

    private val cardPeekCvcLeftMargin: Int
        @JvmSynthetic
        get() {
            return cardPeekDateLeftMargin + dateWidth + dateCvcSeparation
        }

    private val cardPeekPostalCodeLeftMargin: Int
        @JvmSynthetic
        get() {
            return cardPeekCvcLeftMargin + postalCodeWidth + cvcPostalCodeSeparation
        }

    @JvmSynthetic
    internal fun getDateLeftMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            cardWidth + cardDateSeparation
        } else {
            cardPeekDateLeftMargin
        }
    }

    @JvmSynthetic
    internal fun getCvcLeftMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            totalLengthInPixels
        } else {
            cardPeekCvcLeftMargin
        }
    }

    @JvmSynthetic
    internal fun getPostalCodeLeftMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            totalLengthInPixels
        } else {
            cardPeekPostalCodeLeftMargin
        }
    }

    @JvmSynthetic
    internal fun updateSpacing(
        isShowingFullCard: Boolean,
        postalCodeEnabled: Boolean,
        frameStart: Int,
        frameWidth: Int
    ) {
        when {
            isShowingFullCard -> {
                cardDateSeparation = frameWidth - cardWidth - dateWidth
                cardTouchBufferLimit = frameStart + cardWidth + cardDateSeparation / 2
                dateStartPosition = frameStart + cardWidth + cardDateSeparation
            }
            postalCodeEnabled -> {
                this.cardDateSeparation = (frameWidth * 3 / 10) - peekCardWidth - dateWidth / 4
                this.dateCvcSeparation = (frameWidth * 3 / 5) - peekCardWidth - cardDateSeparation -
                    dateWidth - cvcWidth
                this.cvcPostalCodeSeparation = (frameWidth * 4 / 5) - peekCardWidth - cardDateSeparation -
                    dateWidth - cvcWidth - dateCvcSeparation - postalCodeWidth

                val dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
                this.cardTouchBufferLimit = dateStartPosition / 3
                this.dateStartPosition = dateStartPosition

                val cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
                this.dateRightTouchBufferLimit = cvcStartPosition / 3
                this.cvcStartPosition = cvcStartPosition

                val postalCodeStartPosition = cvcStartPosition + cvcWidth + cvcPostalCodeSeparation
                this.cvcRightTouchBufferLimit = postalCodeStartPosition / 3
                this.postalCodeStartPosition = postalCodeStartPosition
            }
            else -> {
                this.cardDateSeparation = frameWidth / 2 - peekCardWidth - dateWidth / 2
                this.dateCvcSeparation = frameWidth - peekCardWidth - cardDateSeparation -
                    dateWidth - cvcWidth

                this.cardTouchBufferLimit = frameStart + peekCardWidth + cardDateSeparation / 2
                this.dateStartPosition = frameStart + peekCardWidth + cardDateSeparation

                this.dateRightTouchBufferLimit = dateStartPosition + dateWidth + dateCvcSeparation / 2
                this.cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
            }
        }
    }

    override fun toString(): String {
        val touchBufferData =
            """
            Touch Buffer Data:
            CardTouchBufferLimit = $cardTouchBufferLimit
            DateStartPosition = $dateStartPosition
            DateRightTouchBufferLimit = $dateRightTouchBufferLimit
            CvcStartPosition = $cvcStartPosition
            CvcRightTouchBufferLimit = $cvcRightTouchBufferLimit
            PostalCodeStartPosition = $postalCodeStartPosition
            """

        val elementSizeData =
            """
            TotalLengthInPixels = $totalLengthInPixels
            CardWidth = $cardWidth
            HiddenCardWidth = $hiddenCardWidth
            PeekCardWidth = $peekCardWidth
            CardDateSeparation = $cardDateSeparation
            DateWidth = $dateWidth
            DateCvcSeparation = $dateCvcSeparation
            CvcWidth = $cvcWidth
            CvcPostalCodeSeparation = $cvcPostalCodeSeparation
            PostalCodeWidth: $postalCodeWidth
            """

        return elementSizeData + touchBufferData
    }
}
