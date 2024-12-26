package io.nekohasekai.sagernet.vpn.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PremiumServicesRepository {

    var services: List<ServiceData> = listOf()

    suspend fun fetchServiceData() {
        withContext(Dispatchers.IO) {
            services = fetchFromServer() // Fetch the list of services dynamically from the server
        }
    }

    private suspend fun fetchFromServer(): List<ServiceData> {
        // Simulated data - replace this with a network call to fetch actual data
        return listOf(
            ServiceData(
                name = "Premium Service A",
                items = listOf("Plan A1", "Plan A2", "Plan A3"),
                prices = mapOf(
                    "Plan A1" to "1.0 $",
                    "Plan A2" to "2.0 $",
                    "Plan A3" to "3.0 $"
                )
            ),
            ServiceData(
                name = "Premium Service B",
                items = listOf("Plan B1", "Plan B2", "Plan B3"),
                prices = mapOf(
                    "Plan B1" to "4.0 $",
                    "Plan B2" to "5.0 $",
                    "Plan B3" to "6.0 $"
                )
            )
        )
    }

    data class ServiceData(
        val name: String,               // Service name (e.g., "Premium Service A")
        val items: List<String>,        // List of items/plans (e.g., ["Plan A1", "Plan A2"])
        val prices: Map<String, String> // Map of items to their prices (e.g., "Plan A1" to "1.0 $")
    )
}
