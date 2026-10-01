package com.pension.alchemy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.Asset
import com.pension.alchemy.data.model.Income
import com.pension.alchemy.data.model.Pension
import com.pension.alchemy.data.model.SimulationSummary
import com.pension.alchemy.data.model.UserProfile
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.*
import com.pension.alchemy.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun DashboardScreen(
    summary: SimulationSummary,
    profile: UserProfile = UserProfile(),
    assets: List<Asset> = emptyList(),
    pensions: List<Pension> = emptyList(),
    incomes: List<Income> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedAge by remember(summary.currentAge, summary.retirementAge, summary.endAge) {
        val initialAge = if (summary.currentAge < summary.retirementAge) summary.retirementAge else summary.currentAge
        mutableIntStateOf(initialAge.coerceIn(summary.currentAge, summary.endAge))
    }
    val safeAge = selectedAge.coerceIn(summary.currentAge, summary.endAge)
    val selectedYearResult = summary.yearlyResults.firstOrNull { it.age == safeAge }
        ?: summary.yearlyResults.firstOrNull()

    // 1초 단위 타이머
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            currentDateTime = LocalDateTime.now()
        }
    }

    // 기준일 기반 실시간 자산 및 순자산 초당 증가 연산
    val realTimeGrowth = remember(assets, pensions, incomes, currentDateTime) {
        RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(assets, pensions, incomes, currentDateTime)
    }

    // 실시간 순자산 및 총자산
    val displayNetWorth = if (assets.isNotEmpty() || pensions.isNotEmpty()) {
        realTimeGrowth.realTimeNetWorth
    } else {
        summary.currentNetWorth
    }

    val displayTotalAssets = if (assets.isNotEmpty() || pensions.isNotEmpty()) {
        realTimeGrowth.realTimeTotalGrossAssets
    } else {
        summary.currentTotalAssets
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 헤더: 앱 타이틀 & 순자산 메인 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "현재 순자산 (Net Worth)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "기준 나이: ${summary.currentAge}세",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyFormatter.formatKoreanWon(displayNetWorth),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
                if (realTimeGrowth.totalNetGain != 0L || realTimeGrowth.netWonPerSecond != 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(EmeraldPrimary, CircleShape)
                        )
                        val prefix = if (realTimeGrowth.totalNetGain >= 0) "+" else ""
                        Text(
                            text = "기준일 대비 $prefix${CurrencyFormatter.formatKoreanWon(realTimeGrowth.totalNetGain)} (초당 +${String.format(Locale.KOREA, "%,.2f", realTimeGrowth.netWonPerSecond)}원)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "총자산 ${CurrencyFormatter.formatKoreanWon(displayTotalAssets)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "총부채 ${CurrencyFormatter.formatKoreanWon(summary.currentTotalDebt, isShort = true)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 1-1. 실시간 초당 자산 수익 다이내믹 티커 카드
        RealTimeAssetGrowthTickerCard(
            metrics = summary.realTimeYield,
            baseDateAccumulatedGain = if (realTimeGrowth.totalNetGain != 0L) realTimeGrowth.totalNetGain else null
        )

        // 2. 은퇴 준비 건강도 점수 배너 (0 ~ 100점)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 점수 뱃지
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(summary.healthScore.gradeColorHex)).copy(alpha = 0.18f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${summary.healthScore.score}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(android.graphics.Color.parseColor(summary.healthScore.gradeColorHex))
                        )
                        Text(
                            text = summary.healthScore.grade.substringBefore(" "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(android.graphics.Color.parseColor(summary.healthScore.gradeColorHex))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "은퇴 준비 건강도: ${summary.healthScore.grade}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary.healthScore.feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. 소득 크레바스 경고 배너 (있을 경우)
        if (summary.crevasseInfo.hasCrevasse) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AmberWarning.copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "소득 크레바스(공백기) 주의: ${summary.crevasseInfo.startAge}세 ~ ${summary.crevasseInfo.endAge}세",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AmberWarning
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "은퇴 후 국민연금 개시까지 ${summary.crevasseInfo.durationYears}년간 월평균 ${CurrencyFormatter.formatKoreanWon(summary.crevasseInfo.monthlyShortfall, isShort = true)} 부족 (필요 브릿지자금: ${CurrencyFormatter.formatKoreanWon(summary.crevasseInfo.totalRequiredBridgeFund, isShort = true)})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. 핵심 2x2 지표 그리드
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "소득 대체율",
                value = CurrencyFormatter.formatPercent(summary.incomeReplacementRate),
                subtitle = "권장치: 50~60%",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "자산 고갈 시점",
                value = if (summary.isSafeRetirement) "100세+ 안전" else "${summary.depletionAge}세 고갈",
                subtitle = if (summary.isSafeRetirement) "평생 자산 유지" else "지출 조정 필요",
                icon = Icons.Default.Shield,
                accentColor = if (summary.isSafeRetirement) EmeraldPrimary else RoseDanger,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "65세 월 연금수령액",
                value = CurrencyFormatter.formatKoreanWon(summary.postRetirementMonthlyPension, isShort = true) + "/월",
                subtitle = "국민+퇴직+개인 합산",
                icon = Icons.Default.AccountBalance,
                accentColor = CyanInfo,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "피크 자산 (정점)",
                value = CurrencyFormatter.formatKoreanWon(summary.peakAssetValue, isShort = true),
                subtitle = "${summary.peakAssetAge}세 도달 예상",
                icon = Icons.Default.Star,
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
        }

        // 5. 생애 자산 궤적 차트
        AssetTrajectoryCanvasChart(
            results = summary.yearlyResults,
            selectedAge = selectedAge
        )

        // 6. 인터랙티브 나이 인스펙터 슬라이더 & 세부 분해 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "나이별 정밀 인스펙터",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${safeAge}세 (${selectedYearResult?.stage ?: ""})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val minSliderAge = summary.currentAge.toFloat()
                val maxSliderAge = (summary.endAge.toFloat()).coerceAtLeast(minSliderAge + 1f)
                Slider(
                    value = safeAge.toFloat().coerceIn(minSliderAge, maxSliderAge),
                    onValueChange = { selectedAge = it.toInt() },
                    valueRange = minSliderAge..maxSliderAge,
                    steps = (maxSliderAge.toInt() - minSliderAge.toInt() - 1).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (selectedYearResult != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.Gray.copy(alpha = 0.2f))

                    // 1) 메인 하이라이트: 생애 순자산
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "예상 순자산",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedYearResult.netAssetValue < 0L) "자산 소진 후 누적 결손" else "총자산 - 총부채",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selectedYearResult.netAssetValue < 0L) RoseDanger else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = CurrencyFormatter.formatKoreanWon(selectedYearResult.netAssetValue),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = if (selectedYearResult.netAssetValue < 0L) RoseDanger else MaterialTheme.colorScheme.onSurface,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2) 월간 현금흐름 밸런스 상세 (세로 행 리스트)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "월간 현금흐름 밸런스",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 월 총소득
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "월 총소득",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "+${CurrencyFormatter.formatKoreanWon(selectedYearResult.monthlyTotalIncome)}/월",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }

                            // 월 생활지출
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = RoseDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "월 생활지출",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "-${CurrencyFormatter.formatKoreanWon(selectedYearResult.monthlyExpenses)}/월",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDanger,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // 월 순현금흐름 (소득 - 지출)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (selectedYearResult.monthlyNetCashFlow >= 0) EmeraldPrimary else RoseDanger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "월 순현금흐름",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (selectedYearResult.monthlyNetCashFlow >= 0) "월 잉여 자금" else "월 부족(적자) 자금",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedYearResult.monthlyNetCashFlow >= 0) EmeraldPrimary else RoseDanger
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (selectedYearResult.monthlyNetCashFlow > 0) "+" else ""}${CurrencyFormatter.formatKoreanWon(selectedYearResult.monthlyNetCashFlow)}/월",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedYearResult.monthlyNetCashFlow >= 0) EmeraldPrimary else RoseDanger,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3) 연금 실수령 및 과세 분석 카드
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "연금 실수령액",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${CurrencyFormatter.formatKoreanWon(selectedYearResult.monthlyPensionIncome)}/월",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }

                            if (selectedYearResult.monthlyPensionTax > 0L) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• 연금 세전 총액",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatKoreanWon(selectedYearResult.grossMonthlyPensionIncome)}/월",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• 예상 원천징수 세금",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmberWarning
                                    )
                                    Text(
                                        text = "-${CurrencyFormatter.formatKoreanWon(selectedYearResult.monthlyPensionTax)}/월",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberWarning,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (selectedYearResult.isHealthInsuranceDisqualified || selectedYearResult.isPrivatePensionLimitExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedYearResult.isHealthInsuranceDisqualified) {
                                Surface(
                                    color = AmberWarning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "공적연금 연 2,000만원 초과: 건강보험 피부양자 자격 박탈(지역가입자 전환) 주의 구간입니다.",
                                            fontSize = 11.sp,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (selectedYearResult.isPrivatePensionLimitExceeded) {
                                Surface(
                                    color = CyanInfo.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = CyanInfo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "사적연금 연 1,500만원 초과: 초과분에 16.5% 분리과세가 적용됩니다. 수령 기간 연장을 고려하세요.",
                                            fontSize = 11.sp,
                                            color = CyanInfo,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. 3층 연금 스택 바 차트
        CashFlowStackCanvasChart(results = summary.yearlyResults)

        // 8. 포트폴리오 자산 배분 도넛 차트
        AssetAllocationDonutChart(
            financialAssets = summary.currentFinancialAssets,
            pensionAssets = summary.currentPensionAssets,
            realEstateAssets = summary.currentRealEstateAssets,
            debt = summary.currentTotalDebt
        )

        // 9. 대한민국 순자산 백분위 분포 차트 (로그정규분포)
        WealthDistributionCanvasChart(
            currentNetWorthWon = summary.currentNetWorth,
            mu = profile.policySettings.wealthDistributionMean,
            sigma = profile.policySettings.wealthDistributionStdDev
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}