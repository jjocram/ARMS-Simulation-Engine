package webService

import io.ktor.server.application.Application
import io.ktor.server.application.log

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("ARMS version: 1.2.a")
    configureSerialization()
    configureRouting()
}