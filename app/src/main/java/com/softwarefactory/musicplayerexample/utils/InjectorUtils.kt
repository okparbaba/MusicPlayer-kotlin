package com.softwarefactory.musicplayerexample.utils

import android.content.ComponentName
import android.content.Context
import com.softwarefactory.mmedia.MusicService
import com.softwarefactory.musicplayerexample.viewmodels.MediaItemFragmentViewModel
import com.softwarefactory.musicplayerexample.MediaSessionConnection
import com.softwarefactory.musicplayerexample.viewmodels.MainActivityViewModel

object InjectorUtils {
    private fun provideMediaSessionConnection(context: Context): MediaSessionConnection {
        return MediaSessionConnection.getInstance(context,
                ComponentName(context, MusicService::class.java))
    }

    fun provideMainActivityViewModel(context: Context): MainActivityViewModel.Factory {
        val applicationContext = context.applicationContext
        val mediaSessionConnection = provideMediaSessionConnection(applicationContext)
        return MainActivityViewModel.Factory(mediaSessionConnection)
    }

    fun provideMediaItemFragmentViewModel(context: Context, mediaId: String)
            : MediaItemFragmentViewModel.Factory {
        val applicationContext = context.applicationContext
        val mediaSessionConnection = provideMediaSessionConnection(applicationContext)
        return MediaItemFragmentViewModel.Factory(mediaId, mediaSessionConnection)
    }
}