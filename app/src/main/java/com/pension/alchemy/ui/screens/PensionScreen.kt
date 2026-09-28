package com.pension.alchemy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.Pension
import com.pension.alchemy.data.model.PensionType
import com.pension.alchemy.domain.engine.PensionPlanCalculator
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.AutoSelectOutlinedTextField
import com.pension.alchemy.util.CurrencyFormatter
import androidx.compose.ui.window.DialogProperties

@Composable
fun PensionScreen(
    pensions: List<Pension>,
    onSavePension: (Pension) -> Unit,
    onDeletePension: (String) -> Unit,
    currentAge: Int = 40,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingPension by remember { mutableStateOf<Pension?>(null) }

    val nationalPension = pensions.firstOrNull { it.type == PensionType.NATIONAL }
    val totalMonthlyPayout = pensions.sumOf { it.expectedMonthlyAmount }
    val totalAccumulatedBalances = pensions.sumOf { it.currentBalance }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPension = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "연금 추가")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 3층 연금 피라미드 요약 카드
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "대한민국 3층 연금 안전망 체계",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 피라미드 바
                        TierBar("3층 세제적격 (개인연금저축)", Tier3PersonalColor, pensions.filter { it.type == PensionType.PERSONAL }.sumOf { it.expectedMonthlyAmount })
                        Spacer(modifier = Modifier.height(4.dp))
                        TierBar("3층 비과세 (개인연금보험)", CyanInfo, pensions.filter { it.type == PensionType.ANNUITY_INSURANCE }.sumOf { it.expectedMonthlyAmount })
                        Spacer(modifier = Modifier.height(4.dp))
                        TierBar("2층 퇴직연금 (DB/DC/IRP)", Tier2RetirementColor, pensions.filter { it.type == PensionType.RETIREMENT }.sumOf { it.expectedMonthlyAmount })
                        Spacer(modifier = Modifier.height(4.dp))
                        TierBar("1층 국민연금 (공적연금)", Tier1NationalColor, pensions.filter { it.type == PensionType.NATIONAL }.sumOf { it.expectedMonthlyAmount })

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "적립 잔액: ${CurrencyFormatter.formatKoreanWon(totalAccumulatedBalances, isShort = true)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = false,
                                maxLines = 1
                            )
                            Text(
                                text = "예상 합산: ${CurrencyFormatter.formatKoreanWon(totalMonthlyPayout, isShort = true)}/월",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. 국민연금 조기/연기 수령 특화 제어 카드
            if (nationalPension != null) {
                item {
                    NationalPensionAdjustmentCard(
                        pension = nationalPension,
                        onUpdate = { onSavePension(it) }
                    )
                }
            }

            // 3. 연금 리스트 헤더
            item {
                Text(
                    text = "나의 연금 플랜 목록 (${pensions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. 연금 아이템들
            items(pensions, key = { it.id }) { p ->
                PensionItemCard(
                    pension = p,
                    onEdit = {
                        editingPension = p
                        showDialog = true
                    },
                    onDelete = { onDeletePension(p.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }
    }

    if (showDialog) {
        PensionEditDialog(
            pension = editingPension,
            currentAge = currentAge,
            onDismiss = { showDialog = false },
            onSave = {
                onSavePension(it)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TierBar(title: String, color: Color, monthlyAmount: Long) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
            Text(
                text = "${CurrencyFormatter.formatKoreanWon(monthlyAmount, isShort = true)}/월",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NationalPensionAdjustmentCard(
    pension: Pension,
    onUpdate: (Pension) -> Unit
) {
    var offsetYears by remember(pension.claimOffsetYears) { mutableFloatStateOf(pension.claimOffsetYears.toFloat()) }

    val ratePercent = when {
        offsetYears < 0 -> offsetYears * 6.0f // -6% per year
        offsetYears > 0 -> offsetYears * 7.2f // +7.2% per year
        else -> 0.0f
    }
    val adjustedAmount = (pension.expectedMonthlyAmount.toDouble() * (1.0 + ratePercent / 100.0)).toLong()
    val effectiveAge = pension.startAge + offsetYears.toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "국민연금 조기/연기 수령 옵션",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                )
                Surface(
                    color = if (offsetYears == 0f) CyanInfo.copy(alpha = 0.2f) else if (offsetYears > 0) EmeraldPrimary.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            offsetYears < 0 -> "조기 ${-offsetYears.toInt()}년 (${String.format("%.1f", ratePercent)}%)"
                            offsetYears > 0 -> "연기 ${offsetYears.toInt()}년 (+${String.format("%.1f", ratePercent)}%)"
                            else -> "정상 수령 (100%)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (offsetYears == 0f) CyanInfo else if (offsetYears > 0) EmeraldPrimary else AmberWarning,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = offsetYears,
                onValueChange = { offsetYears = it },
                onValueChangeFinished = {
                    onUpdate(pension.copy(claimOffsetYears = offsetYears.toInt()))
                },
                valueRange = -5f..5f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = Tier1NationalColor,
                    activeTrackColor = Tier1NationalColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "조기수령 (60세)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "정상 (${pension.startAge}세)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "연기수령 (70세)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "수령 개시: ${effectiveAge}세", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, softWrap = false, maxLines = 1)
                Text(
                    text = "예상: ${CurrencyFormatter.formatKoreanWon(adjustedAmount)}/월",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Tier1NationalColor,
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PensionItemCard(
    pension: Pension,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tierColor = when (pension.type) {
        PensionType.NATIONAL -> Tier1NationalColor
        PensionType.RETIREMENT -> Tier2RetirementColor
        PensionType.PERSONAL -> Tier3PersonalColor
        PensionType.ANNUITY_INSURANCE -> CyanInfo
        PensionType.HOUSING -> TierHousingColor
        PensionType.OTHER -> TierOtherColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(text = pension.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = tierColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = pension.type.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        val taxBadge = when (pension.type) {
                            PensionType.ANNUITY_INSURANCE -> "비과세 (0%)"
                            PensionType.PERSONAL -> "저율과세 (3.3~5.5%)"
                            PensionType.RETIREMENT -> "퇴직세 30~40% 감면"
                            PensionType.HOUSING -> "비과세 (역모기지)"
                            PensionType.NATIONAL -> "소득공제 과세"
                            PensionType.OTHER -> "분리과세 (5.5%)"
                        }
                        Surface(
                            color = if (pension.type == PensionType.ANNUITY_INSURANCE || pension.type == PensionType.HOUSING) EmeraldPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = taxBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pension.type == PensionType.ANNUITY_INSURANCE || pension.type == PensionType.HOUSING) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "수령 기간", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${pension.startAge}세 ~ ${pension.endAge}세", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (pension.currentBalance > 0L) {
                    Column {
                        Text(text = "현재 적립금", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = false, maxLines = 1)
                        Text(text = CurrencyFormatter.formatKoreanWon(pension.currentBalance, isShort = true), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, softWrap = false, maxLines = 1)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "월 예상 수령액", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = false, maxLines = 1)
                    Text(
                        text = "${CurrencyFormatter.formatKoreanWon(pension.expectedMonthlyAmount)}/월",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PensionEditDialog(
    pension: Pension?,
    currentAge: Int = 40,
    onDismiss: () -> Unit,
    onSave: (Pension) -> Unit
) {
    var name by remember { mutableStateOf(pension?.name ?: "") }
    var type by remember { mutableStateOf(pension?.type ?: PensionType.PERSONAL) }
    var startAgeStr by remember { mutableStateOf(pension?.startAge?.toString() ?: "60") }
    var endAgeStr by remember { mutableStateOf(pension?.endAge?.toString() ?: "85") }
    var monthlyPayoutStr by remember { mutableStateOf(pension?.expectedMonthlyAmount?.toString() ?: "0") }
    var balanceStr by remember { mutableStateOf(pension?.currentBalance?.toString() ?: "0") }
    var monthlyContributionStr by remember { mutableStateOf(pension?.monthlyContribution?.toString() ?: "0") }
    var contributionEndAgeStr by remember { mutableStateOf(pension?.contributionEndAge?.toString() ?: (pension?.startAge?.toString() ?: "60")) }
    var growthRateStr by remember { mutableStateOf(pension?.expectedGrowthRate?.toString() ?: "4.5") }

    // 연동 계산 모드: 0 = [수급 기간 -> 월 수령액 산출], 1 = [희망 수령액 -> 수급 기간 산출]
    var linkageMode by remember { mutableIntStateOf(0) }
    // 개인연금보험 모드: 0 = 종신형(100세), 1 = 10년 확정, 2 = 20년 확정, 3 = 직접 입력
    var annuityInsuranceOption by remember { mutableIntStateOf(if (pension?.endAge == 100) 0 else 2) }

    // 파싱된 수치들
    val startAge = startAgeStr.toIntOrNull() ?: 60
    val endAge = endAgeStr.toIntOrNull() ?: 85
    val currentBalance = balanceStr.toLongOrNull() ?: 0L
    val monthlyContribution = monthlyContributionStr.toLongOrNull() ?: 0L
    val contributionEndAge = contributionEndAgeStr.toIntOrNull() ?: startAge
    val growthRate = growthRateStr.toDoubleOrNull() ?: 4.5
    val monthlyPayout = monthlyPayoutStr.toLongOrNull() ?: 0L

    // 1. 수급 개시 시점 예상 적립금 (FV) 실시간 계산
    val isFundedType = type == PensionType.PERSONAL || type == PensionType.RETIREMENT || type == PensionType.ANNUITY_INSURANCE
    val accumulatedFund = remember(currentAge, startAge, currentBalance, monthlyContribution, contributionEndAge, growthRate, isFundedType) {
        if (isFundedType) {
            PensionPlanCalculator.calculateAccumulatedAtStartAge(
                currentAge = currentAge,
                startAge = startAge,
                currentBalance = currentBalance,
                monthlyContribution = monthlyContribution,
                contributionEndAge = contributionEndAge,
                annualGrowthRate = growthRate
            )
        } else {
            0L
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .imePadding(),
        title = { Text(if (pension == null) "연금 플랜 추가" else "연금 플랜 수정") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1) 연금 명칭
                item {
                    AutoSelectOutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("연금 명칭 (예: 개인연금저축, IRP, 연금보험)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2) 연금 분류 칩
                item {
                    Text(text = "연금 분류: ${type.displayName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(PensionType.values()) { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = {
                                    type = t
                                    if (name.isBlank() || PensionType.values().any { it.displayName == name }) {
                                        name = t.displayName
                                    }
                                    if (t == PensionType.ANNUITY_INSURANCE) {
                                        growthRateStr = "3.5"
                                        endAgeStr = "100"
                                        annuityInsuranceOption = 0
                                    } else if (t == PensionType.NATIONAL) {
                                        growthRateStr = "2.0"
                                        startAgeStr = "65"
                                        endAgeStr = "100"
                                    }
                                },
                                label = { Text(t.displayName.substringBefore("/"), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // 3) 연금 유형별 특화 안내 및 입력 UI
                if (type == PensionType.NATIONAL) {
                    item {
                        Surface(
                            color = Tier1NationalColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🏛️ 국민연금 (공적연금)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Tier1NationalColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "국민연금공단 예상연금액 조회 결과를 기준으로 수령 시점의 예상 월 수령액을 입력합니다. (물가상승률 복리 및 조기/연기 수령 옵션이 연동됩니다)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = startAgeStr,
                                onValueChange = { startAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("수령 개시 나이") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = endAgeStr,
                                onValueChange = { endAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("수령 종료 나이") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        AutoSelectOutlinedTextField(
                            value = monthlyPayoutStr,
                            onValueChange = { monthlyPayoutStr = it.filter { c -> c.isDigit() } },
                            label = { Text("월 예상 수령액 (원)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        AutoSelectOutlinedTextField(
                            value = growthRateStr,
                            onValueChange = { growthRateStr = it },
                            label = { Text("물가연동률 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (isFundedType) {
                    // 개인연금 / 퇴직연금(IRP) / 개인연금보험
                    item {
                        Text(
                            text = "1. 적립 및 운용 조건 (현재 나이: ${currentAge}세 기준)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = balanceStr,
                                onValueChange = { balanceStr = it.filter { c -> c.isDigit() } },
                                label = { Text("현재 적립금 (원)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = monthlyContributionStr,
                                onValueChange = { monthlyContributionStr = it.filter { c -> c.isDigit() } },
                                label = { Text("월 납입액 (원)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = contributionEndAgeStr,
                                onValueChange = { contributionEndAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("납입 종료 나이") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = growthRateStr,
                                onValueChange = { growthRateStr = it },
                                label = { Text("기대 운용수익률 (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        AutoSelectOutlinedTextField(
                            value = startAgeStr,
                            onValueChange = {
                                startAgeStr = it.filter { c -> c.isDigit() }
                                val newStart = it.toIntOrNull() ?: 60
                                if (contributionEndAgeStr == "60" || contributionEndAgeStr == startAgeStr) {
                                    contributionEndAgeStr = newStart.toString()
                                }
                            },
                            label = { Text("수령 개시 나이 (세)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ⭐ 수급 개시 시점 총 예상 적립금 하이라이트 카드
                    item {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 ${startAge}세 개시 시점 예상 적립금",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatKoreanWon(accumulatedFund),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldPrimary
                                    )
                                }
                                val yearsToStart = (startAge - currentAge).coerceAtLeast(0)
                                val payYears = ((contributionEndAge.coerceAtMost(startAge) - currentAge).coerceAtLeast(0))
                                val principal = currentBalance + (monthlyContribution * 12L * payYears)
                                val interest = (accumulatedFund - principal).coerceAtLeast(0L)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "원금 ${CurrencyFormatter.formatKoreanWon(principal, isShort = true)} + 복리수익 ${CurrencyFormatter.formatKoreanWon(interest, isShort = true)} (${yearsToStart}년 후 운용)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. 수령 방식 및 상호 연동 계산
                    if (type == PensionType.ANNUITY_INSURANCE) {
                        // 개인연금보험: 종신형 vs 확정기간형
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "2. 수령 방식 선택 (비과세 연금보험)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            Surface(
                                color = CyanInfo.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 10년 이상 유지 시 비과세(이자소득세 0%) 혜택 및 사적연금 1,500만원 종합과세 한도에서 전액 제외됩니다.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = annuityInsuranceOption == 0,
                                    onClick = {
                                        annuityInsuranceOption = 0
                                        endAgeStr = "100"
                                        val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                            accumulatedFund = accumulatedFund,
                                            periodYears = (100 - startAge).coerceAtLeast(1),
                                            annualGrowthRate = growthRate
                                        )
                                        monthlyPayoutStr = payout.toString()
                                    },
                                    label = { Text("종신형 (100세)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = annuityInsuranceOption == 1,
                                    onClick = {
                                        annuityInsuranceOption = 1
                                        endAgeStr = (startAge + 10).toString()
                                        val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                            accumulatedFund = accumulatedFund,
                                            periodYears = 10,
                                            annualGrowthRate = growthRate
                                        )
                                        monthlyPayoutStr = payout.toString()
                                    },
                                    label = { Text("10년 확정", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = annuityInsuranceOption == 2,
                                    onClick = {
                                        annuityInsuranceOption = 2
                                        endAgeStr = (startAge + 20).toString()
                                        val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                            accumulatedFund = accumulatedFund,
                                            periodYears = 20,
                                            annualGrowthRate = growthRate
                                        )
                                        monthlyPayoutStr = payout.toString()
                                    },
                                    label = { Text("20년 확정", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = annuityInsuranceOption == 3,
                                    onClick = { annuityInsuranceOption = 3 },
                                    label = { Text("직접입력", fontSize = 10.sp) }
                                )
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AutoSelectOutlinedTextField(
                                    value = endAgeStr,
                                    onValueChange = {
                                        endAgeStr = it.filter { c -> c.isDigit() }
                                        val endA = it.toIntOrNull() ?: 85
                                        val period = (endA - startAge).coerceAtLeast(1)
                                        val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                            accumulatedFund = accumulatedFund,
                                            periodYears = period,
                                            annualGrowthRate = growthRate
                                        )
                                        monthlyPayoutStr = payout.toString()
                                        annuityInsuranceOption = 3
                                    },
                                    label = { Text("수령 종료 나이") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                AutoSelectOutlinedTextField(
                                    value = monthlyPayoutStr,
                                    onValueChange = { monthlyPayoutStr = it.filter { c -> c.isDigit() } },
                                    label = { Text("월 예상 수령액") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        // 개인연금저축 & 퇴직연금/IRP : 양방향 상호 연동 연산
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "2. 수급 기간 ⇄ 월 수령액 상호 연동 계산",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 모드 전환 세그먼트
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = linkageMode == 0,
                                    onClick = {
                                        linkageMode = 0
                                        // 현재 endAge 기준으로 월 수령액 즉시 재계산
                                        val period = (endAge - startAge).coerceAtLeast(1)
                                        val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                            accumulatedFund = accumulatedFund,
                                            periodYears = period,
                                            annualGrowthRate = growthRate
                                        )
                                        monthlyPayoutStr = payout.toString()
                                    },
                                    label = { Text("수급 기간 기준 ➜ 월 수령액 산출", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = linkageMode == 1,
                                    onClick = {
                                        linkageMode = 1
                                        // 현재 월 수령액 기준으로 endAge 즉시 재계산
                                        val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
                                            accumulatedFund = accumulatedFund,
                                            desiredMonthlyPayout = monthlyPayout,
                                            startAge = startAge,
                                            annualGrowthRate = growthRate
                                        )
                                        endAgeStr = result.endAge.toString()
                                    },
                                    label = { Text("희망 수령액 기준 ➜ 수급 기간 산출", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (linkageMode == 0) {
                            // 모드 A: 수급 기간 지정 -> 월 수령액 자동 연동
                            item {
                                Text(text = "수급 기간 빠른 선택:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val periods = listOf(10, 15, 20, 25, 30)
                                    items(periods) { pYears ->
                                        val targetEnd = startAge + pYears
                                        SuggestionChip(
                                            onClick = {
                                                endAgeStr = targetEnd.toString()
                                                val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                                    accumulatedFund = accumulatedFund,
                                                    periodYears = pYears,
                                                    annualGrowthRate = growthRate
                                                )
                                                monthlyPayoutStr = payout.toString()
                                            },
                                            label = { Text("${pYears}년 (${targetEnd}세)", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AutoSelectOutlinedTextField(
                                        value = endAgeStr,
                                        onValueChange = {
                                            endAgeStr = it.filter { c -> c.isDigit() }
                                            val endA = it.toIntOrNull() ?: 85
                                            val period = (endA - startAge).coerceAtLeast(1)
                                            val payout = PensionPlanCalculator.calculateMonthlyPayoutFromPeriod(
                                                accumulatedFund = accumulatedFund,
                                                periodYears = period,
                                                annualGrowthRate = growthRate
                                            )
                                            monthlyPayoutStr = payout.toString()
                                        },
                                        label = { Text("수령 종료 나이") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    AutoSelectOutlinedTextField(
                                        value = monthlyPayoutStr,
                                        onValueChange = { monthlyPayoutStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("월 예상 수령액 (자동계산)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            item {
                                val period = (endAge - startAge).coerceAtLeast(0)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 ${startAge}세부터 ${endAge}세까지 ${period}년간 매월 약 ${CurrencyFormatter.formatKoreanWon(monthlyPayout)}씩 수령하게 됩니다. (적립금 잔액은 연 ${growthRate}%로 계속 운용)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        } else {
                            // 모드 B: 희망 수령액 지정 -> 수급 기간 자동 연동
                            item {
                                Text(text = "희망 월 수령액 빠른 선택:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val amounts = listOf(500_000L, 800_000L, 1_000_000L, 1_500_000L, 2_000_000L)
                                    items(amounts) { amt ->
                                        SuggestionChip(
                                            onClick = {
                                                monthlyPayoutStr = amt.toString()
                                                val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
                                                    accumulatedFund = accumulatedFund,
                                                    desiredMonthlyPayout = amt,
                                                    startAge = startAge,
                                                    annualGrowthRate = growthRate
                                                )
                                                endAgeStr = result.endAge.toString()
                                            },
                                            label = { Text(CurrencyFormatter.formatKoreanWon(amt, isShort = true), fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }

                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AutoSelectOutlinedTextField(
                                        value = monthlyPayoutStr,
                                        onValueChange = {
                                            monthlyPayoutStr = it.filter { c -> c.isDigit() }
                                            val amt = it.toLongOrNull() ?: 0L
                                            if (amt > 0L) {
                                                val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
                                                    accumulatedFund = accumulatedFund,
                                                    desiredMonthlyPayout = amt,
                                                    startAge = startAge,
                                                    annualGrowthRate = growthRate
                                                )
                                                endAgeStr = result.endAge.toString()
                                            }
                                        },
                                        label = { Text("희망 월 수령액 (원)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    AutoSelectOutlinedTextField(
                                        value = endAgeStr,
                                        onValueChange = { endAgeStr = it.filter { c -> c.isDigit() } },
                                        label = { Text("수령 종료 나이 (자동산출)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            item {
                                val result = PensionPlanCalculator.calculatePeriodFromMonthlyPayout(
                                    accumulatedFund = accumulatedFund,
                                    desiredMonthlyPayout = monthlyPayout,
                                    startAge = startAge,
                                    annualGrowthRate = growthRate
                                )
                                Surface(
                                    color = if (result.isForeverSafe) EmeraldPrimary.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (result.isForeverSafe) {
                                            "🎉 원금 보존! 운용 수익만으로 매월 ${CurrencyFormatter.formatKoreanWon(monthlyPayout)}을 평생(100세+) 수령 가능합니다."
                                        } else {
                                            "⏳ 예상 적립금으로 약 ${result.periodYears}년간 (${result.endAge}세까지) 매월 ${CurrencyFormatter.formatKoreanWon(monthlyPayout)}을 수령할 수 있습니다."
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (result.isForeverSafe) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 주택연금, 기타연금
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = startAgeStr,
                                onValueChange = { startAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("수령 개시 나이") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = endAgeStr,
                                onValueChange = { endAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("수령 종료 나이") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        AutoSelectOutlinedTextField(
                            value = monthlyPayoutStr,
                            onValueChange = { monthlyPayoutStr = it.filter { c -> c.isDigit() } },
                            label = { Text("월 예상 수령액 (원)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        AutoSelectOutlinedTextField(
                            value = growthRateStr,
                            onValueChange = { growthRateStr = it },
                            label = { Text("기대 수익률 / 물가연동률 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalStartAge = startAgeStr.toIntOrNull() ?: 60
                    val finalEndAge = endAgeStr.toIntOrNull() ?: 85
                    val finalMonthlyPayout = monthlyPayoutStr.toLongOrNull() ?: 0L
                    val finalBalance = balanceStr.toLongOrNull() ?: 0L
                    val finalContribution = monthlyContributionStr.toLongOrNull() ?: 0L
                    val finalContributionEndAge = contributionEndAgeStr.toIntOrNull() ?: finalStartAge
                    val finalGrowthRate = growthRateStr.toDoubleOrNull() ?: 4.5
                    val isTaxDeduction = if (type == PensionType.ANNUITY_INSURANCE) false else (pension?.isTaxDeductionEligible ?: (type == PensionType.PERSONAL || type == PensionType.RETIREMENT))

                    val updated = pension?.copy(
                        name = name.ifBlank { type.displayName },
                        type = type,
                        startAge = finalStartAge,
                        endAge = finalEndAge,
                        expectedMonthlyAmount = finalMonthlyPayout,
                        currentBalance = finalBalance,
                        monthlyContribution = finalContribution,
                        contributionEndAge = finalContributionEndAge,
                        expectedGrowthRate = finalGrowthRate,
                        isTaxDeductionEligible = isTaxDeduction
                    ) ?: Pension(
                        name = name.ifBlank { type.displayName },
                        type = type,
                        startAge = finalStartAge,
                        endAge = finalEndAge,
                        expectedMonthlyAmount = finalMonthlyPayout,
                        currentBalance = finalBalance,
                        monthlyContribution = finalContribution,
                        contributionEndAge = finalContributionEndAge,
                        expectedGrowthRate = finalGrowthRate,
                        isTaxDeductionEligible = isTaxDeduction
                    )
                    onSave(updated)
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}