package com.efm.filemanager.ui.feature.whatsnew

import com.efm.filemanager.R

/** One release's worth of "What's New" copy, keyed by the `versionCode` it first shipped in. */
data class WhatsNewEntry(
    val versionCode: Int,
    val titleRes: Int,
    val bodyRes: Int,
)

/**
 * Append-only: a new release that wants a "What's New" entry adds one here, keyed by its own
 * `versionCode` -- never edits or removes an existing entry, since [WhatsNewViewModel] decides
 * what to show by comparing a user's last-seen version code against this list.
 */
val whatsNewEntries =
    listOf(
        WhatsNewEntry(
            versionCode = 37,
            titleRes = R.string.whats_new_v37_title,
            bodyRes = R.string.whats_new_v37_body,
        ),
    )

/**
 * `lastSeen == null` means a fresh install -- nothing is "new" relative to a version the user
 * never saw before, so this returns an empty list rather than every entry up to [currentVersionCode].
 */
internal fun entriesToShowFor(
    lastSeen: Int?,
    currentVersionCode: Int,
    allEntries: List<WhatsNewEntry> = whatsNewEntries,
): List<WhatsNewEntry> =
    if (lastSeen == null) {
        emptyList()
    } else {
        allEntries.filter { it.versionCode in (lastSeen + 1)..currentVersionCode }
    }
