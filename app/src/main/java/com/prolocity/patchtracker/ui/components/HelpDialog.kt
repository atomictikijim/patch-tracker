package com.prolocity.patchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prolocity.patchtracker.ui.help.featureSections

// A top-bar Help button that opens the given FEATURES.md section(s) in a full-screen dialog.
// Drop one into any screen's BrandTopAppBar `actions`. `sections` lets a screen pull more than one
// H2 section (e.g. Sessions also shows "Data & backups").
@Composable
fun HelpAction(title: String, sections: List<String> = listOf(title)) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help for $title")
    }
    if (show) {
        val context = LocalContext.current
        val body = remember(sections) { featureSections(context, sections) }
        HelpDialog(title = title, body = body, onDismiss = { show = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpDialog(title: String, body: String, onDismiss: () -> Unit) {
    val blocks = remember(body) { parseMarkdownBlocks(body) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    BrandTopAppBar(
                        title = "Help — $title",
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close help")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (blocks.isEmpty()) {
                        Text("Help isn't available for this screen.", style = MaterialTheme.typography.bodyMedium)
                    }
                    blocks.forEach { block ->
                        when (block) {
                            is MdHeading -> Text(
                                text = block.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            is MdParagraph -> Text(
                                text = renderInline(block.text),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            is MdBullet -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = renderInline(block.text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- Minimal Markdown for the FEATURES.md subset ----

private sealed interface MdBlock
private data class MdHeading(val text: String) : MdBlock
private data class MdParagraph(val text: String) : MdBlock
private data class MdBullet(val text: String) : MdBlock

// Block-level parse: "### " subheadings, "- " bullets (joining 2-space-indented wrapped
// continuation lines), and paragraphs. Blank lines and "---" rules separate blocks. Inline
// **bold**/*italic*/`code` is handled by renderInline at draw time.
private fun parseMarkdownBlocks(md: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = md.lines()
    val para = StringBuilder()
    fun flushPara() {
        if (para.isNotBlank()) blocks.add(MdParagraph(para.toString().trim()))
        para.setLength(0)
    }
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() || trimmed == "---" -> flushPara()
            trimmed.startsWith("### ") -> {
                flushPara()
                blocks.add(MdHeading(trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("- ") -> {
                flushPara()
                val sb = StringBuilder(trimmed.removePrefix("- ").trim())
                // A following indented, non-bullet line is a wrapped continuation of this bullet.
                while (i + 1 < lines.size) {
                    val next = lines[i + 1]
                    if (next.isNotBlank() && next.first().isWhitespace() && !next.trim().startsWith("- ")) {
                        sb.append(' ').append(next.trim())
                        i++
                    } else break
                }
                blocks.add(MdBullet(sb.toString()))
            }
            else -> {
                if (para.isNotEmpty()) para.append(' ')
                para.append(trimmed)
            }
        }
        i++
    }
    flushPara()
    return blocks
}

private fun renderInline(text: String): AnnotatedString = buildAnnotatedString { appendInline(text) }

// Recursive so bold can wrap code/italic (e.g. FEATURES.md uses **`.zip`**).
private fun AnnotatedString.Builder.appendInline(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end == -1) { append(text.substring(i)); return }
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(text.substring(i + 2, end))
                pop()
                i = end + 2
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end == -1) { append(text.substring(i)); return }
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                append(text.substring(i + 1, end))
                pop()
                i = end + 1
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end == -1) { append(text.substring(i)); return }
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(text.substring(i + 1, end))
                pop()
                i = end + 1
            }
            else -> { append(text[i]); i++ }
        }
    }
}
