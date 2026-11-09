package com.zonify.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zonify.core.ui.theme.AppleMusicDarkBackground

/**
 * Recreates the Apple Music animated blurred backdrop.
 */
@Composable
fun AppleMusicBlurBackground(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "PrimaryGradientAnim"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SecondaryGradientAnim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusicDarkBackground)
    ) {
        // Multi-stop radial and linear gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = 0.55f),
                            animatedSecondary.copy(alpha = 0.35f),
                            AppleMusicDarkBackground.copy(alpha = 0.85f),
                            AppleMusicDarkBackground
                        )
                    )
                )
        )

        content()
    }
}
