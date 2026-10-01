package com.pension.alchemy

import com.pension.alchemy.domain.engine.SimulationEngine
import com.pension.alchemy.util.SampleDataGenerator
import com.pension.alchemy.util.SimulationSpreadsheetExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulationSpreadsheetExporterTest {

    @Test
    fun generateCsvContent_createsValidUtf8BomCsvWithAllMetrics() {
        val profile = SampleDataGenerator.createDefaultProfile()
        val pensions = SampleDataGenerator.createDefaultPensions()
        val assets = SampleDataGenerator.createDefaultAssets()
        val incomes = SampleDataGenerator.createDefaultIncomes()

        val summary = SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)

        val csv = SimulationSpreadsheetExporter.generateCsvContent(summary)

        // 1. UTF-8 BOM 검증 (\uFEFF)
        assertTrue("CSV의 첫 문자는 반드시 UTF-8 BOM이어야 합니다", csv.startsWith("\uFEFF"))

        // 2. 메타데이터 요약 검증
        assertTrue(csv.contains("연금연금술(PensionAlchemy) 생애 순자산 시뮬레이션 데이터 리포트"))
        assertTrue(csv.contains("${summary.currentAge}세"))
        assertTrue(csv.contains("${summary.retirementAge}세"))
        assertTrue(csv.contains("은퇴 진단 결과"))

        // 3. 27개 컬럼 헤더 포함 검증
        val expectedHeaders = listOf(
            "나이(세)", "연도(년)", "생애단계", "소득크레바스",
            "순자산(원)", "총자산(원)", "총부채(원)", "금융자산(원)", "부동산자산(원)", "연금적립자산(원)",
            "월총소득(원)", "월근로사업소득(원)", "월실수령총연금(원)", "월세전총연금(원)", "월연금소득세(원)",
            "월국민연금(원)", "월퇴직연금(원)", "월개인연금(원)", "월개인연금보험(원)", "월주택연금(원)", "월기타연금(원)",
            "월생활비지출(원)", "월사적연금납입(원)", "월부채상환원리금(원)", "월순현금흐름(원)",
            "건보료피부양자탈락경고", "사적연금1500만초과경고"
        )
        expectedHeaders.forEach { header ->
            assertTrue("헤더 '$header'가 CSV에 포함되어야 합니다", csv.contains("\"$header\""))
        }

        // 4. 연도별 데이터 행 수 검증
        val lines = csv.lines().filter { it.isNotBlank() }
        val dataRows = lines.filter { line ->
            val firstToken = line.split(",").firstOrNull()?.replace("\"", "")
            firstToken?.toIntOrNull() != null
        }
        val expectedCount = summary.yearlyResults.size
        assertEquals("시뮬레이션 연령 수와 동일한 데이터 행이 생성되어야 합니다", expectedCount, dataRows.size)
    }
}
