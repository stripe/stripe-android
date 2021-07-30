package com.stripe.android.paymentsheet

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// @ExperimentalAnimationApi
// @Preview(
//    name = "My Preview"
// )
// @Composable
// fun FormPreview(@PreviewParameter(FormViewModelProvider::class) formViewModel: FormViewModel) {
//    Form(formViewModel)
// }
@Preview(
    widthDp = 100,
    heightDp = 360
)
@Composable
fun DefaultPreview() {
    Text(text = "Hello Android!", modifier = Modifier.fillMaxHeight().fillMaxWidth())
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

//
// internal class FormViewModelProvider : PreviewParameterProvider<FormViewModel> {
//    val addressFieldElementRepository = AddressFieldElementRepository(mock())
//    val bankRepository = BankRepository(mock())
//
//    init {
//        addressFieldElementRepository.init(
//            AddressFieldElementRepository.supportedCountries.associateWith { countryCode ->
//                "src/main/assets/addressinfo/$countryCode.json"
//            }
//                .mapValues { (_, assetFileName) ->
//                    requireNotNull(
//                        parseAddressesSchema(
//                            File(assetFileName).inputStream()
//                        )
//                    )
//                }
//        )
//
//        bankRepository.init(
//            SupportedBankType.values().associateWith { bankType ->
//                getInputStream(bankType)
//            }
//        )
//    }
//
//    override val values: Sequence<FormViewModel> = sequenceOf(
//        FormViewModel(
//            sofort.layout,
//            saveForFutureUseInitialValue = true,
//            saveForFutureUseInitialVisibility = true,
//            merchantName = "Merchant Name",
//            resourceRepository = ResourceRepository(
//                addressRepository = addressFieldElementRepository,
//                bankRepository = bankRepository
//            )
//        )
//    )
//
//    private fun getInputStream(bankType: SupportedBankType) =
//        File("src/main/assets/${bankType.assetFileName}").inputStream()
//
// }
