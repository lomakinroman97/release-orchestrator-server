package com.releaseorchestrator.services.versioning

import com.releaseorchestrator.services.github.GithubService
import org.slf4j.LoggerFactory

class VersioningService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val githubService = GithubService()
    
    suspend fun calculateNextVersion(repository: String, branch: String): String {
        val repoPath = repository.replace("https://github.com/", "")
        
        try {
            // Получаем последний тег
            val lastTag = getLastTag(repoPath)
            
            if (lastTag == null) {
                // Если тегов нет, начинаем с 1.0.0
                logger.info("No previous tags found, starting with 1.0.0")
                return "1.0.0"
            }
            
            // Парсим версию
            val versionParts = parseVersion(lastTag)
            if (versionParts == null) {
                logger.warn("Could not parse version from tag: $lastTag, starting with 1.0.0")
                return "1.0.0"
            }
            
            // Увеличиваем patch версию (по умолчанию)
            val nextVersion = "${versionParts.major}.${versionParts.minor}.${versionParts.patch + 1}"
            logger.info("Calculated next version: $nextVersion from previous: $lastTag")
            
            return nextVersion
        } catch (e: Exception) {
            logger.error("Failed to calculate next version", e)
            return "1.0.0"
        }
    }
    
    private suspend fun getLastTag(repoPath: String): String? {
        return try {
            val response = githubService.getLastTag(repoPath)
            response?.removePrefix("v")
        } catch (e: Exception) {
            logger.warn("Failed to get last tag: ${e.message}")
            null
        }
    }
    
    private fun parseVersion(version: String): VersionParts? {
        val regex = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)$")
        val matchResult = regex.find(version)
        
        return if (matchResult != null) {
            val (major, minor, patch) = matchResult.destructured
            VersionParts(
                major = major.toInt(),
                minor = minor.toInt(),
                patch = patch.toInt()
            )
        } else {
            null
        }
    }
    
    fun incrementMajor(version: String): String {
        val parts = parseVersion(version) ?: return "1.0.0"
        return "${parts.major + 1}.0.0"
    }
    
    fun incrementMinor(version: String): String {
        val parts = parseVersion(version) ?: return "1.0.0"
        return "${parts.major}.${parts.minor + 1}.0"
    }
    
    fun incrementPatch(version: String): String {
        val parts = parseVersion(version) ?: return "1.0.0"
        return "${parts.major}.${parts.minor}.${parts.patch + 1}"
    }
    
    private data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int
    )
}
