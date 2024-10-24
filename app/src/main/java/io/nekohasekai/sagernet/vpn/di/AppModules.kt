package io.nekohasekai.sagernet.vpn.di

import io.nekohasekai.sagernet.vpn.models.InfoApiResponse
import io.nekohasekai.sagernet.vpn.models.Service
import io.nekohasekai.sagernet.vpn.models.UserData
import org.koin.dsl.module

val appModule = module {

    // Define dependencies here
    single {
        listOf(
            Service(
                0,
                0,
                "",
                0,
                "",
                "",
                "",
                true,
                true,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            )
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