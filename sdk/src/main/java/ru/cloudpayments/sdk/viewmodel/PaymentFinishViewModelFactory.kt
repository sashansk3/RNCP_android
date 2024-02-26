package ru.cloudpayments.sdk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.cloudpayments.sdk.api.models.CloudpaymentsTransaction
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishStatus

internal class PaymentFinishViewModelFactory(
	val status: PaymentFinishStatus,
	val transaction: CloudpaymentsTransaction? = null,
	val reasonCode: String? = null
): ViewModelProvider.Factory {

	@Suppress("UNCHECKED_CAST")
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return PaymentFinishViewModel(status,
									  transaction,
									  reasonCode) as T
	}
}