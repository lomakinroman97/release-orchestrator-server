package com.releaseorchestrator.services.github

import com.releaseorchestrator.models.Commit
import com.releaseorchestrator.models.ReleaseInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class GithubService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com"
        private val GITHUB_TOKEN = System.getenv("GITHUB_TOKEN") ?: throw IllegalStateException("GITHUB_TOKEN environment variable is not set")
    }
    
    suspend fun getNewCommits(repository: String, branch: String): List<Commit> {
        val repoPath = repository.replace("https://github.com/", "")
        logger.info("Getting commits for repo: $repoPath, branch: $branch")
        
        // Получаем последний тег
        val lastTag = getLastTag(repoPath)
        logger.info("Last tag: $lastTag")
        
        // Получаем коммиты с момента последнего тега
        val commits = if (lastTag != null) {
            logger.info("Getting commits since tag: $lastTag")
            getCommitsSinceTag(repoPath, lastTag)
        } else {
            logger.info("Getting commits from branch: $branch")
            getCommitsFromBranch(repoPath, branch)
        }
        
        logger.info("Raw GitHub commits count: ${commits.size}")
        
        // Логируем детали каждого коммита
        commits.forEachIndexed { index, githubCommit ->
            logger.info("Commit $index: SHA=${githubCommit.sha}, Message='${githubCommit.commit.message}', Author=${githubCommit.commit.author.name}, Date=${githubCommit.commit.author.date}")
        }
        
        val result = commits.map { githubCommit ->
            Commit(
                sha = githubCommit.sha.substring(0, 8),
                message = githubCommit.commit.message,
                author = githubCommit.commit.author.name,
                date = githubCommit.commit.author.date
            )
        }
        
        logger.info("Processed commits count: ${result.size}")
        return result
    }
    
    suspend fun createRelease(repository: String, releaseInfo: ReleaseInfo) {
        val repoPath = repository.replace("https://github.com/", "")
        logger.info("Creating GitHub release for repo: $repoPath, version: ${releaseInfo.version}")
        
        val requestBody = GithubReleaseRequest(
            tag_name = "v${releaseInfo.version}",
            name = "Release v${releaseInfo.version}",
            body = releaseInfo.releaseNotes,
            draft = false,
            prerelease = false
        )
        
        logger.info("Release request body: $requestBody")
        
        val response = client.post("$GITHUB_API_BASE/repos/$repoPath/releases") {
            header("Authorization", "token $GITHUB_TOKEN")
            header("Accept", "application/vnd.github.v3+json")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        if (response.status.isSuccess()) {
            logger.info("Successfully created GitHub release v${releaseInfo.version}")
        } else {
            val errorBody = response.body<String>()
            logger.error("GitHub API error: ${response.status}, body: $errorBody")
            throw Exception("Failed to create GitHub release: ${response.status} - $errorBody")
        }
    }
    
    suspend fun getLastTag(repoPath: String): String? {
        return try {
            val response = client.get("$GITHUB_API_BASE/repos/$repoPath/tags") {
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
            }
            
            if (response.status.isSuccess()) {
                val tags: List<GithubTag> = response.body()
                tags.firstOrNull()?.name?.removePrefix("v")
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to get last tag: ${e.message}")
            null
        }
    }
    
    private suspend fun getCommitsSinceTag(repoPath: String, tag: String): List<GithubCommit> {
        return try {
            val response = client.get("$GITHUB_API_BASE/repos/$repoPath/compare/v$tag...main") {
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
            }
            
            if (response.status.isSuccess()) {
                val compareResult: GithubCompareResult = response.body()
                compareResult.commits
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to get commits since tag: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun getCommitsFromBranch(repoPath: String, branch: String): List<GithubCommit> {
        return try {
            val response = client.get("$GITHUB_API_BASE/repos/$repoPath/commits") {
                header("Authorization", "token $GITHUB_TOKEN")
                header("Accept", "application/vnd.github.v3+json")
                parameter("sha", branch)
                parameter("per_page", 50)
            }
            
            if (response.status.isSuccess()) {
                response.body()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.warn("Failed to get commits from branch: ${e.message}")
            emptyList()
        }
    }
}

@Serializable
data class GithubCommit(
    val sha: String,
    val commit: GithubCommitDetails
)

@Serializable
data class GithubCommitDetails(
    val message: String,
    val author: GithubAuthor
)

@Serializable
data class GithubAuthor(
    val name: String,
    val date: String
)

@Serializable
data class GithubTag(
    val name: String
)

@Serializable
data class GithubCompareResult(
    val commits: List<GithubCommit>
)

@Serializable
data class GithubReleaseRequest(
    val tag_name: String,
    val name: String,
    val body: String,
    val draft: Boolean,
    val prerelease: Boolean
)
