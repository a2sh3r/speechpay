data class CommandResult(val phoneNumber: String?, val cleanAmount: String?, val cleanCode: String?)

class CommandProcessor {

    fun processCommand(command: String?): CommandResult? {
        val lowerCaseCommand = command?.lowercase() ?: return null

        // Обновленное регулярное выражение
        val regex = "(переведи|перевести) (на номер|по номеру) (8\\s?\\d{3}[- ]?\\d{3}[- ]?\\d{2}[- ]?\\d{2}) (сумму|сумма|суммы|сумо|суммо|суму|сума)?\\s*(\\d+(\\.\\d{1,3})?\\s*(тыс|млн|миллион|миллиона|миллионов)?)?|код\\s?(\\d+(?:\\s?\\d+)*)?".toRegex()
        val matchResult = regex.find(lowerCaseCommand)

        return if (matchResult != null) {
            val phoneNumberText = matchResult.groupValues[3].takeIf { it.isNotEmpty() }
            val amount = matchResult.groupValues[5].takeIf { it.isNotEmpty() }
            val code = matchResult.groupValues[8].takeIf { it.isNotEmpty() }

            val cleanAmount = amount?.let { cleanAmountString(it) }
            val cleanCode = code?.replace(" ", "")

            CommandResult(phoneNumberText, cleanAmount, cleanCode)
        } else {
            null
        }
    }

    private fun cleanAmountString(amount: String): String {
        var totalAmount = 0L // Используем Long для целых чисел
        val parts = amount.split(" ")

        for (part in parts) {
            when {
                part.contains("млн") || part.contains("миллионов") -> {
                    totalAmount += part.replace("млн", "").replace("миллионов", "").trim().toLongOrNull()?.times(1_000_000) ?: 0L
                }
                part.contains("тыс") || part.contains("тысяч") -> {
                    totalAmount += part.replace("тыс", "").replace("тысяч", "").trim().toLongOrNull()?.times(1_000) ?: 0L
                }
                // Обработка целых чисел с точками
                part.matches(Regex("\\d+(\\.\\d+)?")) -> {
                    // Удаляем точки и преобразуем в Long
                    totalAmount += part.replace(".", "").toLongOrNull() ?: 0L
                }
                else -> {
                    // Обработка целых чисел
                    totalAmount += part.toLongOrNull() ?: 0L
                }
            }
        }

        return totalAmount.toString() // Возвращаем строку без лишних нулей
    }

    fun convertPhoneTextToNumber(phoneText: String): String {
        return phoneText.replace(Regex("[^0-9]"), "")
    }
}