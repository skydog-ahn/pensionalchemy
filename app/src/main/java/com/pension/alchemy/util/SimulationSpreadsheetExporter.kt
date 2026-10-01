package com.pension.alchemy.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.pension.alchemy.data.model.SimulationSummary
import com.pension.alchemy.data.model.YearlySimulationResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 생애 순자산 시뮬레이션의 전체 결과 데이터를
 * Google Sheets 및 Microsoft Excel에서 한글 깨짐 없이 완벽하게 열 수 있는
 * UTF-8 BOM CSV 테이블 데이터로 생성하고 열람/다운로드/공유할 수 있게 해주는 유틸리티
 */
object SimulationSpreadsheetExporter {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /**
     * Google Sheets / Excel 호환 UTF-8 BOM CSV 문자열 생성
     */
    fun generateCsvContent(summary: SimulationSummary): String {
        val sb = StringBuilder()

        // 1. UTF-8 BOM 추가 (Excel 및 Google Sheets에서 한글 깨짐 원천 방지)
        sb.append("\uFEFF")

        val nowStr = LocalDateTime.now().format(DATE_FORMATTER)

        // 2. 상단 메타데이터 요약 리포트 섹션
        sb.appendLine("\"# 연금연금술(PensionAlchemy) 생애 순자산 시뮬레이션 데이터 리포트\"")
        sb.appendLine("\"리포트 생성 일시\",\"$nowStr\"")
        sb.appendLine("\"현재 나이\",\"${summary.currentAge}세\",\"은퇴 예정 나이\",\"${summary.retirementAge}세\",\"국민연금 개시 나이\",\"${summary.nationalPensionStartAge}세\",\"목표 수명 나이\",\"${summary.endAge}세\"")
        sb.appendLine("\"현재 순자산\",\"${summary.currentNetWorth}\",\"현재 총자산\",\"${summary.currentTotalAssets}\",\"현재 총부채\",\"${summary.currentTotalDebt}\"")
        sb.appendLine("\"현재 금융자산\",\"${summary.currentFinancialAssets}\",\"현재 부동산자산\",\"${summary.currentRealEstateAssets}\",\"현재 연금자산\",\"${summary.currentPensionAssets}\"")

        val safeRetireText = if (summary.isSafeRetirement) "안전 은퇴 (100세까지 자산 유지)" else "자산 조기 고갈 (${summary.depletionAge}세)"
        sb.appendLine("\"은퇴 진단 결과\",\"$safeRetireText\",\"자산 고갈 나이\",${if (summary.depletionAge > 0) "\"${summary.depletionAge}세\"" else "\"고갈 없음\""},\"최고 자산 달성\",${if (summary.peakAssetAge > 0) "\"${summary.peakAssetAge}세 (${summary.peakAssetValue}원)\"" else "\"-\""}")

        val crevasse = summary.crevasseInfo
        val crevasseText = if (crevasse.hasCrevasse) "소득 크레바스 ${crevasse.durationYears}년 (${crevasse.startAge}세~${crevasse.endAge}세), 월 부족액 ${crevasse.monthlyShortfall}원, 필요 브릿지 자금 ${crevasse.totalRequiredBridgeFund}원" else "소득 크레바스 없음"
        sb.appendLine("\"소득 크레바스 분석\",\"$crevasseText\"")
        sb.appendLine("\"은퇴 소득대체율\",\"${String.format("%.1f", summary.incomeReplacementRate)}%\",\"은퇴 건강도 점수\",\"${summary.healthScore.score}점 (${summary.healthScore.grade})\"")
        sb.appendLine()

        // 3. 연도별 시뮬레이션 상세 테이블 헤더 (27개 핵심 컬럼)
        val headers = listOf(
            "나이(세)",
            "연도(년)",
            "생애단계",
            "소득크레바스",
            "순자산(원)",
            "총자산(원)",
            "총부채(원)",
            "금융자산(원)",
            "부동산자산(원)",
            "연금적립자산(원)",
            "월총소득(원)",
            "월근로사업소득(원)",
            "월실수령총연금(원)",
            "월세전총연금(원)",
            "월연금소득세(원)",
            "월국민연금(원)",
            "월퇴직연금(원)",
            "월개인연금(원)",
            "월개인연금보험(원)",
            "월주택연금(원)",
            "월기타연금(원)",
            "월생활비지출(원)",
            "월사적연금납입(원)",
            "월부채상환원리금(원)",
            "월순현금흐름(원)",
            "건보료피부양자탈락경고",
            "사적연금1500만초과경고"
        )
        sb.appendLine(headers.joinToString(",") { "\"$it\"" })

        // 4. 연도별 데이터 행 채우기
        summary.yearlyResults.forEach { r ->
            val stageName = r.stage
            val crevasseStatus = if (r.isCrevasse) "크레바스 발생" else "정상"
            val healthWarning = if (r.isHealthInsuranceDisqualified) "위험(피부양자 탈락)" else "정상"
            val privatePensionWarning = if (r.isPrivatePensionLimitExceeded) "주의(연 1500만 초과 종합과세 대상)" else "정상"

            val rowValues = listOf(
                r.age.toString(),
                r.year.toString(),
                "\"$stageName\"",
                "\"$crevasseStatus\"",
                r.netAssetValue.toString(),
                r.totalGrossAssets.toString(),
                r.totalDebt.toString(),
                r.financialAssets.toString(),
                r.realEstateAssets.toString(),
                r.pensionAssets.toString(),
                r.monthlyTotalIncome.toString(),
                r.monthlyWorkIncome.toString(),
                r.monthlyPensionIncome.toString(),
                r.grossMonthlyPensionIncome.toString(),
                r.monthlyPensionTax.toString(),
                r.monthlyNationalPension.toString(),
                r.monthlyRetirementPension.toString(),
                r.monthlyPersonalPension.toString(),
                r.monthlyAnnuityInsurancePension.toString(),
                r.monthlyHousingPension.toString(),
                r.monthlyOtherPension.toString(),
                r.monthlyExpenses.toString(),
                r.monthlyPensionContribution.toString(),
                r.monthlyDebtService.toString(),
                r.monthlyNetCashFlow.toString(),
                "\"$healthWarning\"",
                "\"$privatePensionWarning\""
            )
            sb.appendLine(rowValues.joinToString(","))
        }

        return sb.toString()
    }

    /**
     * 기본 저장 파일명 반환
     */
    fun getDefaultFileName(): String {
        val dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return "연금연금술_생애순자산시뮬레이션_$dateStr.csv"
    }

    /**
     * 사용자가 SAF 파일 피커로 지정한 위치(Uri)에 CSV 데이터 저장
     */
    fun writeCsvToUri(context: Context, uri: Uri, summary: SimulationSummary): Boolean {
        val csvContent = generateCsvContent(summary)
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(csvContent.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            Toast.makeText(context, "시뮬레이션 데이터가 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "파일 저장 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
