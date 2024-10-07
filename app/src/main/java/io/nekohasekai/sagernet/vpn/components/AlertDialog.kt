package io.nekohasekai.sagernet.vpn.components

import android.content.Context
import androidx.appcompat.app.AlertDialog

class ForceUpdateDialog(context: Context) {

    private val dialog: AlertDialog = AlertDialog.Builder(context)
        .setMessage("A new version of the app is available. Please update to continue.")
        .setPositiveButton("Update Now") { _, _ ->
            // Handle the update action here
        }
        .setCancelable(false)
        .create()

    init {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
