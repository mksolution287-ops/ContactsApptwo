package com.contactsapptwomktech.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val m = seconds / 60
                val s = seconds % 60
                if (s > 0) "${m}m ${s}s" else "${m}m"
            }
            else -> {
                val h = seconds / 3600
                val m = (seconds % 3600) / 60
                if (m > 0) "${h}h ${m}m" else "${h}h"
            }
        }
    }

    fun formatCallDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, callDate) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(now, callDate) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isSameYear(now, callDate) -> {
                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun getDateLabel(timestamp: Long, todayLabel: String, yesterdayLabel: String): String {
        val now = Calendar.getInstance()
        val callDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when {
            isSameDay(now, callDate) -> todayLabel
            isYesterday(now, callDate) -> yesterdayLabel
            isSameYear(now, callDate) ->
                SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
            else ->
                SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, other: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, other)
    }

    private fun isSameYear(c1: Calendar, c2: Calendar): Boolean =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)

    fun groupByDate(
        timestamps: List<Long>,
        todayLabel: String,
        yesterdayLabel: String
    ): Map<String, List<Int>> {
        return timestamps
            .mapIndexed { idx, ts -> idx to getDateLabel(ts, todayLabel, yesterdayLabel) }
            .groupBy({ it.second }, { it.first })
    }
}
