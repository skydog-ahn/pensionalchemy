package com.pension.alchemy.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.BackupData
import com.pension.alchemy.data.model.PolicySettings
import com.pension.alchemy.data.model.UserProfile
import com.pension.alchemy.data.repository.PensionAlchemyRepository
import com.pension.alchemy.theme.*
import com.pension.alchemy.ui.components.AutoSelectOutlinedTextField
import com.pension.alchemy.util.CreateBackupDocumentContract
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    profile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    onApplyPreset: (Int) -> Unit,
    onResetData: () -> Unit,
    onOpenHelp: () -> Unit = {},
    repository: PensionAlchemyRepository? = null,
    calculatorViewModel: CalculatorViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 기본 프로필, 1: 세법·정책 변수(상수)

    // 백업/복구 상태
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var backupSuccessMessage by remember { mutableStateOf("") }
    var showRestoreSuccessDialog by remember { mutableStateOf(false) }
    var restoreSuccessMessage by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestorePreview by remember { mutableStateOf<BackupData?>(null) }
    var isProcessingBackup by remember { mutableStateOf(false) }

    // 1. SAF 백업 파일 저장 Launcher (사용자가 위치 및 파일명 직접 선택)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = CreateBackupDocumentContract()
    ) { uri: Uri? ->
        if (uri != null && repository != null) {
            scope.launch {
                isProcessingBackup = true
                val calcSettings = calculatorViewModel?.toSettings()
                val result = repository.exportBackupToUri(uri, calcSettings)
                isProcessingBackup = false
                result.onSuccess { backup ->
                    backupSuccessMessage = "백업 파일이 안전하게 저장되었습니다.\n\n" +
                        "• 백업 일시: ${backup.backupDate}\n" +
                        "• 연금 플랜: ${backup.pensions.size}건\n" +
                        "• 자산/부채: ${backup.assets.size}건\n" +
                        "• 정기 소득: ${backup.incomes.size}건\n" +
                        "• 계산기 및 세법 설정값 포함 완료"
                    showBackupSuccessDialog = true
                }.onFailure { err ->
                    Toast.makeText(context, "백업 저장에 실패했습니다: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 2. SAF 백업 파일 열기 Launcher (사용자가 복구할 .json 파일 선택)
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && repository != null) {
            scope.launch {
                val previewResult = repository.readBackupPreviewFromUri(uri)
                previewResult.onSuccess { preview ->
                    pendingRestorePreview = preview
                    showRestoreConfirmDialog = true
                }.onFailure { err ->
                    Toast.makeText(context, "올바른 백업 파일(.json)이 아니거나 손상되었습니다: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // 1. 기본 프로필 상태
    // ──────────────────────────────────────────────
    var birthYearStr by remember(profile.birthYear) { mutableStateOf(profile.birthYear.toString()) }
    var retirementAgeStr by remember(profile.retirementAge) { mutableStateOf(profile.retirementAge.toString()) }
    var targetEndAgeStr by remember(profile.targetEndAge) { mutableStateOf(profile.targetEndAge.toString()) }
    var expensesManwonStr by remember(profile.monthlyExpenses) { mutableStateOf((profile.monthlyExpenses / 10_000L).toString()) }
    var inflationStr by remember(profile.inflationRate) { mutableStateOf(profile.inflationRate.toString()) }

    // ──────────────────────────────────────────────
    // 2. 세법 & 정책 변수 (상수) 상태
    // ──────────────────────────────────────────────
    val policy = profile.policySettings
    var privLimitManwonStr by remember(policy.privatePensionAnnualLimit) { mutableStateOf((policy.privatePensionAnnualLimit / 10_000L).toString()) }
    var healthLimitManwonStr by remember(policy.healthInsurancePensionLimit) { mutableStateOf((policy.healthInsurancePensionLimit / 10_000L).toString()) }
    var maxDeductionManwonStr by remember(policy.maxPensionIncomeDeduction) { mutableStateOf((policy.maxPensionIncomeDeduction / 10_000L).toString()) }
    var basicDeductionManwonStr by remember(policy.basicPersonalDeduction) { mutableStateOf((policy.basicPersonalDeduction / 10_000L).toString()) }

    var rateUnder70Str by remember(policy.privatePensionRateUnder70) { mutableStateOf(policy.privatePensionRateUnder70.toString()) }
    var rateUnder80Str by remember(policy.privatePensionRateUnder80) { mutableStateOf(policy.privatePensionRateUnder80.toString()) }
    var rate80OrOverStr by remember(policy.privatePensionRate80OrOver) { mutableStateOf(policy.privatePensionRate80OrOver.toString()) }
    var excessRateStr by remember(policy.privatePensionExcessRate) { mutableStateOf(policy.privatePensionExcessRate.toString()) }

    var retDiscountEarlyStr by remember(policy.retirementTaxDiscountRateEarly) { mutableStateOf(policy.retirementTaxDiscountRateEarly.toString()) }
    var retDiscountLateStr by remember(policy.retirementTaxDiscountRateLate) { mutableStateOf(policy.retirementTaxDiscountRateLate.toString()) }
    var baseRetTaxRateStr by remember(policy.baseRetirementTaxRate) { mutableStateOf(policy.baseRetirementTaxRate.toString()) }

    var taxCreditLimitManwonStr by remember(policy.maxTaxCreditContribution) { mutableStateOf((policy.maxTaxCreditContribution / 10_000L).toString()) }
    var taxCreditLowStr by remember(policy.taxCreditRateLowIncome) { mutableStateOf(policy.taxCreditRateLowIncome.toString()) }
    var taxCreditHighStr by remember(policy.taxCreditRateHighIncome) { mutableStateOf(policy.taxCreditRateHighIncome.toString()) }
    var interestTaxRateStr by remember(policy.generalInterestTaxRate) { mutableStateOf(policy.generalInterestTaxRate.toString()) }

    var natEarlyRateStr by remember(policy.nationalEarlyReductionRatePerYear) { mutableStateOf(policy.nationalEarlyReductionRatePerYear.toString()) }
    var natDelayRateStr by remember(policy.nationalDelayIncreaseRatePerYear) { mutableStateOf(policy.nationalDelayIncreaseRatePerYear.toString()) }

    var finYieldStr by remember(policy.financialAssetReturnRate) { mutableStateOf(policy.financialAssetReturnRate.toString()) }
    var reYieldStr by remember(policy.realEstateGrowthRate) { mutableStateOf(policy.realEstateGrowthRate.toString()) }
    var medSurchargeStr by remember(policy.medicalInflationSurcharge) { mutableStateOf(policy.medicalInflationSurcharge.toString()) }
    var wealthDistMeanStr by remember(policy.wealthDistributionMean) { mutableStateOf(policy.wealthDistributionMean.toString()) }
    var wealthDistStdDevStr by remember(policy.wealthDistributionStdDev) { mutableStateOf(policy.wealthDistributionStdDev.toString()) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        birthYearStr = profile.birthYear.toString()
        retirementAgeStr = profile.retirementAge.toString()
        targetEndAgeStr = profile.targetEndAge.toString()
        expensesManwonStr = (profile.monthlyExpenses / 10_000L).toString()
        inflationStr = profile.inflationRate.toString()

        val pol = profile.policySettings
        privLimitManwonStr = (pol.privatePensionAnnualLimit / 10_000L).toString()
        healthLimitManwonStr = (pol.healthInsurancePensionLimit / 10_000L).toString()
        maxDeductionManwonStr = (pol.maxPensionIncomeDeduction / 10_000L).toString()
        basicDeductionManwonStr = (pol.basicPersonalDeduction / 10_000L).toString()

        rateUnder70Str = pol.privatePensionRateUnder70.toString()
        rateUnder80Str = pol.privatePensionRateUnder80.toString()
        rate80OrOverStr = pol.privatePensionRate80OrOver.toString()
        excessRateStr = pol.privatePensionExcessRate.toString()

        retDiscountEarlyStr = pol.retirementTaxDiscountRateEarly.toString()
        retDiscountLateStr = pol.retirementTaxDiscountRateLate.toString()
        baseRetTaxRateStr = pol.baseRetirementTaxRate.toString()

        taxCreditLimitManwonStr = (pol.maxTaxCreditContribution / 10_000L).toString()
        taxCreditLowStr = pol.taxCreditRateLowIncome.toString()
        taxCreditHighStr = pol.taxCreditRateHighIncome.toString()
        interestTaxRateStr = pol.generalInterestTaxRate.toString()

        natEarlyRateStr = pol.nationalEarlyReductionRatePerYear.toString()
        natDelayRateStr = pol.nationalDelayIncreaseRatePerYear.toString()

        finYieldStr = pol.financialAssetReturnRate.toString()
        reYieldStr = pol.realEstateGrowthRate.toString()
        medSurchargeStr = pol.medicalInflationSurcharge.toString()
        wealthDistMeanStr = pol.wealthDistributionMean.toString()
        wealthDistStdDevStr = pol.wealthDistributionStdDev.toString()
    }

    fun saveAll(isPolicyTab: Boolean = false) {
        val birth = birthYearStr.toIntOrNull() ?: 1985
        val ret = retirementAgeStr.toIntOrNull() ?: 60
        val endAge = targetEndAgeStr.toIntOrNull() ?: 100
        val exp = (expensesManwonStr.toLongOrNull() ?: 250L) * 10_000L
        val inf = inflationStr.toDoubleOrNull() ?: 2.0

        val updatedPolicy = PolicySettings(
            privatePensionAnnualLimit = (privLimitManwonStr.toLongOrNull() ?: 1500L) * 10_000L,
            healthInsurancePensionLimit = (healthLimitManwonStr.toLongOrNull() ?: 2000L) * 10_000L,
            maxPensionIncomeDeduction = (maxDeductionManwonStr.toLongOrNull() ?: 900L) * 10_000L,
            basicPersonalDeduction = (basicDeductionManwonStr.toLongOrNull() ?: 150L) * 10_000L,
            privatePensionRateUnder70 = rateUnder70Str.toDoubleOrNull() ?: 5.5,
            privatePensionRateUnder80 = rateUnder80Str.toDoubleOrNull() ?: 4.4,
            privatePensionRate80OrOver = rate80OrOverStr.toDoubleOrNull() ?: 3.3,
            privatePensionExcessRate = excessRateStr.toDoubleOrNull() ?: 16.5,
            retirementTaxDiscountRateEarly = retDiscountEarlyStr.toDoubleOrNull() ?: 30.0,
            retirementTaxDiscountRateLate = retDiscountLateStr.toDoubleOrNull() ?: 40.0,
            baseRetirementTaxRate = baseRetTaxRateStr.toDoubleOrNull() ?: 5.0,
            maxTaxCreditContribution = (taxCreditLimitManwonStr.toLongOrNull() ?: 900L) * 10_000L,
            taxCreditRateLowIncome = taxCreditLowStr.toDoubleOrNull() ?: 16.5,
            taxCreditRateHighIncome = taxCreditHighStr.toDoubleOrNull() ?: 13.2,
            generalInterestTaxRate = interestTaxRateStr.toDoubleOrNull() ?: 15.4,
            nationalEarlyReductionRatePerYear = natEarlyRateStr.toDoubleOrNull() ?: 6.0,
            nationalDelayIncreaseRatePerYear = natDelayRateStr.toDoubleOrNull() ?: 7.2,
            financialAssetReturnRate = finYieldStr.toDoubleOrNull() ?: 4.5,
            realEstateGrowthRate = reYieldStr.toDoubleOrNull() ?: 2.0,
            medicalInflationSurcharge = medSurchargeStr.toDoubleOrNull() ?: 3.0,
            wealthDistributionMean = wealthDistMeanStr.toDoubleOrNull() ?: 1.00984,
            wealthDistributionStdDev = wealthDistStdDevStr.toDoubleOrNull() ?: 1.1937
        )

        onUpdateProfile(
            profile.copy(
                birthYear = birth,
                retirementAge = ret,
                targetEndAge = endAge,
                monthlyExpenses = exp,
                inflationRate = inf,
                policySettings = updatedPolicy
            )
        )
        val msg = if (isPolicyTab) "세법 및 정책 변수가 저장되었습니다." else "프로필 설정이 저장되었습니다."
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun resetPolicyDefaults() {
        val def = PolicySettings()
        privLimitManwonStr = (def.privatePensionAnnualLimit / 10_000L).toString()
        healthLimitManwonStr = (def.healthInsurancePensionLimit / 10_000L).toString()
        maxDeductionManwonStr = (def.maxPensionIncomeDeduction / 10_000L).toString()
        basicDeductionManwonStr = (def.basicPersonalDeduction / 10_000L).toString()
        rateUnder70Str = def.privatePensionRateUnder70.toString()
        rateUnder80Str = def.privatePensionRateUnder80.toString()
        rate80OrOverStr = def.privatePensionRate80OrOver.toString()
        excessRateStr = def.privatePensionExcessRate.toString()
        retDiscountEarlyStr = def.retirementTaxDiscountRateEarly.toString()
        retDiscountLateStr = def.retirementTaxDiscountRateLate.toString()
        baseRetTaxRateStr = def.baseRetirementTaxRate.toString()
        taxCreditLimitManwonStr = (def.maxTaxCreditContribution / 10_000L).toString()
        taxCreditLowStr = def.taxCreditRateLowIncome.toString()
        taxCreditHighStr = def.taxCreditRateHighIncome.toString()
        interestTaxRateStr = def.generalInterestTaxRate.toString()
        natEarlyRateStr = def.nationalEarlyReductionRatePerYear.toString()
        natDelayRateStr = def.nationalDelayIncreaseRatePerYear.toString()
        finYieldStr = def.financialAssetReturnRate.toString()
        reYieldStr = def.realEstateGrowthRate.toString()
        medSurchargeStr = def.medicalInflationSurcharge.toString()
        wealthDistMeanStr = def.wealthDistributionMean.toString()
        wealthDistStdDevStr = def.wealthDistributionStdDev.toString()
        Toast.makeText(context, "법정 기본값으로 복원되었습니다. 저장을 눌러주세요.", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 상단 탭 (프로필 vs 정책 상수)
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("개인 프로필 & 프리셋", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🏛️ 세법 및 정책 변수 (상수)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // ──────────────────────────────────────────────
                // TAB 0: 개인 프로필 & 생애주기 프리셋
                // ──────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "기본 프로필 & 생애 주기 목표", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = birthYearStr,
                                onValueChange = { birthYearStr = it.filter { c -> c.isDigit() } },
                                label = { Text("출생년도") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = retirementAgeStr,
                                onValueChange = { retirementAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("희망 은퇴나이 (세)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = targetEndAgeStr,
                                onValueChange = { targetEndAgeStr = it.filter { c -> c.isDigit() } },
                                label = { Text("목표 수명 (세)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AutoSelectOutlinedTextField(
                            value = expensesManwonStr,
                            onValueChange = { expensesManwonStr = it.filter { c -> c.isDigit() } },
                            label = { Text("은퇴 후 월 희망 생활비 (현재 가치 기준 / 단위: 만원)") },
                            trailingIcon = {
                                Text(
                                    text = "만원",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 현재 가치 안내 및 은퇴 시점 미래가치 자동 환산 미리보기 배너
                        val currentAgeCalc = (LocalDate.now().year - (birthYearStr.toIntOrNull() ?: 1985)).coerceAtLeast(0)
                        val retAgeCalc = retirementAgeStr.toIntOrNull() ?: 60
                        val yearsToRet = (retAgeCalc - currentAgeCalc).coerceAtLeast(0)
                        val currentExpensesManwon = expensesManwonStr.toDoubleOrNull() ?: 0.0
                        val inflRate = (inflationStr.toDoubleOrNull() ?: 2.0) / 100.0
                        val futureExpensesManwon = (currentExpensesManwon * (1.0 + inflRate).pow(yearsToRet.toDouble())).roundToLong()

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "현재 가치 기준 입력 안내",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• 입력하시는 생활비는 '현재 물가 기준 가치'입니다.\n" +
                                            "• 시뮬레이션 시 은퇴 시점까지의 연간 물가상승률(연 ${String.format(Locale.US, "%.1f", inflRate * 100)}%)이 복리로 자동 환산되어 미래 생활비로 반영됩니다.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                                if (currentExpensesManwon > 0.0 && yearsToRet > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🎯 ${retAgeCalc}세 은퇴 시점 미래가치 환산액: 월 약 ${futureExpensesManwon}만원 (${yearsToRet}년간 물가 복리 반영)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        AutoSelectOutlinedTextField(
                            value = inflationStr,
                            onValueChange = { inflationStr = it },
                            label = { Text("연간 물가상승률 (% / 한국은행 기준 2.0%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { saveAll(isPolicyTab = false) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("프로필 설정 저장 및 시뮬레이션 반영", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 생애주기별 프리셋 로드 카드 (20대 ~ 60대)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "생애주기별 표준 시뮬레이션 프리셋", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "현재 만 ${profile.currentAge}세",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "20대부터 60대까지 연령대별 표준 자산, 부채, 정기소득 및 3층 연금 포트폴리오를 원클릭으로 로드하여 실시간 시뮬레이션을 탐색합니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val currentDecade = when (profile.currentAge) {
                            in 20..29 -> 20
                            in 30..39 -> 30
                            in 40..49 -> 40
                            in 50..59 -> 50
                            else -> 60
                        }

                        // 1행: 20대, 30대, 40대
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val list1 = listOf(
                                Triple(20, "20대", "청년기"),
                                Triple(30, "30대", "형성기"),
                                Triple(40, "40대", "표준가장")
                            )
                            list1.forEach { (decade, title, subtitle) ->
                                val isSelected = currentDecade == decade
                                if (isSelected) {
                                    Button(
                                        onClick = {
                                            onApplyPreset(decade)
                                            Toast.makeText(context, "${title} (${subtitle}) 표준 프리셋이 즉시 반영되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("$title $subtitle", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            onApplyPreset(decade)
                                            Toast.makeText(context, "${title} (${subtitle}) 표준 프리셋이 즉시 반영되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("$title $subtitle", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }

                        // 2행: 50대, 60대
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val list2 = listOf(
                                Triple(50, "50대", "은퇴준비기"),
                                Triple(60, "60대", "은퇴생활기")
                            )
                            list2.forEach { (decade, title, subtitle) ->
                                val isSelected = currentDecade == decade
                                if (isSelected) {
                                    Button(
                                        onClick = {
                                            onApplyPreset(decade)
                                            Toast.makeText(context, "${title} (${subtitle}) 표준 프리셋이 즉시 반영되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("$title $subtitle", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            onApplyPreset(decade)
                                            Toast.makeText(context, "${title} (${subtitle}) 표준 프리셋이 즉시 반영되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("$title $subtitle", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }

                        // 선택된 프리셋 안내 설명 카드
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val desc = when (currentDecade) {
                                    20 -> "💡 20대 청년기: 26세 사회초년생 • 청년도약/청약저축 • 학자금 대출 상환 및 국민연금/개인연금 초기 적립 시작"
                                    30 -> "💡 30대 자산형성기: 35세 직장인 • 전세/주택 마련 및 연금저축/IRP 세액공제 한도(900만원) 본격 활용"
                                    40 -> "💡 40대 표준가장: 45세 대한민국 대표 가장 • 아파트 보유 및 주담대 상환 • 3층 연금(국민+퇴직+개인) 집중 불입"
                                    50 -> "💡 50대 은퇴준비기: 55세 은퇴 직전 • 자산 축적 가속 • 비과세 개인연금보험 및 주택연금(역모기지) 준비"
                                    else -> "💡 60대 은퇴생활기: 63세 은퇴 완료 • 부채 전액 상환 • 국민연금 및 퇴직/개인연금 10년 이상 분할 인출 절세 운용"
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ──────────────────────────────────────────────
                // 💾 데이터 백업 및 복구 카드 (데이터 영구 유지)
                // ──────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "데이터 백업 및 복구",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "사용자 데이터 유지",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "자산, 연금, 기타소득, 생애 프로필, 세법 정책 변수, 계산기 사용자 설정값 등 모든 변수 값을 파일로 안전하게 백업하고 언제든 다시 복구합니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 1. 백업 파일 저장 (위치 선택)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    if (repository != null) {
                                        val defaultFileName = repository.generateBackupFileName()
                                        createDocLauncher.launch(defaultFileName)
                                    } else {
                                        Toast.makeText(context, "저장소 인스턴스를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("백업 파일 저장 (저장 위치 지정)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "• 기본 저장 위치(Documents 또는 Downloads)나 Google Drive 등 원하는 위치를 정하여 JSON 파일로 저장합니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 2. Download 폴더로 빠른 백업
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (repository != null) {
                                        scope.launch {
                                            isProcessingBackup = true
                                            val calcSettings = calculatorViewModel?.toSettings()
                                            val result = repository.exportBackupToDownloads(calcSettings)
                                            isProcessingBackup = false
                                            result.onSuccess { (path, backup) ->
                                                backupSuccessMessage = "기기 Download 폴더에 백업 파일이 저장되었습니다.\n\n" +
                                                    "• 저장 파일: $path\n" +
                                                    "• 백업 일시: ${backup.backupDate}\n" +
                                                    "• 연금 플랜: ${backup.pensions.size}건\n" +
                                                    "• 자산/부채: ${backup.assets.size}건\n" +
                                                    "• 정기 소득: ${backup.incomes.size}건\n" +
                                                    "• 계산기 및 세법 설정값 포함 완료"
                                                showBackupSuccessDialog = true
                                            }.onFailure { e ->
                                                Toast.makeText(context, "빠른 백업 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download 폴더로 빠른 백업", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "• 별도의 위치 선택 없이 기기의 Download 폴더에 즉시 저장합니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 3. 백업 파일 복구
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("백업 파일 읽어 복구 (데이터 복원)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "• 이전에 백업한 파일(.json)을 선택하여 모든 자산, 연금, 계산기 설정을 복원합니다.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 도움말 및 재무 가이드 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(text = "연금술사 사용 가이드 & 공인 정보", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "기능 설명, 정부 공식 포털, 유명 유튜브 채널", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenHelp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("열기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 데이터 초기화 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "데이터 초기화", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "모든 자산/연금/소득을 기본값으로 복원", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = { showResetConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                        ) {
                            Text("초기화")
                        }
                    }
                }
            } else {
                // ──────────────────────────────────────────────
                // TAB 1: 🏛️ 세법 및 정책 변수 (상수 설정)
                // ──────────────────────────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "세법, 건강보험 및 금융 정책 변경 시 모든 계산 상수를 자유롭게 변경할 수 있습니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 1. 공적/사적연금 법정 한도 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "1. 연금 세법 및 건강보험 한도 (단위: 만원)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = privLimitManwonStr,
                                onValueChange = { privLimitManwonStr = it.filter { c -> c.isDigit() } },
                                label = { Text("사적연금 저율과세 한도") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = healthLimitManwonStr,
                                onValueChange = { healthLimitManwonStr = it.filter { c -> c.isDigit() } },
                                label = { Text("건보 피부양자 탈락 한도") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = maxDeductionManwonStr,
                                onValueChange = { maxDeductionManwonStr = it.filter { c -> c.isDigit() } },
                                label = { Text("연금소득공제 최대 한도") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = basicDeductionManwonStr,
                                onValueChange = { basicDeductionManwonStr = it.filter { c -> c.isDigit() } },
                                label = { Text("1인 기본 인적공제") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. 사적연금 연령별 세율 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "2. 사적연금 인출 세율 (%)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = rateUnder70Str,
                                onValueChange = { rateUnder70Str = it },
                                label = { Text("55~69세 세율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = rateUnder80Str,
                                onValueChange = { rateUnder80Str = it },
                                label = { Text("70~79세 세율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = rate80OrOverStr,
                                onValueChange = { rate80OrOverStr = it },
                                label = { Text("80세 이상 세율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = excessRateStr,
                                onValueChange = { excessRateStr = it },
                                label = { Text("한도초과 분리과세율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. 퇴직연금 감면율 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "3. 퇴직연금(IRP) 분할인출 감면율 (%)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = retDiscountEarlyStr,
                                onValueChange = { retDiscountEarlyStr = it },
                                label = { Text("1~10년차 감면율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = retDiscountLateStr,
                                onValueChange = { retDiscountLateStr = it },
                                label = { Text("11년차 이상 감면율") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AutoSelectOutlinedTextField(
                            value = baseRetTaxRateStr,
                            onValueChange = { baseRetTaxRateStr = it },
                            label = { Text("기준 평균 퇴직소득세율 (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. 세액공제, 국민연금 및 자산운용 수익률
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "4. 세액공제, 국민연금 및 시뮬레이션 수익률", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = taxCreditLimitManwonStr,
                                onValueChange = { taxCreditLimitManwonStr = it.filter { c -> c.isDigit() } },
                                label = { Text("세액공제 납입한도 (만원)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = interestTaxRateStr,
                                onValueChange = { interestTaxRateStr = it },
                                label = { Text("이자소득세율 (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = taxCreditLowStr,
                                onValueChange = { taxCreditLowStr = it },
                                label = { Text("총급여 5500만 이하 공제율 (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = taxCreditHighStr,
                                onValueChange = { taxCreditHighStr = it },
                                label = { Text("총급여 5500만 초과 공제율 (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = natEarlyRateStr,
                                onValueChange = { natEarlyRateStr = it },
                                label = { Text("국민연금 조기 감액률 (%/년)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = natDelayRateStr,
                                onValueChange = { natDelayRateStr = it },
                                label = { Text("국민연금 연기 증액률 (%/년)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = finYieldStr,
                                onValueChange = { finYieldStr = it },
                                label = { Text("금융자산 수익률 (%/년)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = reYieldStr,
                                onValueChange = { reYieldStr = it },
                                label = { Text("부동산 성장률 (%/년)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AutoSelectOutlinedTextField(
                            value = medSurchargeStr,
                            onValueChange = { medSurchargeStr = it },
                            label = { Text("노후 의료비 물가 추가 가중률 (%/년)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 5. 대한민국 순자산 분포 모수 (로그정규분포 기준) 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "5. 대한민국 순자산 분포 모수 (로그정규분포, 단위: 억원)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "대시보드 하단의 '대한민국 순자산 백분위 분포' 차트에 반영되는 모수입니다. (2025 가계금융복지조사 기준 평균 μ=1.00984, 표준편차 σ=1.1937)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AutoSelectOutlinedTextField(
                                value = wealthDistMeanStr,
                                onValueChange = { wealthDistMeanStr = it },
                                label = { Text("평균 모수 (μ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            AutoSelectOutlinedTextField(
                                value = wealthDistStdDevStr,
                                onValueChange = { wealthDistStdDevStr = it },
                                label = { Text("표준편차 모수 (σ)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 버튼 그룹: 기본값 복원 & 저장
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { resetPolicyDefaults() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("법정 기본값 복원", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { saveAll(isPolicyTab = true) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("정책 변수 저장 및 적용", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("데이터 초기화") },
            text = { Text("모든 데이터가 기본 샘플 상태로 복원됩니다. 계속하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text("초기화 실행")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("취소") }
            }
        )
    }

    // ──────────────────────────────────────────────
    // 데이터 복구 확인 다이얼로그
    // ──────────────────────────────────────────────
    if (showRestoreConfirmDialog && pendingRestorePreview != null) {
        val preview = pendingRestorePreview!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning) },
            title = { Text("데이터 복구 확인", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "선택한 백업 파일의 내용으로 데이터를 복구하시겠습니까?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• 백업 일시: ${preview.backupDate}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("• 연금 플랜: ${preview.pensions.size}건", fontSize = 12.sp)
                            Text("• 자산/부채: ${preview.assets.size}건", fontSize = 12.sp)
                            Text("• 정기 소득: ${preview.incomes.size}건", fontSize = 12.sp)
                            Text("• 프로필: ${preview.userProfile.birthYear}년생 (은퇴 ${preview.userProfile.retirementAge}세 / 희망월 ${preview.userProfile.monthlyExpenses / 10000}만원)", fontSize = 12.sp)
                            Text("• 계산기 사용자 설정값 포함됨", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "⚠️ 복구 실행 시 현재 앱의 모든 데이터가 백업 파일의 내용으로 완전히 대체됩니다.",
                        fontSize = 11.sp,
                        color = RoseDanger,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repository != null) {
                            scope.launch {
                                val result = repository.restoreFromBackupData(preview)
                                result.onSuccess {
                                    calculatorViewModel?.applySettings(preview.calculatorSettings)
                                    showRestoreConfirmDialog = false
                                    restoreSuccessMessage = "데이터가 성공적으로 복구되었습니다!\n\n" +
                                        "• 복구 기준: ${preview.backupDate}\n" +
                                        "• 연금 ${preview.pensions.size}건, 자산 ${preview.assets.size}건, 소득 ${preview.incomes.size}건\n" +
                                        "• 프로필, 정책 변수, 계산기 설정값 모두 복원 완료"
                                    showRestoreSuccessDialog = true
                                }.onFailure { e ->
                                    Toast.makeText(context, "복구 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("복구 실행", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 백업 완료 다이얼로그
    if (showBackupSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary) },
            title = { Text("백업 완료", fontWeight = FontWeight.Bold) },
            text = { Text(backupSuccessMessage, fontSize = 13.sp, lineHeight = 18.sp) },
            confirmButton = {
                Button(onClick = { showBackupSuccessDialog = false }) {
                    Text("확인")
                }
            }
        )
    }

    // 복구 완료 다이얼로그
    if (showRestoreSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary) },
            title = { Text("복구 완료", fontWeight = FontWeight.Bold) },
            text = { Text(restoreSuccessMessage, fontSize = 13.sp, lineHeight = 18.sp) },
            confirmButton = {
                Button(onClick = { showRestoreSuccessDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}