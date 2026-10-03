package com.pension.alchemy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.RealTimeYieldMetrics
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.theme.*
import com.pension.alchemy.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToLong

/**
 * 실시간 초당 자산 순증가(소득+자산수익 - 소비-이자-납입) 다이내믹 티커 카드
 * 정기 수입과 개별 자산 수익률, 소비 지출, 사적연금 납입을 모두 종합하여
 * 내 순자산이 실시간으로 불어나는 속도를 체감할 수 있습니다.
 */
@Composable
fun RealTimeAssetGrowthTickerCard(
    metrics: RealTimeYieldMetrics,
    baseDateAccumulatedGain: Long? = null,
    globalBaseDateStr: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // 0: 기준일 누적, 1: 오늘 기준
    // baseDateAccumulatedGain은 1초마다 실시간으로 갱신되므로, remember(baseDateAccumulatedGain)으로 key를 주면
    // 1초마다 상태가 초기화되어 '오늘'을 선택해도 1초 뒤 '기준일'로 리셋되는 버그가 발생합니다.
    // 따라서 사용자의 명시적 모드 선택 상태는 별도로 보존합니다.
    var userSelectedMode by remember { mutableStateOf<Int?>(null) }
    val selectedMode = userSelectedMode ?: (if (baseDateAccumulatedGain != null && baseDateAccumulatedGain != 0L) 0 else 1)

    // 오늘 기준 경과 초 (매 1초마다 갱신)
    var currentSecondOfDay by remember {
        mutableLongStateOf(LocalTime.now().toSecondOfDay().toLong())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            currentSecondOfDay = LocalTime.now().toSecondOfDay().toLong()
        }
    }

    // 오늘 자정부터 현재 시각까지 누적된 순자산 순증가
    val todayAccumulatedGain = (currentSecondOfDay * metrics.wonPerSecond).roundToLong()

    // 표시할 누적 금액
    val displayGain = if (selectedMode == 0 && baseDateAccumulatedGain != null) {
        baseDateAccumulatedGain
    } else {
        todayAccumulatedGain
    }

    // 8시간(수면 시간) 동안 순자산 변화량
    val sleepGain8Hours = (metrics.wonPerHour * 8.0).roundToLong()

    // 펄스 애니메이션 (LIVE 인디케이터)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val isPositive = metrics.wonPerSecond >= 0.0
    val trendColor = if (isPositive) EmeraldPrimary else RoseDanger
    val trendIcon = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. 헤더: 실시간 상태 라벨 & LIVE 인디케이터
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(pulseAlpha)
                            .background(color = trendColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "실시간 순자산 증감",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (baseDateAccumulatedGain != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Text(
                                    text = "기준일",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMode == 0) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { userSelectedMode = 0 }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "오늘",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMode == 1) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { userSelectedMode = 1 }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // LIVE 배지
                    Surface(
                        color = trendColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "● LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = trendColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 메인 중심: 계속 변화하는 실시간 누적 증감액 (동일 색상, 동일 폰트로 원 단위까지 표현)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val gainText = if (selectedMode == 0) {
                        val baseDateLabel = if (!globalBaseDateStr.isNullOrBlank()) {
                            RealTimeGrowthCalculator.formatAsBaseDateLabel(globalBaseDateStr)
                        } else {
                            "입력 기준일 대비"
                        }
                        if (displayGain >= 0) "$baseDateLabel 누적 순자산 증가" else "$baseDateLabel 누적 순자산 감소"
                    } else {
                        if (displayGain >= 0) "오늘 누적 순자산 증가" else "오늘 누적 순자산 감소"
                    }
                    Text(
                        text = gainText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val gainPrefix = if (displayGain > 0) "+" else ""
                        Text(
                            text = "$gainPrefix${CurrencyFormatter.formatKoreanWon(displayGain)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            ),
                            color = trendColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 우측 접기/펼치기 아이콘
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "접기" else "상세보기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. 하단 서브: 초당 속도 표현 (칩/배너 형태)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPositive) "⚡ 자산증가속도" else "⚡ 자산감소속도",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val velocitySign = if (metrics.wonPerSecond >= 0) "+" else ""
                    Text(
                        text = "$velocitySign${String.format(Locale.KOREA, "%,.1f", metrics.wonPerSecond)} 원/초",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }

            // 4. 확장 상세 뷰 (클릭 시 전개)
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // A) 단위별 환산 (1시간, 1일, 1달)
                    Text(
                        text = if (isPositive) "⏱️ 주기별 실질 자산증가 환산" else "⏱️ 주기별 실질 자산감소 환산",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prefix = if (metrics.wonPerSecond >= 0) "+" else ""
                        YieldPeriodChip("시간당", "$prefix${CurrencyFormatter.formatKoreanWon(metrics.wonPerHour.roundToLong(), isShort = true)}", Modifier.weight(1f))
                        YieldPeriodChip("하루(일당)", "$prefix${CurrencyFormatter.formatKoreanWon(metrics.wonPerDay, isShort = true)}", Modifier.weight(1f))
                        YieldPeriodChip("한달(월)", "$prefix${CurrencyFormatter.formatKoreanWon(metrics.wonPerMonth, isShort = true)}", Modifier.weight(1f))
                    }

                    // B) 종합 재정 흐름 브레이크다운 (유입 vs 유출)
                    Text(
                        text = "📊 연간 가계 재정 흐름 상세",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // [유입 부문]
                        Text(
                            text = "🟢 유입 (정기소득 & 자산수익)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        if (metrics.annualRegularIncome > 0L) {
                            ContributionRow("정기 근로/사업소득", metrics.annualRegularIncome, EmeraldPrimary)
                        }
                        ContributionRow("금융자산 운용수익", metrics.annualFinancialGain, EmeraldPrimary)
                        ContributionRow("부동산 가치상승 (수익률 반영)", metrics.annualRealEstateGain, AmberWarning)
                        ContributionRow("연금 적립금 복리운용", metrics.annualPensionGain, CyanInfo)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // [유출 부문]
                        Text(
                            text = "🔴 유출 (소비 & 지출)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseDanger
                        )
                        ContributionRow("현재 연간 생활소비 지출", -metrics.annualLivingExpenses, RoseDanger)
                        if (metrics.annualDebtServiceTotal > 0L) {
                            ContributionRow("대출 원리금 상환 지출", -metrics.annualDebtServiceTotal, RoseDanger)
                        } else if (metrics.annualDebtInterestCost > 0L) {
                            ContributionRow("대출 부채 이자비용", -metrics.annualDebtInterestCost, RoseDanger)
                        }
                        if (metrics.annualPensionContributionDeducted > 0L) {
                            ContributionRow("사적연금 직접 납입 지출", -metrics.annualPensionContributionDeducted, Tier3PersonalColor)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // [순증가 합계]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "연간 순자산 순증가 합계",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val netFormatted = if (metrics.annualNetWealthGrowth > 0L) {
                                "+${CurrencyFormatter.formatToManWon(metrics.annualNetWealthGrowth)}"
                            } else {
                                CurrencyFormatter.formatToManWon(metrics.annualNetWealthGrowth)
                            }
                            Text(
                                text = netFormatted,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = trendColor
                            )
                        }
                        if (metrics.annualDebtServiceTotal > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "대출 상환 후 월 가처분소득",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatToManWon(metrics.disposableMonthlyRegularIncome),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanInfo
                                )
                            }
                        }
                    }

                    // C) 수면 중 자본소득 인사이트 배너
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CyanInfo.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛌", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "잠든 시간(8시간) 동안의 순자산 변화",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val sleepPrefix = if (sleepGain8Hours >= 0L) "+" else ""
                                Text(
                                    text = "소득과 자산수익에서 지출을 제하고 하루 8시간 수면 동안 약 $sleepPrefix${CurrencyFormatter.formatKoreanWon(sleepGain8Hours)}의 순자산이 변동합니다.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YieldPeriodChip(
    title: String,
    amountStr: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amountStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ContributionRow(
    label: String,
    amount: Long,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val formattedAmount = if (amount > 0L) {
            "+${CurrencyFormatter.formatToManWon(amount)}"
        } else {
            CurrencyFormatter.formatToManWon(amount)
        }
        Text(
            text = formattedAmount,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (amount >= 0L) MaterialTheme.colorScheme.onSurface else RoseDanger
        )
    }
}
