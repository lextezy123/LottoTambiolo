package com.lextezy.tambiolo.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.lextezy.tambiolo.data.Language

/**
 * All on-screen text, in English and Tagalog. Read with `S.str` inside composables.
 * Game names (Ultra Lotto, Grand Lotto…) stay as-is in both languages.
 */
abstract class Strings {
    // Tabs
    abstract val tabResults: String
    abstract val tabSimulator: String
    abstract val tabSaved: String

    // Results
    abstract val lottoResults: String
    abstract val refresh: String
    abstract val settings: String
    abstract val back: String
    abstract val offlineSaved: String
    abstract val cantLoad: String
    abstract fun updated(time: String): String
    abstract val loadingResults: String
    abstract val noResultsYet: String
    abstract val jackpot: String
    abstract val resultsDisclaimer: String

    // Simulator
    abstract val simTitle: String
    abstract val justForFun: String
    abstract val ready: String
    abstract fun pickMore(n: Int): String
    abstract val mixing: String
    abstract fun ballOf(n: Int): String
    abstract fun matchedSoFar(n: Int): String
    abstract val allMatched: String
    abstract fun youMatched(n: Int): String
    abstract val draw: String
    abstract val drawAgain: String
    abstract val skipToResult: String
    abstract val run1000: String
    abstract val run1000Help: String
    abstract val run1000InfoTitle: String
    abstract val run1000InfoBody: String
    abstract val ok: String
    abstract val yourNumbers: String
    abstract val quickPick: String
    abstract val clear: String
    abstract val randomNote: String
    abstract fun simulatedDraws(n: String): String
    abstract fun matches(h: Int): String
    abstract fun oddsAll(game: String, odds: String): String
    abstract val simTip: String
    abstract val gotIt: String

    // Saved
    abstract val savedTitle: String
    abstract val newCombo: String
    abstract val emptyTitle: String
    abstract val emptyBody: String
    abstract fun noResultsLoaded(code: String): String
    abstract fun latestMatched(code: String, date: String, matched: Int): String
    abstract val simulate: String
    abstract fun deleteLabel(name: String): String
    abstract fun deleteTitle(name: String): String
    abstract val deleteBody: String
    abstract val cancel: String
    abstract val delete: String
    abstract val savedNote: String
    abstract val nameLabel: String
    abstract val pick6: String
    abstract val save: String
    abstract val defaultComboName: String

    // Settings
    abstract val theme: String
    abstract val themeSystem: String
    abstract val themeLight: String
    abstract val themeDark: String
    abstract val language: String
    abstract val ads: String
    abstract val adsRemoved: String
    abstract fun removeAds(price: String?): String
    abstract val purchaseUnavailable: String
    abstract val restorePurchase: String
    abstract val about: String

    // Welcome
    abstract val welcomeTitles: List<String>
    abstract val welcomeBodies: List<String>
    abstract val skip: String
    abstract val next: String
    abstract val getStarted: String
}

object EnStrings : Strings() {
    override val tabResults = "Results"
    override val tabSimulator = "Simulator"
    override val tabSaved = "Saved"

    override val lottoResults = "LOTTO RESULTS"
    override val refresh = "Refresh results"
    override val settings = "Settings"
    override val back = "Back"
    override val offlineSaved = "Offline. Showing saved results."
    override val cantLoad = "Can't load results. Check your connection and tap refresh."
    override fun updated(time: String) = "Updated $time"
    override val loadingResults = "Loading results…"
    override val noResultsYet = "No results yet"
    override val jackpot = "Jackpot "
    override val resultsDisclaimer = "Unofficial results for reference only. Always confirm with PCSO before claiming a prize."

    override val simTitle = "Draw simulator"
    override val justForFun = "Just for fun"
    override val ready = "Ready. Tap Draw."
    override fun pickMore(n: Int) = "Pick $n more number${if (n == 1) "" else "s"} below."
    override val mixing = "Mixing…"
    override fun ballOf(n: Int) = "Ball $n of 6"
    override fun matchedSoFar(n: Int) = "$n matched so far"
    override val allMatched = "All 6 matched! That's a jackpot."
    override fun youMatched(n: Int) = "You matched $n of 6"
    override val draw = "Draw"
    override val drawAgain = "Draw again"
    override val skipToResult = "Skip to result"
    override val run1000 = "Run 1,000 draws"
    override val run1000Help = "What does Run 1,000 draws do?"
    override val run1000InfoTitle = "Run 1,000 draws"
    override val run1000InfoBody = "Instantly runs 1,000 draws with your numbers, without the animation. " +
        "The results appear right below the buttons: how many times you matched 0, 1, 2… up to all 6. " +
        "It's a quick way to see how rare a jackpot really is."
    override val ok = "OK"
    override val yourNumbers = "Your numbers"
    override val quickPick = "Quick pick"
    override val clear = "Clear"
    override val randomNote = "Every draw is random. Picking \"lucky\" or past numbers does not change the odds."
    override fun simulatedDraws(n: String) = "$n simulated draws"
    override fun matches(h: Int) = "$h match${if (h == 1) "" else "es"}"
    override fun oddsAll(game: String, odds: String) = "Odds of matching all 6 in $game: 1 in $odds."
    override val simTip = "Tap 6 numbers below or use Quick pick, then tap Draw."
    override val gotIt = "Got it"

