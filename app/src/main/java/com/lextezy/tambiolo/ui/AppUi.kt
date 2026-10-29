package com.lextezy.tambiolo.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lextezy.tambiolo.TambioloApplication
import com.lextezy.tambiolo.data.Combo
import com.lextezy.tambiolo.data.Language
import com.lextezy.tambiolo.data.ThemeMode
import com.lextezy.tambiolo.ui.components.BannerAd
import com.lextezy.tambiolo.ui.components.DrumIcon
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.results.ResultsScreen
import com.lextezy.tambiolo.ui.results.ResultsViewModel
import com.lextezy.tambiolo.ui.saved.SavedScreen
import com.lextezy.tambiolo.ui.simulator.SimulatorScreen
import com.lextezy.tambiolo.ui.theme.Tambiolo
import com.lextezy.tambiolo.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

private enum class Tab { RESULTS, SIMULATOR, SAVED }

@Composable
fun TambioloAppUi(app: TambioloApplication) {
    val c = Tambiolo.colors
    val scope = rememberCoroutineScope()
    // null until the saved value loads, so the welcome screen doesn't flash for returning users
    val onboarded by app.prefs.onboarded.collectAsState(initial = null)
    val language by app.prefs.language.collectAsState(initial = Language.fromPhone())

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize().background(c.bg))
        false -> WelcomeScreen(
            language = language,
            onLanguage = { lang -> scope.launch { app.prefs.setLanguage(lang) } },
            onDone = { scope.launch { app.prefs.setOnboarded() } },
        )
        true -> MainTabs(app)
    }
}

@Composable
private fun MainTabs(app: TambioloApplication) {
    val c = Tambiolo.colors
    val s = S.str
    val scope = rememberCoroutineScope()
    val resultsVm: ResultsViewModel = viewModel()
    val results by resultsVm.state.collectAsStateWithLifecycle()
    val combos by app.prefs.combos.collectAsState(initial = emptyList())
    val adsRemoved by app.prefs.adsRemoved.collectAsState(initial = true) // assume removed until known, so no ad flashes
    val simTipSeen by app.prefs.simTipSeen.collectAsState(initial = true)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<Combo?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(Modifier.background(c.navBar)) {
                if (!adsRemoved) BannerAd()
                HorizontalDivider(color = c.line)
                NavigationBar(containerColor = c.navBar, tonalElevation = 0.dp) {
                    Tab.entries.forEachIndexed { i, t ->
                        val selected = tab == i
                        val tint = if (selected) c.navPillInk else c.muted
                        val label = when (t) { Tab.RESULTS -> s.tabResults; Tab.SIMULATOR -> s.tabSimulator; Tab.SAVED -> s.tabSaved }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = i },
                            icon = {
                                when (t) {
                                    Tab.RESULTS -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = tint)
                                    Tab.SIMULATOR -> DrumIcon(tint)
                                    Tab.SAVED -> Icon(Icons.Filled.Star, contentDescription = null, tint = tint)
                                }
                            },
                            label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = c.navPill,
                                selectedTextColor = c.ink,
                                unselectedTextColor = c.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).statusBarsPadding()) {
            when (Tab.entries[tab]) {
                Tab.RESULTS -> ResultsScreen(results, onRefresh = resultsVm::refresh, onOpenSettings = { showSettings = true })
                Tab.SIMULATOR -> SimulatorScreen(
                    pending = pending,
                    onPendingUsed = { pending = null },
                    showTip = !simTipSeen,
                    onTipSeen = { scope.launch { app.prefs.setSimTipSeen() } },
                )
                Tab.SAVED -> SavedScreen(
                    combos = combos,
                    latestDraw = { max -> results.latest(max) },
                    onSave = { combo -> scope.launch { app.prefs.saveCombo(combo) } },
                    onDelete = { id -> scope.launch { app.prefs.deleteCombo(id) } },
                    onSimulate = { combo -> pending = combo; tab = Tab.SIMULATOR.ordinal },
                )
            }
        }
    }

    if (showSettings) SettingsSheet(app, adsRemoved, onDismiss = { showSettings = false })
}

@Composable
private fun Segmented(options: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    val c = Tambiolo.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, (label, on) ->
            Box(
                Modifier.weight(1f).height(44.dp)
                    .background(if (on) c.selectedBg else c.card, RoundedCornerShape(22.dp))
                    .border(1.5.dp, if (on) c.selectedBg else c.line, RoundedCornerShape(22.dp))
                    .clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.selectedInk else c.ink)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(app: TambioloApplication, adsRemoved: Boolean, onDismiss: () -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val mode by app.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val language by app.prefs.language.collectAsState(initial = Language.fromPhone())
    val price by app.billing.price.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.bg) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(s.settings, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)

            Text(s.language, fontWeight = FontWeight.Bold, color = c.ink)
            val langs = listOf(Language.EN, Language.TL)
            Segmented(listOf("English" to (language == Language.EN), "Tagalog" to (language == Language.TL))) { i ->
                scope.launch { app.prefs.setLanguage(langs[i]) }
            }

            Text(s.theme, fontWeight = FontWeight.Bold, color = c.ink)
            val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
            Segmented(
                listOf(s.themeSystem to (mode == ThemeMode.SYSTEM), s.themeLight to (mode == ThemeMode.LIGHT), s.themeDark to (mode == ThemeMode.DARK))
            ) { i -> scope.launch { app.prefs.setTheme(modes[i]) } }

            Text(s.ads, fontWeight = FontWeight.Bold, color = c.ink)
            if (adsRemoved) {
                Text(s.adsRemoved, fontSize = 14.sp, color = c.match)
            } else {
                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity == null || !app.billing.buy(activity)) {
                            Toast.makeText(context, s.purchaseUnavailable, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.selectedBg, contentColor = c.selectedInk),
                ) {
                    Text(s.removeAds(price), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { app.billing.restore() }) { Text(s.restorePurchase, color = c.ink) }
            }

            HorizontalDivider(color = c.line)
            Text(s.about, fontSize = 12.sp, color = c.muted, lineHeight = 17.sp)
        }
    }
}
