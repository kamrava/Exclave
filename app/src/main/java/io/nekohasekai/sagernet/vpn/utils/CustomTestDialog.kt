package io.nekohasekai.sagernet.vpn.utils

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.databinding.LayoutProfileBinding
import io.nekohasekai.sagernet.databinding.LayoutProgressListBinding
import io.nekohasekai.sagernet.ktx.FixedLinearLayoutManager
import io.nekohasekai.sagernet.ktx.alert
import io.nekohasekai.sagernet.ktx.getColorAttr
import io.nekohasekai.sagernet.ktx.getColour
import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask
import android.view.LayoutInflater

class CustomTestDialog(val context: Context) {
    val inflater: LayoutInflater = LayoutInflater.from(context)
    val binding = LayoutProgressListBinding.inflate(inflater)
    val builder = MaterialAlertDialogBuilder(binding.root.context).setView(binding.root)
        .setNegativeButton(android.R.string.cancel) { _, _ ->
            close()
            cancel()
        }
        .setCancelable(false)
    lateinit var cancel: () -> Unit
    val results = ArrayList<ProxyEntity>()
    val adapter = TestAdapter()
    val scrollTimer = Timer("insert timer")
    var currentTask: TimerTask? = null

    fun insert(profile: ProxyEntity) {
        binding.listView.post {
            results.add(profile)
            val index = results.size - 1
            adapter.notifyItemInserted(index)
            try {
                scrollTimer.schedule(timerTask {
                    binding.listView.post {
                        if (currentTask == this) binding.listView.smoothScrollToPosition(index)
                    }
                }.also {
                    currentTask?.cancel()
                    currentTask = it
                }, 500L)
            } catch (ignored: Exception) {
            }
        }
    }

    fun update(profile: ProxyEntity) {
        binding.listView.post {
            val index = results.indexOf(profile)
            adapter.notifyItemChanged(index)
        }
    }

    fun close() {
        try {
            scrollTimer.schedule(timerTask {
                scrollTimer.cancel()
            }, 0)
        } catch (ignored: Exception) {
        }
    }

    init {
        binding.listView.layoutManager = FixedLinearLayoutManager(binding.listView)
        binding.listView.itemAnimator = DefaultItemAnimator()
        binding.listView.adapter = adapter
    }

    inner class TestAdapter : RecyclerView.Adapter<TestResultHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TestResultHolder(LayoutProfileBinding.inflate(inflater, parent, false), context)
        override fun onBindViewHolder(holder: TestResultHolder, position: Int) {
            holder.bind(results[position])
        }

        override fun getItemCount() = results.size
    }

    inner class TestResultHolder(val binding: LayoutProfileBinding, val context: Context) : RecyclerView.ViewHolder(
        binding.root
    ) {
        init {
            binding.edit.isGone = true
            binding.share.isGone = true
            binding.deleteIcon.isGone = true
        }

        fun bind(profile: ProxyEntity) {
            binding.profileName.text = profile.displayName()
            binding.profileType.text = profile.displayType()

            when (profile.status) {
                -1 -> {
                    binding.profileStatus.text = profile.error

                    binding.profileStatus.setTextColor(binding.root.context.getColorAttr(android.R.attr.textColorSecondary))
                }
                0 -> {
                    binding.profileStatus.setText(R.string.connection_test_testing)
                    binding.profileStatus.setTextColor(binding.root.context.getColorAttr(android.R.attr.textColorSecondary))
                }
                1 -> {
                    binding.profileStatus.text = context.getString(R.string.available, profile.ping)
                    binding.profileStatus.setTextColor(binding.root.context.getColour(R.color.material_green_500))
                }
                2 -> {
                    binding.profileStatus.text = profile.error
                    binding.profileStatus.setTextColor(binding.root.context.getColour(R.color.material_red_500))
                }
                3 -> {
                    binding.profileStatus.setText(R.string.unavailable)
                    binding.profileStatus.setTextColor(binding.root.context.getColour(R.color.material_red_500))
                }
            }

            if (profile.status == 3) {
                binding.content.setOnClickListener {
                    context.alert(profile.error ?: "<?>").show()
                }
            } else {
                binding.content.setOnClickListener {}
            }
        }
    }
}