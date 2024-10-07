package io.nekohasekai.sagernet.vpn.serverlist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.vpn.repositories.AppRepository

class MyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_list, container, false)
        AppRepository.recyclerView = rootView.findViewById(R.id.recyclerView)
        AppRepository.recyclerView.layoutManager = LinearLayoutManager(activity)
        AppRepository.refreshServersListView()
        return rootView
    }
}