    override val savedTitle = "Saved combos"
    override val newCombo = "New combo"
    override val emptyTitle = "No saved combos yet"
    override val emptyBody = "Tap New combo to save numbers you pick often, like birthdays. " +
        "You can then simulate them or check them against the latest draw."
    override fun noResultsLoaded(code: String) = "No $code results loaded yet"
    override fun latestMatched(code: String, date: String, matched: Int) = "Latest $code draw, $date: matched $matched of 6"
    override val simulate = "Simulate"
    override fun deleteLabel(name: String) = "Delete $name"
    override fun deleteTitle(name: String) = "Delete \"$name\"?"
    override val deleteBody = "This removes the saved numbers from this phone."
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val savedNote = "Every draw is random. Saved or \"lucky\" numbers don't change the odds."
    override val nameLabel = "Name, e.g. Family birthdays"
    override val pick6 = "Pick 6 numbers"
    override val save = "Save"
    override val defaultComboName = "My numbers"

    override val theme = "Theme"
    override val themeSystem = "Phone setting"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val language = "Language"
    override val ads = "Ads"
    override val adsRemoved = "Ads are removed. Salamat for supporting Tambiolo!"
    override fun removeAds(price: String?) = if (price != null) "Remove ads · $price one time" else "Remove ads"
    override val purchaseUnavailable = "Purchase isn't available right now. Try again later."
    override val restorePurchase = "Restore purchase"
    override val about = "Tambiolo is an unofficial app and is not affiliated with PCSO. Results are for reference only; " +
        "always confirm with PCSO before claiming a prize. The simulator is for fun and involves no betting or real money."

    override val welcomeTitles = listOf(
        "Lotto results",
        "Spin the tambiolo",
        "Save your numbers",
        "Just for fun",
    )
    override val welcomeBodies = listOf(
        "The latest 6/42 to 6/58 draws in one place, even when you're offline.",
        "Pick 6 numbers and watch the drum mix and draw the balls one by one.",
        "Keep your regular combos, like birthdays, and see how they did in the latest draw.",
        "No betting and no real money here. Tambiolo is unofficial and not affiliated with PCSO. For ages 18 and up.",
    )
    override val skip = "Skip"
    override val next = "Next"
    override val getStarted = "Get started"
}

object TlStrings : Strings() {
    override val tabResults = "Resulta"
    override val tabSimulator = "Simulator"
    override val tabSaved = "Naka-save"

    override val lottoResults = "RESULTA NG LOTTO"
    override val refresh = "I-refresh ang resulta"
    override val settings = "Settings"
    override val back = "Bumalik"
    override val offlineSaved = "Offline. Ipinapakita ang huling naka-save na resulta."
    override val cantLoad = "Hindi ma-load ang resulta. Tingnan ang internet mo at i-tap ang refresh."
    override fun updated(time: String) = "Na-update $time"
    override val loadingResults = "Nilo-load ang resulta…"
    override val noResultsYet = "Wala pang resulta"
    override val jackpot = "Jackpot "
    override val resultsDisclaimer = "Hindi opisyal na resulta, pang-reference lang. Laging kumpirmahin sa PCSO bago mag-claim ng premyo."

