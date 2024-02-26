package ru.cloudpayments.sdk.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.databinding.DialogCpsdkPaymentTinkoffpayBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentDialogFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.viewmodel.PaymentTinkoffPayViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentTinkoffPayViewState

internal enum class PaymentTinkoffPayStatus {
	GetLink,
	InProcess,
	Succeeded,
	Failed;
}

internal class PaymentTinkoffPayFragment: BasePaymentDialogFragment<PaymentTinkoffPayViewState, PaymentTinkoffPayViewModel>() {
	interface IPaymentTinkoffPayFragment {
		fun onPaymentTinkoffPayFinished(transactionId: Long)
		fun onPaymentTinkoffPayFailed(transactionId: Long, reasonCode: Int?)
		fun finishPayment()
		fun retryPayment()
	}

	companion object {
		private const val ARG_QR_URL = "ARG_QR_URL"
		private const val ARG_TRANSACTION_ID = "ARG_TRANSACTION_ID"


		fun newInstance(qrUrl: String, transactionId: Long) = PaymentTinkoffPayFragment().apply {
			arguments = Bundle()
			arguments?.putString(ARG_QR_URL, qrUrl)
			arguments?.putLong(ARG_TRANSACTION_ID, transactionId)
		}
	}

	private var _binding: DialogCpsdkPaymentTinkoffpayBinding? = null

	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = DialogCpsdkPaymentTinkoffpayBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private var currentState: PaymentTinkoffPayViewState? = null

	override val viewModel: PaymentTinkoffPayViewModel by viewModels {
		InjectorUtils.providePaymentTinkoffPayViewModelFactory(
			qrUrl,
			transactionId,
			paymentConfiguration!!,
			sdkConfig?.saveCard)
	}

	override fun render(state: PaymentTinkoffPayViewState) {
		currentState = state
		updateWith(state.status, state.errorMessage)
	}

	private val qrUrl by lazy {
		arguments?.getString(ARG_QR_URL) ?: ""
	}

	private val transactionId by lazy {
		arguments?.getLong(ARG_TRANSACTION_ID) ?: 0
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		if (savedInstanceState == null) {
			activity().component.inject(viewModel)

			if (qrUrl.isNotEmpty()) {
				currentState?.copy(qrUrl = qrUrl, transactionId = transactionId)
				updateWith(PaymentTinkoffPayStatus.InProcess)
			} else {
				updateWith(PaymentTinkoffPayStatus.GetLink)
			}
		}
	}


