package ru.example.prichastie.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.example.prichastie.ui.theme.palette

val Sans: FontFamily = FontFamily.SansSerif
val Serif: FontFamily = FontFamily.Serif

/** Полоска прогресса (.pbar). */
@Composable
fun PBar(
    progress: Float,
    modifier: Modifier = Modifier,
    ok: Boolean = false,
    height: Dp = 6.dp,
    color: Color? = null
) {
    val p = palette
    val fill = color ?: if (ok) p.ok else p.gold
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(p.line)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(fill)
        )
    }
}

/** Кольцо с процентами на главной и в итогах. */
@Composable
fun Ring(percent: Int, diameter: Dp, color: Color) {
    val p = palette
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 7.dp.toPx()
            val inset = 7.dp.toPx()                     // как в макете: r = размер / 2 - 7
            val d = this.size.minDimension - inset * 2
            val topLeft = Offset(inset, inset)
            drawArc(
                color = p.line, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = Size(d, d), style = Stroke(stroke)
            )
            if (percent > 0) {
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * percent / 100f,
                    useCenter = false, topLeft = topLeft, size = Size(d, d),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            "$percent%", color = p.ink, fontFamily = Sans,
            fontWeight = FontWeight.Bold, fontSize = (diameter.value / 4.2f).sp
        )
    }
}

/** Карточка (.card). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = palette
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(p.card, shape)
            .border(1.dp, borderColor ?: p.line, shape)
            .padding(14.dp),
        content = content
    )
}

/** Кнопка (.btn / .btn.out). */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true
) {
    val p = palette
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .then(if (outlined) Modifier.border(1.dp, p.acc, shape) else Modifier.background(p.acc))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = if (outlined) p.acc else p.onAcc, fontFamily = Sans,
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center
        )
    }
}

/** Ссылка-текст (.lnk). */
@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text, color = palette.acc, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

/** Мелкий поясняющий текст (.note). */
@Composable
fun Note(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text, modifier = modifier, color = palette.mut, fontFamily = Sans,
        fontSize = 12.5.sp, lineHeight = 18.sp, textAlign = align
    )
}

@Composable
fun HLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
}

/** Круглая кнопка в шапке (.ib). */
@Composable
fun CircleButton(glyph: String, onClick: () -> Unit, fontSize: TextUnit = 18.sp) {
    val p = palette
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, p.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(glyph, color = p.ink, fontSize = fontSize, fontFamily = Sans) }
}

/** Шапка экрана (.bar). */
@Composable
fun AppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val p = palette
    Column(Modifier.fillMaxWidth().background(p.card)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBack != null) CircleButton("‹", onBack)
            Column(Modifier.weight(1f)) {
                Text(
                    title, color = p.ink, fontFamily = Serif, fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, lineHeight = 19.sp
                )
                if (subtitle != null) {
                    Text(subtitle, color = p.mut, fontFamily = Sans, fontSize = 12.sp)
                }
            }
            actions()
        }
        HLine()
    }
}

/** Метка этапа (.ph). */
@Composable
fun PhaseTag(text: String, after: Boolean = false) {
    val p = palette
    val c = if (after) p.ok else p.gold
    val shape = RoundedCornerShape(8.dp)
    Text(
        text.uppercase(), color = c, fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, letterSpacing = 0.6.sp,
        modifier = Modifier
            .background(p.hl, shape)
            .border(1.dp, c, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Заголовок этапа со линией (.phh). */
@Composable
fun PhaseHeader(text: String, trailing: (@Composable () -> Unit)? = null) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text.uppercase(), color = p.acc, fontFamily = Sans, fontWeight = FontWeight.Bold,
            fontSize = 12.sp, letterSpacing = 0.6.sp
        )
        Box(Modifier.weight(1f).height(1.dp).background(p.line))
        if (trailing != null) trailing()
    }
}

/** Заголовок группы (.gh). */
@Composable
fun GroupHeader(title: String, right: String) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title, color = p.ink, fontFamily = Serif, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(right, color = p.mut, fontFamily = Sans, fontSize = 12.sp)
    }
}

/** Номер шага в кружке (.nm, .nm.p, .nm.d). */
@Composable
fun NumBadge(text: String, done: Boolean, started: Boolean = false) {
    val p = palette
    val edge = when {
        done -> p.ok
        started -> p.gold
        else -> p.line
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (done) p.ok else Color.Transparent)
            .border(1.dp, edge, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (done) "✓" else text, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = when {
                done -> Color.White
                started -> p.gold
                else -> p.ink
            }
        )
    }
}

/** Строка шага (.row). */
@Composable
fun StepRow(
    number: Int,
    title: String,
    sub: String,
    done: Boolean,
    locked: Boolean,
    trailing: String,
    onClick: () -> Unit,
    fraction: Float = 0f,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val p = palette
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = if (compact) 3.dp else 6.dp)
            .alpha(if (locked) 0.45f else 1f)
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.line, shape)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumBadge(number.toString(), done, started = !done && fraction > 0f)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title, color = p.ink, fontFamily = Serif, fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 13.sp else 14.sp, lineHeight = 17.5.sp
            )
            if (!compact && sub.isNotBlank()) {
                Text(sub, color = p.mut, fontFamily = Sans, fontSize = 11.sp)
            }
            PBar(
                if (done) 1f else fraction, ok = done, height = 4.dp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
        if (trailing.isNotBlank()) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, color = p.mut, fontFamily = Sans, fontSize = 11.sp, textAlign = TextAlign.End)
        }
    }
}

/**
 * Переключатель из двух-трёх вариантов (.tg).
 * dimmed: номера невыбранных вариантов, которые «заперты» (.lk, прозрачность 0.6).
 */
@Composable
fun SegToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Set<Int> = emptySet()
) {
    val p = palette
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .clip(shape)
            .background(p.card)
            .border(1.dp, p.line, shape)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .alpha(if (!on && i in dimmed) 0.6f else 1f)
                    .background(if (on) p.acc else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, color = if (on) p.onAcc else p.mut, fontFamily = Sans,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1
                )
            }
        }
    }
}

enum class Tab(val glyph: String, val label: String) {
    HOME("✝", "Главная"),
    LIST("☰", "Шаги"),
    SUM("✦", "Итоги"),
    SET("⚙", "Настройки")
}

/** Нижняя панель (.nav). */
@Composable
fun NavBar(current: Tab, onSelect: (Tab) -> Unit) {
    val p = palette
    Column(Modifier.fillMaxWidth().background(p.card)) {
        HLine()
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Tab.values().forEach { t ->
                val on = t == current
                Column(
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(t) }
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t.glyph, fontSize = 18.sp, fontFamily = Sans, color = if (on) p.acc else p.mut)
                    Text(
                        t.label, fontSize = 12.sp, fontFamily = Sans,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (on) p.acc else p.mut
                    )
                }
            }
        }
    }
}

/** Вопрос перед сбросом прогресса. */
data class Confirm(
    val title: String,
    val body: String,
    val action: String,
    val onYes: () -> Unit
)

@Composable
fun ConfirmDialog(c: Confirm, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(c.title, fontFamily = Serif) },
        text = { Text(c.body, fontFamily = Sans, fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = { c.onYes(); onDismiss() }) { Text(c.action) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}