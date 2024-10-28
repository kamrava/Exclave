package io.nekohasekai.sagernet.vpn.repositories

import android.annotation.SuppressLint


@SuppressLint("StaticFieldLeak")
object PremiumServicesRepository {

    val goldenServiceItems = listOf(
        "Golden (unlimited traffic) - 2 users",
        "Golden (unlimited traffic) - 3 users",
        "Golden (unlimited traffic) - 4 users"
    )

    val titaniumServiceItems = listOf(
        "Titanium - 2 users",
        "Titanium - 3 users",
        "Titanium - 4 users"
    )

    val goldenPrices = mapOf(
        goldenServiceItems[0] to "1.5 $",
        goldenServiceItems[1] to "2 $",
        goldenServiceItems[2] to "3 $"
    )

    val titaniumPrices = mapOf(
        titaniumServiceItems[0] to "4 $",
        titaniumServiceItems[1] to "5 $",
        titaniumServiceItems[2] to "6 $"
    )
}
