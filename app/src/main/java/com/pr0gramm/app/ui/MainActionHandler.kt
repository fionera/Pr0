package com.pr0gramm.app.ui

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.pr0gramm.app.feed.FeedFilter
import com.pr0gramm.app.ui.fragments.CommentRef

/**
 */
interface MainActionHandler {
    fun onLogoutClicked()

    fun onFeedFilterSelected(filter: FeedFilter)

    fun onFeedFilterSelected(filter: FeedFilter, searchQueryState: Bundle?)

    fun onFeedFilterSelected(filter: FeedFilter, queryState: Bundle?,
                             startAt: CommentRef?, popBackstack: Boolean = false)

    fun bookmarkFilter(filter: FeedFilter, title: String)

    fun showUploadBottomSheet(dialog: DialogFragment)

}
