package com.efm.filemanager.ui.feature.settings

enum class LanguageOption {
    SYSTEM,
    ENGLISH,
    HEBREW,
}

internal fun languageOptionFromLanguageTag(tag: String): LanguageOption =
    when {
        tag.startsWith("he") -> LanguageOption.HEBREW
        tag.startsWith("en") -> LanguageOption.ENGLISH
        else -> LanguageOption.SYSTEM
    }

internal fun LanguageOption.toLanguageTag(): String? =
    when (this) {
        LanguageOption.SYSTEM -> null
        LanguageOption.ENGLISH -> "en"
        LanguageOption.HEBREW -> "he"
    }
