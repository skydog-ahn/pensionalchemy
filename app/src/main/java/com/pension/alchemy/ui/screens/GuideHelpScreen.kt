package com.pension.alchemy.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.theme.*

private fun openBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideHelpScreen(
    onNavigateToTab: (ScreenTab) -> Unit,
    onClose: () -> Unit,
    onOpenOnboardingGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("앱 사용 도움말", "연금술사 재무비법", "공식 & 산식 (부록)", "정부 공식 포털", "추천 전문 채널")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "연금술사 가이드 & 재무 정보",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "앱 사용법, 노후 재무관리 가이드 및 공인 정보 채널",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 상단 카테고리 탭
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedCategoryIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedCategoryIndex) {
                    0 -> {
                        item {
                            Text(
                                text = "💡 연금술사 핵심 기능 안내 및 바로가기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "각 카드의 바로가기 버튼을 누르면 해당 기능 화면으로 즉시 이동합니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "초보자를 위한 빠른 시작 가이드",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "프리셋 선택부터 프로필/소비액 입력, 자산/연금 등록 및 대시보드 진단까지 한눈에 스와이프 카드로 확인하세요.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = onOpenOnboardingGuide,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("가이드 보기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = EmeraldPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "v1.3.2 릴리즈",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "새로워진 주요 기능",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "• 실시간 순자산 증감 티커: 양수(+)·음수(-) 상태별 불어남/줄어듦 및 증감 속도 라이브 동적 전환\n" +
                                            "• 생애 자산 시뮬레이션 궤적 데이터 스프레드시트(CSV) 다운로드 기능 탑재\n" +
                                            "• 연금관리 수령액/수령기간 진입 시 사전 자동 연산 & 예상 적립금 시인성 강화\n" +
                                            "• 자산·연금·소득 항목별 입력 기준일(baseDate) 연동 초당 실시간 가산 엔진",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            FeatureHelpCard(
                                title = "1. 대시보드 (종합 진단 & 실시간 순자산 증감)",
                                icon = Icons.Default.Dashboard,
                                accentColor = EmeraldPrimary,
                                description = "내 순자산, 65세 예상 월연금, 소득대체율과 100점 만점 은퇴 건강 점수를 확인합니다. 특히 [실시간 순자산 증감 티커]를 통해 [기준일 이후] 혹은 [오늘 0시]부터 변동된 순자산 금액을 원 단위까지 깔끔하게 확인하며, 1초 단위 초당 증감 속도를 라이브로 체감할 수 있습니다.",
                                shortcutLabel = "대시보드 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.DASHBOARD) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "2. 자산 및 정기 소득 관리 (기준일 연동 & 순서 이동)",
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = CyanInfo,
                                description = "예금, 적금, 주식, 채권, 부동산, 대출(부채) 등 보유 자산과 근로/사업소득을 등록합니다. 각 자산별 개별 기대수익률 가중합산 및 '입력 기준일(baseDate)' 연동 실시간 평가액이 반영됩니다. 항목 간 순서 이동(▲/▼)과 3층 분리 카드 배치로 금액 겹침 없는 쾌적한 화면을 제공합니다.",
                                shortcutLabel = "자산관리 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.ASSETS) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "3. 3층 연금 관리 (순서 이동 & 수령액 분리 레이아웃)",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = IndigoAccent,
                                description = "국민·퇴직·개인·주택연금을 통합 설계합니다. 연금 항목의 상하 순서 이동(▲/▼)을 지원하며, 적립금과 월 예상 수령액을 분리 배치하여 시인성을 극대화했습니다. 사적연금 급여 직접 납입 여부 스위치와 입력 기준일 연동 실시간 평가액을 제공합니다.",
                                shortcutLabel = "연금관리 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.PENSIONS) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "4. 6대 재무 계산기",
                                icon = Icons.Default.Calculate,
                                accentColor = AmberWarning,
                                description = "①적립·예탁 미래가치 ②자산인출 시뮬레이션 ③자산고갈 타이머 ④목표 필요자산 ⑤국민연금 조기/연기 손익분기 ⑥연금저축/IRP 절세 연금술 등 6가지 전문 공식을 계산합니다.",
                                shortcutLabel = "계산기 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.CALCULATOR) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "5. 환경 설정 & 생애주기 프리셋",
                                icon = Icons.Default.Settings,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                description = "출생연도, 은퇴 희망나이, 은퇴 후 예상 월생활비, 의료비 증가율, 물가상승률을 변경합니다. '30대 사회초년생', '40대 가장', '50대 은퇴임박' 프리셋으로 원터치 시뮬레이션이 가능합니다.",
                                shortcutLabel = "설정 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.SETTINGS) }
                            )
                        }
                    }

                    1 -> {
                        item {
                            Text(
                                text = "📖 성공적인 미래를 위한 5대 연금술사 재무비법",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "공인된 금융 원칙과 학술 연구(트리니티 스터디 등) 및 최신 개정 세법에 기반한 핵심 가이드입니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            FinancialGuideCard(
                                badge = "비법 1",
                                title = "3층 연금 피라미드 황금비율 구축",
                                summary = "기본 생계는 국민연금(1층), 표준 생활은 퇴직연금(2층), 여유 생활은 개인연금(3층)으로 방어망을 구축해야 합니다. 보유 주택이 있다면 주택연금(역모기지)을 평생 안전판으로 결합하세요.",
                                tips = listOf(
                                    "1층 국민연금: 매년 물가상승률(CPI)을 100% 반영하여 평생 종신 지급되는 국내 유일의 공적연금",
                                    "2층 퇴직연금: 임금상승률이 높으면 DB형, 투자 수익률을 추구하면 DC/IRP로 글로벌 지수 ETF 분산투자",
                                    "3층 연금저축/IRP: 연간 최대 900만 원 세액공제와 과세이연을 통한 장기 스노우볼 복리 극대화",
                                    "주택연금 보완: 만 55세 이상, 공시가격 12억 원 이하 주택으로 평생 거주하며 평생 연금 수령 가능"
                                )
                            )
                        }

                        item {
                            FinancialGuideCard(
                                badge = "비법 2",
                                title = "마의 5년 '소득 크레바스' 방어 전략",
                                summary = "법정 정년(만 60세)부터 국민연금 수령 개시(만 63~65세)까지 소득이 단절되는 공백기를 '소득 크레바스'라고 부릅니다. 이 시기를 버틸 '브릿지 자금' 준비가 필수입니다.",
                                tips = listOf(
                                    "개인연금저축과 IRP는 만 55세부터 연금 수령이 가능하므로 55~65세 소득 공백기 브릿지 자금으로 최적",
                                    "퇴직금은 일시금 대신 IRP로 이전하여 10년 이상 분할 수령 시 퇴직소득세 30% 감면(11년차부터 40% 감면)",
                                    "소득 크레바스 기간 국민연금 조기수령은 평생 최대 30% 감액되므로 가급적 사적연금 브릿지를 우선 활용"
                                )
                            )
                        }

                        item {
                            FinancialGuideCard(
                                badge = "비법 3",
                                title = "글로벌 '4% 안전 인출 룰'과 자산 수명 연장",
                                summary = "미국 트리니티 대학의 연구에 따르면 주식/채권 분산 포트폴리오에서 첫해 자산의 4%를 인출하고 매년 물가상승률만큼 증액 인출할 경우, 30년 이상 자산이 고갈되지 않을 확률이 95% 이상입니다.",
                                tips = listOf(
                                    "월 생활비 250만 원(연 3,000만 원) 전액을 자산에서 인출 시 4% 룰 기준 약 7.5억 원의 은퇴자산 필요",
                                    "국민연금으로 월 120만 원을 받는다면 사적 인출액은 월 130만 원으로 줄어 필요자산이 3.9억 원으로 대폭 감소",
                                    "가이튼-클링거 가드레일 전략: 강세장에는 인출액을 늘리고 하락장에는 인출을 동결/축소하여 100세까지 평생 자산 보존"
                                )
                            )
                        }

                        item {
                            FinancialGuideCard(
                                badge = "비법 4",
                                title = "연 900만원 세액공제 & 1,500만원 분리과세 복리 마법",
                                summary = "연금저축(최대 600만)과 IRP(합산 900만)를 채우면 연말정산 시 최대 148.5만 원(총급여 5,500만 이하 16.5%, 초과 시 13.2%)을 환급받습니다. 2024년 세법 개정으로 연간 1,500만 원까지 저율 분리과세됩니다.",
                                tips = listOf(
                                    "최신 개정 세법: 사적연금 분리과세 한도가 연 1,200만 원에서 연 1,500만 원으로 상향되어 3.3%~5.5% 저율 과세 혜택 확대",
                                    "일반계좌의 배당소득세(15.4%)가 매년 원천징수되지 않고 수령 시점까지 과세이연되어 복리 효과 극대화",
                                    "연말정산 환급금을 소비하지 않고 다시 연금계좌에 재투자하는 것이 진정한 연금술사의 복리 비법"
                                )
                            )
                        }

                        item {
                            FinancialGuideCard(
                                badge = "비법 5",
                                title = "국민연금 조기수령 vs 연기수령 황금나이 판정",
                                summary = "조기수령은 1년당 6% 감액(최대 -30%), 연기수령은 1년당 7.2% 증액(최대 +36%)됩니다. 생존 기간과 건강보험료 피부양자 자격을 종합적으로 따져 선택해야 합니다.",
                                tips = listOf(
                                    "조기수령(60세) vs 정상수령(65세) 교차 나이: 만 76~77세 (77세 이전에 사망 시 조기 유리, 이후 생존 시 정상수령 역전)",
                                    "정상수령(65세) vs 연기수령(70세) 교차 나이: 만 81~82세 (82세 이상 장수 시 연기수령 총수령액 압도적 우위)",
                                    "건보료 주의사항: 공적연금 소득이 연 2,000만 원(월 약 167만 원)을 초과할 경우 건강보험 피부양자 자격이 박탈되어 지역가입자 보험료가 부과되므로 종합 판단 필수"
                                )
                            )
                        }
                    }

                    2 -> {
                        item {
                            Text(
                                text = "📐 연금술사 전 분야 계산 공식 & 산식 (부록)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "앱에서 시뮬레이션 및 재무 진단에 사용하는 실제 금융 공학 및 세법 산출 공식입니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 1",
                                title = "1층 국민연금 수령액 & 조기/연기 산식",
                                formula = """
                                    // 1. 법정수급연령(1969년생 이후 65세) 기준 물가상승 복리 반영
                                    수령액(t) = 기본예상액 × (1 + 물가상승률)^(t - 현재연도)

                                    // 2. 조기수령 감액 (-0.5%/월, 1년당 -6%, 최대 5년 -30%)
                                    조기수령액 = 수령액 × [1 - (조기수령월수 × 0.005)]

                                    // 3. 연기수령 증액 (+0.6%/월, 1년당 +7.2%, 최대 5년 +36%)
                                    연기수령액 = 수령액 × [1 + (연기수령월수 × 0.006)]
                                """.trimIndent(),
                                description = "국민연금은 법정 개시 연령부터 수령할 수 있으며, 조기 수령 시 최대 30% 감액, 연기 수령 시 최대 36% 증액됩니다. 매년 설정된 물가상승률(CPI)만큼 수령액이 복리로 증액 반영됩니다.",
                                details = listOf(
                                    "조기수령 손익분기 연령: 약 76~77세 (77세 이전 사망 시 조기 유리, 이후 장수 시 정상 수령 유리)",
                                    "연기수령 손익분기 연령: 약 81~82세 (82세 이상 장수 시 연기수령 총수령액이 압도적으로 우세)",
                                    "건강보험 피부양자 탈락 기준: 공적연금 소득이 연 2,000만원(월 167만원) 초과 시 지역가입자 전환 경고"
                                ),
                                accentColor = EmeraldPrimary
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 2",
                                title = "3층 연금 세제 및 절세 감면 산식",
                                formula = """
                                    // 1. 2층 퇴직연금 (10년 이상 분할 수령 시 퇴직소득세 감면)
                                    수령 1~10년차: 세액 = 퇴직소득세 × 70% (30% 감면)
                                    수령 11년차 이후: 세액 = 퇴직소득세 × 60% (40% 감면)

                                    // 2. 3층 개인연금저축/IRP 저율 연금소득세 (1,500만원 이하)
                                    55~69세: 5.5% | 70~79세: 4.4% | 80세 이상: 3.3%
                                    * 사적연금 연 1,500만원 초과 시: 16.5% 분리과세 또는 종합과세 선택

                                    // 3. 비과세 개인연금보험 & 주택연금: 소득세 0% (비과세 100%)
                                """.trimIndent(),
                                description = "퇴직소득을 IRP로 이전하여 10년 이상 분할 수령하면 퇴직소득세가 30~40% 절감되며, 연금저축/IRP는 연 1,500만원 한도 내에서 3.3~5.5%의 저율 분리과세가 적용됩니다.",
                                details = listOf(
                                    "2024년 세법 개정: 사적연금 분리과세 한도 1,200만원 -> 1,500만원으로 확대 반영",
                                    "일반계좌의 15.4% 배당소득세가 부과되지 않고 인출 시점까지 과세이연되어 복리 효과 극대화"
                                ),
                                accentColor = CyanInfo
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 3",
                                title = "대출 원리금 4대 상환 방식 산식",
                                formula = """
                                    // 1. 원리금균등분할상환 (PMT 공식)
                                    월상환액 = P × [r(1 + r)^n] / [(1 + r)^n - 1]
                                    월이자 = 잔여원금 × r | 월원금 = 월상환액 - 월이자

                                    // 2. 원금균등분할상환
                                    월원금 = P / n | 월이자 = 잔여원금 × r | 월상환액 = 월원금 + 월이자

                                    // 3. 만기일시상환 & 거치(이자만)
                                    월이자 = P × r (만기 시점에 원금 P 전액 일시 상환)
                                    * P: 대출원금, r: 월이자율(연이율/12), n: 총상환개월수
                                """.trimIndent(),
                                description = "등록된 대출의 상환 방식에 따라 매월 납부해야 하는 원리금과 이자비용을 정확히 산출하여 은퇴 전후 가계 현금흐름에서 차감합니다.",
                                details = listOf(
                                    "원리금균등: 매월 나가는 현금흐름이 일정하여 가계 예산 관리에 용이",
                                    "원금균등: 초기 상환 부담이 크나 총 이자비용이 가장 적음",
                                    "만기일시/거치: 만기 시 원금 상환 준비가 없으면 재무 건전성 급격 악화 위험"
                                ),
                                accentColor = AmberWarning
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 4",
                                title = "생애 현금흐름 밸런싱 & 자산 증감 공식",
                                formula = """
                                    // 1. 가계 월 순현금흐름 (Net Cash Flow)
                                    순현금흐름 = 세후총소득 - [생활비 + 직접납입연금 + 대출상환액]

                                    // 2. 잉여금 발생 시 (Net Cash Flow > 0)
                                    금융자산(t+1) = 금융자산(t) × (1 + 수익률) + 연간잉여금

                                    // 3. 적자 발생 시 자산 인출 우선순위 (Net Cash Flow < 0)
                                    1순위 인출: 금융자산(예적금/주식/채권) 인출 충당
                                    2순위 인출: 금융자산 고갈 시 부동산 자산 처분 충당
                                    3순위 누적: 전 자산 고갈 시 누적 순적자(마이너스)로 표시
                                """.trimIndent(),
                                description = "소득에서 소비, 직접 납입 연금, 부채 상환을 제하고 남은 잉여금은 금융자산에 자동 재투자되어 복리로 증식되며, 적자 발생 시 유동성 자산부터 단계적으로 인출되어 자산 고갈 시점을 정밀하게 예측합니다.",
                                details = listOf(
                                    "연금 정기 납입액 중 '급여에서 직접 납입' 항목만 가계 지출에서 차감",
                                    "회사 지원 퇴직연금(DC) 등은 가계 현금흐름 차감 없이 연금 자산에만 충당"
                                ),
                                accentColor = IndigoAccent
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 5",
                                title = "자산 가중수익률 & 실시간 초당 속도 산식",
                                formula = """
                                    // 1. 개별 자산 기대수익률 가중평균
                                    가중수익률 = Σ(개별자산평가액 × 개별기대수익률) / 총자산평가액
                                    * 부동산 수익률 0% 설정 시 가치 변동 없이 완벽 보존

                                    // 2. 실시간 초당 순자산 증식 속도 (wps, Won Per Second)
                                    연간순자산증가액 = 연간총유입(소득+자산수익) - 연간총유출(소비+이자+납입)
                                    초당증식속도 = 연간순자산증가액 / (365.25 × 86,400초)

                                    // 3. 오늘 자정 이후 실시간 누적 순자산 증감액
                                    오늘누적액 = 초당증식속도 × 오늘자정이후경과초수(t)
                                """.trimIndent(),
                                description = "대시보드 상단에서 실시간으로 1초마다 올라가는 순자산 금액을 산출하는 핵심 공식입니다. 내 자산과 소득이 일하는 속도를 시각적으로 체감할 수 있습니다.",
                                details = listOf(
                                    "초당 속도가 양수면 자산이 불어나는 축적 상태, 음수면 소비가 초과하는 고갈 상태 표시",
                                    "8시간 수면 동안 불어나는 순자산 = 초당속도 × 28,800초 (취침 중 축적되는 자산 확인)"
                                ),
                                accentColor = EmeraldPrimary
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 6",
                                title = "6대 전문 재무 계산기 핵심 공식",
                                formula = """
                                    // 1. 적립 복리 미래가치 (월복리)
                                    FV = PMT × [ (1 + r/12)^(12×n) - 1 ] / (r/12) + PV × (1 + r)^n

                                    // 2. 4% 룰 기반 은퇴 목표 필요자산
                                    목표순자산 = 연간부족생활비 / 0.04 = (은퇴월필요액 - 월연금) × 12 × 25

                                    // 3. 자산 고갈 기간 (NPER 역산)
                                    인출이자율이 인출액보다 적을 때 로그 역산으로 고갈 개월 수 산출
                                    n = ln( W / (W - PV × r) ) / ln(1 + r) (W: 월인출액)

                                    // 4. 연금저축/IRP 900만원 세액공제 환급액
                                    환급액 = 연간납입액(최대 900만) × 공제율(총급여 5500만 이하 16.5%, 초과 13.2%)
                                """.trimIndent(),
                                description = "적립식 복리 증식, 4% 안전 인출 룰, 자산 고갈 타이머, 148.5만원 연말정산 세액공제 환급금 재투자 복리 효과를 수학적으로 계산합니다.",
                                details = listOf(
                                    "연말정산 환급금(최대 148.5만)을 다시 연금에 재투자할 때의 복리 스노우볼 보너스 산출",
                                    "월인출액이 운용수익률보다 낮을 경우 원금이 영구 보존되는 '영구 수급' 안내"
                                ),
                                accentColor = MaterialTheme.colorScheme.secondary
                            )
                        }

                        item {
                            FormulaHelpCard(
                                badge = "공식 7",
                                title = "소득 크레바스(소득 공백기) 브릿지 자금 산식",
                                formula = """
                                    // 소득 공백기 구간: 은퇴 나이 ~ 국민연금 수령 개시 나이
                                    공백기간(년) = max(0, 국민연금수급나이 - 은퇴나이)

                                    // 필요 브릿지 총자금
                                    필요브릿지자금 = Σ_{t=은퇴}^{수급전} [ 월은퇴생활비(t) × 12 - 사적연금수령액(t) ]
                                """.trimIndent(),
                                description = "정년 은퇴(예: 60세) 후 국민연금 개시(63~65세) 전까지 월급이 끊기는 공백기를 사적연금(55세 개시 가능) 및 금융자산으로 방어할 수 있도록 필요 자금을 역산합니다.",
                                details = listOf(
                                    "개인연금저축/IRP는 55세부터 연금 인출이 가능하여 최적의 브릿지 자금으로 기능",
                                    "소득 크레바스 기간에 자산이 마이너스로 떨어지지 않도록 유동성 자산 선확보 권장"
                                ),
                                accentColor = RoseDanger
                            )
                        }
                    }

                    3 -> {
                        item {
                            Text(
                                text = "🏛️ 대한민국 정부 & 공적기관 공식 포털",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "카드 클릭 시 공적 기관의 공식 홈페이지로 안전하게 연결됩니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            ExternalLinkCard(
                                title = "국민연금공단 (NPS) - 내 연금 알아보기",
                                subtitle = "공식 포털: nps.or.kr / csa.nps.or.kr",
                                description = "국민연금 예상 수령액, 지금까지 납부한 총 보험료 내역, 예상 가입 기간 및 노령연금 모의계산을 실시간으로 조회할 수 있습니다.",
                                tag = "1층 국민연금",
                                accentColor = EmeraldPrimary,
                                onClick = { openBrowser(context, "https://csa.nps.or.kr") }
                            )
                        }

                        item {
                            ExternalLinkCard(
                                title = "금융감독원 통합연금포털",
                                subtitle = "공식 포털: fss.or.kr/fss/lifeplan",
                                description = "내가 가입한 국민연금, 퇴직연금, 개인연금(은행·보험·증권사)을 한 화면에서 한 번에 통합 조회하고 예상 수령액을 진단합니다.",
                                tag = "통합 연금 조회",
                                accentColor = CyanInfo,
                                onClick = { openBrowser(context, "https://www.fss.or.kr/fss/lifeplan/lifeplanIndex/index.do?menuNo=201101") }
                            )
                        }

                        item {
                            ExternalLinkCard(
                                title = "한국주택금융공사 (주택연금)",
                                subtitle = "공식 포털: hf.go.kr",
                                description = "만 55세 이상 주택 소유자가 보유 주택을 담보로 평생 또는 일정 기간 매월 연금을 받는 역모기지 주택연금의 예상 월지급금을 계산합니다.",
                                tag = "주택연금/역모기지",
                                accentColor = AmberWarning,
                                onClick = { openBrowser(context, "https://www.hf.go.kr") }
                            )
                        }

                        item {
                            ExternalLinkCard(
                                title = "근로복지공단 퇴직연금",
                                subtitle = "공식 포털: pension.comwel.or.kr",
                                description = "퇴직연금(DB 확정급여형, DC 확정기여형, IRP 개인형 퇴직연금) 제도 안내 및 중소기업 퇴직연금기금 푸른씨앗 정보를 제공합니다.",
                                tag = "2층 퇴직연금",
                                accentColor = IndigoAccent,
                                onClick = { openBrowser(context, "https://pension.comwel.or.kr/websquare/?w2xPath=/pages/uti/HP00000001.xml") }
                            )
                        }

                        item {
                            ExternalLinkCard(
                                title = "국세청 홈택스 (연말정산 세액공제)",
                                subtitle = "공식 포털: hometax.go.kr",
                                description = "연금저축 및 IRP 연간 납입금액에 대한 연말정산 세액공제 증빙자료 조회와 절세 혜택 정보를 확인합니다.",
                                tag = "연말정산 절세",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                onClick = { openBrowser(context, "https://www.hometax.go.kr") }
                            )
                        }
                    }

                    4 -> {
                        item {
                            Text(
                                text = "📺 공인 금융 & 연금 전문 유튜브 채널",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "구독자 수와 공신력이 검증된 대표적인 연금·자산관리 전문가 채널입니다.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            YoutubeChannelCard(
                                channelName = "박곰희TV",
                                subscribers = "구독자 약 63만 명",
                                specialty = "연금저축 · IRP · 미국 ETF · 자산배분 1위",
                                description = "증권사 출신 박곰희 대표가 초보자도 쉽게 따라 할 수 있는 연금저축/IRP 포트폴리오 구성법, ISA 계좌 활용법, 퇴직연금 굴리는 법을 명쾌하게 전달합니다.",
                                tag = "자산배분 필수 채널",
                                onClick = { openBrowser(context, "https://www.youtube.com/@gomhee") }
                            )
                        }

                        item {
                            YoutubeChannelCard(
                                channelName = "연금박사 (이영주 대표)",
                                subscribers = "구독자 약 28만 명",
                                specialty = "은퇴설계 · 국민연금 · 연금 인출 설계",
                                description = "국내 대표 연금 전문가 이영주 소장이 국민연금 수령 시기, 건강보험료 피부양자 탈락 방어, 퇴직 후 현금흐름 창출 전략을 실전 사례 위주로 심층 분석합니다.",
                                tag = "은퇴설계 전문",
                                onClick = { openBrowser(context, "https://www.youtube.com/@pension500") }
                            )
                        }

                        item {
                            YoutubeChannelCard(
                                channelName = "미래에셋 투자와연금TV",
                                subscribers = "구독자 약 16만 명",
                                specialty = "미래에셋 투자와연금센터 공식 채널",
                                description = "금융 대기업 산하 연금 전문 싱크탱크 연구원들이 퇴직연금 시장 트렌드, TDF 펀드 분석, 사적연금 세금 관리 및 절세 인출 솔루션을 체계적으로 제공합니다.",
                                tag = "기관 싱크탱크",
                                onClick = { openBrowser(context, "https://www.youtube.com/@investpension") }
                            )
                        }

                        item {
                            YoutubeChannelCard(
                                channelName = "삼프로TV (경제의 신과함께)",
                                subscribers = "구독자 약 250만 명",
                                specialty = "대한민국 대표 경제·투자 전문 미디어",
                                description = "국내 최고 이코노미스트와 펀드매니저들이 거시경제 전망, 글로벌 시장 분석 및 중장년 노후 자산 관리 노하우를 깊이 있게 다룹니다.",
                                tag = "거시경제 & 투자",
                                onClick = { openBrowser(context, "https://www.youtube.com/@3protv") }
                            )
                        }

                        item {
                            YoutubeChannelCard(
                                channelName = "김경필 머니트레이너",
                                subscribers = "구독자 약 40만 명",
                                specialty = "월급쟁이 재테크 · 노후자금 저축 플랜",
                                description = "국민 머니트레이너 김경필 대표가 은퇴 전 반드시 모아야 할 시드머니 구축과 과소비 방지, 현실적인 연금 자산 증식 방법을 조언합니다.",
                                tag = "실전 저축 훈련",
                                onClick = { openBrowser(context, "https://www.youtube.com/@phill_ssam") }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureHelpCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    description: String,
    shortcutLabel: String,
    onShortcut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Button(
                onClick = onShortcut,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = shortcutLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FinancialGuideCard(
    badge: String,
    title: String,
    summary: String,
    tips: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "📌 실천 핵심 포인트:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    tips.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(text = tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalLinkCard(
    title: String,
    subtitle: String,
    description: String,
    tag: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tag,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "방문하기", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "열기",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun YoutubeChannelCard(
    channelName: String,
    subscribers: String,
    specialty: String,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = RoseDanger.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "YouTube",
                            color = RoseDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tag,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "채널 보기", fontSize = 11.sp, color = RoseDanger, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "유튜브 열기",
                        tint = RoseDanger,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subscribers,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "주요 테마: $specialty",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FormulaHelpCard(
    badge: String,
    title: String,
    formula: String,
    description: String,
    details: List<String> = emptyList(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formula,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (details.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    details.forEach { detail ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "• ",
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
