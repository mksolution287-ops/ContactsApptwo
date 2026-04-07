package com.contactsapptwomktech.data.repository

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import com.contactsapptwomktech.data.model.CallLogEntry
import com.contactsapptwomktech.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    suspend fun fetchCallLogs(limit: Int = 200): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_PHOTO_URI
        )

        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )
        Log.d("CallLogs", "Cursor count: ${cursor?.count}")
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numberIdx = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameIdx = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationIdx = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val photoIdx = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)

            var count = 0
            while (it.moveToNext() && count < limit) {
                entries.add(
                    CallLogEntry(
                        id = it.getLong(idIdx),
                        number = it.getString(numberIdx) ?: "Unknown",
                        contactName = it.getString(nameIdx),
                        callType = CallType.fromInt(it.getInt(typeIdx)),
                        date = it.getLong(dateIdx),
                        duration = it.getLong(durationIdx),
                        photoUri = it.getString(photoIdx)
                    )
                )
                count++
            }
        }


        entries
    }
}
