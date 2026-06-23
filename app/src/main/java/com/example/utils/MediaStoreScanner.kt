package com.example.utils

import android.content.Context
import android.provider.MediaStore
import com.example.audio.Track
import kotlin.math.absoluteValue

object MediaStoreScanner {
    fun scanLocalAudioFiles(context: Context): List<Track> {
        val audioList = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )

        // Query only files marked as music records
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                var idCounter = 100 // Local track IDs start from 100 to avoid preset synth conflicts
                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Song"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val durationMs = c.getLong(durationCol)
                    
                    val contentUri = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$mediaId"

                    // Multi-hue color generator derived from title hashing to keep track accents visual, unique and elegant
                    val hash = title.hashCode().absoluteValue
                    val r = (hash % 120) + 40
                    val g = ((hash / 100) % 120) + 30
                    val b = ((hash / 10000) % 120) + 60
                    val hexColor = String.format("#%02X%02X%02X", r, g, b)

                    audioList.add(
                        Track(
                            id = idCounter++,
                            title = title,
                            artist = artist,
                            album = album,
                            bpm = 110, // Default base speed of local MP3
                            hexColor = hexColor,
                            scale = emptyList(),
                            contentUri = contentUri,
                            durationMs = if (durationMs > 0) durationMs else 180000L,
                            isLocal = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return audioList
    }
}
