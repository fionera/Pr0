package com.pr0gramm.app.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 */
class TabsStateAdapter : FragmentStateAdapter {
    constructor(context: Context, fragment: Fragment) : super(fragment) {
        this.context = context
    }

    constructor(activity: FragmentActivity) : super(activity) {
        this.context = activity
    }

    private val context: Context
    private val tabs: MutableList<TabInfo> = mutableListOf()

    override fun getItemCount(): Int {
        return tabs.size
    }

    override fun createFragment(position: Int): Fragment {
        val info = tabs[position]

        return info.fragmentConstructor().also { fr ->
            if (info.args != null) {
                val existing = fr.arguments
                if (existing != null) {
                    existing.putAll(info.args)
                } else {
                    fr.arguments = Bundle(info.args)
                }
            }
        }
    }

    fun addTab(title: String, args: Bundle? = null, id: Any? = null, fragmentConstructor: () -> Fragment) {
        val info = TabInfo(id ?: Any(), title, fragmentConstructor, args)
        tabs.add(info)

        // one item was added
        notifyItemInserted(tabs.size - 1)
    }

    fun updateTabTitle(id: Any, title: String) {
        for ((idx, tab) in tabs.withIndex()) {
            if (tab.id == id) {
                tabs[idx] = tab.copy(title = title)
                notifyItemChanged(idx)
            }
        }
    }

    fun getPageTitle(position: Int): CharSequence {
        return tabs[position].title
    }

    private data class TabInfo(val id: Any, val title: String, val fragmentConstructor: () -> Fragment, val args: Bundle?)
}
