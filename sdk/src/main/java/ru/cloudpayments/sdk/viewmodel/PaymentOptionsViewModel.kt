package ru.cloudpayments.sdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.models.SBPBanksItem
import ru.cloudpayments.sdk.api.models.SBPQrLinkBody
import ru.cloudpayments.sdk.api.models.TinkoffPayQrLinkBody
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.dialogs.PaymentOptionsStatus
import javax.inject.Inject

internal class PaymentOptionsViewModel(
    private val paymentConfiguration: PaymentConfiguration
): BaseViewModel<PaymentOptionsViewState>() {
    override var currentState = PaymentOptionsViewState()
    override val viewState: MutableLiveData<PaymentOptionsViewState> by lazy {
        MutableLiveData(currentState)
    }

    private var disposable: Disposable? = null

    @Inject
    lateinit var api: CloudpaymentsApi

    fun getTinkoffQrPayLink(saveCard: Boolean?) {

        val jsonDataString: String = try {
            val parser = JsonParser()
            val jsonElement = parser.parse(paymentConfiguration.paymentData.jsonData)
            Gson().toJson(jsonElement)
        } catch (e: JsonSyntaxException) {
            Log.e("CloudPaymentsSDK", "JsonSyntaxException in JsonData")
            ""
        }

        val body = TinkoffPayQrLinkBody(amount = paymentConfiguration.paymentData.amount,
                                        currency = paymentConfiguration.paymentData.currency,
                                        description = paymentConfiguration.paymentData.description ?: "",
                                        accountId = paymentConfiguration.paymentData.accountId ?: "",
                                        email = paymentConfiguration.paymentData.email ?: "",
                                        jsonData = jsonDataString,
                                        invoiceId = paymentConfiguration.paymentData.invoiceId ?: "",
                                        successRedirectUrl = paymentConfiguration.tinkoffPaySuccessRedirectUrl,
                                        failRedirectUrl = paymentConfiguration.tinkoffPayFailRedirectUrl,
                                        scheme = if (paymentConfiguration.useDualMessagePayment) "auth" else "charge")

        if (saveCard != null) {
            body.saveCard = saveCard
        }

        val state = currentState.copy(status = PaymentOptionsStatus.TinkoffPayLoading)
        stateChanged(state)

        disposable = api.getTinkoffPayQrLink(body)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.success == true) {
                    currentState.copy(status = PaymentOptionsStatus.TinkoffPaySuccess,
                                      qrUrl = response.transaction?.qrUrl,
                                      transactionId = response.transaction?.transactionId)
                } else {
                    currentState.copy(status = PaymentOptionsStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = PaymentOptionsStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    fun getSBPQrPayLink(saveCard: Boolean?) {

        val jsonDataString: String = try {
            val parser = JsonParser()
            val jsonElement = parser.parse(paymentConfiguration.paymentData.jsonData)
            Gson().toJson(jsonElement)
        } catch (e: JsonSyntaxException) {
            Log.e("CloudPaymentsSDK", "JsonSyntaxException in JsonData")
            ""
        }

        val body = SBPQrLinkBody(amount = paymentConfiguration.paymentData.amount,
                                 currency = paymentConfiguration.paymentData.currency,
                                 description = paymentConfiguration.paymentData.description ?: "",
                                 accountId = paymentConfiguration.paymentData.accountId ?: "",
                                 email = paymentConfiguration.paymentData.email ?: "",
                                 jsonData = jsonDataString,
                                 invoiceId = paymentConfiguration.paymentData.invoiceId ?: "",
                                 scheme = if (paymentConfiguration.useDualMessagePayment) "auth" else "charge")

        if (saveCard != null) {
            body.saveCard = saveCard
        }

        val state = currentState.copy(status = PaymentOptionsStatus.SbpLoading)
        stateChanged(state)

        disposable = api.getSBPQrLink(body)
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .map { response ->
                val state = if (response.success == true) {
                    currentState.copy(status = PaymentOptionsStatus.SbpSuccess,
                                      qrUrl = response.transaction?.qrUrl,
                                      transactionId = response.transaction?.transactionId,
                                      listOfBanks = response.transaction?.banks?.dictionary)
                } else {
                    currentState.copy(status = PaymentOptionsStatus.Failed)
                }
                stateChanged(state)
            }
            .onErrorReturn {
                val state = currentState.copy(status = PaymentOptionsStatus.Failed, reasonCode = ApiError.CODE_ERROR_CONNECTION)
                stateChanged(state)
            }
            .subscribe()
    }

    private fun stateChanged(viewState: PaymentOptionsViewState) {
        currentState = viewState.copy()
        this.viewState.apply {
            value = viewState
        }
    }

    override fun onCleared() {
        super.onCleared()

        disposable?.dispose()
    }
}

internal data class PaymentOptionsViewState(
    val status: PaymentOptionsStatus = PaymentOptionsStatus.Waiting,
    val reasonCode: String? = null,
    val qrUrl: String? = null,
    val transactionId: Long? = null,
    val listOfBanks: ArrayList<SBPBanksItem>? = null,
    val isSaveCard: Int? = null

): BaseViewState()