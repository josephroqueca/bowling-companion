package ca.josephroque.bowlingcompanion.common.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ca.josephroque.bowlingcompanion.R
import ca.josephroque.bowlingcompanion.common.interfaces.IFloatingActionButtonHandler
import ca.josephroque.bowlingcompanion.common.interfaces.IRefreshable
import kotlinx.android.synthetic.main.fragment_common_tabs.*
import kotlinx.android.synthetic.main.fragment_common_tabs.view.*
import java.lang.ref.WeakReference

/**
 * Copyright (C) 2018 Joseph Roque
 *
 * Base implementation for a fragment with tabs.
 */
abstract class TabbedFragment : BaseFragment(),
        IFloatingActionButtonHandler {

    companion object {
        /** Logging identifier. */
        private const val TAG = "TabbedFragment"
    }

    /** Active tab. */
    protected val currentTab: Int
        get() = tabbed_fragment_pager?.currentItem ?: 0

    /** Delegate for [TabbedFragment] events. */
    private var delegate: TabbedFragmentDelegate? = null

    /** @Override */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_common_tabs, container, false)
        configureToolbar(rootView)
        configureTabLayout(rootView)
        return rootView
    }

    /** @Override */
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        context as? TabbedFragmentDelegate ?: throw RuntimeException("${context!!} must implement OnLeagueDialogInteractionListener")
        delegate = context
    }

    /** @Override */
    override fun onDetach() {
        super.onDetach()
        delegate = null
    }

    /**
     * Add tabs to the tab layout.
     *
     * @param tabLayout to create and add tabs
     */
    abstract fun addTabs(tabLayout: TabLayout)

    /**
     * Create an instance of [BaseFragmentPagerAdapter] to manage the fragment instances for each tab.
     *
     * @param tabCount number of tabs
     */
    abstract fun buildPagerAdapter(tabCount: Int): BaseFragmentPagerAdapter

    /**
     * Configure toolbar for rendering.
     *
     * @param rootView the root view of the fragment
     */
    private fun configureToolbar(rootView: View) {
        // TODO: configure toolbar for tabbed fragment
    }

    /**
     * Configure tab layout for rendering.
     *
     * @param rootView the root view of the fragment
     */
    private fun configureTabLayout(rootView: View) {
        addTabs(rootView.tabbed_fragment_tabs)
        rootView.tabbed_fragment_pager.scrollingEnabled = false

        val adapter = buildPagerAdapter(rootView.tabbed_fragment_tabs.tabCount)
        rootView.tabbed_fragment_pager.adapter = adapter

        rootView.tabbed_fragment_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(rootView.tabbed_fragment_tabs))
        rootView.tabbed_fragment_tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                rootView.tabbed_fragment_pager.currentItem = tab.position
                delegate?.onTabSwitched()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    /**
     * Refresh lists in all tabs.
     *
     * @param ignored fragment tags to ignore
     */
    fun refreshTabs(ignored: Set<Int> = HashSet()) {
        // TODO: figure out how to use ignored
        val adapter = tabbed_fragment_pager.adapter as? BaseFragmentPagerAdapter
        adapter?.let {
            for (i in 0 until it.count) {
                if (ignored.contains(i)) {
                    continue
                }

                val fragment = it.getFragment(i) as? IRefreshable
                fragment?.refresh()
            }
        }
    }

    abstract class BaseFragmentPagerAdapter(
            fragmentManager: FragmentManager,
            private val tabCount: Int): FragmentPagerAdapter(fragmentManager) {

        /** Weak references to the fragments in the pager. */
        private val fragmentReferenceMap: MutableMap<Int, WeakReference<Fragment>> = HashMap(tabCount)

        /** @Override */
        final override fun getItem(position: Int): Fragment? {
            val fragment = buildFragment(position) ?: return null
            fragmentReferenceMap[position] = WeakReference(fragment)
            return fragment
        }

        /** @Override. */
        final override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            super.destroyItem(container, position, `object`)
            fragmentReferenceMap.remove(position)
        }

        /** @Override. */
        override fun getCount(): Int {
            return tabCount
        }

        /**
         * Get a reference to a fragment in the pager.
         *
         * @param position the fragment to get
         * @return the fragment at [position]
         */
        fun getFragment(position: Int): Fragment? {
            return fragmentReferenceMap[position]?.get()
        }

        /**
         * Build the fragment for the position in the pager.
         *
         * @param position position of fragment
         * @return the fragment, or null if the position is invalid
         */
        abstract fun buildFragment(position: Int): Fragment?
    }

    /**
     * Delegate for [TabbedFragment] events.
     */
    interface TabbedFragmentDelegate {

        /**
         * Invoked when the current tab changes.
         */
        fun onTabSwitched()
    }
}


