package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

/**
 * A beautiful transparent glass card with subtle gradient border and surface coloration.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.03f),
                            borderColor.copy(alpha = 0.05f)
                        )
                    )
                ),
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * Animated Ambient Background to give a gorgeous fluid liquid glow.
 */
@Composable
fun AnimatedAmbientGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A13))
    ) {
        // Cyan Glow Sphere
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = 50.dp)
                .blur(90.dp)
        ) {
            val radius = size.minDimension / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(radius, radius),
                    radius = radius
                ),
                radius = radius,
                center = Offset(radius, radius)
            )
        }

        // Purple Glow Sphere
        Canvas(
            modifier = Modifier
                .size(350.dp)
                .offset(x = 200.dp, y = 400.dp)
                .blur(110.dp)
        ) {
            val radius = size.minDimension / 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(radius, radius),
                    radius = radius
                ),
                radius = radius,
                center = Offset(radius, radius)
            )
        }
    }
}

/**
 * Glassmorphic Ripple Button for sleek interactions.
 */
@Composable
fun GlassIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        icon()
    }
}
