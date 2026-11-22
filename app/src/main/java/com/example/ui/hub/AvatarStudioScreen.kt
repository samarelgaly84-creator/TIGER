package com.example.ui.hub

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.TigerStrings
import org.json.JSONObject

private val SKIN_TONES = listOf(
    Color(0xFFFAD7C0),
    Color(0xFFE8B490),
    Color(0xFFC68658),
    Color(0xFF965A38),
    Color(0xFF5C3824)
)

private val HAIR_COLORS = listOf(
    Color(0xFF18181B),
    Color(0xFF451A03),
    Color(0xFFD97706),
    Color(0xFFF97316),
    Color(0xFF38BDF8)
)

private val OUTFIT_COLORS = listOf(
    Color(0xFFF97316),
    Color(0xFF1E293B),
    Color(0xFF10B981),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6)
)

@Composable
fun AvatarStudioScreen(
    strings: TigerStrings,
    onBack: () -> Unit,
    onExportAvatar: (String, Bitmap) -> Unit
) {
    BackHandler(onBack = onBack)

    var skinIdx by remember { mutableIntStateOf(1) }
    var hairStyle by remember { mutableIntStateOf(0) } // 0=Spiky, 1=Curly, 2=Sleek, 3=Crown
    var hairColorIdx by remember { mutableIntStateOf(0) }
    var eyeStyle by remember { mutableIntStateOf(0) } // 0=Focused, 1=Happy, 2=Wink
    var browStyle by remember { mutableIntStateOf(0) } // 0=Sharp, 1=Arched, 2=Thick
    var mouthStyle by remember { mutableIntStateOf(0) } // 0=Smile, 1=Big Grin, 2=Cool
    var outfitIdx by remember { mutableIntStateOf(0) }
    var accessoryIdx by remember { mutableIntStateOf(1) } // 0=None, 1=Shades, 2=Headset
    var stickerIdx by remember { mutableIntStateOf(0) } // 0=🐯, 1=🔥, 2=⚡, 3=👑

    val hairNames = listOf("Spiky", "Curly", "Sleek", "Crown")
    val eyeNames = listOf("Focused", "Happy", "Wink")
    val browNames = listOf("Sharp", "Arched", "Thick")
    val mouthNames = listOf("Smile", "Grin", "Cool")
    val accNames = listOf("None", "Shades", "Headset")
    val stickers = listOf("🐯", "🔥", "⚡", "👑")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubAvatarStudio, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Live Interactive Bitmoji Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Brush.radialGradient(listOf(OUTFIT_COLORS[outfitIdx].copy(alpha = 0.25f), Color(0xFF0F172A)))),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(210.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h * 0.48f
                    val headR = w * 0.27f

                    // Outfit / Shoulders
                    drawRoundRect(
                        color = OUTFIT_COLORS[outfitIdx],
                        topLeft = Offset(cx - w * 0.34f, cy + headR * 0.85f),
                        size = Size(w * 0.68f, h * 0.32f),
                        cornerRadius = CornerRadius(48f, 48f)
                    )
                    // Neck
                    drawRect(
                        color = SKIN_TONES[skinIdx],
                        topLeft = Offset(cx - w * 0.08f, cy + headR * 0.6f),
                        size = Size(w * 0.16f, h * 0.14f)
                    )
                    // Ears
                    drawCircle(SKIN_TONES[skinIdx], radius = headR * 0.22f, center = Offset(cx - headR, cy))
                    drawCircle(SKIN_TONES[skinIdx], radius = headR * 0.22f, center = Offset(cx + headR, cy))
                    // Head
                    drawCircle(SKIN_TONES[skinIdx], radius = headR, center = Offset(cx, cy))

                    // Hairstyles
                    val hairColor = HAIR_COLORS[hairColorIdx]
                    when (hairStyle) {
                        0 -> { // Spiky
                            drawArc(hairColor, startAngle = 185f, sweepAngle = 170f, useCenter = true, topLeft = Offset(cx - headR * 1.04f, cy - headR * 1.12f), size = Size(headR * 2.08f, headR * 1.3f))
                            drawCircle(hairColor, radius = headR * 0.28f, center = Offset(cx - headR * 0.4f, cy - headR * 0.92f))
                            drawCircle(hairColor, radius = headR * 0.32f, center = Offset(cx, cy - headR * 1.02f))
                            drawCircle(hairColor, radius = headR * 0.26f, center = Offset(cx + headR * 0.42f, cy - headR * 0.9f))
                        }
                        1 -> { // Curly
                            for (i in -3..3) {
                                drawCircle(hairColor, radius = headR * 0.26f, center = Offset(cx + i * headR * 0.3f, cy - headR * 0.78f))
                            }
                        }
                        2 -> { // Sleek
                            drawRoundRect(hairColor, topLeft = Offset(cx - headR * 1.02f, cy - headR * 1.05f), size = Size(headR * 2.04f, headR * 0.72f), cornerRadius = CornerRadius(44f, 44f))
                        }
                        else -> { // Crown
                            drawArc(hairColor, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - headR, cy - headR * 1.05f), size = Size(headR * 2f, headR * 1.15f))
                            drawRoundRect(Color(0xFFFBBF24), topLeft = Offset(cx - headR * 0.55f, cy - headR * 1.38f), size = Size(headR * 1.1f, headR * 0.36f), cornerRadius = CornerRadius(12f, 12f))
                        }
                    }

                    // Eyebrows
                    val browStroke = if (browStyle == 2) 11f else 6f
                    val browY = cy - headR * 0.24f
                    drawLine(hairColor, Offset(cx - headR * 0.55f, browY), Offset(cx - headR * 0.16f, browY - if (browStyle == 1) 8f else 0f), strokeWidth = browStroke, cap = StrokeCap.Round)
                    drawLine(hairColor, Offset(cx + headR * 0.16f, browY - if (browStyle == 1) 8f else 0f), Offset(cx + headR * 0.55f, browY), strokeWidth = browStroke, cap = StrokeCap.Round)

                    // Eyes
                    val eyeY = cy - headR * 0.04f
                    drawCircle(Color(0xFF0F172A), radius = headR * 0.11f, center = Offset(cx - headR * 0.35f, eyeY))
                    if (eyeStyle == 2) {
                        drawLine(Color(0xFF0F172A), Offset(cx + headR * 0.2f, eyeY), Offset(cx + headR * 0.5f, eyeY), strokeWidth = 7f, cap = StrokeCap.Round)
                    } else {
                        drawCircle(Color(0xFF0F172A), radius = headR * 0.11f, center = Offset(cx + headR * 0.35f, eyeY))
                    }

                    // Mouth
                    val mouthY = cy + headR * 0.32f
                    when (mouthStyle) {
                        0 -> drawArc(Color(0xFF7C2D12), 15f, 150f, false, topLeft = Offset(cx - headR * 0.35f, mouthY - 12f), size = Size(headR * 0.7f, headR * 0.36f), style = Stroke(7f, cap = StrokeCap.Round))
                        1 -> drawArc(Color.White, 0f, 180f, true, topLeft = Offset(cx - headR * 0.36f, mouthY - 10f), size = Size(headR * 0.72f, headR * 0.4f))
                        else -> drawLine(Color(0xFF7C2D12), Offset(cx - headR * 0.28f, mouthY + 6f), Offset(cx + headR * 0.28f, mouthY + 2f), strokeWidth = 7f, cap = StrokeCap.Round)
                    }

                    // Accessories
                    if (accessoryIdx == 1) { // Shades
                        drawRoundRect(Color(0xFF0F172A), Offset(cx - headR * 0.72f, eyeY - headR * 0.18f), Size(headR * 0.62f, headR * 0.34f), CornerRadius(12f, 12f))
                        drawRoundRect(Color(0xFF0F172A), Offset(cx + headR * 0.1f, eyeY - headR * 0.18f), Size(headR * 0.62f, headR * 0.34f), CornerRadius(12f, 12f))
                        drawLine(Color(0xFFF97316), Offset(cx - headR * 0.1f, eyeY), Offset(cx + headR * 0.1f, eyeY), strokeWidth = 5f)
                    } else if (accessoryIdx == 2) { // Headset
                        drawArc(Color(0xFFF97316), 180f, 180f, false, topLeft = Offset(cx - headR * 1.15f, cy - headR * 1.15f), size = Size(headR * 2.3f, headR * 2.1f), style = Stroke(12f))
                        drawRoundRect(Color(0xFFF97316), Offset(cx - headR * 1.28f, cy - headR * 0.22f), Size(headR * 0.28f, headR * 0.5f), CornerRadius(10f, 10f))
                        drawRoundRect(Color(0xFFF97316), Offset(cx + headR * 1.0f, cy - headR * 0.22f), Size(headR * 0.28f, headR * 0.5f), CornerRadius(10f, 10f))
                    }
                }

                // Corner Sticker Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stickers[stickerIdx], style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // Swatch Selectors
        ColorPickerRow(strings.pick("لون البشرة", "Skin Tone"), SKIN_TONES, skinIdx) { skinIdx = it }
        ChipPickerRow(strings.pick("تسريحة الشعر", "Hairstyle"), hairNames, hairStyle) { hairStyle = it }
        ColorPickerRow(strings.pick("لون الشعر", "Hair Color"), HAIR_COLORS, hairColorIdx) { hairColorIdx = it }
        ChipPickerRow(strings.pick("العيون", "Eyes"), eyeNames, eyeStyle) { eyeStyle = it }
        ChipPickerRow(strings.pick("الحواجب", "Eyebrows"), browNames, browStyle) { browStyle = it }
        ChipPickerRow(strings.pick("الفم", "Mouth"), mouthNames, mouthStyle) { mouthStyle = it }
        ColorPickerRow(strings.pick("الزي", "Outfit"), OUTFIT_COLORS, outfitIdx) { outfitIdx = it }
        ChipPickerRow(strings.pick("الإكسسوارات", "Accessories"), accNames, accessoryIdx) { accessoryIdx = it }
        ChipPickerRow(strings.pick("الملصق", "Sticker"), stickers, stickerIdx) { stickerIdx = it }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    skinIdx = SKIN_TONES.indices.random()
                    hairStyle = hairNames.indices.random()
                    hairColorIdx = HAIR_COLORS.indices.random()
                    eyeStyle = eyeNames.indices.random()
                    browStyle = browNames.indices.random()
                    mouthStyle = mouthNames.indices.random()
                    outfitIdx = OUTFIT_COLORS.indices.random()
                    accessoryIdx = accNames.indices.random()
                    stickerIdx = stickers.indices.random()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(strings.pick("عشوائي", "Randomize"))
            }
            Button(
                onClick = {
                    val json = JSONObject().apply {
                        put("skin", skinIdx)
                        put("hairStyle", hairNames[hairStyle])
                        put("hairColor", hairColorIdx)
                        put("eyes", eyeNames[eyeStyle])
                        put("brows", browNames[browStyle])
                        put("mouth", mouthNames[mouthStyle])
                        put("outfit", outfitIdx)
                        put("accessory", accNames[accessoryIdx])
                        put("sticker", stickers[stickerIdx])
                    }.toString()
                    val bmp = renderAvatarPngBitmap(SKIN_TONES[skinIdx], HAIR_COLORS[hairColorIdx], OUTFIT_COLORS[outfitIdx])
                    onExportAvatar(json, bmp)
                },
                modifier = Modifier.weight(1f).testTag("export_avatar_png_btn")
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(strings.pick("تصدير PNG وحفظ", "Export PNG & Save"))
            }
        }
    }
}

@Composable
private fun ColorPickerRow(label: String, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(colors) { idx, c ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected == idx) 3.dp else 1.dp, if (selected == idx) Color.White else Color.Gray, CircleShape)
                        .clickable { onSelect(idx) }
                )
            }
        }
    }
}

@Composable
private fun ChipPickerRow(label: String, items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items) { idx, item ->
                FilterChip(selected = selected == idx, onClick = { onSelect(idx) }, label = { Text(item) })
            }
        }
    }
}

private fun renderAvatarPngBitmap(skin: Color, hair: Color, outfit: Color): Bitmap {
    val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.parseColor("#0F172A")
    canvas.drawRect(0f, 0f, 512f, 512f, paint)
    paint.color = outfit.toArgb()
    canvas.drawRoundRect(RectF(96f, 340f, 416f, 512f), 80f, 80f, paint)
    paint.color = skin.toArgb()
    canvas.drawCircle(256f, 230f, 120f, paint)
    paint.color = hair.toArgb()
    canvas.drawArc(RectF(132f, 96f, 380f, 250f), 180f, 180f, true, paint)
    return bmp
}
