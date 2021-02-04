package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.DataLoggerService
import org.openobd2.core.logger.bl.ModelChangePublisher
import org.openobd2.core.logger.ui.preferences.DashItemPreferences
import org.openobd2.core.logger.ui.preferences.Preferences
import javax.inject.Inject


internal class DragManageAdapter(ctx: Context,adapter: DashViewAdapter,dragDirs: Int, swipeDirs: Int) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
    private var dashViewAdapter = adapter
    private var context = ctx

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        dashViewAdapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        DashItemPreferences.store(context,dashViewAdapter.mData)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

}

@AndroidEntryPoint
class DashFragment : Fragment() {

    lateinit var root: View
    @Inject lateinit var dataLogger: DataLogger

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root.let {
            setupRecyclerView()
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        root = inflater.inflate(R.layout.fragment_dash, container, false)
        setupRecyclerView()
        return root
    }


    private fun setupRecyclerView() {
        val selectedPids = Preferences.getStringSet(requireContext(), "pref.dash.pids.selected")
        val data = this.dataLogger.buildMetricsBy(selectedPids)

        val dashItemPositionMap = DashItemPreferences.asPositionMap(requireContext())

        val dashItemPositionComparator = Comparator { m1: Metric<*>, m2: Metric<*> ->
            if (dashItemPositionMap.containsKey(m1.command.query)) {
                dashItemPositionMap[m1.command.query]!!
                    .compareTo(dashItemPositionMap[m2.command.query]!!)
            }else{
                -1
            }
        }

        data.sortWith(dashItemPositionComparator)

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
        recyclerView.adapter = adapter

        val callback = DragManageAdapter(
            requireContext(),
            adapter,
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END
        )
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)

        adapter.notifyDataSetChanged()

        ModelChangePublisher.liveData.observe(viewLifecycleOwner, Observer {
            if (selectedPids.contains((it.command as ObdCommand).pid.pid)) {
                val indexOf = data.indexOf(it)
                if (indexOf == -1) {
                    data.add(it)
                    adapter.notifyItemInserted(data.indexOf(it))
                } else {
                    data[indexOf] = it
                    adapter.notifyItemChanged(indexOf, it)
                }
            }
        })
        recyclerView.refreshDrawableState()
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    }
}