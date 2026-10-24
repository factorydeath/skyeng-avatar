package ru.skypro.avatar.student.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlin.math.cos
import kotlin.math.sin

private class FloatingShape(
    val path: Path,
    val color: Color,
    val x: Float,
    val y: Float,
    val diameter: Float,
    val phase: Float,
    val turns: Float,
)

@Composable
fun FloatingShapes(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shapes = remember(colors) {
        listOf(
            FloatingShape(
                path = RoundedPolygon.star(numVerticesPerRadius = 6, innerRadius = 0.8f, rounding = CornerRounding(0.25f)).toComposePath(),
                color = colors.primaryContainer.copy(alpha = 0.45f),
                x = 0.18f, y = 0.16f, diameter = 0.42f, phase = 0f, turns = 1f,
            ),
            FloatingShape(
                path = RoundedPolygon(numVertices = 4, rounding = CornerRounding(1f)).toComposePath(),
                color = colors.tertiaryContainer.copy(alpha = 0.4f),
                x = 0.82f, y = 0.42f, diameter = 0.34f, phase = 0.33f, turns = -1f,
            ),
            FloatingShape(
                path = RoundedPolygon.circle().toComposePath(),
                color = colors.secondaryContainer.copy(alpha = 0.35f),
                x = 0.25f, y = 0.86f, diameter = 0.46f, phase = 0.66f, turns = 1f,
            ),
        )
    }
    val transition = rememberInfiniteTransition(label = "floatingShapes")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floatingShapesTime",
    )
    Canvas(modifier) {
        shapes.forEach { drawFloatingShape(it, t) }
    }
}

private fun RoundedPolygon.toComposePath(): Path = toPath().asComposePath()

private fun DrawScope.drawFloatingShape(shape: FloatingShape, t: Float) {
    val wave = (t + shape.phase) * 2f * Math.PI.toFloat()
    val dx = sin(wave) * size.width * 0.04f
    val dy = cos(wave) * size.height * 0.03f
    val radius = size.width * shape.diameter / 2f
    val angle = t * 360f * shape.turns
    withTransform({
        translate(size.width * shape.x + dx, size.height * shape.y + dy)
        rotate(angle, Offset.Zero)
        scale(radius, radius, Offset.Zero)
    }) {
        drawPath(shape.path, shape.color)
    }
}
