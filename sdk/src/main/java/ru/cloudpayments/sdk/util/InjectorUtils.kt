package ru.cloudpayments.sdk.util

import ru.cloudpayments.sdk.api.models.CloudpaymentsTransaction
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.configuration.PaymentData
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishStatus
import ru.cloudpayments.sdk.viewmodel.PaymentFinishViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentOptionsViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentProcessViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentSBPViewModelFactory
import ru.cloudpayments.sdk.viewmodel.PaymentTinkoffPayViewModelFactory

internal object InjectorUtils {

    fun providePaymentOptionsViewModelFactory(paymentConfiguration: PaymentConfiguration): PaymentOptionsViewModelFactory {
        return PaymentOptionsViewModelFactory(paymentConfiguration)
    }
    fun providePaymentProcessViewModelFactory(paymentData: PaymentData, cryptogram: String, useDualMessagePayment: Boolean, saveCard: Boolean?): PaymentProcessViewModelFactory {
        return PaymentProcessViewModelFactory(paymentData, cryptogram, useDualMessagePayment, saveCard)
    }

    fun providePaymentTinkoffPayViewModelFactory(qrUrl: String, transactionId: Long, paymentConfiguration: PaymentConfiguration, saveCard: Boolean?): PaymentTinkoffPayViewModelFactory {
        return PaymentTinkoffPayViewModelFactory(qrUrl, transactionId, paymentConfiguration, saveCard)
    }

    fun providePaymentSBPViewModelFactory(paymentData: PaymentData, useDualMessagePayment: Boolean, saveCard: Boolean?): PaymentSBPViewModelFactory {
        return PaymentSBPViewModelFactory(paymentData, useDualMessagePayment, saveCard)
    }

    fun providePaymentFinishViewModelFactory(status: PaymentFinishStatus,
                                             transaction: CloudpaymentsTransaction?,
                                             reasonCode: String?): PaymentFinishViewModelFactory {
        return PaymentFinishViewModelFactory(status,
                                             transaction,
                                             reasonCode)
    }
}