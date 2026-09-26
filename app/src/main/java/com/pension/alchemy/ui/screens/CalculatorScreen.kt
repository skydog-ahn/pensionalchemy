package com.pension.alchemy.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pension.alchemy.data.model.PolicySettings
import com.pension.alchemy.data.model.UserProfile
import com.pension.alchemy.domain.engine.CalculatorsEngine
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.AutoSelectBasicTextField
import com.pension.alchemy.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile = UserProfile(),
    viewModel: CalculatorViewModel = viewModel()
) {
    val modes = listOf(
        "적립·예탁",
        "자산 인출",
        "고갈 타이머",
        "목표 필요자산",
        "국민연금 손익",
        "절세 연금술사"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        PrimaryScrollableTabRow(
            selectedTabIndex = viewModel.selectedMode,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            modes.forEachIndexed { index, title ->
                Tab(
                    selected = viewModel.selectedMode == index,
                    onClick = { viewModel.selectedMode = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (viewModel.selectedMode == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (viewModel.selectedMode) {
                0 -> AccumulationModeView(viewModel)
                1 -> WithdrawalModeView(viewModel)
                2 -> DepletionModeView(viewModel)
                3 -> RequiredWealthModeView(viewModel, profile)
                4 -> BreakEvenModeView(viewModel, profile.policySettings)
                5 -> TaxBenefitModeView(viewModel, profile.policySettings)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────
// 1. 적립 및 예탁 미래가치
// ──────────────────────────────────────────────
@Composable
private fun AccumulationModeView(vm: CalculatorViewModel) {
    val context = LocalContext.current

    val initialDeposit = if (vm.accDepositType == 1) 0L else vm.accInitialDeposit
    val monthlyDeposit = if (vm.accDepositType == 2) 0L else vm.accMonthlyDeposit

    val result = remember(initialDeposit, monthlyDeposit, vm.accPeriodYears, vm.accInterestRate) {
        CalculatorsEngine.calculateAccumulation(
            monthlyDeposit = monthlyDeposit,
            periodYears = vm.accPeriodYears,
            annualInterestRate = vm.accInterestRate,
            initialDeposit = initialDeposit
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "적립·예탁 미래가치 계산",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 운용 방식 3분할 탭 (가로 1:1:1 균등 배열)
            SecondaryTabRow(
                selectedTabIndex = vm.accDepositType,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                listOf("혼합(예탁+적립)", "적립식만", "예탁식만").forEachIndexed { idx, title ->
                    Tab(
                        selected = vm.accDepositType == idx,
                        onClick = { vm.accDepositType = idx },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (vm.accDepositType == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (vm.accDepositType != 1) {
                ManwonSliderInput(
                    label = "초기 목돈 예탁금",
                    value = vm.accInitialDeposit,
                    onValueChange = { vm.accInitialDeposit = it },
                    min = vm.accInitialDepositMin,
                    onMinChange = { vm.accInitialDepositMin = it },
                    max = vm.accInitialDepositMax,
                    onMaxChange = { vm.accInitialDepositMax = it },
                    stepManwon = 100L
                )
            }

            if (vm.accDepositType != 2) {
                ManwonSliderInput(
                    label = "매월 정기 적립액",
                    value = vm.accMonthlyDeposit,
                    onValueChange = { vm.accMonthlyDeposit = it },
                    min = vm.accMonthlyDepositMin,
                    onMinChange = { vm.accMonthlyDepositMin = it },
                    max = vm.accMonthlyDepositMax,
                    onMaxChange = { vm.accMonthlyDepositMax = it },
                    stepManwon = 10L
                )
            }

            YearsSliderInput(
                label = "투자 / 운용 기간",
                value = vm.accPeriodYears,
                onValueChange = { vm.accPeriodYears = it },
                min = vm.accPeriodYearsMin,
                onMinChange = { vm.accPeriodYearsMin = it },
                max = vm.accPeriodYearsMax,
                onMaxChange = { vm.accPeriodYearsMax = it },
                unit = "년"
            )

            PercentSliderInput(
                label = "연간 기대수익률",
                value = vm.accInterestRate,
                onValueChange = { vm.accInterestRate = it },
                min = vm.accInterestRateMin,
                onMinChange = { vm.accInterestRateMin = it },
                max = vm.accInterestRateMax,
                onMaxChange = { vm.accInterestRateMax = it }
            )
        }
    }

    // 결과 카드 (헤더에 컴팩트 복사 아이콘 배치)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "${vm.accPeriodYears}년 후 예상 만기 자산",
                onCopy = {
                    val typeStr = when (vm.accDepositType) {
                        1 -> "적립식"
                        2 -> "예탁식(목돈 거치)"
                        else -> "혼합(예탁+적립)"
                    }
                    val copyText = buildString {
                        appendLine("[연금술사 - 적립·예탁 미래가치 계산 결과]")
                        appendLine("• 운용 방식: $typeStr")
                        if (initialDeposit > 0L) appendLine("• 초기 예탁금: ${CurrencyFormatter.formatKoreanWon(initialDeposit)}")
                        if (monthlyDeposit > 0L) appendLine("• 월 적립액: ${CurrencyFormatter.formatKoreanWon(monthlyDeposit)}")
                        appendLine("• 운용 기간: ${vm.accPeriodYears}년")
                        appendLine("• 연간 기대수익률: ${String.format("%.1f", vm.accInterestRate)}%")
                        appendLine("────────────────────────")
                        appendLine("▶ 예상 만기 자산: ${CurrencyFormatter.formatKoreanWon(result.futureValue)}")
                        appendLine("• 총 납입 원금: ${CurrencyFormatter.formatKoreanWon(result.totalPrincipal)}")
                        appendLine("• 총 이자 수익: +${CurrencyFormatter.formatKoreanWon(result.totalInterest)}")
                    }
                    vm.copyToClipboard(context, "적립·예탁 미래가치 결과", copyText)
                }
            )

            Text(
                text = CurrencyFormatter.formatKoreanWon(result.futureValue),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldPrimary
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("총 원금 합계", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(CurrencyFormatter.formatKoreanWon(result.totalPrincipal, isShort = true), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("복리 이자 수익", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("+${CurrencyFormatter.formatKoreanWon(result.totalInterest, isShort = true)}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
            }
        }
    }
}

// ──────────────────────────────────────────────
// 2. 자산 인출 시뮬레이션
// ──────────────────────────────────────────────
@Composable
private fun WithdrawalModeView(vm: CalculatorViewModel) {
    val context = LocalContext.current

    val result = remember(vm.wdWealth, vm.wdMonthlyWithdrawal, vm.wdGrowthRate) {
        CalculatorsEngine.calculateWithdrawal(
            startingWealth = vm.wdWealth,
            annualGrowthRate = vm.wdGrowthRate,
            isFixedAmount = true,
            monthlyWithdrawal = vm.wdMonthlyWithdrawal,
            annualWithdrawalRate = 4.0,
            startAge = 60,
            yearsToSimulate = 35
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "자산 인출 시뮬레이션",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ManwonSliderInput(
                label = "시작 보유 자산",
                value = vm.wdWealth,
                onValueChange = { vm.wdWealth = it },
                min = vm.wdWealthMin,
                onMinChange = { vm.wdWealthMin = it },
                max = vm.wdWealthMax,
                onMaxChange = { vm.wdWealthMax = it },
                stepManwon = 1_000L
            )

            ManwonSliderInput(
                label = "매월 정액 인출액",
                value = vm.wdMonthlyWithdrawal,
                onValueChange = { vm.wdMonthlyWithdrawal = it },
                min = vm.wdMonthlyWithdrawalMin,
                onMinChange = { vm.wdMonthlyWithdrawalMin = it },
                max = vm.wdMonthlyWithdrawalMax,
                onMaxChange = { vm.wdMonthlyWithdrawalMax = it },
                stepManwon = 10L
            )

            PercentSliderInput(
                label = "자산 운용 수익률",
                value = vm.wdGrowthRate,
                onValueChange = { vm.wdGrowthRate = it },
                min = vm.wdGrowthRateMin,
                onMinChange = { vm.wdGrowthRateMin = it },
                max = vm.wdGrowthRateMax,
                onMaxChange = { vm.wdGrowthRateMax = it }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "35년 인출 시뮬레이션 결과",
                onCopy = {
                    val copyText = buildString {
                        appendLine("[연금술사 - 자산 인출 시뮬레이션 결과]")
                        appendLine("• 시작 자산: ${CurrencyFormatter.formatKoreanWon(vm.wdWealth)}")
                        appendLine("• 월 인출액: ${CurrencyFormatter.formatKoreanWon(vm.wdMonthlyWithdrawal)}/월")
                        appendLine("• 운용 수익률: 연 ${String.format("%.1f", vm.wdGrowthRate)}%")
                        appendLine("────────────────────────")
                        if (result.isDepleted) {
                            appendLine("▶ 판정 결과: ${result.depletionAge}세에 자산 고갈 (인출액 축소 필요)")
                        } else {
                            appendLine("▶ 판정 결과: 35년간 안전 유지 (95세 이후에도 잔여 자산 보존)")
                            appendLine("• 95세 잔여 자산: ${CurrencyFormatter.formatKoreanWon(result.finalBalance)}")
                        }
                    }
                    vm.copyToClipboard(context, "자산 인출 시뮬레이션 결과", copyText)
                }
            )

            Text(
                text = if (result.isDepleted) "⚠️ ${result.depletionAge}세에 자산 조기 소진" else "✅ 35년간 안전 유지 (95세+ 생존)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (result.isDepleted) RoseDanger else EmeraldPrimary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("95세 시점 잔여 자산", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(CurrencyFormatter.formatKoreanWon(result.finalBalance), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ──────────────────────────────────────────────
// 3. 자산 고갈 타이머
// ──────────────────────────────────────────────
@Composable
private fun DepletionModeView(vm: CalculatorViewModel) {
    val context = LocalContext.current

    val result = remember(vm.depWealth, vm.depWithdrawal, vm.depGrowth, vm.depInflation) {
        CalculatorsEngine.calculateDepletion(vm.depWealth, vm.depWithdrawal, vm.depGrowth, vm.depInflation, 60)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "자산 고갈 타이머",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ManwonSliderInput(
                label = "현재 보유 자산",
                value = vm.depWealth,
                onValueChange = { vm.depWealth = it },
                min = vm.depWealthMin,
                onMinChange = { vm.depWealthMin = it },
                max = vm.depWealthMax,
                onMaxChange = { vm.depWealthMax = it },
                stepManwon = 1_000L
            )

            ManwonSliderInput(
                label = "매월 인출 생활비",
                value = vm.depWithdrawal,
                onValueChange = { vm.depWithdrawal = it },
                min = vm.depWithdrawalMin,
                onMinChange = { vm.depWithdrawalMin = it },
                max = vm.depWithdrawalMax,
                onMaxChange = { vm.depWithdrawalMax = it },
                stepManwon = 10L
            )

            PercentSliderInput(
                label = "자산 운용 수익률",
                value = vm.depGrowth,
                onValueChange = { vm.depGrowth = it },
                min = vm.depGrowthMin,
                onMinChange = { vm.depGrowthMin = it },
                max = vm.depGrowthMax,
                onMaxChange = { vm.depGrowthMax = it }
            )

            PercentSliderInput(
                label = "예상 물가상승률",
                value = vm.depInflation,
                onValueChange = { vm.depInflation = it },
                min = vm.depInflationMin,
                onMinChange = { vm.depInflationMin = it },
                max = vm.depInflationMax,
                onMaxChange = { vm.depInflationMax = it }
            )

            val netRate = vm.depGrowth - vm.depInflation
            Text(
                text = "💡 실질 수익률 (수익률 - 물가): ${String.format("%.1f", netRate)}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (netRate > 0) EmeraldPrimary else RoseDanger
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "자산 고갈 판정 결과",
                onCopy = {
                    val copyText = buildString {
                        appendLine("[연금술사 - 자산 고갈 타이머 결과]")
                        appendLine("• 보유 자산: ${CurrencyFormatter.formatKoreanWon(vm.depWealth)}")
                        appendLine("• 월 인출액: ${CurrencyFormatter.formatKoreanWon(vm.depWithdrawal)}/월")
                        appendLine("• 운용 수익률: 연 ${String.format("%.1f", vm.depGrowth)}%")
                        appendLine("• 물가상승률: 연 ${String.format("%.1f", vm.depInflation)}% (실질수익률 ${String.format("%.1f", vm.depGrowth - vm.depInflation)}%)")
                        appendLine("────────────────────────")
                        if (result.isForeverSafe) {
                            appendLine("▶ 고갈 판정: 영구 보존 안전 (수익금이 인출액 초과)")
                        } else {
                            appendLine("▶ 고갈 판정: 약 ${String.format("%.1f", result.depletionYears)}년 후 (${String.format("%.0f", result.depletionAge)}세) 소진")
                        }
                        appendLine("• 원금 보존 권장 인출한도: ${CurrencyFormatter.formatKoreanWon(result.recommendedSafeMonthlyWithdrawal)}/월")
                    }
                    vm.copyToClipboard(context, "자산 고갈 타이머 결과", copyText)
                }
            )

            Text(
                text = if (result.isForeverSafe) "🎉 영구 보존 (수익금이 인출액을 초과)" else "⏳ 약 ${String.format("%.1f", result.depletionYears)}년 후 (${String.format("%.0f", result.depletionAge)}세) 고갈",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (result.isForeverSafe) EmeraldPrimary else RoseDanger
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("원금 보존 안전 인출한도", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${CurrencyFormatter.formatKoreanWon(result.recommendedSafeMonthlyWithdrawal)}/월", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ──────────────────────────────────────────────
// 4. 목표 필요자산 역산기
// ──────────────────────────────────────────────
@Composable
private fun RequiredWealthModeView(
    vm: CalculatorViewModel,
    profile: UserProfile = UserProfile()
) {
    val context = LocalContext.current

    val currentAge = profile.currentAge.coerceIn(20, 90)
    val targetRetirementAge = profile.retirementAge.coerceAtLeast(currentAge + 1)

    val result = remember(vm.reqExpense, vm.reqYears, vm.reqReturnRate, currentAge, targetRetirementAge) {
        CalculatorsEngine.calculateRequiredWealth(vm.reqExpense, vm.reqYears, vm.reqReturnRate, currentAge, targetRetirementAge)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "목표 필요자산 역산기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ManwonSliderInput(
                label = "목표 희망 월 생활비",
                value = vm.reqExpense,
                onValueChange = { vm.reqExpense = it },
                min = vm.reqExpenseMin,
                onMinChange = { vm.reqExpenseMin = it },
                max = vm.reqExpenseMax,
                onMaxChange = { vm.reqExpenseMax = it },
                stepManwon = 10L
            )

            YearsSliderInput(
                label = "자산 인출 유지 기간",
                value = vm.reqYears,
                onValueChange = { vm.reqYears = it },
                min = vm.reqYearsMin,
                onMinChange = { vm.reqYearsMin = it },
                max = vm.reqYearsMax,
                onMaxChange = { vm.reqYearsMax = it },
                unit = "년"
            )

            PercentSliderInput(
                label = "기대 운용 수익률",
                value = vm.reqReturnRate,
                onValueChange = { vm.reqReturnRate = it },
                min = vm.reqReturnRateMin,
                onMinChange = { vm.reqReturnRateMin = it },
                max = vm.reqReturnRateMax,
                onMaxChange = { vm.reqReturnRateMax = it }
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "목표 필요 총자산",
                onCopy = {
                    val copyText = buildString {
                        appendLine("[연금술사 - 목표 필요자산 역산 결과]")
                        appendLine("• 목표 월 생활비: ${CurrencyFormatter.formatKoreanWon(vm.reqExpense)}/월")
                        appendLine("• 인출 유지 기간: ${vm.reqYears}년")
                        appendLine("• 기대 운용 수익률: 연 ${String.format("%.1f", vm.reqReturnRate)}%")
                        appendLine("────────────────────────")
                        appendLine("▶ 목표 필요 총자산: ${CurrencyFormatter.formatKoreanWon(result.targetTotalWealth)}")
                        appendLine("• 매월 필요 추가 저축액: ${CurrencyFormatter.formatKoreanWon(result.monthlySavingsNeeded)}/월 (15년 적립 기준)")
                    }
                    vm.copyToClipboard(context, "목표 필요자산 결과", copyText)
                }
            )

            Text(
                text = CurrencyFormatter.formatKoreanWon(result.targetTotalWealth),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldPrimary
            )
            val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("매월 필요 추가 저축액", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${CurrencyFormatter.formatKoreanWon(result.monthlySavingsNeeded)}/월 (15년 적립 기준)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("매월 필요 추가 저축액 (15년 적립 기준)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${CurrencyFormatter.formatKoreanWon(result.monthlySavingsNeeded)} / 월", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 5. 국민연금 손익분기점
// ──────────────────────────────────────────────
@Composable
private fun BreakEvenModeView(
    vm: CalculatorViewModel,
    policy: PolicySettings = PolicySettings()
) {
    val context = LocalContext.current

    val result = remember(vm.beNormalAmount, policy) {
        CalculatorsEngine.calculateNationalPensionBreakEven(vm.beNormalAmount, 65, policy)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "국민연금 조기 vs 정상 vs 연기 손익분기점",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ManwonSliderInput(
                label = "정상수령 월 예상액",
                subLabel = "만 65세 정상수령 기준액",
                value = vm.beNormalAmount,
                onValueChange = { vm.beNormalAmount = it },
                min = vm.beNormalAmountMin,
                onMinChange = { vm.beNormalAmountMin = it },
                max = vm.beNormalAmountMax,
                onMaxChange = { vm.beNormalAmountMax = it },
                stepManwon = 10L
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "수령 시기별 월 수령액 및 교차 나이",
                onCopy = {
                    val copyText = buildString {
                        appendLine("[연금술사 - 국민연금 수령시기 손익분기 분석]")
                        appendLine("• 65세 정상수령 기준액: ${CurrencyFormatter.formatKoreanWon(vm.beNormalAmount)}/월")
                        appendLine("• 조기수령(60세, -30%): ${CurrencyFormatter.formatKoreanWon(result.earlyMonthlyAmount)}/월")
                        appendLine("• 정상수령(65세, 100%): ${CurrencyFormatter.formatKoreanWon(result.normalMonthlyAmount)}/월")
                        appendLine("• 연기수령(70세, +36%): ${CurrencyFormatter.formatKoreanWon(result.delayedMonthlyAmount)}/월")
                        appendLine("────────────────────────")
                        appendLine("▶ 조기 vs 정상 교차 나이: 만 ${result.earlyVsNormalBreakEvenAge}세 (이후 정상수령 총수령액 우위)")
                        appendLine("▶ 정상 vs 연기 교차 나이: 만 ${result.normalVsDelayedBreakEvenAge}세 (이후 연기수령 총수령액 최대)")
                    }
                    vm.copyToClipboard(context, "국민연금 손익분기 결과", copyText)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = AmberWarning.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "조기 (60세)", fontSize = 11.sp, color = AmberWarning, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                        Text(text = "${CurrencyFormatter.formatKoreanWon(result.earlyMonthlyAmount, isShort = true)}/월", fontSize = 12.sp, color = AmberWarning, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
                Surface(
                    color = CyanInfo.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "정상 (65세)", fontSize = 11.sp, color = CyanInfo, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                        Text(text = "${CurrencyFormatter.formatKoreanWon(result.normalMonthlyAmount, isShort = true)}/월", fontSize = 12.sp, color = CyanInfo, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "연기 (70세)", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                        Text(text = "${CurrencyFormatter.formatKoreanWon(result.delayedMonthlyAmount, isShort = true)}/월", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "📌 조기 vs 정상 교차: 만 ${result.earlyVsNormalBreakEvenAge}세 (${result.earlyVsNormalBreakEvenAge}세 이후 정상수령 역전)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "📌 정상 vs 연기 교차: 만 ${result.normalVsDelayedBreakEvenAge}세 (${result.normalVsDelayedBreakEvenAge}세 이상 장수 시 연기수령 최대 이익)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// ──────────────────────────────────────────────
// 6. 절세 연금술사
// ──────────────────────────────────────────────
@Composable
private fun TaxBenefitModeView(
    vm: CalculatorViewModel,
    policy: PolicySettings = PolicySettings()
) {
    val context = LocalContext.current
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

    val result = remember(vm.taxContribution, vm.taxIsLowIncome, policy) {
        CalculatorsEngine.calculateTaxBenefit(vm.taxContribution, vm.taxIsLowIncome, policy)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "연금저축/IRP 세액공제 & 복리 증폭",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ManwonSliderInput(
                label = "연간 납입액",
                subLabel = "연금저축 + IRP 합산 (공제한도: ${(policy.maxTaxCreditContribution / 10_000L)}만원)",
                value = vm.taxContribution,
                onValueChange = { vm.taxContribution = it },
                min = vm.taxContributionMin,
                onMinChange = { vm.taxContributionMin = it },
                max = vm.taxContributionMax,
                onMaxChange = { vm.taxContributionMax = it },
                stepManwon = 50L
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = vm.taxIsLowIncome, onCheckedChange = { vm.taxIsLowIncome = it })
                Text(
                    text = if (vm.taxIsLowIncome) "총급여 5,500만원 이하 (세액공제율 ${policy.taxCreditRateLowIncome}%)" else "총급여 5,500만원 초과 (세액공제율 ${policy.taxCreditRateHighIncome}%)",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultCardHeader(
                title = "매년 연말정산 환급금",
                onCopy = {
                    val rateStr = if (vm.taxIsLowIncome) "16.5% (총급여 5,500만 이하)" else "13.2% (총급여 5,500만 초과)"
                    val copyText = buildString {
                        appendLine("[연금술사 - 연금저축/IRP 절세 및 복리 효과]")
                        appendLine("• 연간 납입액: ${CurrencyFormatter.formatKoreanWon(result.annualContribution)}")
                        appendLine("• 세액공제율: $rateStr")
                        appendLine("────────────────────────")
                        appendLine("▶ 매년 연말정산 환급금: ${CurrencyFormatter.formatKoreanWon(result.taxRefundAmount)}")
                        appendLine("▶ 30년간 환급금 재투자 미래가치 (연 6%): +${CurrencyFormatter.formatKoreanWon(result.reinvestedFutureValue30Years)}")
                        appendLine("▶ 일반계좌 대비 절세 보너스: +${CurrencyFormatter.formatKoreanWon(result.taxSavingsAlchemyBonus)}")
                    }
                    vm.copyToClipboard(context, "절세 연금술사 결과", copyText)
                }
            )

            Text(
                text = "${CurrencyFormatter.formatKoreanWon(result.taxRefundAmount)} 환급",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldPrimary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ 30년간 환급금 재투자 (연 6%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "+${CurrencyFormatter.formatKoreanWon(result.reinvestedFutureValue30Years, isShort = true)} 창출",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ 일반계좌 대비 절세 차액",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "+${CurrencyFormatter.formatKoreanWon(result.taxSavingsAlchemyBonus, isShort = true)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "✨ 30년간 환급금 재투자 효과 (연 6% 가정)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${CurrencyFormatter.formatKoreanWon(result.reinvestedFutureValue30Years, isShort = true)} 추가 자산 창출",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "✨ 일반 금융계좌 대비 세금 절감액",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${CurrencyFormatter.formatKoreanWon(result.taxSavingsAlchemyBonus, isShort = true)} 절세 보너스",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 재사용 컴포넌트: 만원 단위 통합 입력 + 슬라이더 + 최소/최대 직접 수정
// ──────────────────────────────────────────────
@Composable
private fun ManwonSliderInput(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    min: Long,
    onMinChange: (Long) -> Unit,
    max: Long,
    onMaxChange: (Long) -> Unit,
    stepManwon: Long = 10L,
    subLabel: String? = null
) {
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

    val valueManwon = (value / 10_000L).coerceAtLeast(0L)
    val minManwon = (min / 10_000L).coerceAtLeast(0L)
    val maxManwon = (max / 10_000L).coerceAtLeast(minManwon + stepManwon)

    var inputTxt by remember(valueManwon) { mutableStateOf(valueManwon.toString()) }
    var minTxt by remember(minManwon) { mutableStateOf(minManwon.toString()) }
    var maxTxt by remember(maxManwon) { mutableStateOf(maxManwon.toString()) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isWideScreen) {
            // 넓은 화면 (태블릿, 가로보기): 가로 1열 좌우 배치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subLabel.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(단위: 만원)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        AutoSelectBasicTextField(
                            value = inputTxt,
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                inputTxt = clean
                                val parsed = clean.toLongOrNull() ?: 0L
                                if (parsed > maxManwon) onMaxChange(parsed * 10_000L)
                                if (parsed < minManwon && parsed > 0L) onMinChange(parsed * 10_000L)
                                onValueChange(parsed * 10_000L)
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.widthIn(min = 55.dp, max = 85.dp),
                            singleLine = true
                        )
                    }
                }
            }
        } else {
            // 스마트폰 세로 보기: 라벨(및 subLabel)을 윗줄 전체 너비로 넉넉하게 표시하고 입력창을 아래로 내려 배치
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(단위: 만원)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                if (!subLabel.isNullOrEmpty()) {
                    Text(
                        text = subLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            AutoSelectBasicTextField(
                                value = inputTxt,
                                onValueChange = { str ->
                                    val clean = str.filter { it.isDigit() }
                                    inputTxt = clean
                                    val parsed = clean.toLongOrNull() ?: 0L
                                    if (parsed > maxManwon) onMaxChange(parsed * 10_000L)
                                    if (parsed < minManwon && parsed > 0L) onMinChange(parsed * 10_000L)
                                    onValueChange(parsed * 10_000L)
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                modifier = Modifier.widthIn(min = 55.dp, max = 85.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        // 슬라이더
        Slider(
            value = valueManwon.coerceIn(minManwon, maxManwon).toFloat(),
            onValueChange = { newVal ->
                val stepped = ((newVal / stepManwon).toLong() * stepManwon).coerceIn(minManwon, maxManwon)
                onValueChange(stepped * 10_000L)
            },
            valueRange = minManwon.toFloat()..maxManwon.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )

        // 최소값 / 최대값 직접 수정 마이크로 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최소", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(
                    valueText = minTxt,
                    onValueChange = { str ->
                        minTxt = str
                        val p = str.toLongOrNull() ?: 0L
                        if (p < maxManwon) onMinChange(p * 10_000L)
                    },
                    suffix = ""
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최대", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(
                    valueText = maxTxt,
                    onValueChange = { str ->
                        maxTxt = str
                        val p = str.toLongOrNull() ?: 100L
                        if (p > minManwon) onMaxChange(p * 10_000L)
                    },
                    suffix = ""
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
// 재사용 컴포넌트: 백분율(%) 입력 + 슬라이더 + 최소/최대 직접 수정
// ──────────────────────────────────────────────
@Composable
private fun PercentSliderInput(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    min: Double,
    onMinChange: (Double) -> Unit,
    max: Double,
    onMaxChange: (Double) -> Unit,
    subLabel: String? = null
) {
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

    val safeMin = min.coerceAtLeast(0.0)
    val safeMax = max.coerceAtLeast(safeMin + 0.1)

    var inputTxt by remember(value) { mutableStateOf(String.format("%.1f", value)) }
    var minTxt by remember(safeMin) { mutableStateOf(String.format("%.1f", safeMin)) }
    var maxTxt by remember(safeMax) { mutableStateOf(String.format("%.1f", safeMax)) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!subLabel.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(subLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        AutoSelectBasicTextField(
                            value = inputTxt,
                            onValueChange = { str ->
                                inputTxt = str
                                str.toDoubleOrNull()?.let { p ->
                                    if (p > safeMax) onMaxChange(p)
                                    if (p < safeMin && p >= 0.0) onMinChange(p)
                                    onValueChange(p)
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.widthIn(min = 45.dp, max = 70.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subLabel.isNullOrEmpty()) {
                    Text(
                        text = subLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            AutoSelectBasicTextField(
                                value = inputTxt,
                                onValueChange = { str ->
                                    inputTxt = str
                                    str.toDoubleOrNull()?.let { p ->
                                        if (p > safeMax) onMaxChange(p)
                                        if (p < safeMin && p >= 0.0) onMinChange(p)
                                        onValueChange(p)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                modifier = Modifier.widthIn(min = 45.dp, max = 70.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Slider(
            value = value.coerceIn(safeMin, safeMax).toFloat(),
            onValueChange = { newVal ->
                val rounded = Math.round(newVal * 10.0) / 10.0
                onValueChange(rounded.coerceIn(safeMin, safeMax))
            },
            valueRange = safeMin.toFloat()..safeMax.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최소", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(minTxt, onValueChange = { minTxt = it; it.toDoubleOrNull()?.let { p -> if (p < safeMax) onMinChange(p) } }, suffix = "%")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최대", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(maxTxt, onValueChange = { maxTxt = it; it.toDoubleOrNull()?.let { p -> if (p > safeMin) onMaxChange(p) } }, suffix = "%")
            }
        }
    }
}

// ──────────────────────────────────────────────
// 재사용 컴포넌트: 기간(년) 입력 + 슬라이더 + 최소/최대 직접 수정
// ──────────────────────────────────────────────
@Composable
private fun YearsSliderInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    onMinChange: (Int) -> Unit,
    max: Int,
    onMaxChange: (Int) -> Unit,
    unit: String = "년",
    subLabel: String? = null
) {
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600

    val safeMin = min.coerceAtLeast(1)
    val safeMax = max.coerceAtLeast(safeMin + 1)

    var inputTxt by remember(value) { mutableStateOf(value.toString()) }
    var minTxt by remember(safeMin) { mutableStateOf(safeMin.toString()) }
    var maxTxt by remember(safeMax) { mutableStateOf(safeMax.toString()) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!subLabel.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(subLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        AutoSelectBasicTextField(
                            value = inputTxt,
                            onValueChange = { str ->
                                val clean = str.filter { it.isDigit() }
                                inputTxt = clean
                                clean.toIntOrNull()?.let { p ->
                                    if (p > safeMax) onMaxChange(p)
                                    if (p < safeMin && p > 0) onMinChange(p)
                                    onValueChange(p)
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.widthIn(min = 45.dp, max = 65.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subLabel.isNullOrEmpty()) {
                    Text(
                        text = subLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            AutoSelectBasicTextField(
                                value = inputTxt,
                                onValueChange = { str ->
                                    val clean = str.filter { it.isDigit() }
                                    inputTxt = clean
                                    clean.toIntOrNull()?.let { p ->
                                        if (p > safeMax) onMaxChange(p)
                                        if (p < safeMin && p > 0) onMinChange(p)
                                        onValueChange(p)
                                    }
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                modifier = Modifier.widthIn(min = 45.dp, max = 65.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(unit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Slider(
            value = value.coerceIn(safeMin, safeMax).toFloat(),
            onValueChange = { newVal ->
                onValueChange(newVal.toInt().coerceIn(safeMin, safeMax))
            },
            valueRange = safeMin.toFloat()..safeMax.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최소", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(minTxt, onValueChange = { minTxt = it; it.toIntOrNull()?.let { p -> if (p < safeMax) onMinChange(p) } }, suffix = unit)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("최대", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicNumberBox(maxTxt, onValueChange = { maxTxt = it; it.toIntOrNull()?.let { p -> if (p > safeMin) onMaxChange(p) } }, suffix = unit)
            }
        }
    }
}

// ──────────────────────────────────────────────
// 소형 인라인 수치 박스 (최소/최대 수정용)
// ──────────────────────────────────────────────
@Composable
private fun BasicNumberBox(
    valueText: String,
    onValueChange: (String) -> Unit,
    suffix: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            AutoSelectBasicTextField(
                value = valueText,
                onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.widthIn(min = 28.dp, max = 50.dp),
                singleLine = true
            )
            if (suffix.isNotEmpty()) {
                Text(suffix, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ──────────────────────────────────────────────
// 결과 카드 헤더 컴포넌트 (타이틀 + 우측 복사 아이콘 버튼)
// ──────────────────────────────────────────────
@Composable
private fun ResultCardHeader(
    title: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "결과 텍스트 복사",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}