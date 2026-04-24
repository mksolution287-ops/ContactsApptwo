package com.callerinfo.utils

import androidx.compose.ui.graphics.Color

object AvatarColorUtils {
    private val palette = listOf(
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF26A69A), // Teal
        Color(0xFFEF5350), // Red
        Color(0xFFAB47BC), // Purple
        Color(0xFF26C6DA), // Cyan
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF66BB6A), // Green
        Color(0xFFFFCA28), // Amber
        Color(0xFF42A5F5), // Blue
        Color(0xFFEC407A), // Pink
        Color(0xFF8D6E63), // Brown
        Color(0xFF78909C), // Blue Grey
    )

    fun getColor(name: String): Color {
        val index = name.firstOrNull()?.lowercaseChar()?.code?.let {
            it % palette.size
        } ?: 0
        return palette[index]
    }
}
