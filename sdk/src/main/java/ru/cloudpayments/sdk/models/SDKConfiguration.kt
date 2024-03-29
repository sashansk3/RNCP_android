package ru.cloudpayments.sdk.models

data class SDKConfiguration(
	var publicKey: PublicKey = PublicKey(pem = null, version = null),
	var availablePaymentMethods: AvailablePaymentMethods = AvailablePaymentMethods(),
	var terminalConfiguration: TerminalConfiguration = TerminalConfiguration(),
	var saveCard: Boolean? = null
	)

data class PublicKey(
	var pem: String? = null,
	var version: Int? = null
)

data class AvailablePaymentMethods(
	var googlePayAvailable: Boolean = false,
	var sbpAvailable: Boolean = false,
	var tinkoffPayAvailable: Boolean = false
)

data class TerminalConfiguration(
	var isSaveCard: Int? = null
)