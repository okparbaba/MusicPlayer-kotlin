package com.softwarefactory.mmedia.library

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import com.softwarefactory.mmedia.MusicService
import com.softwarefactory.mmedia.extensions.album
import com.softwarefactory.mmedia.extensions.albumArt
import com.softwarefactory.mmedia.extensions.albumArtUri
import com.softwarefactory.mmedia.extensions.artist
import com.softwarefactory.mmedia.extensions.flag
import com.softwarefactory.mmedia.extensions.id
import com.softwarefactory.mmedia.extensions.title
import com.softwarefactory.mmedia.extensions.urlEncoded

class BrowseTree(musicSource: MusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        musicSource.forEach { mediaItem ->
            val albumMediaId = mediaItem.album.urlEncoded
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            albumChildren += mediaItem
        }
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.album.urlEncoded
            title = mediaItem.album
            artist = mediaItem.artist
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri?.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Ensure the root node exists and add this album to the list.
        val rootList = mediaIdToChildren[UAMP_BROWSABLE_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[UAMP_BROWSABLE_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id] = it
        }
    }
}

const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"

