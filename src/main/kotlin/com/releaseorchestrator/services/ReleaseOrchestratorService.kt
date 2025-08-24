package com.releaseorchestrator.services

import com.releaseorchestrator.models.*
import com.releaseorchestrator.services.github.GithubService
import com.releaseorchestrator.services.llm.YandexGptService
import com.releaseorchestrator.services.versioning.VersioningService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class ReleaseOrchestratorService {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    private val githubService = GithubService()
    private val yandexGptService = YandexGptService()
    private val versioningService = VersioningService()
    
    suspend fun processRelease(request: ReleaseRequest): ReleaseResponse {
        return try {
            logger.info("Starting release pipeline for repository: ${request.repository}")
            
            // 1. Анализируем репозиторий через GitHub API
            val commits = githubService.getNewCommits(request.repository, request.branch)
            logger.info("Found ${commits.size} new commits")
            
            // 2. Определяем новую версию
            val newVersion = request.forceVersion ?: versioningService.calculateNextVersion(
                request.repository, 
                request.branch
            )
            logger.info("Calculated new version: $newVersion")
            
            // 3. Генерируем release notes через Yandex GPT
            val releaseNotes = yandexGptService.generateReleaseNotes(commits, newVersion)
            logger.info("Generated release notes")
            
            // 4. Создаем GitHub Release
            val releaseInfo = ReleaseInfo(newVersion, releaseNotes, commits)
            githubService.createRelease(request.repository, releaseInfo)
            logger.info("Created GitHub release: $newVersion")
            
            ReleaseResponse(
                success = true,
                message = "Release pipeline completed successfully",
                version = newVersion,
                pipelineId = System.currentTimeMillis().toString()
            )
        } catch (e: Exception) {
            logger.error("Release pipeline failed", e)
            ReleaseResponse(
                success = false,
                message = "Release pipeline failed: ${e.message}",
                version = null,
                pipelineId = System.currentTimeMillis().toString()
            )
        }
    }
    
    suspend fun processReleaseSync(request: ReleaseRequest): ReleaseResponse {
        return withContext(Dispatchers.IO) {
            processRelease(request)
        }
    }
}
