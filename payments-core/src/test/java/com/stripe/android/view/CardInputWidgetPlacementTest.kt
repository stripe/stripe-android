package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class CardInputWidgetPlacementTest {
    private val placementFullCardWithPostalCode = CardInputWidgetPlacement(
        totalLengthInPixels = SCREEN_WIDTH,
        cardWidth = 230,
        hiddenCardWidth = 150,
        peekCardWidth = 40,
        cardDateSeparation = 220,
        dateWidth = 50,
        dateCvcSeparation = 0,
        cvcWidth = 30,
        cvcPostalCodeSeparation = 0,
        postalCodeWidth = 100,
        cardTouchBufferLimit = 400,
        dateStartPosition = 510,
        dateEndTouchBufferLimit = 0,
        cvcStartPosition = 0,
        cvcEndTouchBufferLimit = 0,
        postalCodeStartPosition = 0
    )

    private val placementPeekCardWithPostalCode = CardInputWidgetPlacement(
        totalLengthInPixels = SCREEN_WIDTH,
        cardWidth = 230,
        hiddenCardWidth = 150,
        peekCardWidth = 40,
        cardDateSeparation = 98,
        dateWidth = 50,
        dateCvcSeparation = 82,
        cvcWidth = 30,
        cvcPostalCodeSeparation = 0,
        postalCodeWidth = 100,
        cardTouchBufferLimit = 66,
        dateStartPosition = 198,
        dateEndTouchBufferLimit = 110,
        cvcStartPosition = 330,
        cvcEndTouchBufferLimit = 120,
        postalCodeStartPosition = 360
    )

    private val placementPeekCardWithoutPostalCode = CardInputWidgetPlacement(
        totalLengthInPixels = SCREEN_WIDTH,
        cardWidth = 230,
        hiddenCardWidth = 150,
        peekCardWidth = 40,
        cardDateSeparation = 185,
        dateWidth = 50,
        dateCvcSeparation = 195,
        cvcWidth = 30,
        cvcPostalCodeSeparation = 0,
        postalCodeWidth = 100,
        cardTouchBufferLimit = 192,
        dateStartPosition = 285,
        dateEndTouchBufferLimit = 432,
        cvcStartPosition = 530,
        cvcEndTouchBufferLimit = 0,
        postalCodeStartPosition = 0
    )

    @Test
    fun getFocusField_whenTouchInDateSlop_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 420 and 510 needs to send focus to the date editor
        assertThat(
            placementFullCardWithPostalCode.getFocusField(
                430,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = true,
                postalCodeEnabled = true
            )
        ).isEqualTo(CardInputWidget.Field.Expiry)
    }

    @Test
    fun getFocusField_whenTouchInDateEditor_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch over 510 doesn't need to do anything
        assertThat(
            placementFullCardWithPostalCode.getFocusField(
                530,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = true,
                postalCodeEnabled = true
            )
        ).isNull()
    }

    @Test
    fun getFocusField_whenTouchOnImage_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch lower than 60 will be the icon
        assertThat(
            placementFullCardWithPostalCode.getFocusField(
                30,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = true,
                postalCodeEnabled = true
            )
        ).isNull()
    }

    @Test
    fun getFocusField_whenTouchActualCardWidget_returnsNull() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 60 and 250 will be the actual card widget
        assertThat(
            placementFullCardWithPostalCode.getFocusField(
                200,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = true,
                postalCodeEnabled = true
            )
        ).isNull()
    }

    @Test
    fun getFocusField_whenTouchInCardEditorSlop_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(card==230)--(space==220)--(date==50)|
        // |img==60||  cardTouchArea | 420 | dateTouchArea | dateStart==510 |
        // So any touch between 250 and 420 needs to send focus to the card editor
        assertThat(
            placementFullCardWithPostalCode.getFocusField(
                300,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = true,
                postalCodeEnabled = true
            )
        ).isEqualTo(CardInputWidget.Field.Number)
    }

    @Test
    fun getFocusField_whenInPeekAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 60 and 100 does nothing
        assertThat(
            placementPeekCardWithPostalCode.getFocusField(
                75,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = true
            )
        ).isNull()
    }

    @Test
    fun getFocusField_whenInDateStartSlopAfterShift_withPostalCodeEnabled_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor
        assertThat(
            placementPeekCardWithPostalCode.getFocusField(
                170,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = true
            )
        ).isEqualTo(CardInputWidget.Field.Expiry)
    }

    @Test
    fun getFocusField_whenInPeekSlopAfterShift_withPostalCodeDisabled_returnsCardEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 100 and 192 returns the card editor
        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                150,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false

            )
        ).isEqualTo(CardInputWidget.Field.Number)
    }

    @Test
    fun getFocusField_whenInDateStartSlopAfterShift_withPostalCodeDisabled_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 192 and 285 returns the date editor

        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                200,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isEqualTo(CardInputWidget.Field.Expiry)
    }

    @Test
    fun getFocusField_whenInDateAfterShift_withPostalCodeDisabled_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing

        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                300,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isNull()
    }

    @Test
    fun getFocusField_withPostalCodeEnabled_whenInDateAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 285 and 335 does nothing
        assertThat(
            placementPeekCardWithPostalCode.getFocusField(
                200,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = true
            )
        ).isNull()
    }

    @Test
    fun getFocusField_withPostalCodeDisabled_whenInDateEndSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                400,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isEqualTo(CardInputWidget.Field.Expiry)
    }

    @Test
    fun getFocusField_withPostalCodeEnabled_whenInDateEndSlopAfterShift_returnsDateEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 335 and 432 returns the date editor
        assertThat(
            placementPeekCardWithPostalCode.getFocusField(
                185,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isEqualTo(CardInputWidget.Field.Expiry)
    }

    @Test
    fun getFocusField_withPostalCodeDisabled_whenInCvcSlopAfterShift_returnsCvcEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor

        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                485,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isEqualTo(CardInputWidget.Field.Cvc)
    }

    @Test
    fun getFocusField_withPostalCodeEnabled_whenInCvcSlopAfterShift_returnsCvcEditor() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch between 432 and 530 returns the date editor
        assertThat(
            placementPeekCardWithPostalCode.getFocusField(
                300,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isEqualTo(CardInputWidget.Field.Cvc)
    }

    @Test
    fun getFocusField_whenInCvcAfterShift_returnsNull() {
        // |img==60||---total == 500--------|
        // |(peek==40)--(space==185)--(date==50)--(space==195)--(cvc==30)|
        // |img=60|cardTouchLimit==192|dateStart==285|dateTouchLim==432|cvcStart==530|
        // So any touch over 530 does nothing
        assertThat(
            placementPeekCardWithoutPostalCode.getFocusField(
                545,
                frameStart = BRAND_ICON_WIDTH,
                isShowingFullCard = false,
                postalCodeEnabled = false
            )
        ).isNull()
    }

    private companion object {
        private const val SCREEN_WIDTH = 500
        private const val BRAND_ICON_WIDTH = 60
    }
}
