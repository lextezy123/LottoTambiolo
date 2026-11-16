package com.lextezy.tambiolo.data

import java.util.Locale

enum class Language {
    EN, TL;

    companion object {
        fun fromPhone(): Language =
            if (Locale.getDefault().language in setOf("fil", "tl")) TL else EN
    }
}
