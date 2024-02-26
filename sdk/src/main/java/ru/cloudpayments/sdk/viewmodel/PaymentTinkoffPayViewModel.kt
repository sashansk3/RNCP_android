package ru.cloudpayments.sdk.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransaction
import ru.cloudpayments.sdk.api.models.QrLinkStatusWaitBody
import ru.cloudpayments.sdk.api.models.QrLinkStatusWaitResponse
import ru.cloudpayments.sdk.api.models.TinkoffPayQrLinkBody
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.configuration.PaymentData
import ru.cloudpayments.sdk.ui.dialogs.PaymentTinkoffPayStatus
import javax.inject.Inject

internal class PaymentTinkoffPayViewModel(
	private val qrUrl: String,
	private val transactionId: Long,
	private val paymentConfiguration: PaymentConfiguration,
	private val saveCard: Boolean?
): BaseViewModel<PaymentTinkoffPayViewState>() {
	override var currentState = PaymentTinkoffPayViewState()
	override val viewState: MutableLiveData<PaymentTinkoffPayViewState> by lazy {
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

		disposable = api.getTinkoffPayQrLink(body)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				val state = if (response.success == true) {
					currentState.copy(status = PaymentTinkoffPayStatus.InProcess, qrUrl = response.transaction?.qrUrl,
									  transactionId = response.transaction?.transactionId)
				} else {
					currentState.copy(status = PaymentTinkoffPayStatus.Failed)
				}
				stateChanged(state)
			}
			.onErrorReturn {
				val state = currentState.copy(status = PaymentTinkoffPayStatus.Failed)
				stateChanged(state)
			}
			.subscribe()
	}

	fun qrLinkStatusWait(transactionId: Long?) {

		val body = QrLinkStatusWaitBody(transactionId ?: 0)

		disposable = api.qrLinkStatusWait(body)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				checkQrLinkStatusWaitResponse(response)
			}
			.onErrorReturn {
				val state = currentState.copy(status = PaymentTinkoffPayStatus.Failed)
				stateChanged(state)
			}
			.subscribe()
	}

	private fun checkQrLinkStatusWaitResponse(response: QrLinkStatusWaitResponse) {

		if (response.success == true) {
			when (response.transaction?.status) {
				"Authorized", "Completed", "Cancelled" -> {
					val state = currentState.copy(status = PaymentTinkoffPayStatus.Succeeded)
					stateChanged(state)
				}
				"Declined" -> {
					val state = currentState.copy(status = PaymentTinkoffPayStatus.Failed)
					stateChanged(state)
				}
				else -> {
					qrLinkStatusWait(response.transaction?.transactionId)
				}
			}

		} else {
			val state = currentState.copy(status = PaymentTinkoffPayStatus.Failed)
			stateChanged(state)
		}
	}

	private fun stateChanged(viewState: PaymentTinkoffPayViewState) {
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

internal data class PaymentTinkoffPayViewState(
	val status: PaymentTinkoffPayStatus = PaymentTinkoffPayStatus.GetLink,
	val succeeded: Boolean = false,
	val qrUrl: String? = null,
	val transactionId: Long? = null,
	val transaction: CloudpaymentsTransaction? = null,
	val errorMessage: String? = null,
	val reasonCode: Int? = null
): BaseViewState()