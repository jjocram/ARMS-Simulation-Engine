package webService

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.application.log

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("ARMS version: 1.3.a")
    install(CORS) {
        anyHost()

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)

        // 3. Allow headers your client might send (like Content-Type, Authorization, etc.)
        allowHeaders { true } // Be specific in a real production app (e.g., allowHeader(HttpHeaders.ContentType))
    }
    configureSerialization()
    configureRouting()
}