package com.verbanode.mobile.knowledge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeCatalogTest {
    private val documents = listOf(
        KnowledgeDocumentRef(1, 10, "legacy_information"),
        KnowledgeDocumentRef(2, 10, "manual_text"),
        KnowledgeDocumentRef(3, 20, "pdf"),
        KnowledgeDocumentRef(4, 30, "legacy_information"),
    )

    @Test
    fun legacySourcesAreSeparatedFromCurrentKnowledge() {
        assertTrue(isLegacyKnowledgeSource("legacy_information"))
        assertFalse(isLegacyKnowledgeSource("manual_text"))
        assertEquals("Legacy", knowledgeSourceLabel("legacy_information"))
        assertEquals("Built-in", knowledgeSourceLabel("packaged_default"))
        assertEquals("Text", knowledgeSourceLabel("manual_text"))
        assertEquals("PDF", knowledgeSourceLabel("pdf"))
    }

    @Test
    fun overviewCountsAllLegacyCurrentAndSelectedSources() {
        val counts = knowledgeOverviewCounts(documents, selectedLibraryId = 10)
        assertEquals(4, counts.total)
        assertEquals(2, counts.legacy)
        assertEquals(2, counts.current)
        assertEquals(2, counts.selected)
    }

    @Test
    fun scopesExposeLegacyAcrossLibrariesWithoutLosingSelectedLibraryView() {
        assertEquals(4, documents.count { knowledgeMatchesScope(it, KnowledgeDocumentScope.ALL, 10) })
        assertEquals(2, documents.count { knowledgeMatchesScope(it, KnowledgeDocumentScope.LEGACY, 10) })
        assertEquals(2, documents.count { knowledgeMatchesScope(it, KnowledgeDocumentScope.CURRENT, 10) })
        assertEquals(2, documents.count { knowledgeMatchesScope(it, KnowledgeDocumentScope.SELECTED_LIBRARY, 10) })
        assertEquals(0, documents.count { knowledgeMatchesScope(it, KnowledgeDocumentScope.SELECTED_LIBRARY, null) })
    }
    @Test
    fun phase2FiltersSearchStatusSourceAndJobProgress() {
        val refund = KnowledgeDocumentRef(5, 10, "manual_text", "Refund Policy", "Refund Policy", "ready")
        val manual = KnowledgeDocumentRef(6, 20, "pdf", "Product Manual", "manual.pdf", "processing")
        val failed = KnowledgeDocumentRef(7, 20, "docx", "Broken Import", "broken.docx", "failed")

        assertTrue(knowledgeMatchesQuery(refund, "Company", "refund"))
        assertTrue(knowledgeMatchesQuery(manual, "Docs", "manual.pdf"))
        assertTrue(knowledgeMatchesStatus(refund, KnowledgeStatusFilter.READY))
        assertTrue(knowledgeMatchesStatus(KnowledgeDocumentRef(8, 10, "pdf", "Parsed", "parsed.pdf", "parsed"), KnowledgeStatusFilter.READY))
        assertTrue(knowledgeMatchesStatus(manual, KnowledgeStatusFilter.PROCESSING))
        assertTrue(knowledgeMatchesStatus(failed, KnowledgeStatusFilter.ERROR))
        assertTrue(knowledgeMatchesSourceFilter(refund, KnowledgeSourceFilter.TEXT))
        assertTrue(knowledgeMatchesSourceFilter(manual, KnowledgeSourceFilter.FILE))

        val jobs = listOf(
            KnowledgeIngestionJobRef(11, 6, "ingest", "running", "extract", 0.42, null),
            KnowledgeIngestionJobRef(12, 6, "reingest", "queued", "queued", 0.0, null),
        )
        assertEquals(12, latestKnowledgeJob(6, jobs)?.id)
        assertEquals(42, knowledgeJobProgressPercent(jobs.first()))
    }

}
