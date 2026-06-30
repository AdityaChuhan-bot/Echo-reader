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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

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
    Box(
        modifier = modifier
            .testTag(testTag)
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = borderColor.copy(alpha = 0.25f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.38f),
                        backgroundColor.copy(alpha = 0.12f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = shape
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.04f),
                            borderColor.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.02f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                ),
                shape = shape
            )
    ) {
        // Specular diagonal gloss layer
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height * 0.45f)
                close()
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.35f, size.height * 0.35f)
                )
            )
        }

        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * Animated Ambient Background to give a gorgeous fluid liquid glow.
 */
@Composable
fun AnimatedAmbientGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glow")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing)
        ),
        label = "time"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090F)) // Deep space navy base
    ) {
        val width = maxWidth
        val height = maxHeight

        // 1. Vibrant Liquid Magenta Blob (Top-Left floating to center)
        val magentaX = width * 0.2f + (sin(time) * (width * 0.15f).value).dp
        val magentaY = height * 0.25f + (cos(time * 0.8f) * (height * 0.12f).value).dp
        
        // 2. Bright Liquid Cyan Blob (Bottom-Right floating up/left)
        val cyanX = width * 0.75f + (cos(time * 1.1f) * (width * 0.18f).value).dp
        val cyanY = height * 0.65f + (sin(time * 0.9f) * (height * 0.15f).value).dp

        // 3. Electric Violet Blob (Center/Middle drifting right/down)
        val violetX = width * 0.45f + (sin(time * 0.7f + 1f) * (width * 0.12f).value).dp
        val violetY = height * 0.45f + (cos(time * 1.2f + 1.5f) * (height * 0.14f).value).dp

        // 4. Amber/Gold Liquid Blob (Top-Right accent)
        val goldX = width * 0.8f + (cos(time * 0.6f + 2f) * (width * 0.1f).value).dp
        val goldY = height * 0.15f + (sin(time * 1.3f + 0.5f) * (height * 0.08f).value).dp

        // Render Blobs with distinct layers and radial blurring
        // Magenta Liquid Orb
        Canvas(
            modifier = Modifier
                .size(450.dp)
                .offset(x = magentaX - 225.dp, y = magentaY - 225.dp)
                .blur(90.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEC4899).copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }

        // Cyan Liquid Orb
        Canvas(
            modifier = Modifier
                .size(500.dp)
                .offset(x = cyanX - 250.dp, y = cyanY - 250.dp)
                .blur(100.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }

        // Violet Liquid Orb
        Canvas(
            modifier = Modifier
                .size(480.dp)
                .offset(x = violetX - 240.dp, y = violetY - 240.dp)
                .blur(95.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }

        // Amber/Gold Accent Orb
        Canvas(
            modifier = Modifier
                .size(350.dp)
                .offset(x = goldX - 175.dp, y = goldY - 175.dp)
                .blur(80.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.width / 2
                )
            )
        }

        // Tech Mesh: Beautiful 100% responsive Dot Grid Overlay
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val dotSpacing = 32.dp.toPx()
            val dotRadius = 1.2f.dp.toPx()
            val cols = (size.width / dotSpacing).toInt() + 1
            val rows = (size.height / dotSpacing).toInt() + 1

            for (c in 0 until cols) {
                for (r in 0 until rows) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.035f),
                        radius = dotRadius,
                        center = Offset(c * dotSpacing + (dotSpacing / 2), r * dotSpacing + (dotSpacing / 2))
                    )
                }
            }
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

