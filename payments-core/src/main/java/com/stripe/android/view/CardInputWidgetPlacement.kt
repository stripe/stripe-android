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
    internal var dateEndTouchBufferLimit: Int = 0,
    internal var cvcStartPosition: Int = 0,
    internal var cvcEndTouchBufferLimit: Int = 0,
    internal var postalCodeStartPosition: Int = 0
) {
    private val cardPeekDateStartMargin: Int
        @JvmSynthetic
        get() {
            return peekCardWidth + cardDateSeparation
        }

    private val cardPeekCvcStartMargin: Int
        @JvmSynthetic
        get() {
            return cardPeekDateStartMargin + dateWidth + dateCvcSeparation
        }

    private val cardPeekPostalCodeStartMargin: Int
        @JvmSynthetic
        get() {
            return cardPeekCvcStartMargin + cvcWidth + cvcPostalCodeSeparation
        }

    @JvmSynthetic
    internal fun getDateStartMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            cardWidth + cardDateSeparation
        } else {
            cardPeekDateStartMargin
        }
    }

    @JvmSynthetic
    internal fun getCvcStartMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            totalLengthInPixels
        } else {
            cardPeekCvcStartMargin
        }
    }

    @JvmSynthetic
    internal fun getPostalCodeStartMargin(isFullCard: Boolean): Int {
        return if (isFullCard) {
            totalLengthInPixels
        } else {
            cardPeekPostalCodeStartMargin
        }
    }

    private fun toMinimalValueIfNegative(value: Int) = if (value >= 0) {
        value
    } else {
        MIN_SEPARATION_IN_PX
    }

    /**
     * Calculates the total required width for all fields and separations in the compact layout.
     * This helps detect when accessibility font scaling would cause field overflow.
     */
    @JvmSynthetic
    internal fun calculateRequiredWidth(postalCodeEnabled: Boolean): Int {
        return if (postalCodeEnabled) {
            peekCardWidth + cardDateSeparation + dateWidth + dateCvcSeparation + 
            cvcWidth + cvcPostalCodeSeparation + postalCodeWidth
        } else {
            peekCardWidth + cardDateSeparation + dateWidth + dateCvcSeparation + cvcWidth
        }
    }

    /**
     * Detects if the current field widths would cause overflow and adapts spacing accordingly.
     * This addresses accessibility font scaling issues where enlarged text causes field truncation.
     */
    @JvmSynthetic
    internal fun detectAndAdaptForOverflow(
        frameWidth: Int,
        postalCodeEnabled: Boolean
    ): AccessibilityAdaptation {
        val requiredWidth = calculateRequiredWidth(postalCodeEnabled)
        val availableWidth = frameWidth
        
        return if (requiredWidth > availableWidth) {
            val overflowAmount = requiredWidth - availableWidth
            AccessibilityAdaptation.OverflowDetected(
                overflowAmount = overflowAmount,
                compressionRatio = availableWidth.toFloat() / requiredWidth.toFloat()
            )
        } else {
            AccessibilityAdaptation.NoOverflow
        }
    }

    /**
     * Sealed class representing different accessibility adaptation states
     */
    internal sealed class AccessibilityAdaptation {
        object NoOverflow : AccessibilityAdaptation()
        data class OverflowDetected(
            val overflowAmount: Int,
            val compressionRatio: Float
        ) : AccessibilityAdaptation()
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
                cardDateSeparation =
                    toMinimalValueIfNegative(frameWidth - cardWidth - dateWidth)
                cardTouchBufferLimit = frameStart + cardWidth + cardDateSeparation / 2
                dateStartPosition = frameStart + cardWidth + cardDateSeparation
            }
            postalCodeEnabled -> {
                updateSpacingWithAccessibilitySupport(
                    postalCodeEnabled = true,
                    frameStart = frameStart,
                    frameWidth = frameWidth
                )
            }
            else -> {
                updateSpacingWithAccessibilitySupport(
                    postalCodeEnabled = false,
                    frameStart = frameStart,
                    frameWidth = frameWidth
                )
            }
        }
    }

    /**
     * Updates spacing with accessibility font scaling support.
     * Detects overflow conditions and adapts field spacing to prevent truncation.
     */
    @JvmSynthetic
    private fun updateSpacingWithAccessibilitySupport(
        postalCodeEnabled: Boolean,
        frameStart: Int,
        frameWidth: Int
    ) {
        // First, calculate spacing using the original algorithm
        if (postalCodeEnabled) {
            updateOriginalPostalCodeSpacing(frameStart, frameWidth)
        } else {
            updateOriginalNonPostalCodeSpacing(frameStart, frameWidth)
        }

        // Check for overflow and adapt if necessary
        val adaptation = detectAndAdaptForOverflow(frameWidth, postalCodeEnabled)
        
        when (adaptation) {
            is AccessibilityAdaptation.NoOverflow -> {
                // No adaptation needed, keep original spacing
            }
            is AccessibilityAdaptation.OverflowDetected -> {
                // Apply adaptive spacing to prevent field truncation
                applyAdaptiveSpacing(
                    postalCodeEnabled = postalCodeEnabled,
                    frameStart = frameStart,
                    frameWidth = frameWidth,
                    compressionRatio = adaptation.compressionRatio
                )
            }
        }
    }

    /**
     * Applies the original postal code spacing algorithm
     */
    @JvmSynthetic
    private fun updateOriginalPostalCodeSpacing(frameStart: Int, frameWidth: Int) {
        this.cardDateSeparation = toMinimalValueIfNegative(
            (frameWidth * 3 / 10) - peekCardWidth - dateWidth / 4
        )
        this.dateCvcSeparation = toMinimalValueIfNegative(
            (frameWidth * 3 / 5) - peekCardWidth - cardDateSeparation - dateWidth - cvcWidth
        )
        this.cvcPostalCodeSeparation =
            toMinimalValueIfNegative(
                frameWidth - peekCardWidth - cardDateSeparation - dateWidth - cvcWidth - dateCvcSeparation -
                    postalCodeWidth
            )

        val dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
        this.cardTouchBufferLimit = dateStartPosition / 3
        this.dateStartPosition = dateStartPosition

        val cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
        this.dateEndTouchBufferLimit = cvcStartPosition / 3
        this.cvcStartPosition = cvcStartPosition

        val postalCodeStartPosition = cvcStartPosition + cvcWidth + cvcPostalCodeSeparation
        this.cvcEndTouchBufferLimit = postalCodeStartPosition / 3
        this.postalCodeStartPosition = postalCodeStartPosition
    }

    /**
     * Applies the original non-postal code spacing algorithm
     */
    @JvmSynthetic
    private fun updateOriginalNonPostalCodeSpacing(frameStart: Int, frameWidth: Int) {
        this.cardDateSeparation = toMinimalValueIfNegative(
            frameWidth / 2 - peekCardWidth - dateWidth / 2
        )
        this.dateCvcSeparation = toMinimalValueIfNegative(
            frameWidth - peekCardWidth - cardDateSeparation - dateWidth - cvcWidth
        )

        this.cardTouchBufferLimit = frameStart + peekCardWidth + cardDateSeparation / 2
        this.dateStartPosition = frameStart + peekCardWidth + cardDateSeparation

        this.dateEndTouchBufferLimit = dateStartPosition + dateWidth + dateCvcSeparation / 2
        this.cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
    }

    /**
     * Applies adaptive spacing when accessibility font scaling causes overflow.
     * Proportionally reduces field separations while maintaining minimum touch targets.
     */
    @JvmSynthetic
    private fun applyAdaptiveSpacing(
        postalCodeEnabled: Boolean,
        frameStart: Int,
        frameWidth: Int,
        compressionRatio: Float
    ) {
        // Calculate adaptive separations that fit the available space
        val totalFieldWidth = if (postalCodeEnabled) {
            peekCardWidth + dateWidth + cvcWidth + postalCodeWidth
        } else {
            peekCardWidth + dateWidth + cvcWidth
        }
        
        val availableSpaceForSeparations = frameWidth - totalFieldWidth
        val minTotalSeparation = if (postalCodeEnabled) {
            MIN_SEPARATION_IN_PX * 3 // card-date, date-cvc, cvc-postal
        } else {
            MIN_SEPARATION_IN_PX * 2 // card-date, date-cvc
        }

        if (availableSpaceForSeparations >= minTotalSeparation) {
            // We can fit with minimal separations
            if (postalCodeEnabled) {
                val remainingSpace = availableSpaceForSeparations - minTotalSeparation
                this.cardDateSeparation = MIN_SEPARATION_IN_PX + remainingSpace / 3
                this.dateCvcSeparation = MIN_SEPARATION_IN_PX + remainingSpace / 3
                this.cvcPostalCodeSeparation = MIN_SEPARATION_IN_PX + remainingSpace / 3
            } else {
                val remainingSpace = availableSpaceForSeparations - minTotalSeparation
                this.cardDateSeparation = MIN_SEPARATION_IN_PX + remainingSpace / 2
                this.dateCvcSeparation = MIN_SEPARATION_IN_PX + remainingSpace / 2
            }
        } else {
            // Extreme case: use absolute minimum separations
            this.cardDateSeparation = MIN_SEPARATION_IN_PX
            this.dateCvcSeparation = MIN_SEPARATION_IN_PX
            if (postalCodeEnabled) {
                this.cvcPostalCodeSeparation = MIN_SEPARATION_IN_PX
            }
        }

        // Recalculate positions with new separations
        val dateStartPosition = frameStart + peekCardWidth + cardDateSeparation
        this.cardTouchBufferLimit = dateStartPosition / 3
        this.dateStartPosition = dateStartPosition

        val cvcStartPosition = dateStartPosition + dateWidth + dateCvcSeparation
        this.dateEndTouchBufferLimit = cvcStartPosition / 3
        this.cvcStartPosition = cvcStartPosition

        if (postalCodeEnabled) {
            val postalCodeStartPosition = cvcStartPosition + cvcWidth + cvcPostalCodeSeparation
            this.cvcEndTouchBufferLimit = postalCodeStartPosition / 3
            this.postalCodeStartPosition = postalCodeStartPosition
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
     * @param touchX distance in pixels from the start of this control
     * @return a [Field] that represents the [View] to request focus, or `null`
     * if no such request is necessary.
     */
    @Suppress("ComplexMethod")
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
                // This was a touch on the card number editor, so we don't need to handle it.
                touchX < frameStart + peekCardWidth ->
                    null
                touchX < cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                    Field.Number
                touchX < dateStartPosition -> // Then we need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < dateStartPosition + dateWidth -> // Just a regular touch on the date editor.
                    null
                touchX < dateEndTouchBufferLimit -> // We need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < cvcStartPosition -> // We need to act like this was a touch on the cvc editor.
                    Field.Cvc
                touchX < cvcStartPosition + cvcWidth -> // Just a regular touch on the cvc editor.
                    null
                touchX < cvcEndTouchBufferLimit -> // We need to act like this was a touch on the cvc editor.
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
                // This was a touch on the card number editor, so we don't need to handle it.
                touchX < frameStart + peekCardWidth ->
                    null
                touchX < cardTouchBufferLimit -> // Then we need to act like the user touched the card editor
                    Field.Number
                touchX < dateStartPosition -> // Then we need to act like this was a touch on the date editor
                    Field.Expiry
                touchX < dateStartPosition + dateWidth -> // Just a regular touch on the date editor.
                    null
                touchX < dateEndTouchBufferLimit -> // We need to act like this was a touch on the date editor
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
            DateEndTouchBufferLimit = $dateEndTouchBufferLimit
            CvcStartPosition = $cvcStartPosition
            CvcEndTouchBufferLimit = $cvcEndTouchBufferLimit
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

    private companion object {
        const val MIN_SEPARATION_IN_PX = 10
    }
}
