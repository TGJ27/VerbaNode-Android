package com.verbanode.mobile.knowledge

import kotlin.math.roundToInt

enum class KnowledgeDocumentScope {
    ALL,
    LEGACY,
    CURRENT,
    SELECTED_LIBRARY,
}

enum class KnowledgeStatusFilter {
    ALL,
    READY,
    PROCESSING,
    ERROR,
}

enum class KnowledgeSourceFilter {
    ALL,
    LEGACY,
    TEXT,
    FILE,
}

data class KnowledgeDocumentRef(
    val id: Int,
    val libraryId: Int,
    val sourceType: String,
    val title: String = "",
    val sourceName: String = "",
    val status: String = "",
)

data class KnowledgeIngestionJobRef(
    val id: Int,
    val documentId: Int,
    val jobType: String,
    val status: String,
    val stage: String,
    val progress: Double,
    val error: String?,
)

data class KnowledgeOverviewCounts(
    val total: Int,
    val legacy: Int,
    val current: Int,
    val selected: Int,
)

fun isLegacyKnowledgeSource(sourceType: String): Boolean =
    sourceType.trim().equals("legacy_information", ignoreCase = true)

fun knowledgeSourceLabel(sourceType: String): String {
    val normalized = sourceType.trim().lowercase()
    return when (normalized) {
        "legacy_information" -> "Legacy"
        "packaged_default" -> "Built-in"
        "manual_text" -> "Text"
        "pdf", "docx", "xlsx", "pptx", "txt", "md", "csv", "html", "htm", "json" -> normalized.uppercase()
        "" -> "Source"
        else -> "File"
    }
}

fun knowledgeMatchesScope(
    document: KnowledgeDocumentRef,
    scope: KnowledgeDocumentScope,
    selectedLibraryId: Int?,
): Boolean = when (scope) {
    KnowledgeDocumentScope.ALL -> true
    KnowledgeDocumentScope.LEGACY -> isLegacyKnowledgeSource(document.sourceType)
    KnowledgeDocumentScope.CURRENT -> !isLegacyKnowledgeSource(document.sourceType)
    KnowledgeDocumentScope.SELECTED_LIBRARY -> selectedLibraryId != null && document.libraryId == selectedLibraryId
}

fun knowledgeMatchesQuery(document: KnowledgeDocumentRef, libraryName: String, query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return sequenceOf(document.title, document.sourceName, document.sourceType, libraryName)
        .map { it.lowercase() }
        .any { needle in it }
}

fun knowledgeMatchesStatus(document: KnowledgeDocumentRef, filter: KnowledgeStatusFilter): Boolean {
    if (filter == KnowledgeStatusFilter.ALL) return true
    return when (document.status.trim().lowercase()) {
        "ready", "parsed", "indexed", "complete", "completed" -> filter == KnowledgeStatusFilter.READY
        "registered", "queued", "parsing", "processing", "running", "indexing", "reindexing", "ingesting" -> filter == KnowledgeStatusFilter.PROCESSING
        "failed", "error", "invalid" -> filter == KnowledgeStatusFilter.ERROR
        else -> filter == KnowledgeStatusFilter.PROCESSING
    }
}

fun knowledgeMatchesSourceFilter(document: KnowledgeDocumentRef, filter: KnowledgeSourceFilter): Boolean = when (filter) {
    KnowledgeSourceFilter.ALL -> true
    KnowledgeSourceFilter.LEGACY -> isLegacyKnowledgeSource(document.sourceType)
    KnowledgeSourceFilter.TEXT -> document.sourceType.trim().lowercase() in setOf("manual_text", "packaged_default")
    KnowledgeSourceFilter.FILE -> !isLegacyKnowledgeSource(document.sourceType) && document.sourceType.trim().lowercase() !in setOf("manual_text", "packaged_default")
}

fun latestKnowledgeJob(documentId: Int, jobs: List<KnowledgeIngestionJobRef>): KnowledgeIngestionJobRef? =
    jobs.asSequence().filter { it.documentId == documentId }.maxByOrNull { it.id }

fun knowledgeJobProgressPercent(job: KnowledgeIngestionJobRef): Int =
    (job.progress.coerceIn(0.0, 1.0) * 100.0).roundToInt()

fun knowledgeOverviewCounts(
    documents: List<KnowledgeDocumentRef>,
    selectedLibraryId: Int?,
): KnowledgeOverviewCounts {
    val legacy = documents.count { isLegacyKnowledgeSource(it.sourceType) }
    return KnowledgeOverviewCounts(
        total = documents.size,
        legacy = legacy,
        current = documents.size - legacy,
        selected = if (selectedLibraryId == null) 0 else documents.count { it.libraryId == selectedLibraryId },
    )
}
