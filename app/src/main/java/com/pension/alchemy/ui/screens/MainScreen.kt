package com.pension.alchemy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pension.alchemy.BuildConfig
import com.pension.alchemy.data.repository.PensionAlchemyRepository
import com.pension.alchemy.domain.engine.SimulationEngine
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String, val icon: ImageVector) {
    DASHBOARD("대시보드", Icons.Default.Dashboard),
    ASSETS("자산관리", Icons.Default.AccountBalanceWallet),
    PENSIONS("연금플랜", Icons.AutoMirrored.Filled.TrendingUp),
    CALCULATOR("계산기", Icons.Default.Calculate),
    SETTINGS("설정", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { PensionAlchemyRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val calculatorViewModel: CalculatorViewModel = viewModel()

    val pensions by repository.pensions.collectAsState()
    val assets by repository.assets.collectAsState()
    val incomes by repository.incomes.collectAsState()
    val profile by repository.userProfile.collectAsState()
    val calcSettings by repository.calculatorSettings.collectAsState()

    // 저장된 계산기 설정값을 CalculatorViewModel에 실시간/초기 동기화
    LaunchedEffect(calcSettings) {
        calculatorViewModel.applySettings(calcSettings)
    }

    // 실시간 생애 시뮬레이션 계산
    val simulationSummary = remember(profile, pensions, assets, incomes) {
        SimulationEngine.runComprehensiveSimulation(profile, pensions, assets, incomes)
    }

    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showOnboardingGuide by remember { mutableStateOf(false) }

    // 앱 시작 시 온보딩 가이드 노출 여부 확인
    LaunchedEffect(Unit) {
        if (repository.shouldShowOnboardingGuide()) {
            showOnboardingGuide = true
        }
    }

    if (showOnboardingGuide) {
        com.pension.alchemy.ui.components.OnboardingGuideDialog(
            onDismiss = { dontShowAgain ->
                if (dontShowAgain) {
                    repository.setHideOnboardingGuide(true)
                }
                showOnboardingGuide = false
            }
        )
    }

    if (showHelpScreen) {
        GuideHelpScreen(
            onNavigateToTab = { targetTab ->
                currentTab = targetTab
                showHelpScreen = false
            },
            onClose = { showHelpScreen = false },
            onOpenOnboardingGuide = { showOnboardingGuide = true }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "연금술사",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "v ${BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                )
                            }
                            Text(
                                text = currentTab.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showHelpScreen = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "도움말 및 재무 가이드",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    ScreenTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(text = tab.title, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                when (currentTab) {
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        summary = simulationSummary,
                        profile = profile
                    )
                    ScreenTab.ASSETS -> AssetScreen(
                        assets = assets,
                        incomes = incomes,
                        onSaveAsset = { scope.launch { repository.saveAsset(it) } },
                        onDeleteAsset = { scope.launch { repository.deleteAsset(it) } },
                        onReorderAssets = { scope.launch { repository.reorderAssets(it) } },
                        onSaveIncome = { scope.launch { repository.saveIncome(it) } },
                        onDeleteIncome = { scope.launch { repository.deleteIncome(it) } },
                        onReorderIncomes = { scope.launch { repository.reorderIncomes(it) } }
                    )
                    ScreenTab.PENSIONS -> PensionScreen(
                        pensions = pensions,
                        onSavePension = { scope.launch { repository.savePension(it) } },
                        onDeletePension = { scope.launch { repository.deletePension(it) } },
                        currentAge = profile.currentAge
                    )
                    ScreenTab.CALCULATOR -> CalculatorScreen(
                        profile = profile,
                        viewModel = calculatorViewModel
                    )
                    ScreenTab.SETTINGS -> SettingsScreen(
                        profile = profile,
                        repository = repository,
                        calculatorViewModel = calculatorViewModel,
                        onUpdateProfile = { scope.launch { repository.updateProfile(it) } },
                        onApplyPreset = { preset -> scope.launch { repository.applyPreset(preset) } },
                        onResetData = { scope.launch { repository.resetAllData() } },
                        onOpenHelp = { showHelpScreen = true },
                        onOpenOnboardingGuide = { showOnboardingGuide = true }
                    )
                }
            }
        }
    }
}