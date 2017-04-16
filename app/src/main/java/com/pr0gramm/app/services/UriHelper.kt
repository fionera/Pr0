package com.pr0gramm.app.services

import android.content.Context
import android.net.Uri

import com.google.common.base.Strings
import com.google.common.collect.ImmutableMap
import com.pr0gramm.app.Dagger
import com.pr0gramm.app.HasThumbnail
import com.pr0gramm.app.Settings
import com.pr0gramm.app.feed.FeedItem
import com.pr0gramm.app.feed.FeedType
import com.pr0gramm.app.services.preloading.PreloadManager
import com.pr0gramm.app.util.map


/**
 * A little helper class to work with URLs
 */
class UriHelper private constructor(context: Context) {
    private val settings: Settings = Settings.get()
    private val preloadManager: PreloadManager = Dagger.appComponent(context).preloadManager()
    private val noPreload = NoPreload()

    private fun start(): Uri.Builder {
        return Uri.Builder()
                .scheme(scheme())
                .authority("pr0gramm.com")
    }

    private fun scheme(): String {
        return if (settings.useHttps) "https" else "http"
    }

    internal fun start(subdomain: String): Uri.Builder {
        return Uri.Builder()
                .scheme(scheme())
                .authority(subdomain + ".pr0gramm.com")
    }

    fun thumbnail(item: HasThumbnail): Uri {
        return preloadManager.get(item.id())
                .map { pi -> Uri.fromFile(pi.thumbnail) }
                .or { noPreload.thumbnail(item) }
    }

    @JvmOverloads fun media(item: FeedItem, hq: Boolean = false): Uri {
        if (hq && !Strings.isNullOrEmpty(item.fullsize))
            return noPreload.media(item, true)

        return preloadManager.get(item.id())
                .map { pi -> Uri.fromFile(pi.media) }
                .or { noPreload.media(item, false) }
    }

    fun base(): Uri {
        return start().build()
    }

    fun post(type: FeedType, itemId: Long): Uri {
        return start().path(FEED_TYPES[type])
                .appendPath(itemId.toString())
                .build()
    }

    fun post(type: FeedType, itemId: Long, commentId: Long): Uri {
        return start().path(FEED_TYPES[type])
                .appendEncodedPath(itemId.toString() + ":comment" + commentId)
                .build()
    }

    fun uploads(user: String): Uri {
        return start().path("/user/$user/uploads").build()
    }

    fun favorites(user: String): Uri {
        return start().path("/user/$user/likes").build()
    }

    fun noPreload(): NoPreload {
        return noPreload
    }

    inner class NoPreload internal constructor() {
        fun media(item: FeedItem): Uri {
            return media(item, false)
        }

        internal fun media(item: FeedItem, highQuality: Boolean): Uri {
            return if (highQuality && !item.isVideo)
                join(start("full"), item.fullsize)
            else
                join(start(if (item.isVideo) "vid" else "img"), item.image)
        }

        fun thumbnail(item: HasThumbnail): Uri {
            return join(start("thumb"), item.thumbnail() ?: "")
        }
    }

    internal fun join(builder: Uri.Builder, path: String): Uri {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return Uri.parse(path)
        }

        if (path.startsWith("//")) {
            return Uri.parse(scheme() + ":" + path)
        }

        val normalized = if (path.startsWith("/")) path else "/" + path
        return builder.path(normalized).build()
    }

    companion object {
        fun of(context: Context): UriHelper {
            return UriHelper(context)
        }

        private val FEED_TYPES = ImmutableMap.builder<FeedType, String>()
                .put(FeedType.NEW, "new")
                .put(FeedType.PROMOTED, "top")
                .put(FeedType.PREMIUM, "stalk")
                .build()
    }
}