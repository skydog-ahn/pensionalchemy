package com.pension.alchemy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.pension.alchemy.data.model.*
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.AutoSelectOutlinedTextField
import com.pension.alchemy.ui.components.BaseDateInputField
import com.pension.alchemy.ui.components.CompactRealTimeCurrencyText
import com.pension.alchemy.util.CurrencyFormatter
import com.pension.alchemy.util.LoanCalculator
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

@Composable
fun AssetScreen(
    assets: List<Asset>,
    incomes: List<Income>,
    onSaveAsset: (Asset) -> Unit,
    onDeleteAsset: (String) -> Unit,
    onReorderAssets: (List<Asset>) -> Unit = {},
    onSaveIncome: (Income) -> Unit,
    onDeleteIncome: (String) -> Unit,
    onReorderIncomes: (List<Income>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 자산, 1: 소득
    var showAssetDialog by remember { mutableStateOf(false) }
    var editingAsset by remember { mutableStateOf<Asset?>(null) }

    var showIncomeDialog by remember { mutableStateOf(false) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }

    // 1초 단위 타이머
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            currentDateTime = LocalDateTime.now()
        }
    }

    val realTimeGrowth = remember(assets, incomes, currentDateTime) {
        RealTimeGrowthCalculator.calculateTotalRealTimeGrowth(
            assets = assets,
            pensions = emptyList(),
            incomes = incomes,
            currentDateTime = currentDateTime
        )
    }

    val totalAssets = if (assets.any { !it.isLiability }) realTimeGrowth.realTimeTotalGrossAssets else 0L
    val totalDebt = assets.filter { it.isLiability }.sumOf { it.currentValue }
    val netWorth = (totalAssets - totalDebt).coerceAtLeast(0L)
    val totalMonthlyIncome = incomes.sumOf { it.monthlyAmount }

    fun moveAsset(from: Int, to: Int) {
        if (from in assets.indices && to in assets.indices) {
            val list = assets.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            onReorderAssets(list)
        }
    }

    fun moveIncome(from: Int, to: Int) {
        if (from in incomes.indices && to in incomes.indices) {
            val list = incomes.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            onReorderIncomes(list)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingAsset = null
                        showAssetDialog = true
                    } else {
                        editingIncome = null
                        showIncomeDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "추가")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 상단 탭 (자산 vs 소득)
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("자산 및 부채 (${assets.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("정기 소득 (${incomes.size})", fontWeight = FontWeight.Bold) }
                )
            }

            // 상단 요약 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 1) 순자산 (주요 지표 - 상단 단독 배치로 긴 금액도 넉넉하게 표시)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "순자산 (Net Worth)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatKoreanWon(netWorth),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )

                        // 2) 총자산 & 총부채 (서브 지표 2열)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "총자산", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatKoreanWon(totalAssets),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "총부채", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatKoreanWon(totalDebt, isShort = false),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalDebt > 0) RoseDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryCol("총 월 소득", CurrencyFormatter.formatKoreanWon(totalMonthlyIncome, isShort = true) + "/월", EmeraldPrimary)
                        SummaryCol("연간 환산", CurrencyFormatter.formatKoreanWon(totalMonthlyIncome * 12L, isShort = true), CyanInfo)
                    }
                }
            }

            // 리스트 (순서 조정 가능)
            if (selectedTab == 0) {
                if (assets.isEmpty()) {
                    EmptyStateView("등록된 자산이 없습니다.\n우측 하단 + 버튼을 눌러 추가하세요.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
                            AssetItemRow(
                                asset = asset,
                                currentDateTime = currentDateTime,
                                canMoveUp = index > 0,
                                canMoveDown = index < assets.lastIndex,
                                onMoveUp = { moveAsset(index, index - 1) },
                                onMoveDown = { moveAsset(index, index + 1) },
                                onEdit = {
                                    editingAsset = asset
                                    showAssetDialog = true
                                },
                                onDelete = { onDeleteAsset(asset.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(96.dp)) }
                    }
                }
            } else {
                if (incomes.isEmpty()) {
                    EmptyStateView("등록된 소득이 없습니다.\n우측 하단 + 버튼을 눌러 추가하세요.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(incomes, key = { _, income -> income.id }) { index, income ->
                            IncomeItemRow(
                                income = income,
                                canMoveUp = index > 0,
                                canMoveDown = index < incomes.lastIndex,
                                onMoveUp = { moveIncome(index, index - 1) },
                                onMoveDown = { moveIncome(index, index + 1) },
                                onEdit = {
                                    editingIncome = income
                                    showIncomeDialog = true
                                },
                                onDelete = { onDeleteIncome(income.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(96.dp)) }
                    }
                }
            }
        }
    }

    // 자산 추가/수정 다이얼로그
    if (showAssetDialog) {
        AssetEditDialog(
            asset = editingAsset,
            onDismiss = { showAssetDialog = false },
            onSave = {
                onSaveAsset(it)
                showAssetDialog = false
            }
        )
    }

    // 소득 추가/수정 다이얼로그
    if (showIncomeDialog) {
        IncomeEditDialog(
            income = editingIncome,
            onDismiss = { showIncomeDialog = false },
            onSave = {
                onSaveIncome(it)
                showIncomeDialog = false
            }
        )
    }
}

