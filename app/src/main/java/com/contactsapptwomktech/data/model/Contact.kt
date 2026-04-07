package com.contactsapptwomktech.data.model

import android.net.Uri

data class Contact(
    val id: Long,
    val name: String,
    val firstName: String,
    val lastName: String,
    val phoneNumbers: List<PhoneNumber>,
    val emails: List<EmailAddress>,
    val photoUri: Uri?,
    val isFavorite: Boolean = false,
    val company: String? = null,
    val notes: String? = null
) {
    val initials: String
        get() {
            val first = firstName.firstOrNull()?.uppercaseChar()
            val last = lastName.firstOrNull()?.uppercaseChar()
            return when {
                first != null && last != null -> "$first$last"
                first != null -> "$first"
                name.isNotBlank() -> name.take(2).uppercase()
                else -> "?"
            }
        }

    val primaryPhone: String?
        get() = phoneNumbers.firstOrNull()?.number

    val displayName: String
        get() = name.ifBlank { phoneNumbers.firstOrNull()?.number ?: "Unknown" }
}

data class PhoneNumber(
    val number: String,
    val type: PhoneType = PhoneType.MOBILE,
    val label: String? = null
)

data class EmailAddress(
    val address: String,
    val type: EmailType = EmailType.OTHER,
    val label: String? = null
)

enum class PhoneType { MOBILE, HOME, WORK, OTHER }
enum class EmailType { HOME, WORK, OTHER }
