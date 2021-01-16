package org.openobd2.core.logger.ui.livedata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R


class LiveDataFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_livedata, container, false)

        var data: MutableList<CommandReply<*>> = arrayListOf()
        val adapter = LiveDataViewAdapter(root.context, data)

        Model.liveData.observe(viewLifecycleOwner, Observer {
            val indexOf = data.indexOf(it);
            if (indexOf == -1) {
                data.add(it);
                adapter.notifyDataSetChanged()
            } else {
                data.set(indexOf, it)
                adapter.notifyItemChanged(indexOf, it)
            }
        })

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        return root
    }
}