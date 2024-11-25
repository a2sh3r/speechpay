package com.example.speechpay

import CommandProcessor
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.speechpay.ui.theme.SpeechPayTheme

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionListener: RecognitionListener
    private var isVoiceCommandListening by mutableStateOf(false)
    private var recognizedText by mutableStateOf("")
    private var messageText by mutableStateOf("")

    private lateinit var smsReceiver: BroadcastReceiver

    private val commandProcessor = CommandProcessor()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                ), PERMISSION_REQUEST_CODE
            )
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
                            messageText += body
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
                @OptIn(ExperimentalMaterial3Api::class) Scaffold(modifier = Modifier.fillMaxSize(),
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
                    })
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
                isVoiceCommandListening = false
                Toast.makeText(
                    this@MainActivity, "Ошибка распознавания: $errorMessage", Toast.LENGTH_SHORT
                ).show()
                Log.e("SpeechPay", "Speech recognition error: $errorMessage")
            }

            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onEndOfSpeech() {
                isVoiceCommandListening = false
                Log.d("SpeechPay", "End of speech")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (partialMatches != null && partialMatches.isNotEmpty()) {
                    recognizedText = partialMatches.joinToString(" ")
                }
            }
        }
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    // Function to check if all required permissions are granted
    private fun hasAllPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
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
            Toast.makeText(
                this, "Неизвестная ошибка при остановке прослушивания", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun processCommand(command: String?) {
        val commandResult = commandProcessor.processCommand(command)
        if (commandResult != null) {
            commandResult.phoneNumber?.let { phoneNumber ->
                commandResult.cleanAmount?.let { cleanAmount ->
                    sendSms(phoneNumber, cleanAmount)
                }
            }

            commandResult.cleanCode?.let { cleanCode ->
                if (cleanCode.isNotEmpty()) {
                    sendCodeToSms(cleanCode)
                } else {
                    Toast.makeText(this, "Код не распознан", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("SpeechPay", "Не удалось распознать команду.")
            Toast.makeText(this, "Не удалось распознать команду", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendSms(phoneNumber: String, cleanAmount: String) {
        val cleanedText = phoneNumber.replace(Regex("[^0-9]"), "")
        val smsManager = SmsManager.getDefault()
        val message = "Перевод $cleanedText $cleanAmount"
        smsManager.sendTextMessage("900", null, message, null, null)
        Toast.makeText(this, "SMS отправлено", Toast.LENGTH_SHORT).show()
        Log.d("SpeechPay", "SMS отправлено: $message")
    }

    private fun sendCodeToSms(cleanCode: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage("900", null, cleanCode, null, null)
        Toast.makeText(this, "SMS с кодом $cleanCode отправлено", Toast.LENGTH_SHORT).show()
        Log.d("SpeechPay", "SMS с кодом отправлено: $cleanCode")
        messageText = ""
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
