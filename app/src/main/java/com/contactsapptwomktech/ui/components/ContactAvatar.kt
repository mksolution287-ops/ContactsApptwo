package com.contactsapptwomktech.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.contactsapptwomktech.R
import com.contactsapptwomktech.utils.AvatarColorUtils

@Composable
fun ContactAvatar(
    name: String,
    photoUri: Uri?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = 18.sp
) {
    val initials = buildInitials(name)
    val bgColor = AvatarColorUtils.getColor(name)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.cd_contact_photo),
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun buildInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    return when {
        parts.size >= 2 -> "${parts.first().firstOrNull()?.uppercaseChar() ?: ""}${parts.last().firstOrNull()?.uppercaseChar() ?: ""}"
        parts.size == 1 && parts[0].isNotBlank() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}
