package com.stripe.android.view

import android.view.View
import com.stripe.android.view.CardInputWidget.Field

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

    /**
     * Checks on the horizontal position of a touch event to see if
     * that event needs to be associated with one of the controls even
     * without having actually touched it. This essentially gives a larger
     * touch surface to the controls. We return `null` if the user touches
     * actually inside the widget because no interception is necessary - the touch will
     * naturally give focus to that control, and we don't want to interfere with what
     * Android will naturally do in response to that touch.
     *
     * @param touchX distance in pixels from the left side of this control
     * @return a [Field] that represents the [View] to request focus, or `null`
     * if no such request is necessary.
     */
    internal fun getFocusField(
        touchX: Int,
        frameStart: Int,
        isShowingFullCard: Boolean,
        postalCodeEnabled: Boolean
    ) = when {
        isShowingFullCard -> {
            // Then our view is
            // |full card||space||date|

            when {
                touchX < frameStart + cardWidth -> // Then the card edit view will already handle this touch.
                    null
                touchX < cardTouchBufferLimit -> // Then we want to act like this was a touch on the card view
                    Field.Number
                touchX < dateStartPosition -> // Then we act like this was a touch on the date editor.
                    Field.Expiry
                else -> // Then the date editor will already handle this touch.
                    null
            }
        }
        postalCodeEnabled -> {
            // Our view is
            // |peek card||space||date||space||cvc||space||postal code|
            when {
                touchX < frameStart + peekCardWidth -> // This was a touch on the card number editor, so we don't need to handle it.
                    null
                touchX < cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                    Field.Number
                touchX < dateStartPosition -> // Then we need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < dateStartPosition + dateWidth -> // Just a regular touch on the date editor.
                    null
                touchX < dateRightTouchBufferLimit -> // We need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                    Field.Cvc
                touchX < cvcStartPosition + cvcWidth -> // Just a regular touch on the cvc editor.
                    null
                touchX < cvcRightTouchBufferLimit -> // We need to act like this was a touch on the cvc editor.
                    Field.Cvc
                touchX < postalCodeStartPosition -> // We need to act like this was a touch on the postal code editor.
                    Field.PostalCode
                else -> null
            }
        }
        else -> {
            // Our view is
            // |peek card||space||date||space||cvc|
            when {
                touchX < frameStart + peekCardWidth -> // This was a touch on the card number editor, so we don't need to handle it.
                    null
                touchX < cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                    Field.Number
                touchX < dateStartPosition -> // Then we need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < dateStartPosition + dateWidth -> // Just a regular touch on the date editor.
                    null
                touchX < dateRightTouchBufferLimit -> // We need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                    Field.Cvc
                else -> null
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
