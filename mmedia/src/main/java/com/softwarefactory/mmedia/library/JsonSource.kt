package com.softwarefactory.mmedia.library

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.softwarefactory.mmedia.extensions.album
import com.softwarefactory.mmedia.extensions.albumArt
import com.softwarefactory.mmedia.extensions.albumArtUri
import com.softwarefactory.mmedia.extensions.artist
import com.softwarefactory.mmedia.extensions.displayDescription
import com.softwarefactory.mmedia.extensions.displayIconUri
import com.softwarefactory.mmedia.extensions.displaySubtitle
import com.softwarefactory.mmedia.extensions.displayTitle
import com.softwarefactory.mmedia.extensions.downloadStatus
import com.softwarefactory.mmedia.extensions.duration
import com.softwarefactory.mmedia.extensions.flag
import com.softwarefactory.mmedia.extensions.genre
import com.softwarefactory.mmedia.extensions.id
import com.softwarefactory.mmedia.extensions.mediaUri
import com.softwarefactory.mmedia.extensions.title
import com.softwarefactory.mmedia.extensions.trackCount
import com.softwarefactory.mmedia.extensions.trackNumber
import com.google.gson.Gson
import com.softwarefactory.mmedia.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Source of [MediaMetadataCompat] objects created from a basic JSON stream.
 *
 * The definition of the JSON is specified in the docs of [JsonMusic] in this file,
 * which is the object representation of it.
 */
class JsonSource(context: Context, source: Uri) : AbstractMusicSource() {
    private var catalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING

        UpdateCatalogTask(Glide.with(context)) { mediaItems ->
            catalog = mediaItems
            state = STATE_INITIALIZED
        }.execute(source)
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()
}

/**
 * Task to connect to remote URIs and download/process JSON files that correspond to
 * [MediaMetadataCompat] objects.
 */
private class UpdateCatalogTask(val glide: RequestManager,
                                val listener: (List<MediaMetadataCompat>) -> Unit) :
        AsyncTask<Uri, Void, List<MediaMetadataCompat>>() {

    override fun doInBackground(vararg params: Uri): List<MediaMetadataCompat> {
        val mediaItems = ArrayList<MediaMetadataCompat>()

        params.forEach { catalogUri ->
            val musicCat = tryDownloadJson(catalogUri)

            // Get the base URI to fix up relative references later.
            val baseUri = catalogUri.toString().removeSuffix(catalogUri.lastPathSegment)

            mediaItems += musicCat.music.map { song ->
                // The JSON may have paths that are relative to the source of the JSON
                // itself. We need to fix them up here to turn them into absolute paths.
                if (!song.source.startsWith(catalogUri.scheme)) {
                    song.source = baseUri + song.source
                }
                if (!song.image.startsWith(catalogUri.scheme)) {
                    song.image = baseUri + song.image
                }

                // Block on downloading artwork.
                val art = glide.applyDefaultRequestOptions(glideOptions)
                        .asBitmap()
                        .load(song.image)
                        .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                        .get()

                MediaMetadataCompat.Builder()
                        .from(song)
                        .apply {
                            albumArt = art
                        }
                        .build()
            }.toList()
        }

        return mediaItems
    }

    override fun onPostExecute(mediaItems: List<MediaMetadataCompat>) {
        super.onPostExecute(mediaItems)
        listener(mediaItems)
    }


    private fun tryDownloadJson(catalogUri: Uri) =
        try {
            val catalogConn = URL(catalogUri.toString())
            val reader = BufferedReader(InputStreamReader(catalogConn.openStream()))
            Gson().fromJson<JsonCatalog>(reader, JsonCatalog::class.java)
        } catch (ioEx: IOException) {
            JsonCatalog()
        }
}

fun MediaMetadataCompat.Builder.from(jsonMusic: JsonMusic): MediaMetadataCompat.Builder {
    // The duration from the JSON is given in seconds, but the rest of the code works in
    // milliseconds. Here's where we convert to the proper units.
    val durationMs = TimeUnit.SECONDS.toMillis(jsonMusic.duration)

    id = jsonMusic.id
    title = jsonMusic.title
    artist = jsonMusic.artist
    album = jsonMusic.album
    duration = durationMs
    genre = jsonMusic.genre
    mediaUri = jsonMusic.source
    albumArtUri = jsonMusic.image
    trackNumber = jsonMusic.trackNumber
    trackCount = jsonMusic.totalTrackCount
    flag = MediaItem.FLAG_PLAYABLE

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = jsonMusic.title
    displaySubtitle = jsonMusic.artist
    displayDescription = jsonMusic.album
    displayIconUri = jsonMusic.image

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = STATUS_NOT_DOWNLOADED

    // Allow it to be used in the typical builder style.
    return this
}

class JsonCatalog {
    var music: List<JsonMusic> = ArrayList()
}


class JsonMusic {
    var id: String = ""
    var title: String = ""
    var album: String = ""
    var artist: String = ""
    var genre: String = ""
    var source: String = ""
    var image: String = ""
    var trackNumber: Long = 0
    var totalTrackCount: Long = 0
    var duration: Long = -1
    var site: String = ""
}

private const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
        .fallback(R.drawable.default_art)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