	private fun updateWith(status: PaymentTinkoffPayStatus, error: String? = null) {

		var status = status

		when (status) {
			PaymentTinkoffPayStatus.GetLink -> {
				binding.iconStatus.setImageResource(R.drawable.cpsdk_ic_progress)
				binding.textStatus.setText(R.string.cpsdk_text_process_title_tinkoff_pay)
				binding.textDescription.visibility = View.VISIBLE
				binding.textDescription.setText(R.string.cpsdk_text_process_description_tinkoff_pay)
				binding.buttonFinish.visibility = View.VISIBLE
				binding.buttonFinish.setBackgroundResource(R.drawable.cpsdk_bg_rounded_white_button_with_border)
				binding.buttonFinish.setTextColor(context?.let { ContextCompat.getColor(it, R.color.cpsdk_blue) } ?: 0xFFFFFF)

				if (paymentConfiguration?.mode == CloudpaymentsSDK.SDKRunMode.TinkoffPay) {
					binding.buttonFinish.setText(R.string.cpsdk_text_process_button_close)
					binding.buttonFinish.setOnClickListener {
						val listener = requireActivity() as? IPaymentTinkoffPayFragment
						listener?.finishPayment()
						dismiss()
					}
				} else {
					binding.buttonFinish.setText(R.string.cpsdk_text_process_button_tinkoff_pay_sbp)
					binding.buttonFinish.setOnClickListener {
						val listener = requireActivity() as? IPaymentTinkoffPayFragment
						listener?.retryPayment()
						dismiss()
					}
				}

				viewModel.getTinkoffQrPayLink(true)
			}

			PaymentTinkoffPayStatus.InProcess -> {

				binding.iconStatus.setImageResource(R.drawable.cpsdk_ic_progress)
				binding.textStatus.setText(R.string.cpsdk_text_process_title_tinkoff_pay)
				binding.textDescription.visibility = View.VISIBLE
				binding.textDescription.setText(R.string.cpsdk_text_process_description_tinkoff_pay)
				binding.buttonFinish.visibility = View.VISIBLE
				binding.buttonFinish.setBackgroundResource(R.drawable.cpsdk_bg_rounded_white_button_with_border)
				binding.buttonFinish.setTextColor(context?.let { ContextCompat.getColor(it, R.color.cpsdk_blue) } ?: 0xFFFFFF)

				if (paymentConfiguration?.mode == CloudpaymentsSDK.SDKRunMode.TinkoffPay) {
					binding.buttonFinish.setText(R.string.cpsdk_text_process_button_close)
					binding.buttonFinish.setOnClickListener {
						val listener = requireActivity() as? IPaymentTinkoffPayFragment
						listener?.finishPayment()
						dismiss()
					}

					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentState?.qrUrl))
					context?.startActivity(intent)
					viewModel.qrLinkStatusWait(currentState?.transactionId)

				} else {

					binding.buttonFinish.setText(R.string.cpsdk_text_process_button_tinkoff_pay_sbp)
					binding.buttonFinish.setOnClickListener {
						val listener = requireActivity() as? IPaymentTinkoffPayFragment
						listener?.retryPayment()
						dismiss()
					}

					val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrUrl))
					context?.startActivity(intent)
					viewModel.qrLinkStatusWait(transactionId)
				}
			}

			PaymentTinkoffPayStatus.Succeeded, PaymentTinkoffPayStatus.Failed -> {
				binding.buttonFinish.visibility = View.VISIBLE
				binding.buttonFinish.setBackgroundResource(R.drawable.cpsdk_bg_rounded_blue_button)
				binding.buttonFinish.setTextColor(context?.let { ContextCompat.getColor(it, R.color.cpsdk_white) } ?: 0xFFFFFF)

				val listener = requireActivity() as? IPaymentTinkoffPayFragment

				if (status == PaymentTinkoffPayStatus.Succeeded) {
					binding.iconStatus.setImageResource(R.drawable.cpsdk_ic_success)
					binding.textStatus.setText(R.string.cpsdk_text_process_title_success)
					binding.textDescription.text = ""
					binding.textDescription.visibility = View.GONE
					binding.buttonFinish.setText(R.string.cpsdk_text_process_button_success)

					listener?.onPaymentTinkoffPayFinished(currentState?.transaction?.transactionId ?: 0)

					binding.buttonFinish.setOnClickListener {

						listener?.finishPayment()
						dismiss()
					}
				} else {

					binding.iconStatus.setImageResource(R.drawable.cpsdk_ic_failure)
					binding.textStatus.text =
						context?.let { ApiError.getErrorDescription(it, currentState?.reasonCode.toString()) }
					binding.textDescription.text =
						context?.let {
							val desc = ApiError.getErrorDescriptionExtra(it, currentState?.reasonCode.toString())
							if (desc.isEmpty()) {
								binding.textDescription.visibility = View.GONE
								""
							} else {
								binding.textDescription.visibility = View.VISIBLE
								desc
							}
						}

					if (paymentConfiguration?.mode == CloudpaymentsSDK.SDKRunMode.TinkoffPay) {
						binding.buttonFinish.setText(R.string.cpsdk_text_process_button_close)
					} else {
						binding.buttonFinish.setText(R.string.cpsdk_text_process_button_error)
					}

					listener?.onPaymentTinkoffPayFailed(currentState?.transaction?.transactionId ?: 0, currentState?.reasonCode)

					binding.buttonFinish.setOnClickListener {
						if (paymentConfiguration?.mode == CloudpaymentsSDK.SDKRunMode.TinkoffPay) {
							listener?.finishPayment()
						} else {
							listener?.retryPayment()
						}
						dismiss()
					}
				}
			}
		}
	}
}