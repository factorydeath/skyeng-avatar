package ru.skypro.avatar.student.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import ru.noties.jlatexmath.JLatexMathDrawable
import ru.skypro.avatar.student.core.logging.AppLog
import ru.skypro.avatar.student.domain.trainer.ConditionPart

@Composable
fun FormulaRichText(parts: List<ConditionPart>, textStyle: TextStyle = MaterialTheme.typography.bodyMedium) {
    val color = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val textSizePx = with(density) { textStyle.fontSize.toPx() }
    key(parts, color, textSizePx) {
        FormulaContent(parts, color.toArgb(), textSizePx, textStyle)
    }
}

private val formulaRenderer = Mutex()

@Composable
private fun FormulaContent(parts: List<ConditionPart>, color: Int, textSizePx: Float, textStyle: TextStyle) {
    val density = LocalDensity.current
    val formulas by produceState<Map<String, Bitmap?>>(emptyMap(), parts, color, textSizePx) {
        value = withContext(Dispatchers.Default) {
            formulaRenderer.withLock {
                parts.filterIsInstance<ConditionPart.Formula>().map { it.latex }.distinct()
                    .associateWith {
                        currentCoroutineContext().ensureActive()
                        renderFormulaBitmap(it, color, textSizePx)
                    }
            }
        }
    }
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            when (part) {
                is ConditionPart.Text -> append(part.value)
                is ConditionPart.Image -> Unit
                is ConditionPart.Formula -> {
                    val bitmap = formulas[part.latex]
                    if (bitmap == null) {
                        append(part.latex)
                    } else {
                        val id = "formula_$index"
                        val width = with(density) { bitmap.width.toFloat().toSp() }
                        val height = with(density) { bitmap.height.toFloat().toSp() }
                        inlineContent[id] = InlineTextContent(
                            Placeholder(width, height, PlaceholderVerticalAlign.TextCenter),
                        ) {
                            Image(bitmap.asImageBitmap(), contentDescription = null)
                        }
                        appendInlineContent(id, part.latex)
                    }
                }
            }
        }
    }
    Text(text, inlineContent = inlineContent, style = textStyle)
}

private fun renderFormulaBitmap(latex: String, color: Int, textSizePx: Float): Bitmap? {
    if (latex.length > MediaLimits.FORMULA_CHARACTERS || !textSizePx.isFinite() || textSizePx <= 0) return null
    return try {
        val drawable = JLatexMathDrawable.builder(latex)
            .textSize(textSizePx)
            .color(color)
            .build()
        if (!MediaLimits.fits(drawable.intrinsicWidth, drawable.intrinsicHeight, MediaLimits.FORMULA_PIXELS)) return null
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        AppLog.w(AppLog.TRAINER, "formula render failed", e)
        null
    } catch (_: StackOverflowError) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}
