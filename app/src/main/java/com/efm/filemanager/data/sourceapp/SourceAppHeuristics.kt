package com.efm.filemanager.data.sourceapp

/**
 * Folder-name -> package heuristic used only when MediaStore's OWNER_PACKAGE_NAME
 * (the exact signal) has nothing for a file. Grows over time as real usage surfaces
 * more well-known app folder names -- see docs/PLAN.md Phase 1 for why this can
 * only ever be a best-effort fallback, never a certainty.
 */
object SourceAppHeuristics {
    val packageByFolderName: Map<String, String> =
        mapOf(
            "WhatsApp Images" to "com.whatsapp",
            "WhatsApp Video" to "com.whatsapp",
            "WhatsApp Documents" to "com.whatsapp",
            "WhatsApp Audio" to "com.whatsapp",
            "WhatsApp Voice Notes" to "com.whatsapp",
            "WhatsApp Animated Gifs" to "com.whatsapp",
            "WhatsApp Stickers" to "com.whatsapp",
            "Telegram Images" to "org.telegram.messenger",
            "Telegram Video" to "org.telegram.messenger",
            "Telegram Documents" to "org.telegram.messenger",
            "Telegram Audio" to "org.telegram.messenger",
        )
}
