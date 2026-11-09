package com.zonify.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zonify.core.model.AudioQualityBadge
import com.zonify.core.ui.theme.AudiophileGold
import com.zonify.core.ui.theme.BitPerfectCyan

/**
 * Apple Music style Hi-Res Lossless & Bit-Perfect Badge.
 */
@Composable
fun AudiophileBadge(
    qualityBadge: AudioQualityBadge,
    isBitPerfect: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isBitPerfect) BitPerfectCyan else AudiophileGold.copy(alpha = 0.6f)
    val textColor = if (isBitPerfect) BitPerfectCyan else Color.White

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isBitPerfect) Icons.Default.Usb else Icons.Default.GraphicEq,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isBitPerfect) "Bit-Perfect • ${qualityBadge.label}" else qualityBadge.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                letterSpacing = 0.5.sp
            )
        }
    }
}
