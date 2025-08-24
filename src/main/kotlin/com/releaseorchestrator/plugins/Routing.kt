package com.releaseorchestrator.plugins

import com.releaseorchestrator.routes.releaseRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        releaseRoutes()
    }
}
