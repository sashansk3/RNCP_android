package ru.cloudpayments.sdk.configuration

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.cloudpayments.sdk.scanner.CardScanner

@Parcelize
data class PaymentConfiguration(val publicId: String,
								val paymentData: PaymentData,
								val scanner: CardScanner? = null,
								val requireEmail: Boolean = false,
								val useDualMessagePayment: Boolean = false,
								val disableGPay: Boolean = false,
								val disableSbpPay: Boolean = false,
								var tinkoffPaySuccessRedirectUrl: String = "",
								var tinkoffPayFailRedirectUrl: String = "",
								val apiUrl: String = "",
								val mode: CloudpaymentsSDK.SDKRunMode = CloudpaymentsSDK.SDKRunMode.SelectPaymentMethod,
								val testMode: Boolean = false): Parcelable