    override val simTitle = "Draw simulator"
    override val justForFun = "Laro lang"
    override val ready = "Handa na. I-tap ang Draw."
    override fun pickMore(n: Int) = "Pumili pa ng $n numero sa ibaba."
    override val mixing = "Hinahalo…"
    override fun ballOf(n: Int) = "Bola $n sa 6"
    override fun matchedSoFar(n: Int) = "$n na ang tumama"
    override val allMatched = "Tumama lahat ng 6! Jackpot 'yan."
    override fun youMatched(n: Int) = "Tumama ka ng $n sa 6"
    override val draw = "Draw"
    override val drawAgain = "Draw ulit"
    override val skipToResult = "Laktawan"
    override val run1000 = "1,000 draws"
    override val run1000Help = "Ano ang ginagawa ng 1,000 draws?"
    override val run1000InfoTitle = "1,000 draws"
    override val run1000InfoBody = "Agad itong gagawa ng 1,000 draw gamit ang mga numero mo, nang walang animation. " +
        "Lalabas ang resulta sa ilalim ng mga button: ilang beses kang tumama ng 0, 1, 2… hanggang 6. " +
        "Mabilis na paraan para makita kung gaano kabihira ang jackpot."
    override val ok = "Sige"
    override val yourNumbers = "Mga numero mo"
    override val quickPick = "Quick pick"
    override val clear = "Burahin"
    override val randomNote = "Random ang bawat draw. Hindi nagbabago ang tsansa kahit \"swerte\" o lumang numero ang piliin mo."
    override fun simulatedDraws(n: String) = "$n na simulated draw"
    override fun matches(h: Int) = "$h tama"
    override fun oddsAll(game: String, odds: String) = "Tsansang tumama lahat ng 6 sa $game: 1 sa $odds."
    override val simTip = "Pumili ng 6 na numero sa ibaba o i-tap ang Quick pick, tapos i-tap ang Draw."
    override val gotIt = "Sige"

    override val savedTitle = "Naka-save na combo"
    override val newCombo = "Bagong combo"
    override val emptyTitle = "Wala ka pang naka-save na combo"
    override val emptyBody = "I-tap ang Bagong combo para i-save ang mga numerong madalas mong piliin, gaya ng birthdays. " +
        "Puwede mo silang i-simulate o itapat sa pinakabagong draw."
    override fun noResultsLoaded(code: String) = "Wala pang na-load na resulta ng $code"
    override fun latestMatched(code: String, date: String, matched: Int) = "Huling $code draw, $date: $matched sa 6 ang tumama"
    override val simulate = "I-simulate"
    override fun deleteLabel(name: String) = "Burahin ang $name"
    override fun deleteTitle(name: String) = "Burahin ang \"$name\"?"
    override val deleteBody = "Mabubura ang mga numerong ito sa phone na ito."
    override val cancel = "Kanselahin"
    override val delete = "Burahin"
    override val savedNote = "Random ang bawat draw. Hindi nagbabago ang tsansa kahit naka-save o \"swerte\" ang numero."
    override val nameLabel = "Pangalan, hal. Birthdays ng pamilya"
    override val pick6 = "Pumili ng 6 na numero"
    override val save = "I-save"
    override val defaultComboName = "Mga numero ko"

    override val theme = "Itsura"
    override val themeSystem = "Sundin ang phone"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val language = "Wika"
    override val ads = "Ads"
    override val adsRemoved = "Tinanggal na ang ads. Salamat sa pagsuporta sa Tambiolo!"
    override fun removeAds(price: String?) = if (price != null) "Tanggalin ang ads · $price isang beses" else "Tanggalin ang ads"
    override val purchaseUnavailable = "Hindi pa puwedeng bumili ngayon. Subukan ulit mamaya."
    override val restorePurchase = "Ibalik ang nabili"
    override val about = "Hindi opisyal na app ang Tambiolo at hindi konektado sa PCSO. Pang-reference lang ang resulta; " +
        "laging kumpirmahin sa PCSO bago mag-claim ng premyo. Laro lang ang simulator, walang taya at walang totoong pera."

    override val welcomeTitles = listOf(
        "Resulta ng lotto",
        "Paikutin ang tambiolo",
        "I-save ang mga numero mo",
        "Laro lang ito",
    )
    override val welcomeBodies = listOf(
        "Ang pinakabagong draw ng 6/42 hanggang 6/58 sa iisang lugar, kahit offline.",
        "Pumili ng 6 na numero at panoorin ang paghalo at pagbunot ng mga bola isa-isa.",
        "Itabi ang mga paborito mong combo, gaya ng birthdays, at tingnan kung tumama sila sa huling draw.",
        "Walang taya at walang totoong pera dito. Hindi opisyal ang Tambiolo at hindi konektado sa PCSO. Para sa 18 pataas.",
    )
    override val skip = "Laktawan"
    override val next = "Susunod"
    override val getStarted = "Simulan"
}

fun stringsFor(lang: Language): Strings = if (lang == Language.TL) TlStrings else EnStrings

val LocalStrings = staticCompositionLocalOf<Strings> { EnStrings }

object S {
    val str: Strings
        @Composable get() = LocalStrings.current
}
