package ru.cloudpayments.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import ru.cloudpayments.sdk.R
import ru.cloudpayments.sdk.api.CloudpaymentsApi
import ru.cloudpayments.sdk.api.models.SBPBanksItem
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.dagger2.CloudpaymentsComponent
import ru.cloudpayments.sdk.dagger2.CloudpaymentsModule
import ru.cloudpayments.sdk.dagger2.CloudpaymentsNetModule
import ru.cloudpayments.sdk.dagger2.DaggerCloudpaymentsComponent
import ru.cloudpayments.sdk.databinding.ActivityCpsdkPaymentBinding
import ru.cloudpayments.sdk.models.ApiError
import ru.cloudpayments.sdk.models.SDKConfiguration
import ru.cloudpayments.sdk.ui.dialogs.PaymentCardFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishFragmentFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentFinishStatus
import ru.cloudpayments.sdk.ui.dialogs.PaymentOptionsFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentProcessFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentSBPFragment
import ru.cloudpayments.sdk.ui.dialogs.PaymentTinkoffPayFragment
import ru.cloudpayments.sdk.ui.dialogs.base.BasePaymentBottomSheetFragment
import ru.cloudpayments.sdk.util.GooglePayHandler
import ru.cloudpayments.sdk.util.nextFragment
import javax.inject.Inject

