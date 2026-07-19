package com.prolocity.patchtracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SessionBackupPatch(
    val name: String,
    val iconKey: String?,
    val badgeText: String?,
    val imagePath: String?,
    val awardedAtTime: Boolean,
    val fulfilledDate: LocalDate?,
    val optedForRaffle: Boolean
) {
    val status: PatchLineStatus get() = patchLineStatus(awardedAtTime, fulfilledDate, optedForRaffle)
}

data class SessionBackupAward(
    val playerName: String,
    val playerNumber: String,
    val division: String,
    val dateEarned: LocalDate,
    val photoPath: String?,
    val patches: List<SessionBackupPatch>
)

data class SessionBackupData(
    val sessionName: String,
    val createdDate: LocalDate,
    val exportedAt: LocalDate,
    val awards: List<SessionBackupAward>
)

// Groups the session's flattened award lines by event, mirroring PatchListScreen's PatchEventGroup.
fun buildSessionBackupData(
    session: Session,
    lines: List<PatchAwardLineDetails>,
    exportedAt: LocalDate = LocalDate.now()
): SessionBackupData {
    val awards = lines.groupBy { it.eventId }.map { (_, group) ->
        val first = group.first()
        SessionBackupAward(
            playerName = first.playerName,
            playerNumber = first.playerNumber,
            division = first.division,
            dateEarned = first.dateEarned,
            photoPath = first.photoPath,
            patches = group.map { line ->
                SessionBackupPatch(
                    name = line.patchName,
                    iconKey = line.patchIconKey,
                    badgeText = line.patchBadgeText,
                    imagePath = line.patchImagePath,
                    awardedAtTime = line.awardedAtTime,
                    fulfilledDate = line.fulfilledDate,
                    optedForRaffle = line.optedForRaffle
                )
            }
        )
    }.sortedWith(compareByDescending<SessionBackupAward> { it.dateEarned }.thenBy { it.playerName })
    return SessionBackupData(session.name, session.createdDate, exportedAt, awards)
}

// Writes a self-contained backup: session.json (denormalized award/patch data, referencing
// photos by filename) plus a photos/ folder holding the actual JPEGs, deduped by absolute path.
fun writeSessionBackup(outputStream: OutputStream, data: SessionBackupData) {
    val photoPaths = (
        data.awards.mapNotNull { it.photoPath } +
            data.awards.flatMap { award -> award.patches.mapNotNull { it.imagePath } }
        ).distinct()

    ZipOutputStream(outputStream).use { zip ->
        zip.putNextEntry(ZipEntry("session.json"))
        zip.write(toJson(data).toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        photoPaths.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                zip.putNextEntry(ZipEntry("photos/${file.name}"))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}

// Unzips into a fresh app-cache directory and resolves each photo filename to its extracted
// absolute path, so the review screen can hand paths straight to the existing PatchIcon/AsyncImage
// rendering without knowing anything about the backup file's zip/json structure.
fun readSessionBackup(context: Context, inputStream: InputStream): SessionBackupData {
    val extractDir = File(context.cacheDir, "session_review/${UUID.randomUUID()}")
    val photosDir = File(extractDir, "photos").apply { mkdirs() }

    var jsonText: String? = null
    ZipInputStream(inputStream).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            when {
                entry.name == "session.json" -> jsonText = zip.readBytes().toString(Charsets.UTF_8)
                entry.name.startsWith("photos/") -> {
                    File(photosDir, entry.name.removePrefix("photos/")).outputStream().use { zip.copyTo(it) }
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    val root = JSONObject(jsonText ?: throw IllegalArgumentException("Not a valid session backup file"))
    return fromJson(root) { fileName -> fileName?.let { File(photosDir, it).absolutePath } }
}

private fun toJson(data: SessionBackupData): JSONObject = JSONObject().apply {
    put("sessionName", data.sessionName)
    put("createdDate", data.createdDate.toString())
    put("exportedAt", data.exportedAt.toString())
    put("awards", JSONArray().apply {
        data.awards.forEach { award ->
            put(JSONObject().apply {
                put("playerName", award.playerName)
                put("playerNumber", award.playerNumber)
                put("division", award.division)
                put("dateEarned", award.dateEarned.toString())
                put("photoFileName", award.photoPath?.let { File(it).name })
                put("patches", JSONArray().apply {
                    award.patches.forEach { patch ->
                        put(JSONObject().apply {
                            put("name", patch.name)
                            put("iconKey", patch.iconKey)
                            put("badgeText", patch.badgeText)
                            put("photoFileName", patch.imagePath?.let { File(it).name })
                            put("awardedAtTime", patch.awardedAtTime)
                            put("fulfilledDate", patch.fulfilledDate?.toString())
                            put("optedForRaffle", patch.optedForRaffle)
                        })
                    }
                })
            })
        }
    })
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun fromJson(root: JSONObject, resolvePhoto: (String?) -> String?): SessionBackupData {
    val awardsArray = root.getJSONArray("awards")
    val awards = (0 until awardsArray.length()).map { i ->
        val awardObj = awardsArray.getJSONObject(i)
        val patchesArray = awardObj.getJSONArray("patches")
        val patches = (0 until patchesArray.length()).map { j ->
            val patchObj = patchesArray.getJSONObject(j)
            SessionBackupPatch(
                name = patchObj.getString("name"),
                iconKey = patchObj.optStringOrNull("iconKey"),
                badgeText = patchObj.optStringOrNull("badgeText"),
                imagePath = resolvePhoto(patchObj.optStringOrNull("photoFileName")),
                awardedAtTime = patchObj.getBoolean("awardedAtTime"),
                fulfilledDate = patchObj.optStringOrNull("fulfilledDate")?.let { LocalDate.parse(it) },
                optedForRaffle = patchObj.optBoolean("optedForRaffle", false)
            )
        }
        SessionBackupAward(
            playerName = awardObj.getString("playerName"),
            playerNumber = awardObj.getString("playerNumber"),
            division = awardObj.getString("division"),
            dateEarned = LocalDate.parse(awardObj.getString("dateEarned")),
            photoPath = resolvePhoto(awardObj.optStringOrNull("photoFileName")),
            patches = patches
        )
    }
    return SessionBackupData(
        sessionName = root.getString("sessionName"),
        createdDate = LocalDate.parse(root.getString("createdDate")),
        exportedAt = LocalDate.parse(root.getString("exportedAt")),
        awards = awards
    )
}
