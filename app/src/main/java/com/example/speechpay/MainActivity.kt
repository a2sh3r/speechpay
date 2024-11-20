package com.example.speechpay

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.speechpay.ui.theme.SpeechPayTheme
import android.util.Log
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionListener: RecognitionListener
    private var isVoiceCommandListening by mutableStateOf(false)
    private var recognizedText by mutableStateOf("")
    private var messageText by mutableStateOf("")

    private lateinit var smsReceiver: BroadcastReceiver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }

        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val pdus = intent.extras?.get("pdus") as Array<Any>?
                pdus?.let {
                    for (pdu in it) {
                        val message = SmsMessage.createFromPdu(pdu as ByteArray)
                        val sender = message.displayOriginatingAddress
                        val body = message.displayMessageBody

                        if (sender != null && sender == "900") {
                            messageText = body
                            Log.d("SpeechPay", "Получено сообщение от 900: $body")
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        intentFilter.priority = 999
        registerReceiver(smsReceiver, intentFilter)

        setContent {
            SpeechPayTheme {
                @OptIn(ExperimentalMaterial3Api::class)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("SpeechPay") },
                        )
                    },
                    content = { innerPadding ->
                        MainScreen(
                            modifier = Modifier.padding(innerPadding),
                            isVoiceCommandListening = isVoiceCommandListening,
                            onStartListening = { startVoiceRecognition() },
                            onStopListening = { stopVoiceRecognition() },
                            recognizedText = recognizedText,
                            messageText = messageText
                        )
                    }
                )
            }
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionListener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches.joinToString(" ")
                    processCommand(recognizedText)
                }
            }

            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудио"
                    SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно прав"
                    SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Тайм-аут сети"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Не найдено совпадений"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят"
                    SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Тайм-аут речи"
                    else -> "Неизвестная ошибка"
                }
                Toast.makeText(this@MainActivity, "Ошибка распознавания: $errorMessage", Toast.LENGTH_SHORT).show()
                Log.e("SpeechPay", "Speech recognition error: $errorMessage")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (partialMatches != null && partialMatches.isNotEmpty()) {
                    recognizedText = partialMatches.joinToString(" ")
                }
            }
        }
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        speechRecognizer.startListening(intent)
        isVoiceCommandListening = true
        Log.d("SpeechPay", "Start Listening")
    }

    private fun stopVoiceRecognition() {
        try {
            if (isVoiceCommandListening) {
                speechRecognizer.stopListening()
                isVoiceCommandListening = false
                Log.d("SpeechPay", "Stop Listening")
            } else {
                Log.d("SpeechPay", "Stop Listening: Not active")
                Toast.makeText(this, "Распознавание речи не активно", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalStateException) {
            Log.e("SpeechPay", "Ошибка при остановке прослушивания: ${e.message}")
            Toast.makeText(this, "Ошибка при остановке прослушивания", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SpeechPay", "Неизвестная ошибка при остановке: ${e.message}")
            Toast.makeText(this, "Неизвестная ошибка при остановке прослушивания", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processCommand(command: String?) {
        if (command != null) {
            Log.d("SpeechPay", "Распознанная команда: $command")
            val lowerCaseCommand = command.lowercase()

            val regex = "(переведи|перевести) (на номер|по номеру) (8\\s?\\d{3}\\s?\\d{3}[-]?\\d{2}[-]?\\d{2}) (сумму|сумма) (\\d+)".toRegex()

            val matchResult = regex.find(lowerCaseCommand)

            if (matchResult != null) {
                val phoneNumberText = matchResult.groupValues[3].trim()
                val amount = matchResult.groupValues[5].trim()

                Log.d("SpeechPay", "Найден номер: $phoneNumberText, сумма: $amount")

                val phoneNumber = convertPhoneTextToNumber(phoneNumberText)
                if (phoneNumber != null) {
                    sendSms(phoneNumber, amount)
                } else {
                    Toast.makeText(this, "Неверный номер телефона", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("SpeechPay", "Не удалось распознать команду.")
                Toast.makeText(this, "Не удалось распознать команду", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertPhoneTextToNumber(phoneText: String): String? {
        return phoneText.replace(Regex("[^\\d]"), "").takeIf { it.length == 11 }
    }

    private fun sendSms(phoneNumber: String, amount: String) {
        val cleanedText = phoneNumber.replace(Regex("[^0-9]"), "")
        val sms = "Перевод $cleanedText $amount"
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage("900", null, sms, null, null)

        Toast.makeText(this, "SMS отправлено на $phoneNumber", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}


@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    isVoiceCommandListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    recognizedText: String,
    messageText: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {
                if (isVoiceCommandListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            }
        ) {
            Text(if (isVoiceCommandListening) "Остановить" else "Начать")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Распознанный текст:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = recognizedText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Текст из SMS:", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
                .border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = messageText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
