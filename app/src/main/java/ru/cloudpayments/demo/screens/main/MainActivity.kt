package ru.cloudpayments.demo.screens.main

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import ru.cloudpayments.demo.R
import ru.cloudpayments.demo.base.BaseActivity
import ru.cloudpayments.demo.databinding.ActivityMainBinding
import ru.cloudpayments.demo.support.CardIOScanner
import ru.cloudpayments.sdk.api.models.PaymentDataPayer
import ru.cloudpayments.sdk.configuration.CloudpaymentsSDK
import ru.cloudpayments.sdk.configuration.PaymentConfiguration
import ru.cloudpayments.sdk.configuration.PaymentData

class MainActivity : BaseActivity() {

	private val cpSdkLauncher = CloudpaymentsSDK.getInstance().launcher(this, result = {

		if (it.status != null) {

			val builder: AlertDialog.Builder = AlertDialog.Builder(this)
			builder.setPositiveButton("OK") { dialog, which ->

			}

			if (it.status == CloudpaymentsSDK.TransactionStatus.Succeeded) {
				builder
					.setTitle("Success")
					.setMessage("Transaction ID: ${it.transactionId}")
			} else {
				builder.setTitle("Fail")
				if (it.reasonCode != 0) {
					builder.setMessage("Transaction ID: ${it.transactionId}, reason code: ${it.reasonCode}")
				} else {
					builder.setMessage("Transaction ID: ${it.transactionId}")
				}
			}

			val dialog: AlertDialog = builder.create()
			dialog.show()
		}
	})

	override val layoutId = R.layout.activity_main

	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		val view = binding.root
		setContentView(view)

		binding.buttonRunTop.setOnClickListener {
			runCpSdk()
		}

		binding.buttonRun.setOnClickListener {
			runCpSdk()
		}

		binding.buttonRunTpayTop.setOnClickListener {
			runCpSdkTinkoffPayMode()
		}

		binding.buttonRunTpay.setOnClickListener {
			runCpSdkTinkoffPayMode()
		}
	}

	private fun runCpSdk() {

		val apiUrl = binding.editApiUrl.text.toString()
		val publicId = binding.editPublicId.text.toString()
		val amount = binding.editAmount.text.toString()
		val currency = binding.editCurrency.text.toString()
		val invoiceId = binding.editInvoiceId.text.toString()
		val description = binding.editDescription.text.toString()
		val accountId = binding.editAccountId.text.toString()
		val email = binding.editEmail.text.toString()

		val payerFirstName = binding.editPayerFirstName.text.toString()
		val payerLastName = binding.editPayerLastName.text.toString()
		val payerMiddleName = binding.editPayerMiddleName.text.toString()
		val payerBirthDay = binding.editPayerBirth.text.toString()
		val payerAddress = binding.editPayerAddress.text.toString()
		val payerStreet = binding.editPayerStreet.text.toString()
		val payerCity = binding.editPayerCity.text.toString()
		val payerCountry = binding.editPayerCountry.text.toString()
		val payerPhone = binding.editPayerPhone.text.toString()
		val payerPostcode = binding.editPayerPostcode.text.toString()

		val jsonData = binding.editJsonData.text.toString()

		val tinkoffPaySuccessRedirectUrl = binding.editTinkoffPaySuccessRedirectUrl.text.toString()
		val tinkoffPayFailRedirectUrl = binding.editTinkoffPayFailRedirectUrl.text.toString()

		val isDualMessagePayment = binding.checkboxDualMessagePayment.isChecked

		var payer = PaymentDataPayer()
		payer.firstName = payerFirstName
		payer.lastName = payerLastName
		payer.middleName = payerMiddleName
		payer.birthDay = payerBirthDay
		payer.address = payerAddress
		payer.street = payerStreet
		payer.city = payerCity
		payer.country = payerCountry
		payer.phone = payerPhone
		payer.postcode = payerPostcode

		val paymentData = PaymentData(
			amount = amount,
			currency = currency,
			invoiceId = invoiceId,
			description = description,
			accountId = accountId,
			email = email,
			payer = payer,
			jsonData = jsonData
		)

		val configuration = PaymentConfiguration(
			publicId = publicId,
			paymentData = paymentData,
			scanner = CardIOScanner(),
			requireEmail = true,
			useDualMessagePayment = isDualMessagePayment,
			disableGPay = false,
			tinkoffPaySuccessRedirectUrl = tinkoffPaySuccessRedirectUrl,
			tinkoffPayFailRedirectUrl = tinkoffPayFailRedirectUrl,
			apiUrl = apiUrl,
			testMode = true
		)
		cpSdkLauncher.launch(configuration)
	}

	private fun runCpSdkTinkoffPayMode() {

		val apiUrl = binding.editApiUrl.text.toString()
		val publicId = binding.editPublicId.text.toString()
		val amount = binding.editAmount.text.toString()
		val currency = binding.editCurrency.text.toString()
		val invoiceId = binding.editInvoiceId.text.toString()
		val description = binding.editDescription.text.toString()
		val accountId = binding.editAccountId.text.toString()
		val email = binding.editEmail.text.toString()

		val payerFirstName = binding.editPayerFirstName.text.toString()
		val payerLastName = binding.editPayerLastName.text.toString()
		val payerMiddleName = binding.editPayerMiddleName.text.toString()
		val payerBirthDay = binding.editPayerBirth.text.toString()
		val payerAddress = binding.editPayerAddress.text.toString()
		val payerStreet = binding.editPayerStreet.text.toString()
		val payerCity = binding.editPayerCity.text.toString()
		val payerCountry = binding.editPayerCountry.text.toString()
		val payerPhone = binding.editPayerPhone.text.toString()
		val payerPostcode = binding.editPayerPostcode.text.toString()

		val jsonData = binding.editJsonData.text.toString()

		val tinkoffPaySuccessRedirectUrl = binding.editTinkoffPaySuccessRedirectUrl.text.toString()
		val tinkoffPayFailRedirectUrl = binding.editTinkoffPayFailRedirectUrl.text.toString()

		val isDualMessagePayment = binding.checkboxDualMessagePayment.isChecked

		var payer = PaymentDataPayer()
		payer.firstName = payerFirstName
		payer.lastName = payerLastName
		payer.middleName = payerMiddleName
		payer.birthDay = payerBirthDay
		payer.address = payerAddress
		payer.street = payerStreet
		payer.city = payerCity
		payer.country = payerCountry
		payer.phone = payerPhone
		payer.postcode = payerPostcode

		val paymentData = PaymentData(
			amount = amount,
			currency = currency,
			invoiceId = invoiceId,
			description = description,
			accountId = accountId,
			email = email,
			payer = payer,
			jsonData = jsonData
		)

		val configuration = PaymentConfiguration(
			publicId = publicId,
			paymentData = paymentData,
			useDualMessagePayment = isDualMessagePayment,
			tinkoffPaySuccessRedirectUrl = tinkoffPaySuccessRedirectUrl,
			tinkoffPayFailRedirectUrl = tinkoffPayFailRedirectUrl,
			apiUrl = apiUrl,
			mode = CloudpaymentsSDK.SDKRunMode.TinkoffPay,
			testMode = true
		)
		cpSdkLauncher.launch(configuration)
	}
}