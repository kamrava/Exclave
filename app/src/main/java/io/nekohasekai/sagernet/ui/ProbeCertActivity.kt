package io.nekohasekai.sagernet.ui

import android.content.ClipData
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.databinding.LayoutProbeCertBinding
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.runOnDefaultDispatcher
import libcore.Libcore

class ProbeCertActivity : ThemedActivity() {

    private lateinit var binding: LayoutProbeCertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutProbeCertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.probe_cert)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.baseline_arrow_back_24)
        }

        binding.probeCert.setOnClickListener {
            copyCert()
        }
    }


    private fun copyCert() {
        binding.waitLayout.isVisible = true

        val server = binding.probeCertServer.text.toString()
        val serverName = binding.probeCertServerName.text.toString()
        val protocol: String = when (binding.probeCertProtocol.selectedItemPosition) {
            0 -> "tls"
            1 -> "quic"
            else -> error("unknown protocol")
        }

        runOnDefaultDispatcher {
            try {
                val certificate = Libcore.probeCert(server, serverName, protocol, DataStore.socksPort)
                Logs.i(certificate)

                val clipData = ClipData.newPlainText("Certificate", certificate)
                SagerNet.clipboard.setPrimaryClip(clipData)

                Snackbar.make(
                    binding.root,
                    R.string.probe_cert_success,
                    Snackbar.LENGTH_SHORT
                ).show()

                onMainDispatcher {
                    binding.waitLayout.isVisible = false
                }
            } catch (e: Exception) {
                Logs.w(e)
                onMainDispatcher {
                    binding.waitLayout.isVisible = false
                    AlertDialog.Builder(this@ProbeCertActivity)
                        .setTitle(R.string.error_title)
                        .setMessage(e.toString())
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .runCatching { show() }
                }
            }
        }
    }

}