@Composable
private fun SummaryCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun AssetItemRow(
    asset: Asset,
    currentDateTime: LocalDateTime = LocalDateTime.now(),
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val growth = remember(asset, currentDateTime) {
        RealTimeGrowthCalculator.calculateAssetGrowth(asset, currentDateTime)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. 상단 행: 자산 명칭 + 분류 뱃지 vs 위/아래 이동 및 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        color = if (asset.isLiability) RoseDanger.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = asset.type.displayName,
                            fontSize = 11.sp,
                            color = if (asset.isLiability) RoseDanger else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "위로 이동", tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "아래로 이동", tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(17.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 메인 금액 행: 전체 가로폭 활용하여 긴 금액도 줄바꿈 없이 표시
            val displayVal = if (!asset.isLiability && growth.accumulatedGrowth != 0L) growth.realTimeValue else asset.currentValue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (asset.isLiability) "대출 원금 (잔여 ${asset.maturityYears}년)" else "현재 평가액",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatKoreanWon(displayVal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (asset.isLiability) RoseDanger else MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 서브 메타 정보 행: 기준일 / 수익률 / 실시간 가산액
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (asset.isLiability) {
                    val monthlyPay = LoanCalculator.calculateMonthlyPayment(
                        principal = asset.currentValue,
                        annualRatePercent = asset.expectedGrowthRate,
                        maturityYears = asset.maturityYears,
                        repaymentMethod = asset.repaymentMethod
                    )
                    Text(
                        text = "기준: ${asset.effectiveBaseDate} · 금리 ${asset.expectedGrowthRate}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "월 상환 약 ${CurrencyFormatter.formatKoreanWon(monthlyPay)}/월",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoseDanger.copy(alpha = 0.9f)
                    )
                } else {
                    Text(
                        text = "기준: ${asset.effectiveBaseDate} · 연 ${asset.expectedGrowthRate}%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (growth.accumulatedGrowth != 0L) {
                        val growthPrefix = if (growth.accumulatedGrowth >= 0) "+" else ""
                        Text(
                            text = "가산 $growthPrefix${CurrencyFormatter.formatKoreanWon(growth.accumulatedGrowth)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeItemRow(
    income: Income,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. 헤더: 소득 명칭 + 분류 뱃지 vs 위/아래 이동 및 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = income.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        color = CyanInfo.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = income.type.displayName,
                            fontSize = 11.sp,
                            color = CyanInfo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "위로 이동", tint = if (canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "아래로 이동", tint = if (canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray.copy(alpha = 0.25f), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(17.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 메인 금액
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "월 정기 유입",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+${CurrencyFormatter.formatKoreanWon(income.monthlyAmount)}/월",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary,
                    softWrap = false,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 서브 메타 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "기준: ${income.effectiveBaseDate} · 종료: ${income.endAge}세",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "연 상승률 ${income.expectedGrowthRate}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetEditDialog(
    asset: Asset?,
    onDismiss: () -> Unit,
    onSave: (Asset) -> Unit
) {
    var name by remember { mutableStateOf(asset?.name ?: "") }
    var type by remember { mutableStateOf(asset?.type ?: AssetType.DEPOSIT) }
    var valueStr by remember { mutableStateOf(asset?.currentValue?.toString() ?: "0") }
    var rateStr by remember { mutableStateOf(asset?.expectedGrowthRate?.toString() ?: "3.0") }
    var repaymentMethod by remember { mutableStateOf(asset?.repaymentMethod ?: RepaymentMethod.EQUAL_PRINCIPAL_AND_INTEREST) }
    var maturityYearsStr by remember { mutableStateOf(asset?.maturityYears?.toString() ?: "10") }
    var baseDate by remember {
        mutableStateOf(asset?.baseDate?.ifBlank { LocalDate.now().toString() } ?: LocalDate.now().toString())
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
        title = { Text(if (asset == null) (if (type.isLiability) "부채(대출) 추가" else "자산 추가") else (if (type.isLiability) "부채(대출) 수정" else "자산 수정")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AutoSelectOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (type.isLiability) "대출 이름 (예: 주택담보대출, 전세자금대출 등)" else "자산 이름 (예: 거주 아파트, 주식 등)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 자산 타입 선택 드롭다운 대안: 스크롤 가능한 칩 Row
                Text(text = "분류: ${type.displayName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(AssetType.values()) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                if (asset == null) {
                                    rateStr = when (t) {
                                        AssetType.DEPOSIT -> "3.0"
                                        AssetType.SAVINGS -> "3.5"
                                        AssetType.BOND -> "4.0"
                                        AssetType.COMMODITY -> "4.5"
                                        AssetType.STOCK, AssetType.ETF -> "7.0"
                                        AssetType.REAL_ESTATE -> "3.0"
                                        AssetType.CRYPTO -> "10.0"
                                        AssetType.DEBT -> "4.5"
                                        AssetType.OTHER -> "3.0"
                                    }
                                }
                            },
                            label = { Text(t.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                AutoSelectOutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it.filter { c -> c.isDigit() } },
                    label = { Text(if (type.isLiability) "대출 잔액 (원)" else "금액 (원)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                AutoSelectOutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text(if (type.isLiability) "대출 금리 (%)" else "연간 기대수익률 (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                BaseDateInputField(
                    baseDate = baseDate,
                    onDateChange = { baseDate = it },
                    label = if (type.isLiability) "대출 기준일 (실행일자)" else "자산 기준일 (평가/입력일자)"
                )

                // 부채(대출)일 경우 상환 방식 및 기간 설정 추가
                if (type.isLiability) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "대출 상환 설정",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoseDanger
                    )

                    Text(text = "상환 방식: ${repaymentMethod.displayName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(RepaymentMethod.values()) { m ->
                            FilterChip(
                                selected = repaymentMethod == m,
                                onClick = { repaymentMethod = m },
                                label = { Text(m.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    AutoSelectOutlinedTextField(
                        value = maturityYearsStr,
                        onValueChange = { maturityYearsStr = it.filter { c -> c.isDigit() } },
                        label = { Text("상환 기간 (년, 1~40년)") },
                        trailingIcon = {
                            Text(
                                text = "년",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 실시간 계산된 월 예상 상환액 미리보기
                    val previewPrincipal = valueStr.toLongOrNull() ?: 0L
                    val previewRate = rateStr.toDoubleOrNull() ?: 0.0
                    val previewYears = (maturityYearsStr.toIntOrNull() ?: 10).coerceAtLeast(1)
                    val previewMonthly = LoanCalculator.calculateMonthlyPayment(
                        principal = previewPrincipal,
                        annualRatePercent = previewRate,
                        maturityYears = previewYears,
                        repaymentMethod = repaymentMethod
                    )

                    Surface(
                        color = RoseDanger.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "예상 월 상환액: 약 ${CurrencyFormatter.formatKoreanWon(previewMonthly)}/월",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseDanger
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = repaymentMethod.shortDescription,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = valueStr.toLongOrNull() ?: 0L
                    val rate = rateStr.toDoubleOrNull() ?: 3.0
                    val years = (maturityYearsStr.toIntOrNull() ?: 10).coerceIn(1, 50)
                    val updated = asset?.copy(
                        name = name.ifBlank { if (type.isLiability) "대출" else "자산" },
                        type = type,
                        currentValue = value,
                        expectedGrowthRate = rate,
                        repaymentMethod = repaymentMethod,
                        maturityYears = years,
                        baseDate = baseDate.trim()
                    ) ?: Asset(
                        name = name.ifBlank { if (type.isLiability) "대출" else "자산" },
                        type = type,
                        currentValue = value,
                        expectedGrowthRate = rate,
                        repaymentMethod = repaymentMethod,
                        maturityYears = years,
                        baseDate = baseDate.trim()
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

@Composable
private fun IncomeEditDialog(
    income: Income?,
    onDismiss: () -> Unit,
    onSave: (Income) -> Unit
) {
    var name by remember { mutableStateOf(income?.name ?: "") }
    var type by remember { mutableStateOf(income?.type ?: IncomeType.SALARY) }
    var amountStr by remember { mutableStateOf(income?.monthlyAmount?.toString() ?: "0") }
    var endAgeStr by remember { mutableStateOf(income?.endAge?.toString() ?: "60") }
    var rateStr by remember { mutableStateOf(income?.expectedGrowthRate?.toString() ?: "2.0") }
    var baseDate by remember {
        mutableStateOf(income?.baseDate?.ifBlank { LocalDate.now().toString() } ?: LocalDate.now().toString())
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
        title = { Text(if (income == null) "정기 소득 추가" else "소득 수정") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AutoSelectOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("소득 명칭 (예: 회사 급여, 임대료 등)") },
                    modifier = Modifier.fillMaxWidth()
                )

                AutoSelectOutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() } },
                    label = { Text("월 수령액 (원)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                AutoSelectOutlinedTextField(
                    value = endAgeStr,
                    onValueChange = { endAgeStr = it.filter { c -> c.isDigit() } },
                    label = { Text("종료 나이 (은퇴 시점, 세)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                AutoSelectOutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("소득 상승률 (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                BaseDateInputField(
                    baseDate = baseDate,
                    onDateChange = { baseDate = it },
                    label = "소득 기준일 (입력일자)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toLongOrNull() ?: 0L
                    val endAge = endAgeStr.toIntOrNull() ?: 60
                    val rate = rateStr.toDoubleOrNull() ?: 2.0
                    val updated = income?.copy(
                        name = name.ifBlank { "소득" },
                        type = type,
                        monthlyAmount = amount,
                        endAge = endAge,
                        expectedGrowthRate = rate,
                        baseDate = baseDate.trim()
                    ) ?: Income(
                        name = name.ifBlank { "소득" },
                        type = type,
                        monthlyAmount = amount,
                        endAge = endAge,
                        expectedGrowthRate = rate,
                        baseDate = baseDate.trim()
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