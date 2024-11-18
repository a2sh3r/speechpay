package com.example.speechpay

import android.Manifest
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

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognitionListener: RecognitionListener
    private var isVoiceCommandListening by mutableStateOf(false)
    private var recognizedText by mutableStateOf("")  // Для хранения распознанного текста

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка разрешений
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

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
                            recognizedText = recognizedText  // Отправляем текст в MainScreen для отображения
                        )
                    }
                )
            }
        }

        // Инициализация распознавания речи
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionListener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    recognizedText = matches.joinToString(" ") // Обновляем текст с распознанными фразами
                    processCommand(recognizedText)  // Обрабатываем команду
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
                    // Обновляем распознанный текст в реальном времени
                    recognizedText = partialMatches.joinToString(" ") // Объединяем слова
                }
            }
        }
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")  // Устанавливаем русский язык
        speechRecognizer.startListening(intent)
        isVoiceCommandListening = true
        Log.d("SpeechPay", "Start Listening")
    }

    private fun stopVoiceRecognition() {
        try {
            if (isVoiceCommandListening) {
                speechRecognizer.stopListening()  // Пытаемся остановить прослушивание
                isVoiceCommandListening = false   // Обновляем состояние на "не слушаем"
                Log.d("SpeechPay", "Stop Listening")
            } else {
                Log.d("SpeechPay", "Stop Listening: Not active")
                Toast.makeText(this, "Распознавание речи не активно", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalStateException) {
            // Это исключение может быть выброшено, если SpeechRecognizer не в правильном состоянии для остановки
            Log.e("SpeechPay", "Ошибка при остановке прослушивания: ${e.message}")
            Toast.makeText(this, "Ошибка при остановке прослушивания", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Общий catch для других исключений
            Log.e("SpeechPay", "Неизвестная ошибка при остановке: ${e.message}")
            Toast.makeText(this, "Неизвестная ошибка при остановке прослушивания", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processCommand(command: String?) {
        if (command != null) {
            Log.d("SpeechPay", "Распознанная команда: $command") // Логируем распознанную команду

            // Приводим команду к нижнему регистру для более гибкого сравнения
            val lowerCaseCommand = command.lowercase()

            // Регулярное выражение для распознавания команды
            val regex = "(переведи|перевести) (на номер|по номеру) (8\\s?\\d{3}\\s?\\d{3}[-]?\\d{2}[-]?\\d{2}) (сумму|сумма) (\\d+)".toRegex()

            // Поиск совпадений с командой
            val matchResult = regex.find(lowerCaseCommand)

            if (matchResult != null) {
                val phoneNumberText = matchResult.groupValues[3].trim() // Извлекаем номер
                val amount = matchResult.groupValues[5].trim()         // Извлекаем сумму

                Log.d("SpeechPay", "Найден номер: $phoneNumberText, сумма: $amount") // Логируем номер и сумму

                val phoneNumber = convertPhoneTextToNumber(phoneNumberText)  // Преобразуем номер в нужный формат
                if (phoneNumber != null) {
                    sendSms(phoneNumber, amount)  // Отправляем SMS
                } else {
                    Log.d("SpeechPay", "Не удалось распознать номер телефона")
                    Toast.makeText(this, "Не удалось распознать номер телефона", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("SpeechPay", "Команда не распознана")
                Toast.makeText(this, "Команда не распознана", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertPhoneTextToNumber(phoneText: String): String? {
        // Убираем все нецифровые символы (пробелы, дефисы и т.д.)
        val cleanedText = phoneText.replace(Regex("[^0-9]"), "")

        // Проверяем, что после очистки текст состоит из 11 цифр (для России номер телефона начинается с 8)
        return if (cleanedText.length == 11 && cleanedText.startsWith("8")) {
            // Формируем номер телефона в формате +7XXXXXXXXXX
            "${cleanedText.substring(1)}"
        } else {
            null  // Если номер невалидный
        }
    }

    private fun sendSms(phoneNumber: String, amount: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage("900", null, "Перевод $phoneNumber $amount", null, null)
            Toast.makeText(this, "SMS отправлено на номер 900 с переводом на номер $phoneNumber сумммы $amount", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при отправке SMS", Toast.LENGTH_SHORT).show()
            Log.e("SpeechPay", "Error sending SMS: ${e.message}")
        }
    }

    // Запрашиваем разрешение для отправки SMS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("SpeechPay", "Permission granted")
        } else {
            Log.d("SpeechPay", "Permission denied")
            Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
        }
    }
}


@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    isVoiceCommandListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    recognizedText: String
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Голосовой платеж",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isVoiceCommandListening) onStopListening() else onStartListening()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isVoiceCommandListening) "Остановить прослушивание" else "Начать прослушивание")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Индикатор загрузки
        if (isVoiceCommandListening) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Распознанный текст: $recognizedText",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SpeechPayTheme {
        MainScreen(
            isVoiceCommandListening = false,
            onStartListening = {},
            onStopListening = {},
            recognizedText = ""
        )
    }
}
