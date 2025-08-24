package com.releaseorchestrator.models

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseRequest(
    val repository: String,
    val branch: String = "main",
    val forceVersion: String? = null
)

@Serializable
data class ReleaseResponse(
    val success: Boolean,
    val message: String,
    val version: String?,
    val pipelineId: String?
)

@Serializable
data class Commit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String
)

@Serializable
data class ReleaseInfo(
    val version: String,
    val releaseNotes: String,
    val commits: List<Commit>
)
