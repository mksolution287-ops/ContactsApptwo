package com.callerinfocom.data.model

import android.net.Uri

data class CallLogEntry(
    val id: Long,
    val number: String,
    val contactName: String?,
    val callType: CallType,
    val date: Long,
    val duration: Long,
    val photoUri: Uri
) {
    val displayName: String
        get() = contactName?.takeIf { it.isNotBlank() } ?: number

    val hasContactName: Boolean
        get() = !contactName.isNullOrBlank()
}

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, VOICEMAIL, UNKNOWN;

    companion object {
        fun fromInt(value: Int): CallType = when (value) {
            1 -> INCOMING
            2 -> OUTGOING
            3 -> MISSED
            5 -> REJECTED
            4 -> VOICEMAIL
            else -> UNKNOWN
        }
    }
}