internal class PaymentActivity : FragmentActivity(),
								 BasePaymentBottomSheetFragment.IPaymentFragment,
								 PaymentOptionsFragment.IPaymentOptionsFragment,
								 PaymentCardFragment.IPaymentCardFragment,
								 PaymentProcessFragment.IPaymentProcessFragment,
								 PaymentSBPFragment.IPaymentSBPFragment,
								 PaymentTinkoffPayFragment.IPaymentTinkoffPayFragment {

	val SDKConfiguration: SDKConfiguration = SDKConfiguration()

	private var disposable: Disposable? = null

	@Inject
	lateinit var api: CloudpaymentsApi


	companion object {
		private const val REQUEST_CODE_GOOGLE_PAY = 1
		private const val EXTRA_CONFIGURATION = "EXTRA_CONFIGURATION"

		fun getStartIntent(context: Context, configuration: PaymentConfiguration): Intent {
			val intent = Intent(context, PaymentActivity::class.java)
			intent.putExtra(EXTRA_CONFIGURATION, configuration)
			return intent
		}
	}

	override fun finish() {
		super.finish()
		overridePendingTransition(R.anim.cpsdk_fade_in, R.anim.cpsdk_fade_out)
	}

	internal val component: CloudpaymentsComponent by lazy {
		DaggerCloudpaymentsComponent
			.builder()
			.cloudpaymentsModule(CloudpaymentsModule())
			.cloudpaymentsNetModule(
				CloudpaymentsNetModule(
					paymentConfiguration!!.publicId,
					paymentConfiguration!!.apiUrl
				)
			)
			.build()
	}

	val paymentConfiguration by lazy {
		intent.getParcelableExtra<PaymentConfiguration>(EXTRA_CONFIGURATION)
	}

	private lateinit var binding: ActivityCpsdkPaymentBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityCpsdkPaymentBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		component.inject(this)

		checkCurrency()

		getPublicKey()
	}

	private fun getPublicKey() {
		disposable = api.getPublicKey()
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->
				SDKConfiguration.publicKey.pem = response.pem
				SDKConfiguration.publicKey.version = response.version

				paymentConfiguration?.let { getMerchantConfiguration(it.publicId) }
			}
			.onErrorReturn {
				onInternetConnectionError()
			}
			.subscribe()
	}

	private fun getMerchantConfiguration(publicId: String) {
		disposable = api.getMerchantConfiguration(publicId)
			.toObservable()
			.observeOn(AndroidSchedulers.mainThread())
			.map { response ->

				if (response.success == true) {

					response.model?.terminalFullUrl.toString().let {
						if (paymentConfiguration?.tinkoffPaySuccessRedirectUrl.isNullOrEmpty()) {
							paymentConfiguration?.tinkoffPaySuccessRedirectUrl = it
						}
						if (paymentConfiguration?.tinkoffPayFailRedirectUrl.isNullOrEmpty()) {
							paymentConfiguration?.tinkoffPayFailRedirectUrl = it
						}
					}

					var isSbpAvailable = false
					var isTinkoffPayAvailable = false

					for (paymentMethod in response.model?.externalPaymentMethods!!) {
						if (paymentMethod.type == 5) {
							isSbpAvailable = paymentConfiguration?.disableSbpPay ? false : paymentMethod.enabled!!
						}
						if (paymentMethod.type == 6) {
							isTinkoffPayAvailable = paymentMethod.enabled!!
						}
					}

					SDKConfiguration.availablePaymentMethods.sbpAvailable = isSbpAvailable
					SDKConfiguration.availablePaymentMethods.tinkoffPayAvailable =
						isTinkoffPayAvailable
					SDKConfiguration.terminalConfiguration.isSaveCard =
						response.model?.features?.isSaveCard

					when (paymentConfiguration?.mode) {
						CloudpaymentsSDK.SDKRunMode.TinkoffPay -> {
							if (isTinkoffPayAvailable) {
								runTinkoffPay("", 0)
							} else {
								Toast.makeText(this, "TinkoffPay not available", Toast.LENGTH_SHORT).show()
								finish()
							}
						}

						else -> {
							prepareGooglePay()
						}
					}
				} else {
					Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
					finish()
				}
			}
			.onErrorReturn {
				onInternetConnectionError()
			}
			.subscribe()
	}

	private fun prepareGooglePay() {

		if (paymentConfiguration!!.disableGPay) {
			SDKConfiguration.availablePaymentMethods.googlePayAvailable = false
			showUi()
			return
		}

		if (supportFragmentManager.backStackEntryCount == 0) {
			GooglePayHandler.isReadyToMakeGooglePay(this)
				.toObservable()
				.observeOn(AndroidSchedulers.mainThread())
				.map {
					SDKConfiguration.availablePaymentMethods.googlePayAvailable = it
					showUi()
				}
				.onErrorReturn {
					SDKConfiguration.availablePaymentMethods.googlePayAvailable = false
					showUi()
				}
				.subscribe()
		}
	}

	private fun showUi() {
		binding.layoutProgress.isVisible = false
		val fragment = PaymentOptionsFragment.newInstance()
		fragment.show(supportFragmentManager, "")
	}

	override fun onBackPressed() {
		val fragment = supportFragmentManager.findFragmentById(R.id.frame_content)
		if (fragment is BasePaymentBottomSheetFragment<*, *>) {
			fragment.handleBackButton()
		} else {
			super.onBackPressed()
		}
	}

	override fun runCardPayment() {
		val fragment = PaymentCardFragment.newInstance()
		fragment.show(supportFragmentManager, "")
	}

	override fun runTinkoffPay(qrUrl: String, transactionId: Long) {
		val fragment = PaymentTinkoffPayFragment.newInstance(qrUrl, transactionId)
		fragment.show(supportFragmentManager, "")
	}

	override fun runSbp(qrUrl: String, transactionId: Long, listOfBanks: ArrayList<SBPBanksItem>) {
		val fragment = PaymentSBPFragment.newInstance(qrUrl, transactionId, listOfBanks)
		fragment.show(supportFragmentManager, "")
	}

	override fun runGooglePay() {
		GooglePayHandler.present(paymentConfiguration!!, this, REQUEST_CODE_GOOGLE_PAY)
	}

	override fun onSBPFinish(success: Boolean) {
		if (success) {
			val fragment = PaymentFinishFragmentFragment.newInstance(PaymentFinishStatus.Succeeded)
			fragment.show(supportFragmentManager, "")
		} else {
			val fragment = PaymentFinishFragmentFragment.newInstance(PaymentFinishStatus.Failed)
			fragment.show(supportFragmentManager, "")
		}
	}

	override fun onPayClicked(cryptogram: String) {
		val fragment = PaymentProcessFragment.newInstance(cryptogram)
		fragment.show(supportFragmentManager, "")
	}

	override fun onPaymentFinished(transactionId: Long) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})
	}

	override fun onPaymentFailed(transactionId: Long, reasonCode: Int?) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Failed
			)
			reasonCode?.let { putExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, it) }
		})
	}

	override fun onPaymentTinkoffPayFinished(transactionId: Long) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Succeeded
			)
		})
	}

	override fun onPaymentTinkoffPayFailed(transactionId: Long, reasonCode: Int?) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, transactionId)
			putExtra(
				CloudpaymentsSDK.IntentKeys.TransactionStatus.name,
				CloudpaymentsSDK.TransactionStatus.Failed
			)
			reasonCode?.let { putExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, it) }
		})
	}

	fun onInternetConnectionError() {
		val fragment = PaymentFinishFragmentFragment.newInstance(
			PaymentFinishStatus.Failed,
			ApiError.CODE_ERROR_CONNECTION,
			false
		)
		fragment.show(supportFragmentManager, "")
	}

	override fun finishPayment() {
		finish()
	}

	override fun retryPayment() {
		setResult(Activity.RESULT_CANCELED, Intent())
		showUi()
	}

	override fun paymentWillFinish() {
		finish()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

		when (requestCode) {

			REQUEST_CODE_GOOGLE_PAY -> {
				when (resultCode) {
					Activity.RESULT_OK -> {
						handleGooglePaySuccess(data)
					}

					Activity.RESULT_CANCELED, AutoResolveHelper.RESULT_ERROR -> {
						handleGooglePayFailure(data)
					}

					else -> super.onActivityResult(requestCode, resultCode, data)
				}
			}

			else -> super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun handleGooglePaySuccess(intent: Intent?) {
		if (intent != null) {
			val paymentData = PaymentData.getFromIntent(intent)
			val token = paymentData?.paymentMethodToken?.token

			if (token != null) {
				val runnable = {
					val fragment = PaymentProcessFragment.newInstance(token)
					nextFragment(fragment, true, R.id.frame_content)
				}
				Handler().postDelayed(runnable, 1000)
			}
		}
	}

	private fun handleGooglePayFailure(intent: Intent?) {
		finish()
	}

	private fun checkCurrency() {
		if (paymentConfiguration!!.paymentData.currency.isEmpty()) {
			paymentConfiguration!!.paymentData.currency = "RUB"
		}
	}
}