package com.lextezy.tambiolo.ui.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.Language
import com.lextezy.tambiolo.ui.components.BallRow
import com.lextezy.tambiolo.ui.components.DrumLogo
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo

/** Shown once, on first open. Four short cards with a language switch on top. */
@Composable
fun WelcomeScreen(language: Language, onLanguage: (Language) -> Unit, onDone: () -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = s.welcomeTitles.lastIndex
    if (page > 0) BackHandler { page-- }

    Column(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DrumLogo(30.dp)
            Spacer(Modifier.width(8.dp))
            Text("Tambiolo", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, modifier = Modifier.weight(1f))
            LanguageToggle(language, onLanguage)
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "welcome") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    WelcomeArt(p)
                    Text(
                        s.welcomeTitles[p], fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = c.ink,
                        textAlign = TextAlign.Center, lineHeight = 34.sp,
                    )
                    Text(
                        s.welcomeBodies[p], fontSize = 16.sp, color = c.muted, textAlign = TextAlign.Center, lineHeight = 23.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }

        // Page dots
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center) {
            for (i in 0..last) {
                Box(
                    Modifier.padding(horizontal = 4.dp).size(width = if (i == page) 22.dp else 8.dp, height = 8.dp)
                        .background(if (i == page) c.ink else c.line, CircleShape)
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (page < last) {
                TextButton(onClick = onDone, modifier = Modifier.height(52.dp)) { Text(s.skip, color = c.muted, fontSize = 16.sp) }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (page < last) page++ else onDone() },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.selectedBg, contentColor = c.selectedInk),
            ) {
                Text(if (page < last) s.next else s.getStarted, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun LanguageToggle(language: Language, onLanguage: (Language) -> Unit) {
    val c = Tambiolo.colors
    Row(Modifier.border(1.5.dp, c.line, RoundedCornerShape(20.dp)).padding(3.dp)) {
        listOf(Language.EN to "English", Language.TL to "Tagalog").forEach { (lang, label) ->
            val on = lang == language
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (on) c.selectedInk else c.ink,
                modifier = Modifier
                    .background(if (on) c.selectedBg else Color.Transparent, RoundedCornerShape(17.dp))
                    .clickable { onLanguage(lang) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** Simple illustration per card, built from the app's own pieces. */
@Composable
private fun WelcomeArt(page: Int) {
    val c = Tambiolo.colors
    Box(Modifier.size(width = 280.dp, height = 150.dp), contentAlignment = Alignment.Center) {
        when (page) {
            0 -> Column(
                Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("6/58 · Ultra Lotto", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 14.sp)
                BallRow(listOf(4, 17, 23, 38, 41, 56), maxBall = 38.dp)
            }
            1 -> Canvas(Modifier.size(140.dp)) {
                val r = size.minDimension / 2f
                drawCircle(c.drumRim, radius = r - 4f, style = Stroke(width = 8f))
                val balls = listOf(
                    Offset(0.35f, 0.62f) to Color(0xFFFFC23D), Offset(0.55f, 0.7f) to Color(0xFFF2545B),
                    Offset(0.5f, 0.42f) to Color(0xFF4FB3E8), Offset(0.68f, 0.5f) to Color(0xFF4CD39A),
                    Offset(0.3f, 0.4f) to Color(0xFFB68CFF), Offset(0.45f, 0.25f) to Color(0xFFFF8F5A),
                )
                balls.forEach { (o, col) -> drawCircle(col, radius = r * 0.17f, center = Offset(size.width * o.x, size.height * o.y)) }
            }
            2 -> Column(
                Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("★  Family birthdays", fontWeight = FontWeight.Bold, color = c.ink, fontSize = 14.sp)
                BallRow(listOf(5, 12, 22, 30, 44, 51), maxBall = 38.dp)
            }
            else -> Box(Modifier.size(120.dp).background(c.navPill, CircleShape), contentAlignment = Alignment.Center) {
                Text("18+", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = c.navPillInk)
            }
        }
    }
}
