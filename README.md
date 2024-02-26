[![](https://jitpack.io/v/ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android.svg)](https://jitpack.io/#ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android)

## CloudPayments SDK for Android 

CloudPayments SDK позволяет интегрировать прием платежей в мобильные приложение для платформы Android.

### Требования
Для работы CloudPayments SDK необходим Android версии 6.0 или выше (API level 23)

### Подключение
В build.gradle уровня проекта добавить репозиторий Jitpack

```
repositories {
	maven { url 'https://jitpack.io' }
}
```
В build.gradle уровня приложения добавить зависимость указав последнюю доступную версию SDK:

[![](https://jitpack.io/v/ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android.svg)](https://jitpack.io/#ru.cloudpayments.gitpub.integrations.sdk/cloudpayments-android)

```
implementation 'implementation 'ru.cloudpayments.gitpub.integrations.sdk:cloudpayments-android:latest-release'
```

### Структура проекта:

* **app** - Пример реализации приложения с использованием SDK
* **sdk** - Исходный код SDK


### Возможности CloudPayments SDK:

Вы можете использовать SDK одним из трех способов: 

* использовать стандартную платежную форму Cloudpayments
* реализовать свою платежную форму с использованием функций CloudpaymentsApi без вашего сервера
* реализовать свою платежную форму, сформировать криптограмму и отправить ее в CloudPayments через свой сервер

### Использование стандартной платежной формы Cloudpayments:

1.	Создайте CpSdkLauncher для получения результата через Activity Result API (рекомендуется использовать, но если хотите получить результат в onActivityResult этот шаг можно пропустить)

```
val cpSdkLauncher = CloudpaymentsSDK.getInstance().launcher(this, result = {
		if (it.status != null) {
			if (it.status == CloudpaymentsSDK.TransactionStatus.Succeeded) {
				Toast.makeText(this, "Успешно! Транзакция №${it.transactionId}", Toast.LENGTH_SHORT).show()
				CartManager.getInstance()?.clear()
				finish()
			} else {
				if (it.reasonCode != 0) {
					Toast.makeText(this, "Ошибка! Транзакция №${it.transactionId}. Код ошибки ${it.reasonCode}", Toast.LENGTH_SHORT).show()
				} else {
					Toast.makeText(this, "Ошибка! Транзакция №${it.transactionId}.", Toast.LENGTH_SHORT).show()
				}
			}
		}
	})
```

2. Создайте объект PaymentData, передайте в него сумму платежа, валюту и другие данные, если необходимо передать подробную информацию о плательщике создайте объект PaymentDataPayer с информацией о плательщике и также пердайте этот объект в PaymentData.

```
var payer = PaymentDataPayer() // Доп. поле, куда передается информация о плательщике:
payer.firstName = payerFirstName // Имя
payer.lastName = payerLastName // Фамилия
payer.middleName = payerMiddleName // Отчество
payer.birthDay = payerBirthDay // День рождения
payer.address = payerAddress // Адрес
payer.street = payerStreet // Улица
payer.city = payerCity // Город
payer.country = payerCountry // Страна
payer.phone = payerPhone // Телефон
payer.postcode = payerPostcode // Почтовый индекс

val paymentData = PaymentData(
	amount = amount, // Cумма платежа в валюте
	currency = currency, // Валюта
	invoiceId = invoiceId, // Номер счета или заказа
	description = description, // Описание оплаты в свободной форме
	accountId = accountId, // Идентификатор пользователя
	email = email, // E-mail плательщика, на который будет отправлена квитанция об оплате
	payer = payer, // Информация о плательщике
	jsonData = jsonData // Любые другие данные, которые будут связаны с транзакцией {name: Ivan}
)
```

3. Создайте объект PaymentConfiguration, передайте в него Public Id из [личного кабинета CloudPayments](https://merchant.cloudpayments.ru/), объект PaymentData, а так же укажите другие параметры.

```
val configuration = PaymentConfiguration(
	publicId = publicId, // Ваш PublicID в полученный в ЛК CloudPayments
	paymentData = paymentData, // Информация о платеже
	scanner = CardIOScanner(), // Сканер банковских карт
	requireEmail = false, // Обязателный email для проведения оплаты (по умолчанию false)
	useDualMessagePayment = true, // Использовать двухстадийную схему проведения платежа, по умолчанию используется одностадийная схема
	disableGPay = true, // Выключить Google Pay, по умолчанию Google Pay включен
	tinkoffPaySuccessRedirectUrl = tinkoffPaySuccessRedirectUrl, // deeplink для перехода из приложения Tinkoff в случае успешной оплаты
	tinkoffPayFailRedirectUrl = tinkoffPayFailRedirectUrl, // deeplink для перехода из приложения Tinkoff в случае ошибки
)
```

4. Вызовите форму оплаты. 

```
cpSdkLauncher.launch(configuration) // Если используете Activity Result API

// или

CloudpaymentsSDK.getInstance().start(configuration, this, REQUEST_CODE_PAYMENT) // Если хотите получть результат в onActivityResult 
```

5. Получите результат в onActivityResult (если не используете Activity Result API)

```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) = when (requestCode) {
		REQUEST_CODE_PAYMENT -> {
			val transactionId = data?.getIntExtra(CloudpaymentsSDK.IntentKeys.TransactionId.name, 0) ?: 0
			val transactionStatus = data?.getSerializableExtra(CloudpaymentsSDK.IntentKeys.TransactionStatus.name) as? CloudpaymentsSDK.TransactionStatus


			if (transactionStatus != null) {
				if (transactionStatus == CloudpaymentsSDK.TransactionStatus.Succeeded) {
					Toast.makeText(this, "Успешно! Транзакция №$transactionId", Toast.LENGTH_SHORT).show()
				} else {
					val reasonCode = data.getIntExtra(CloudpaymentsSDK.IntentKeys.TransactionReasonCode.name, 0) ?: 0
					if (reasonCode > 0) {
						Toast.makeText(this, "Ошибка! Транзакция №$transactionId. Код ошибки $reasonCode", Toast.LENGTH_SHORT).show()
					} else {
						Toast.makeText(this, "Ошибка! Транзакция №$transactionId.", Toast.LENGTH_SHORT).show()
					}
				}
			}
		}
		else -> super.onActivityResult(requestCode, resultCode, data)
```

### Использование TinkoffPay:

1. Включить TinkoffPay в [личном кабинете Cloudpayments](https://merchant.cloudpayments.ru/).

### Использование SDK в режиме отдельной кнопки для TinkoffPay:

Вы можете использовать SDK в режиме TinkoffPay, в этом режиме SDK пропускает экран выбора способов оплаты и сразу запускает оплату через TinkoffPay (если такой способ подключен и доступен). Для этого в конфигурации укажите: mode = CloudpaymentsSDK.SDKRunMode.TinkoffPay.

```
val configuration = PaymentConfiguration(
	publicId = publicId, // Ваш PublicID в полученный в ЛК CloudPayments
	paymentData = paymentData, // Информация о платеже
	scanner = CardIOScanner(), // Сканер банковских карт
	requireEmail = false, // Обязателный email для проведения оплаты (по умолчанию false)
	useDualMessagePayment = true, // Использовать двухстадийную схему проведения платежа, по умолчанию используется одностадийная схема
	disableGPay = true, // Выключить Google Pay, по умолчанию Google Pay включен
	tinkoffPaySuccessRedirectUrl = tinkoffPaySuccessRedirectUrl, // deeplink для перехода из приложения Tinkoff в случае успешной оплаты
	tinkoffPayFailRedirectUrl = tinkoffPayFailRedirectUrl, // deeplink для перехода из приложения Tinkoff в случае ошибки
	mode = CloudpaymentsSDK.SDKRunMode.TinkoffPay // Режим запуска TinkoffPay
	)
```

### Использование СБП: 

1. Включить СБП через вашего курирующего менеджера.
2. В файл AndroidManifest.xml вашего приложения добавьте:


```
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
```

### Использование вашей платежной формы с использованием функций CloudpaymentsApi:

1. Создайте криптограмму карточных данных

```
// Обязательно проверяйте входящие данные карты (номер, срок действия и cvc код) на корректность, иначе функция создания криптограммы вернет null.
val cardCryptogram = Card.cardCryptogram(cardNumber, cardDate, cardCVC, Constants.MERCHANT_PUBLIC_ID)
```

2. Выполните запрос на проведения платежа. Создайте объект CloudpaymentApi и вызовите функцию charge для одностадийного платежа или auth для двухстадийного. Укажите email, на который будет выслана квитанция об оплате.

```
val api = CloudpaymentsSDK.createApi(Constants.merchantPublicId)
val body = PaymentRequestBody(amount = "10.00", currency = "RUB", ipAddress = "85.54.125.55", name = cardHolderName, cryptogram = cardCryptogramPacket)
api.charge(body)
	.toObservable()
	.flatMap(CloudpaymentsTransactionResponse::handleError)
	.map { it.transaction }
```

3. Если необходимо, покажите 3DS форму для подтверждения платежа

```
val acsUrl = transaction.acsUrl
val paReq = transaction.paReq
val md = transaction.transactionId
ThreeDsDialogFragment
	.newInstance(acsUrl, paReq, md)
	.show(supportFragmentManager, "3DS")
```

4. Для получения формы 3DS и получения результатов прохождения 3DS аутентификации реализуйте протокол ThreeDSDialogListener. Передайте в запрос также threeDsCallbackId, полученный в ответ на auth или charge

```
override fun onAuthorizationCompleted(md: String, paRes: String) {
	api.postThreeDs(transactionId, threeDsCallbackId, paRes)
}

override fun onAuthorizationFailed(error: String?) {
	Log.d("Error", "AuthorizationFailed: $error")
}
```

#### Подключение Google Pay  через CloudPayments

[О Google Pay](https://cloudpayments.ru/wiki/integration/products/googlepay)

[Документация](https://developers.google.com/payments/setup)

#### Включение Google Pay 

В файл build.gradle подключите следующую зависимость:

```
implementation 'com.google.android.gms:play-services-wallet:18.1.2'
```

В файл AndroidManifest.xml вашего приложения добавьте мета информацию:

```
<meta-data
	android:name="com.google.android.gms.wallet.api.enabled"
	android:value="true" />
```
#### Проведение платежа через Google Pay с помощью формы Cloudpayments

Никаких дополнительных шагов не требуется. Форма автоматически определяет, подключен Google Pay или нет. В зависимости от этого покажется форма выбора способа оплаты (Google Pay или карта) или форма ввода карточных данных

#### Проведение платежа через Google Pay в своей форме

Сконфигурируйте параметры:

```
PaymentMethodTokenizationParameters params =
		PaymentMethodTokenizationParameters.newBuilder()
				.setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
				.addParameter("gateway", "cloudpayments")
				.addParameter("gatewayMerchantId", "Ваш Public ID")
				.build();
```

Укажите тип оплаты через шлюз (Wallet-Constants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY) и добавьте два параметра:

1) gateway: cloudpayments

2) gatewayMerchantId: Ваш Public ID, его можно посмотреть в [личном кабинете](https://merchant.cloudpayments.ru/).

С этими параметрами запросите токен Google Pay:

```
String tokenGP = paymentData.getPaymentMethodToken().getToken();
```

Используя токен Google Pay в качестве криптограммы карточных данных, совершите платёж методами API, указанными ранее.

**В случае проведения платежа с токеном Google Pay в качестве имени держателя карты неоходимо указать: "Google Pay"**

### Другие функции

* Проверка карточного номера на корректность

```
Card.isValidNumber(cardNumber)

```

* Проверка срока действия карты

```
Card.isValidExpDate(expDate) // expDate в формате MM/yy

```

* Определение типа платежной системы

```
let cardType: CardType = Card.cardType(from: cardNumberString)
```

* Определение банка эмитента

```
val api = CloudpaymentsSDK.createApi(Constants.merchantPublicId)
api.getBinInfo(firstSixDigits)
	.subscribeOn(Schedulers.io())
	.observeOn(AndroidSchedulers.mainThread())
	.subscribe({ info -> Log.d("Bank name", info.bankName.orEmpty()) }, this::handleError)
```

* Шифрование карточных данных и создание криптограммы для отправки на сервер

```
val cardCryptogram = Card.cardCryptogram(cardNumber, cardDate, cardCVC, Constants.MERCHANT_PUBLIC_ID)
```

* Шифрование cvv при оплате сохраненной картой и создание криптограммы для отправки на сервер

```
val cvvCryptogramPacket = Card.cardCryptogramForCVV(cvv)
```

* Отображение 3DS формы и получении результата 3DS аутентификации

```
val acsUrl = transaction.acsUrl
val paReq = transaction.paReq
val md = transaction.transactionId
ThreeDsDialogFragment
	.newInstance(acsUrl, paReq, md)
	.show(supportFragmentManager, "3DS")

interface ThreeDSDialogListener {
	fun onAuthorizationCompleted(md: String, paRes: String)
	fun onAuthorizationFailed(error: String?)
}
```

* Сканер карт
Вы можете подключить любой сканер карт, который вызывается с помощью Activity. Для этого нужно реализовать протокол CardScanner и передать объект, реализующий протокол, при создании PaymentConfiguration. Если протокол не будет реализован, то кнопка сканирования не будет показана

Пример со сканером CardIO

```
@Parcelize
class CardIOScanner: CardScanner() {
	override fun getScannerIntent(context: Context) =
		Intent(context, CardIOActivity::class.java).apply {
			putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true)
		}

	override fun getCardDataFromIntent(data: Intent) =
		if (data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
			val scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT) as? CreditCard
			val month = (scanResult?.expiryMonth ?: 0).toString().padStart(2, '0')
			val yearString = scanResult?.expiryYear?.toString() ?: "00"
			val year = if (yearString.length > 2) {
				yearString.substring(yearString.lastIndex - 1)
			} else {
				yearString.padStart(2, '0')
			}
			val cardData = CardData(scanResult?.cardNumber, month, year, scanResult?.cardholderName)
			cardData
		} else {
			null
		}
}
```

### История обновлений:

#### 1.4.0
* Добавлен новый способ оплаты: оплата через СБП (см. документацию для получения более подробной информации: https://gitpub.cloudpayments.ru/integrations/sdk/cloudpayments-android/-/blob/master/README.md)

* Оптимизировано получение параметров шлюза и проверка доступности способов оплаты: теперь экран способов оплаты появляется сразу со всеми подключенными и доступными способами оплаты

* Внесено значительное количество небольших исправлений и улучшений

#### 1.4.1
* Оптимизированны запросы

* Обновлены библиотеки

* ВНИМАНИЕ: Обновлен Yandex pay теперь для тестирования Yandex pay необоходимо в конфигурации SDK указывать testMode = true

#### 1.5.0
* Повышена надежность

#### 1.5.1
* Добавлен режим запуска SDK TinkoffPay

* Добавлена возможность педедать deeplink для перехода из приложения Tinkoff после оплаты

* Отключен YandexPay


### Поддержка

По возникающим вопросам техничечкого характера обращайтесь на support@cp.ru
