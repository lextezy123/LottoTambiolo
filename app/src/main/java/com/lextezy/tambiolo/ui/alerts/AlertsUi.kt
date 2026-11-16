package com.lextezy.tambiolo.ui.alerts

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.TambioloApplication
import com.lextezy.tambiolo.data.DEFAULT_ALERT_GAMES
import com.lextezy.tambiolo.data.DIGIT_GAMES
import com.lextezy.tambiolo.data.GAMES
import com.lextezy.tambiolo.data.ResultAlerts
import com.lextezy.tambiolo.ui.components.Mono
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo
import kotlinx.coroutines.launch

/**
 * Returns a function that turns result alerts on. On Android 13 and newer it first
 * asks for the notification permission; if the user says no, alerts stay off.
 */
@Composable
fun rememberEnableAlerts(app: TambioloApplication): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blockedMessage = S.str.alertsBlocked
    val enable: () -> Unit = {
        scope.launch {
            app.prefs.setAlertsEnabled(true)
            ResultAlerts.schedule(context.applicationContext)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else Toast.makeText(context, blockedMessage, Toast.LENGTH_LONG).show()
    }
    return {
        if (ResultAlerts.canNotify(context)) enable()
        else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** The "Result alerts" block in Settings: on/off switch, and which games to be alerted for. */
@Composable
fun AlertsSettings(app: TambioloApplication) {
    val c = Tambiolo.colors
    val s = S.str
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by app.prefs.alertsEnabled.collectAsState(initial = false)
    val games by app.prefs.alertGames.collectAsState(initial = DEFAULT_ALERT_GAMES)
    val enableAlerts = rememberEnableAlerts(app)
    // If the permission was taken away in the phone's settings, show the switch as off.
    val on = enabled && ResultAlerts.canNotify(context)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.alertsSetting, fontWeight = FontWeight.Bold, color = c.ink, modifier = Modifier.weight(1f))
            Switch(
                checked = on,
                onCheckedChange = { want ->
                    if (want) {
                        enableAlerts()
                    } else {
                        scope.launch {
                            app.prefs.setAlertsEnabled(false)
                            ResultAlerts.cancel(context.applicationContext)
                        }
                    }
                },
            )
        }
        if (on) {
            Text(s.alertsNote, fontSize = 13.sp, color = c.muted, lineHeight = 18.sp)
            Text(s.alertsGames, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            val toggle: (String) -> Unit = { code ->
                scope.launch { app.prefs.setAlertGames(if (code in games) games - code else games + code) }
            }
            GameToggleRow(GAMES.sortedBy { it.max }.map { it.code }, games, toggle)
            GameToggleRow(DIGIT_GAMES.map { it.code }, games, toggle)
        }
    }
}

@Composable
private fun GameToggleRow(codes: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    val c = Tambiolo.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        codes.forEach { code ->
            val on = code in selected
            Box(
                Modifier.weight(1f).height(40.dp)
                    .background(if (on) c.selectedBg else c.card, RoundedCornerShape(20.dp))
                    .border(1.5.dp, if (on) c.selectedBg else c.line, RoundedCornerShape(20.dp))
                    .clickable { onToggle(code) },
                contentAlignment = Alignment.Center,
            ) {
                Text(code, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (on) c.selectedInk else c.ink)
            }
        }
    }
}
