package com.pension.alchemy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pension.alchemy.theme.*
import kotlinx.coroutines.launch

data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val description: String,
    val actionTips: List<String>,
    val highlightBadge: String
)

@Composable
fun OnboardingGuideDialog(
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                stepNumber = 1,
                title = "⚡ 맞춤형 프리셋으로 1초 만에 시작하기",
                subtitle = "내 연령대와 상황에 꼭 맞는 현실적인 기본 데이터 자동 세팅",
                icon = Icons.Default.Bolt,
                accentColor = AmberWarning,
                description = "처음 시작할 때 모든 것을 일일이 입력하기 어려우신가요? [설정] 탭의 생애주기 프리셋을 터치해보세요.",
                actionTips = listOf(
                    "20대 사회초년생, 30대 신혼·맞벌이, 40대 대한민국 표준 가장",
                    "50대 은퇴가속기, 60대 은퇴생활자 맞춤 시나리오 제공",
                    "프리셋을 불러온 뒤 내 실제 상황에 맞게 손쉽게 숫자만 변경 가능"
                ),
                highlightBadge = "[설정] 탭 ➔ 생애주기 프리셋 터치"
            ),
            OnboardingStep(
                stepNumber = 2,
                title = "👤 내 프로필 & 현재/은퇴 후 지출 입력",
                subtitle = "현재 생활비와 은퇴 후 필요 지출을 명확히 구분하여 계산",
                icon = Icons.Default.AccountCircle,
                accentColor = EmeraldPrimary,
                description = "출생년도, 은퇴예정나이와 함께 '현재 생활 소비액'과 '은퇴 후 필요 지출'을 입력합니다.",
                actionTips = listOf(
                    "현재 월 생활비: 현재 나이부터 은퇴 전까지 소득에서 차감되어 저축/자산 축적 계산에 반영",
                    "은퇴 후 필요 지출: 현재가치 기준으로 입력하며, 은퇴 시점 실제 미래가치(FV)가 복리로 자동 환산되어 표기",
                    "은퇴 시점 도달 시 은퇴 후 지출로 자동 전환되어 크레바스 및 자산 인출 계산 시작"
                ),
                highlightBadge = "[설정] 탭 ➔ 현재 생활비 & 은퇴 후 생활비 입력"
            ),
            OnboardingStep(
                stepNumber = 3,
                title = "💰 자산·부채 및 3층 연금 플랜 등록",
                subtitle = "국민·퇴직·개인·주택연금과 보유 자산/대출 정밀 등록",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = CyanInfo,
                description = "보유 중인 자산(예적금, 부동산, 주식/ETF)과 부채(대출 원리금 상환), 그리고 든든한 연금을 등록하세요.",
                actionTips = listOf(
                    "[자산관리]: 자산별 기대수익률, 대출 상환방식(원리금균등 등)과 만기 연수 입력",
                    "[연금플랜]: 국민연금 출생연도별 법정수령나이 자동 판정 및 조기/연기(-30%~+36%) 슬라이더 제공",
                    "퇴직연금(DB/DC/IRP), 세액공제 개인연금저축, 주택연금 종신 수령액 정밀 반영"
                ),
                highlightBadge = "[자산관리] & [연금플랜] 탭에서 입력"
            ),
            OnboardingStep(
                stepNumber = 4,
                title = "📊 대시보드에서 100세 인생 시뮬레이션 진단",
                subtitle = "자산 궤적, 소득 크레바스, 연금 골든타임, 세금·건보료 한눈에 확인",
                icon = Icons.Default.Dashboard,
                accentColor = IndigoAccent,
                description = "20세부터 100세까지 나이 슬라이더를 움직이며 내 노후 자산의 수명과 월 현금흐름을 시각적으로 탐색하세요.",
                actionTips = listOf(
                    "소득 크레바스: 은퇴 후 국민연금 수령 전까지의 소득 공백기 부족액 정밀 진단",
                    "세무 및 건보료: 사적연금 연 1,500만원 분리과세 한도 및 건보료 피부양자 자격 탈락 조기 경보",
                    "100점 만점 연금술사 은퇴 건강 점수로 노후 준비도 즉시 평가"
                ),
                highlightBadge = "[대시보드] 탭 ➔ 나이 슬라이더 움직이며 확인"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()
    var dontShowAgain by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 상단 헤더: 타이틀 & 닫기 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${pagerState.currentPage + 1}/${steps.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "연금술사 시작 가이드",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { onDismiss(dontShowAgain) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "가이드 닫기",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 페이저 (스와이프 카드 영역)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    OnboardingCardContent(step = step)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 인디케이터 (도트 점들)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { idx, _ ->
                        val isCurrent = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isCurrent) 22.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // "다시 보지 않기" 체크박스
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "이 시작 가이드를 다시 보지 않기 (도움말 탭에서 언제든 다시 볼 수 있습니다)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 하단 내비게이션 버튼 (이전 / 다음 / 시작하기)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("이전", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val isLastPage = pagerState.currentPage == steps.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onDismiss(dontShowAgain)
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(if (pagerState.currentPage > 0) 1.5f else 1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastPage) EmeraldPrimary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isLastPage) "연금술사 시작하기 🎉" else "다음 단계",
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLastPage) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingCardContent(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 아이콘 및 하이라이트 뱃지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = step.accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = step.accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = step.highlightBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // 제목 및 부제목
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = step.subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // 주요 설명
            Text(
                text = step.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // 행동 팁 리스트
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💡 주요 활용 팁",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    step.actionTips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "• ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = step.accentColor
                            )
                            Text(
                                text = tip,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
