package org.openobd2.core.logger.ui.gauge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.SelectedPids
import org.openobd2.core.logger.bl.ModelChangePublisher

class GaugeViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var (selectedPids, data: MutableList<Metric<*>>) = SelectedPids.get(
            requireContext(),
            "pref.gauge.pids.selected"
        )

        val root = inflater.inflate(R.layout.fragment_gauge, container, false)
        val adapter = GaugeViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 2)
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        ModelChangePublisher.liveData.observe(viewLifecycleOwner, Observer {
            selectedPids.contains((it.command as ObdCommand).pid.pid).apply {
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
            }
        })
        return root
    }
}