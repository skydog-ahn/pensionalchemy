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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("앱 사용 도움말", "연금술사 재무비법", "정부 공식 포털", "추천 전문 채널")

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
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        item {
                            FeatureHelpCard(
                                title = "1. 대시보드 (종합 진단 & 생애 자산 궤적)",
                                icon = Icons.Default.Dashboard,
                                accentColor = EmeraldPrimary,
                                description = "내 순자산, 65세 예상 월연금, 소득대체율과 100점 만점 은퇴 건강 점수를 확인합니다. 나이 슬라이더(20~100세)를 움직여 각 연령대의 자산과 월 현금흐름을 시각적으로 탐색할 수 있습니다.",
                                shortcutLabel = "대시보드 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.DASHBOARD) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "2. 자산 및 정기 소득 관리",
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = CyanInfo,
                                description = "예금, 적금, 주식, 채권, 부동산, 대출(부채) 등 보유 자산과 근로/사업/임대소득을 등록합니다. 각 자산의 기대수익률과 부채 이자율이 100세 시뮬레이션에 정밀하게 반영됩니다.",
                                shortcutLabel = "자산관리 바로가기",
                                onShortcut = { onNavigateToTab(ScreenTab.ASSETS) }
                            )
                        }

                        item {
                            FeatureHelpCard(
                                title = "3. 3층 연금 플랜 (국민·퇴직·개인·주택연금)",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                accentColor = IndigoAccent,
                                description = "출생연도에 따른 국민연금 법정 개시 연령을 자동 판정하고, 조기(-5년)/연기(+5년) 슬라이더로 감액(-30%) 및 증액(+36%) 효과를 실시간으로 확인하고 조정합니다.",
                                shortcutLabel = "연금플랜 바로가기",
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

                    3 -> {
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
