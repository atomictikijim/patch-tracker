package com.prolocity.patchtracker.ui.help

import android.content.Context

// In-app Help is sourced from the bundled FEATURES.md (copied into assets at build time — see
// app/build.gradle.kts). This file loads that asset and splits it into per-screen sections so each
// tab's Help button can show just its own part of the guide. HelpDialog renders the Markdown.

private const val FEATURES_ASSET = "FEATURES.md"

// Splits the doc into sections keyed by their H2 heading (the "## Title" lines). The heading line
// itself is dropped; the body is returned verbatim (still Markdown) and trimmed of surrounding
// blank lines / rules. Anything before the first H2 (the intro) is ignored.
fun parseFeatureSections(markdown: String): Map<String, String> {
    val sections = LinkedHashMap<String, StringBuilder>()
    var current: StringBuilder? = null
    markdown.lineSequence().forEach { line ->
        if (line.startsWith("## ")) {
            current = StringBuilder()
            sections[line.removePrefix("## ").trim()] = current!!
        } else {
            current?.appendLine(line)
        }
    }
    // Trailing "---" section separators are left in; the renderer skips horizontal-rule lines.
    return sections.mapValues { it.value.toString().trim() }
}

// Parsed once per process; the asset only changes across builds.
@Volatile
private var cachedSections: Map<String, String>? = null

private fun sections(context: Context): Map<String, String> =
    cachedSections ?: run {
        val text = runCatching {
            context.assets.open(FEATURES_ASSET).bufferedReader().use { it.readText() }
        }.getOrDefault("")
        parseFeatureSections(text).also { cachedSections = it }
    }

// Returns the requested sections (in order, skipping any that are missing) joined into one body.
fun featureSections(context: Context, titles: List<String>): String {
    val all = sections(context)
    return titles.mapNotNull { all[it] }.joinToString("\n\n")
}
