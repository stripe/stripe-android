package com.stripe.android.paymentsheet.example.playground.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.activity.BUTTON_COLOR
import com.stripe.android.paymentsheet.example.samples.activity.BasePaymentSheetActivity
import com.stripe.android.paymentsheet.example.samples.activity.MAIN_FONT_SIZE

internal class AppearancePlaygroundActivity : BasePaymentSheetActivity() {
    private val BASE_FONT_SIZE = 20.sp
    private val BASE_PADDING = 8.dp
    private val BACKGROUND_COLOR = Color(242, 242, 247)
    private val SECTION_LABEL_COLOR = Color(159, 159, 169)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        setContent {
            MaterialTheme {
                val inProgress by viewModel.inProgress.observeAsState(false)
                val status by viewModel.status.observeAsState("")
                val appearance by viewModel.appearance.observeAsState(PaymentSheet.Appearance())
                if (status.isNotBlank()) {
                    snackbar.setText(status).show()
                    viewModel.statusDisplayed()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .background(BACKGROUND_COLOR)
                ) {
                    CustomizationUi(appearance)
                    MainButton(
                        label = stringResource(R.string.reset_defaults),
                        enabled = !inProgress
                    ) {
                        viewModel.appearance.postValue(PaymentSheet.Appearance())
                    }
                    MainButton(
                        label = stringResource(R.string.show_sheet),
                        enabled = !inProgress,
                        onClick = {
                            prepareCheckout { customerConfig, clientSecret ->
                                paymentSheet.presentWithPaymentIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration(
                                        merchantDisplayName = merchantName,
                                        customer = customerConfig,
                                        googlePay = googlePayConfig,
                                        allowsDelayedPaymentMethods = true,
                                        appearance = appearance
                                    )
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    @Composable
    private fun MainButton(
        label: String,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        TextButton(
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = BUTTON_COLOR,
                contentColor = Color.White
            )
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(vertical = 2.dp),
                fontSize = MAIN_FONT_SIZE
            )
        }
    }

    @Composable
    private fun CustomizationCard(
        label: String,
        content: @Composable () -> Unit
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            color = SECTION_LABEL_COLOR,
            modifier = Modifier.padding(vertical = BASE_PADDING)
        )
        Card(
            backgroundColor = Color.White,
            elevation = 2.dp
        ) {
            Column {
                content()
            }
        }
    }

    @Composable
    private fun CustomizationUi(currentAppearance: PaymentSheet.Appearance) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Appearance",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            CustomizationCard("Colors") {
                Colors(currentAppearance)
            }
            CustomizationCard("Shapes") {
                Shapes(currentAppearance)
            }
            CustomizationCard("Typography") {
                Typography(currentAppearance)
            }
        }
    }

