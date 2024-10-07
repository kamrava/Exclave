package io.nekohasekai.sagernet.vpn.di

import org.koin.dsl.module
import io.nekohasekai.sagernet.vpn.models.*

val appModule = module {

    // Define dependencies here
    single {
        listOf(
            Service(0, 0, "", 0, "", "", "", "", "", "", "", "", "","", "", "", "", "", "")
        )
    }

    single {
        UserData(
            uid = 0,
            username = "",
            email = "",
            money = "0",
            services = get(),
            count = 2,
            xmp_token = null,
            token = null
        )
    }

    factory { InfoApiResponse(status = "success", code = 200, data = get()) }
}