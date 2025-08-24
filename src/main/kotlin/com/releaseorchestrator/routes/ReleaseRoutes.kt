package com.releaseorchestrator.routes

import com.releaseorchestrator.models.ReleaseRequest
import com.releaseorchestrator.models.ReleaseResponse
import com.releaseorchestrator.services.ReleaseOrchestratorService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.launch

fun Route.releaseRoutes() {
    val releaseService = ReleaseOrchestratorService()
    
    post("/api/release/trigger") {
        try {
            val request = call.receive<ReleaseRequest>()
            
            // Запускаем асинхронно процесс релиза
            launch {
                releaseService.processRelease(request)
            }
            
            // Сразу возвращаем ответ о том, что пайплайн запущен
            call.respond(
                status = HttpStatusCode.Accepted,
                message = ReleaseResponse(
                    success = true,
                    message = "Release pipeline started",
                    version = null,
                    pipelineId = System.currentTimeMillis().toString()
                )
            )
        } catch (e: Exception) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ReleaseResponse(
                    success = false,
                    message = "Failed to start release pipeline: ${e.message}",
                    version = null,
                    pipelineId = null
                )
            )
        }
    }
    
    post("/api/release/trigger/sync") {
        try {
            val request = call.receive<ReleaseRequest>()
            
            // Синхронный запуск релиза (для тестирования)
            val result = releaseService.processReleaseSync(request)
            
            call.respond(
                status = HttpStatusCode.OK,
                message = result
            )
        } catch (e: Exception) {
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ReleaseResponse(
                    success = false,
                    message = "Failed to process release: ${e.message}",
                    version = null,
                    pipelineId = null
                )
            )
        }
    }
}