    @Composable
    private fun Colors(
        currentAppearance: PaymentSheet.Appearance
    ) {
        ColorItem(label = "primary", currentColor = Color(currentAppearance.colorsLight.primary)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    primary = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    primary = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "surface", currentColor = Color(currentAppearance.colorsLight.surface)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    surface = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    surface = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "component", currentColor = Color(currentAppearance.colorsLight.component)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    component = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    component = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "componentBorder", currentColor = Color(currentAppearance.colorsLight.componentBorder)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    componentBorder = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    componentBorder = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "componentDivider", currentColor = Color(currentAppearance.colorsLight.componentDivider)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    componentDivider = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    componentDivider = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "onComponent", currentColor = Color(currentAppearance.colorsLight.onComponent)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    onComponent = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    onComponent = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "onSurface", currentColor = Color(currentAppearance.colorsLight.onSurface)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    onSurface = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    onSurface = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "subtitle", currentColor = Color(currentAppearance.colorsLight.subtitle)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    subtitle = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    subtitle = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "placeholderText", currentColor = Color(currentAppearance.colorsLight.placeholderText)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    placeholderText = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    placeholderText = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "appBarIcon", currentColor = Color(currentAppearance.colorsLight.appBarIcon)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    appBarIcon = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    appBarIcon = it.toArgb()
                )
            )
        }
        Divider()
        ColorItem(label = "error", currentColor = Color(currentAppearance.colorsLight.error)) {
            currentAppearance.copy(
                colorsLight = currentAppearance.colorsLight.copy(
                    error = it.toArgb()
                ),
                colorsDark = currentAppearance.colorsDark.copy(
                    error = it.toArgb()
                )
            )
        }
    }

    @Composable
    private fun Shapes(
        currentAppearance: PaymentSheet.Appearance
    ) {
        IncrementDecrementItem("cornerRadiusDp", currentAppearance.shapes.cornerRadiusDp) {
            viewModel.appearance.postValue(
                currentAppearance.copy(
                    shapes = currentAppearance.shapes.copy(
                        cornerRadiusDp = it
                    )
                )
            )
        }
        Divider()
        IncrementDecrementItem("borderStrokeWidthDp", currentAppearance.shapes.borderStrokeWidthDp) {
            viewModel.appearance.postValue(
                currentAppearance.copy(
                    shapes = currentAppearance.shapes.copy(
                        borderStrokeWidthDp = it
                    )
                )
            )
        }
    }

    @Composable
    private fun Typography(
        currentAppearance: PaymentSheet.Appearance
    ) {
        FontScaleSlider(currentAppearance.typography.sizeScaleFactor) {
            viewModel.appearance.postValue(
                currentAppearance.copy(
                    typography = currentAppearance.typography.copy(
                        sizeScaleFactor = it
                    )
                )
            )
        }
        Divider()
        FontDropDown(currentAppearance.typography.fontResId) {
            viewModel.appearance.postValue(
                currentAppearance.copy(
                    typography = currentAppearance.typography.copy(
                        fontResId = it
                    )
                )
            )
        }
    }

    @Composable
    private fun ColorItem(label: String, currentColor: Color, onColorPicked: (Color) -> PaymentSheet.Appearance) {
        val openDialog = remember { mutableStateOf(false) }
        ColorPicker(openDialog, currentColor) {
            viewModel.appearance.postValue(
                onColorPicked(it)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = BASE_PADDING)
                .clickable { openDialog.value = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = BASE_FONT_SIZE)
            ColorIcon(innerColor = currentColor)
        }
    }

    @Composable
    private fun ColorPicker(
        openDialog: MutableState<Boolean>,
        defaultColor: Color,
        onClose: (Color) -> Unit,
    ) {
        val currentColor = remember { mutableStateOf(defaultColor) }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    openDialog.value = false
                    onClose(currentColor.value)
                },
                text = {
                    ClassicColorPicker(
                        modifier = Modifier.fillMaxSize(),
                        onColorChanged = {
                            currentColor.value = it.toColor()
                        }
                    )
                },
                buttons = {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = BASE_PADDING),
                        colors = ButtonDefaults.buttonColors(backgroundColor = currentColor.value),
                        onClick = {
                            openDialog.value = false
                            onClose(currentColor.value)
                        }
                    ) {
                        Text(text = "Pick Color")
                    }
                }
            )
        }
    }

    @Composable
    private fun ColorIcon(innerColor: Color) {
        val brush = Brush.verticalGradient(
            listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta
            )
        )
        Box(
            modifier = Modifier
                .border(BorderStroke(3.dp, brush), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(innerColor)
                    .align(Alignment.Center)
            )
        }
    }

    @Composable
    private fun IncrementDecrementItem(label: String, value: Float, onValueChange: (Float) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = BASE_PADDING),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label: $value", fontSize = BASE_FONT_SIZE)
            Row {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(32.dp)
                        .width(50.dp)
                        .clickable {
                            val newValue = value - 1
                            onValueChange(if (newValue < 0) 0.0f else newValue)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = null,
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(32.dp)
                        .width(50.dp)
                        .clickable {
                            onValueChange(value + 1)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Composable
    private fun FontScaleSlider(sliderPosition: Float, onValueChange: (Float) -> Unit) {
        Text(
            text = "sizeScaleFactor: $sliderPosition",
            fontSize = BASE_FONT_SIZE,
            modifier = Modifier.padding(horizontal = BASE_PADDING)
        )
        Slider(
            value = sliderPosition,
            valueRange = 0f..2f,
            onValueChange = {
                onValueChange(it)
            }
        )
    }

    @Composable
    private fun FontDropDown(fontResId: Int?, fontSelectedCallback: (Int?) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val items = mapOf(
            R.font.cursive to "Cursive",
            R.font.opensans to "OpenSans",
            null to "Default"
        )

        items[fontResId]?.let {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BASE_PADDING)
                    .wrapContentSize(Alignment.TopStart)
            ) {
                Text(
                    text = "Font Resource: $it",
                    fontFamily = getFontFromResource(fontResId),
                    fontSize = BASE_FONT_SIZE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { expanded = true })
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEach { font ->
                        FontDropDownMenuItem(label = font.value, fontResId = font.key) {
                            expanded = false
                            fontSelectedCallback(font.key)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FontDropDownMenuItem(label: String, fontResId: Int?, onClick: () -> Unit) {
        DropdownMenuItem(
            onClick = onClick,
        ) {
            Text(
                text = label,
                fontFamily = getFontFromResource(fontResId)
            )
        }
    }

    private fun getFontFromResource(fontResId: Int?): FontFamily {
        return fontResId?.let {
            FontFamily(Font(it))
        } ?: run {
            FontFamily.Default
        }
    }
}
