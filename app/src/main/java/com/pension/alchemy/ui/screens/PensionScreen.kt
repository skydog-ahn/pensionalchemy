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
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.AutoSelectOutlinedTextField
import com.pension.alchemy.util.CurrencyFormatter

@Composable
fun PensionScreen(
    pensions: List<Pension>,
    onSavePension: (Pension) -> Unit,
    onDeletePension: (String) -> Unit,
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
    onDismiss: () -> Unit,
    onSave: (Pension) -> Unit
) {
    var name by remember { mutableStateOf(pension?.name ?: "") }
    var type by remember { mutableStateOf(pension?.type ?: PensionType.PERSONAL) }
    var monthlyPayoutStr by remember { mutableStateOf(pension?.expectedMonthlyAmount?.toString() ?: "0") }
    var startAgeStr by remember { mutableStateOf(pension?.startAge?.toString() ?: "60") }
    var endAgeStr by remember { mutableStateOf(pension?.endAge?.toString() ?: "85") }
    var balanceStr by remember { mutableStateOf(pension?.currentBalance?.toString() ?: "0") }
    var monthlyContributionStr by remember { mutableStateOf(pension?.monthlyContribution?.toString() ?: "0") }
    var growthRateStr by remember { mutableStateOf(pension?.expectedGrowthRate?.toString() ?: "4.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pension == null) "연금 플랜 추가" else "연금 플랜 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AutoSelectOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("연금 이름 (예: IRP, 개인연금보험, 연금저축)") },
                    modifier = Modifier.fillMaxWidth()
                )

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
                                if (t == PensionType.ANNUITY_INSURANCE && (pension == null || pension.type != PensionType.ANNUITY_INSURANCE)) {
                                    growthRateStr = "3.5"
                                }
                            },
                            label = { Text(t.displayName.substringBefore("/"), fontSize = 10.sp) }
                        )
                    }
                }

                if (type == PensionType.ANNUITY_INSURANCE) {
                    Surface(
                        color = CyanInfo.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 개인연금보험: 10년 이상 유지 시 전액 비과세 혜택이 적용되어 연금 수령 시 세금이 부과되지 않으며, 사적연금 1,500만원 한도 합산에서도 제외됩니다.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

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

                AutoSelectOutlinedTextField(
                    value = monthlyPayoutStr,
                    onValueChange = { monthlyPayoutStr = it.filter { c -> c.isDigit() } },
                    label = { Text("월 예상 수령액 (원)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (type != PensionType.NATIONAL && type != PensionType.HOUSING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AutoSelectOutlinedTextField(
                            value = balanceStr,
                            onValueChange = { balanceStr = it.filter { c -> c.isDigit() } },
                            label = { Text("현재 적립금") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        AutoSelectOutlinedTextField(
                            value = monthlyContributionStr,
                            onValueChange = { monthlyContributionStr = it.filter { c -> c.isDigit() } },
                            label = { Text("월 납입액") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                AutoSelectOutlinedTextField(
                    value = growthRateStr,
                    onValueChange = { growthRateStr = it },
                    label = { Text("기대 운용수익률 / 물가연동률 (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startAge = startAgeStr.toIntOrNull() ?: 60
                    val endAge = endAgeStr.toIntOrNull() ?: 85
                    val monthlyPayout = monthlyPayoutStr.toLongOrNull() ?: 0L
                    val balance = balanceStr.toLongOrNull() ?: 0L
                    val contribution = monthlyContributionStr.toLongOrNull() ?: 0L
                    val growthRate = growthRateStr.toDoubleOrNull() ?: 4.5
                    val isTaxDeduction = if (type == PensionType.ANNUITY_INSURANCE) false else (pension?.isTaxDeductionEligible ?: (type == PensionType.PERSONAL || type == PensionType.RETIREMENT))

                    val updated = pension?.copy(
                        name = name.ifBlank { type.displayName },
                        type = type,
                        startAge = startAge,
                        endAge = endAge,
                        expectedMonthlyAmount = monthlyPayout,
                        currentBalance = balance,
                        monthlyContribution = contribution,
                        expectedGrowthRate = growthRate,
                        isTaxDeductionEligible = isTaxDeduction
                    ) ?: Pension(
                        name = name.ifBlank { type.displayName },
                        type = type,
                        startAge = startAge,
                        endAge = endAge,
                        expectedMonthlyAmount = monthlyPayout,
                        currentBalance = balance,
                        monthlyContribution = contribution,
                        expectedGrowthRate = growthRate,
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