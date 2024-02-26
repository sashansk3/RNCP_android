package ru.cloudpayments.sdk.ui.dialogs

import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import com.google.android.material.checkbox.MaterialCheckBox
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.api.models.SBPBanksItem
import ru.cloudpayments.sdk.databinding.DialogCpsdkPaymentOptionsBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.ui.PaymentActivity
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.InjectorUtils
import ru.cloudpayments.sdk.util.TextWatcherAdapter
import ru.cloudpayments.sdk.util.emailIsValid
import ru.cloudpayments.sdk.util.hideKeyboard
import ru.cloudpayments.sdk.viewmodel.PaymentOptionsViewModel
import ru.cloudpayments.sdk.viewmodel.PaymentOptionsViewState

internal enum class PaymentOptionsStatus {
	Waiting,
	TinkoffPayLoading,
	TinkoffPaySuccess,
	SbpLoading,
	SbpSuccess,
	Failed;
}
internal class PaymentOptionsFragment :
	BasePaymentBottomSheetFragment<PaymentOptionsViewState, PaymentOptionsViewModel>() {
	interface IPaymentOptionsFragment {
		fun runCardPayment()
		fun runTinkoffPay(qrUrl: String, transactionId: Long)
		fun runSbp(qrUrl: String, transactionId: Long, listOfBanks: ArrayList<SBPBanksItem>)
		fun runGooglePay()
	}

	companion object {
		fun newInstance() = PaymentOptionsFragment().apply {
			arguments = Bundle()
		}
	}

	private var _binding: DialogCpsdkPaymentOptionsBinding? = null

	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = DialogCpsdkPaymentOptionsBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	override val viewModel: PaymentOptionsViewModel by viewModels {
		InjectorUtils.providePaymentOptionsViewModelFactory(paymentConfiguration!!)
	}

	override fun render(state: PaymentOptionsViewState) {

		if (sdkConfig?.availablePaymentMethods?.googlePayAvailable == true) {
			binding.buttonGooglepay.root.visibility = View.VISIBLE
		} else {
			binding.buttonGooglepay.root.visibility = View.GONE
		}

		if (sdkConfig?.availablePaymentMethods?.tinkoffPayAvailable == true) {
			binding.buttonTinkoffPay.visibility = View.VISIBLE
		} else {
			binding.buttonTinkoffPay.visibility = View.GONE
		}

		if (sdkConfig?.availablePaymentMethods?.sbpAvailable == true) {
			binding.buttonSbp.visibility = View.VISIBLE
		} else {
			binding.buttonSbp.visibility = View.GONE
		}

		updateWith(state.status, state.reasonCode)
	}

	private fun updateWith(status: PaymentOptionsStatus, errorCode: String? = null) {

		var status = status

		when (status) {
			PaymentOptionsStatus.Waiting -> {
				setTinkoffPayLoading(false)
				setSbpLoading(false)
			}
			PaymentOptionsStatus.TinkoffPayLoading -> {
				setTinkoffPayLoading(true)
				disableAllButtons()
			}
			PaymentOptionsStatus.TinkoffPaySuccess -> {
				setTinkoffPayLoading(false)
				enableAllButtons()
				val listener = requireActivity() as? IPaymentOptionsFragment
				val qrUrl = viewModel.currentState.qrUrl
				val transactionId = viewModel.currentState.transactionId
				if (qrUrl != null && transactionId != null) {
					listener?.runTinkoffPay(qrUrl, transactionId)
					dismiss()
				}
			}
			PaymentOptionsStatus.SbpLoading -> {
				setSbpLoading(true)
				disableAllButtons()
			}
			PaymentOptionsStatus.SbpSuccess -> {
				setSbpLoading(false)
				enableAllButtons()
				val listener = requireActivity() as? IPaymentOptionsFragment
				val qrUrl = viewModel.currentState.qrUrl
				val transactionId = viewModel.currentState.transactionId
				val listOfBanks = viewModel.currentState.listOfBanks
				if (qrUrl != null && transactionId != null && listOfBanks != null) {
					listener?.runSbp(qrUrl, transactionId, listOfBanks)
					dismiss()
				}
			}
			PaymentOptionsStatus.Failed -> {
				setTinkoffPayLoading(false)
				setSbpLoading(false)
				enableAllButtons()

				if (errorCode == ApiError.CODE_ERROR_CONNECTION) {
					val listener = requireActivity() as? PaymentActivity
					listener?.onInternetConnectionError()
					dismiss()
					return
				}
			}
		}
	}

	private fun checkSaveCardState () {

		paymentConfiguration?.paymentData?.accountId?.let { accountId ->
			if (accountId.isNotEmpty()) {
				if (paymentConfiguration?.paymentData?.jsonDataHasRecurrent() == true && sdkConfig?.terminalConfiguration?.isSaveCard == 1) {
					setSaveCardHintVisible()
				}
				if (paymentConfiguration?.paymentData?.jsonDataHasRecurrent() == true && sdkConfig?.terminalConfiguration?.isSaveCard == 2) {
					setSaveCardHintVisible()
				}
				if (paymentConfiguration?.paymentData?.jsonDataHasRecurrent() == false && sdkConfig?.terminalConfiguration?.isSaveCard == 2) {
					setSaveCardCheckBoxVisible()
				}
				if (sdkConfig?.terminalConfiguration?.isSaveCard == 3) {
					setSaveCardHintVisible()
				}
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		activity().component.inject(viewModel)

		setTinkoffPayLoading(false)
		setSbpLoading(false)

		checkSaveCardState()

		binding.editEmail.setText(paymentConfiguration!!.paymentData.email)

		binding.editEmail.setOnFocusChangeListener { _, hasFocus ->
			errorMode(
				!hasFocus && !emailIsValid(binding.editEmail.text.toString()),
				binding.editEmail, binding.textFieldEmail
			)
		}

		binding.editEmail.addTextChangedListener(object : TextWatcherAdapter() {
			override fun afterTextChanged(s: Editable?) {
				super.afterTextChanged(s)
				updateStateButtons()
			}
		})

		if (paymentConfiguration!!.requireEmail) {
			binding.checkboxSendReceipt.visibility = View.GONE
			binding.textFieldEmail.visibility = View.VISIBLE
			binding.textEmailRequire.visibility = View.VISIBLE
		} else {
			binding.checkboxSendReceipt.visibility = View.VISIBLE
			if (paymentConfiguration!!.paymentData.email.isNullOrEmpty()) {
				binding.checkboxSendReceipt.checkedState = MaterialCheckBox.STATE_UNCHECKED
				binding.textFieldEmail.visibility = View.GONE
			} else {
				binding.checkboxSendReceipt.checkedState = MaterialCheckBox.STATE_CHECKED
				binding.textFieldEmail.visibility = View.VISIBLE
			}
			binding.textEmailRequire.visibility = View.GONE
		}

		binding.checkboxSendReceipt.setOnCheckedChangeListener { _, isChecked ->
			binding.textFieldEmail.isGone = !isChecked
			requireActivity().hideKeyboard()
			updateStateButtons()
		}

		updateStateButtons()

		binding.buttonPayCard.setOnClickListener {
			updateEmail()
			updateSaveCard()

			val listener = requireActivity() as? IPaymentOptionsFragment
			listener?.runCardPayment()
			dismiss()
		}

		binding.buttonGooglepay.root.setOnClickListener {
			val listener = requireActivity() as? IPaymentOptionsFragment
			listener?.runGooglePay()
			dismiss()
		}

		binding.buttonTinkoffPay.setOnClickListener {
			updateEmail()
			updateSaveCard()

			viewModel.getTinkoffQrPayLink(sdkConfig?.saveCard)
		}

		binding.buttonSbp.setOnClickListener {
			updateEmail()
			updateSaveCard()

			viewModel.getSBPQrPayLink(sdkConfig?.saveCard)

//			val listener = requireActivity() as? IPaymentOptionsFragment
//			listener?.onSbpGetQrLinkSuccess()
//			dismiss()
		}

		binding.buttonSaveCardPopup.setOnClickListener {
			showPopupSaveCardInfo()
		}

		binding.buttonCardBeSavedPopup.setOnClickListener {
			showPopupSaveCardInfo()
		}
	}

	private fun updateEmail() {
		if (paymentConfiguration!!.requireEmail || binding.checkboxSendReceipt.isChecked) {
			paymentConfiguration?.paymentData?.email = binding.editEmail.text.toString()
		} else {
			paymentConfiguration?.paymentData?.email = ""
		}
	}

	private fun updateSaveCard() {
		if (binding.checkboxSaveCard.visibility == View.VISIBLE) {
			sdkConfig?.saveCard = binding.checkboxSaveCard.isChecked
		}
	}

	private fun setSaveCardCheckBoxVisible() {
		binding.checkboxSaveCard.visibility = View.VISIBLE
		binding.buttonSaveCardPopup.visibility = View.VISIBLE
		binding.checkboxSaveCard.checkedState = MaterialCheckBox.STATE_UNCHECKED
	}

	private fun setSaveCardHintVisible() {
		binding.textCardBeSaved.visibility = View.VISIBLE
		binding.buttonCardBeSavedPopup.visibility = View.VISIBLE
	}

	private fun showPopupSaveCardInfo() {
		val popupView = layoutInflater.inflate(R.layout.popup_cpsdk_save_card_info, null)

		val wid = LinearLayout.LayoutParams.WRAP_CONTENT
		val high = LinearLayout.LayoutParams.WRAP_CONTENT
		val focus= true
		val popupWindow = PopupWindow(popupView, wid, high, focus)

		val background = activity?.let { ContextCompat.getDrawable(it, R.drawable.cpsdk_bg_popup) }
		popupView.background = background

		popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
	}

	private fun updateStateButtons() {
		if (paymentConfiguration!!.requireEmail) {
			if (isValid()) {
				enableAllButtons()
			} else {
				disableAllButtons()
			}
		} else {
			if (binding.checkboxSendReceipt.checkedState == MaterialCheckBox.STATE_CHECKED) {
				if (isValid()) {
					enableAllButtons()
				} else {
					disableAllButtons()
				}
			} else {
				enableAllButtons()
			}
		}
	}
	private fun isValid(): Boolean {

		val valid = if (paymentConfiguration!!.requireEmail) {
			emailIsValid(binding.editEmail.text.toString())
		} else {
			!binding.checkboxSendReceipt.isChecked || emailIsValid(binding.editEmail.text.toString())
		}

		return valid
	}

	private fun disableAllButtons() {
		binding.viewBlockButtons.visibility = View.VISIBLE
	}

	private fun enableAllButtons() {
		binding.viewBlockButtons.visibility = View.GONE
	}

	private fun setTinkoffPayLoading(isLoading: Boolean) {
		if (isLoading) {
			binding.buttonTinkoffPayLogo.visibility = View.GONE
			binding.buttonTinkoffPayProgress.visibility = View.VISIBLE
		} else {
			binding.buttonTinkoffPayLogo.visibility = View.VISIBLE
			binding.buttonTinkoffPayProgress.visibility = View.GONE
		}

	}

	private fun setSbpLoading(isLoading: Boolean) {
		if (isLoading) {
			binding.buttonSbpLogo.visibility = View.GONE
			binding.buttonSbpProgress.visibility = View.VISIBLE
		} else {
			binding.buttonSbpLogo.visibility = View.VISIBLE
			binding.buttonSbpProgress.visibility = View.GONE
		}
	}
}