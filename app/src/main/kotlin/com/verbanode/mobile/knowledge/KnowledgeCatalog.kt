package com.verbanode.mobile.knowledge

enum class KnowledgeDocumentScope {
    ALL,
    LEGACY,
    CURRENT,
    SELECTED_LIBRARY,
}

data class KnowledgeDocumentRef(
    val id: Int,
    val libraryId: Int,
    val sourceType: String,
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
