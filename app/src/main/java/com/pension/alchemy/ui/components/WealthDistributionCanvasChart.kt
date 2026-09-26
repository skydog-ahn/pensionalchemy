package com.pension.alchemy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.theme.EmeraldPrimary
import com.pension.alchemy.util.CurrencyFormatter
import com.pension.alchemy.util.LogNormalDistribution
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

/**
 * 대한민국 국내 순자산 백분위 분포 차트 (로그정규분포 기반)
 *
 * - 순자산 단위: 억원 (1억원 = 100,000,000원)
 * - 2025년 기준 로그정규분포 추정 모수: 평균(mu)=1.00984, 표준편차(sigma)=1.1937
 * - 확률 표현: P(X > x)
 * - 현재 순자산 기준 위치 표기 및 상위 % 표시
 * - 차트 터치/드래그 인터랙션 및 직접 억원 값 입력 지원
 */
@Composable
fun WealthDistributionCanvasChart(
    currentNetWorthWon: Long,
    mu: Double = 1.00984,
    sigma: Double = 1.1937,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // 현재 사용자 순자산 (억원 단위, 0 이하인 경우 0.0)
    val currentNetWorthOk = (currentNetWorthWon.toDouble() / 100_000_000.0).coerceAtLeast(0.0)

    // 사용자가 현재 조회/탐색 중인 자산 (억원)
    var inspectedOk by remember(currentNetWorthWon) {
        mutableDoubleStateOf(currentNetWorthOk)
    }

    // 직접 입력 텍스트: 사용자 타이핑 중 자동 포맷팅 덮어쓰기 방지를 위해 currentNetWorthWon 기준으로만 초기화
    var inputStr by remember(currentNetWorthWon) {
        mutableStateOf(formatAssetNumber(currentNetWorthOk))
    }

    // 모수 안전 보정
    val safeMu = if (mu.isNaN() || mu == 0.0) 1.00984 else mu
    val safeSigma = if (sigma.isNaN() || sigma <= 0.0) 1.1937 else sigma

    // 상위 확률 P(X > x) 계산
    val inspectedProb = LogNormalDistribution.probabilityExceeding(inspectedOk, safeMu, safeSigma)
    val currentProb = LogNormalDistribution.probabilityExceeding(currentNetWorthOk, safeMu, safeSigma)

    // 차트 가로축 최대 범위 (maxX, 억원) 동적 스케일링
    val maxX = remember(inspectedOk, currentNetWorthOk, safeMu, safeSigma) {
        val benchmarkTop1 = LogNormalDistribution.topPercentileValue(1.0, safeMu, safeSigma) // 상위 1% 약 44억
        val maxTarget = max(inspectedOk, currentNetWorthOk)
        val calculatedMax = max(35.0, max(benchmarkTop1 * 0.8, maxTarget * 1.3))
        // 5단위로 올림하여 정갈한 눈금 유지
        (ceil(calculatedMax / 5.0) * 5.0).coerceIn(35.0, 300.0)
    }

    // 색상 토큰
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val textVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    // 주요 벤치마크 (상위 50% 중위, 20%, 10%, 5%, 1%)
    val p50Value = remember(safeMu, safeSigma) { LogNormalDistribution.topPercentileValue(50.0, safeMu, safeSigma) }
    val p20Value = remember(safeMu, safeSigma) { LogNormalDistribution.topPercentileValue(20.0, safeMu, safeSigma) }
    val p10Value = remember(safeMu, safeSigma) { LogNormalDistribution.topPercentileValue(10.0, safeMu, safeSigma) }
    val p5Value = remember(safeMu, safeSigma) { LogNormalDistribution.topPercentileValue(5.0, safeMu, safeSigma) }
    val p1Value = remember(safeMu, safeSigma) { LogNormalDistribution.topPercentileValue(1.0, safeMu, safeSigma) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 헤더: 타이틀 & 모수 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "대한민국 순자산 백분위 분포",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "2025 대한민국 가계 순자산 로그정규분포 모델",
                    style = MaterialTheme.typography.labelSmall,
                    color = textVariantColor
                )
            }
            Surface(
                color = primaryColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "μ=${String.format(Locale.US, "%.2f", safeMu)}, σ=${String.format(Locale.US, "%.2f", safeSigma)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )
                }
            }
        }

        // 2. 핵심 확률 표기 배너: P(X > x) (세로 배치로 드래그 시 텍스트 찌그러짐 방지)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1) 상태 구분 뱃지 및 내 자산 복귀 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMyAsset = abs(inspectedOk - currentNetWorthOk) < 0.05
                    Surface(
                        color = if (isMyAsset) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isMyAsset) "현재 내 순자산 기준 백분위" else "선택/탐색 자산 기준 백분위",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMyAsset) primaryColor else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (!isMyAsset) {
                        OutlinedButton(
                            onClick = {
                                inspectedOk = currentNetWorthOk
                                inputStr = formatAssetNumber(currentNetWorthOk)
                                focusManager.clearFocus()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("내 자산 복귀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2) 대형 P(X > x) 표기 (단독 세로 행, 가로 전체 폭 확보)
                Text(
                    text = LogNormalDistribution.formatPXExceeds(inspectedOk, inspectedProb),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = primaryColor,
                    softWrap = false,
                    maxLines = 1
                )

                // 3) 계층 평가 (단독 세로 행)
                val topRankPercent = inspectedProb * 100.0
                val rankText = when {
                    topRankPercent <= 1.0 -> "대한민국 상위 1% 이내 초부유층"
                    topRankPercent <= 5.0 -> "대한민국 상위 5% 이내 최상위층"
                    topRankPercent <= 10.0 -> "대한민국 상위 10% 이내 상류층"
                    topRankPercent <= 20.0 -> "대한민국 상위 20% 이내 고자산가"
                    topRankPercent <= 50.0 -> "대한민국 상위 50% 이내 중산층 이상"
                    else -> "대한민국 평균 및 중위권 자산 구간"
                }

                Text(
                    text = "• 자산 계층: $rankText",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    softWrap = false,
                    maxLines = 1
                )

                // 4) 기준 순자산 환산 금액 (단독 세로 행)
                val wonAmount = (inspectedOk * 100_000_000.0).toLong()
                Text(
                    text = "• 기준 순자산: ${CurrencyFormatter.formatKoreanWon(wonAmount)}",
                    fontSize = 12.sp,
                    color = textVariantColor,
                    softWrap = false,
                    maxLines = 1
                )

                // 5) 내 현재 순자산 및 확률 비교 (행 분리로 글자 잘림 방지 및 카드 높이 완전 고정)
                HorizontalDivider(color = outlineColor.copy(alpha = 0.3f))
                val isDifferent = abs(inspectedOk - currentNetWorthOk) >= 0.05
                Text(
                    text = if (isDifferent) {
                        "• 내 현재 순자산: ${CurrencyFormatter.formatKoreanWon(currentNetWorthWon)}"
                    } else {
                        "• 내 현재 순자산: ${CurrencyFormatter.formatKoreanWon(currentNetWorthWon)} (현재 일치)"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isDifferent) FontWeight.Medium else FontWeight.Normal,
                    color = if (isDifferent) primaryColor else textVariantColor.copy(alpha = 0.85f),
                    softWrap = false,
                    maxLines = 1
                )
                Text(
                    text = "• 내 순자산 확률: P(X > ${String.format(Locale.US, "%.1f", currentNetWorthOk)}억) = ${String.format(Locale.US, "%.1f", currentProb * 100.0)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDifferent) primaryColor else textVariantColor.copy(alpha = 0.85f),
                    softWrap = false,
                    maxLines = 1
                )
            }
        }

        // 3. 차트 조작 안내 문구
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = textVariantColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "차트를 터치하거나 드래그하여 원하는 자산 위치의 백분위를 확인하세요.",
                    fontSize = 10.sp,
                    color = textVariantColor
                )
            }
        }

        // 4. 대화형 인터랙티브 로그정규분포 캔버스 차트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxX) {
                        detectTapGestures { offset ->
                            val touchRatio = (offset.x / size.width).coerceIn(0f, 1f)
                            val selectedOk = touchRatio * maxX
                            inspectedOk = (round(selectedOk * 10.0) / 10.0).coerceAtLeast(0.0)
                            inputStr = formatAssetNumber(inspectedOk)
                            focusManager.clearFocus()
                        }
                    }
                    .pointerInput(maxX) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val touchRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            val selectedOk = touchRatio * maxX
                            inspectedOk = (round(selectedOk * 10.0) / 10.0).coerceAtLeast(0.0)
                            inputStr = formatAssetNumber(inspectedOk)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 22.dp.toPx()
                val usableHeight = height - paddingBottom

                // 로그정규분포의 Mode(최빈값): exp(mu - sigma^2)
                val modeX = exp(safeMu - safeSigma * safeSigma)
                val peakPdf = LogNormalDistribution.pdf(modeX, safeMu, safeSigma)
                val maxPdf = if (peakPdf <= 0.0) 0.3 else peakPdf * 1.08

                // X좌표 변환 함수 (자산 억원 -> 픽셀)
                fun getCanvasX(assetOk: Double): Float {
                    return ((assetOk / maxX).toFloat().coerceIn(0f, 1f)) * width
                }

                // Y좌표 변환 함수 (PDF 밀도 -> 픽셀)
                fun getCanvasY(pdfVal: Double): Float {
                    val ratio = (pdfVal / maxPdf).toFloat().coerceIn(0f, 1f)
                    return usableHeight - (ratio * (usableHeight - 12.dp.toPx()))
                }

                // 배경 가로 가이드선 (3개)
                val guideLines = 3
                for (i in 0..guideLines) {
                    val y = (usableHeight / guideLines) * i
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 곡선 포인트 샘플링 (120개 구간)
                val sampleCount = 140
                val points = ArrayList<Offset>(sampleCount + 1)
                for (i in 0..sampleCount) {
                    val xVal = (i.toDouble() / sampleCount.toDouble()) * maxX
                    val pdfVal = LogNormalDistribution.pdf(xVal, safeMu, safeSigma)
                    points.add(Offset(getCanvasX(xVal), getCanvasY(pdfVal)))
                }

                // 1. 전체 PDF 영역 기본 채우기 (연한 단색/그라데이션)
                val fullFillPath = Path().apply {
                    moveTo(0f, usableHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(width, usableHeight)
                    close()
                }
                drawPath(
                    path = fullFillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.15f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = usableHeight
                    )
                )

                // 2. 상위 영역 P(X > x) 하이라이트 채우기 (선택된 x부터 오른쪽 끝까지)
                val inspectedX = getCanvasX(inspectedOk)
                val inspectedPdf = LogNormalDistribution.pdf(inspectedOk, safeMu, safeSigma)
                val inspectedY = getCanvasY(inspectedPdf)

                val rightTailPath = Path().apply {
                    moveTo(inspectedX, usableHeight)
                    lineTo(inspectedX, inspectedY)

                    // inspectedX 이후의 포인트들 연결
                    points.filter { it.x >= inspectedX }.forEach { pt ->
                        lineTo(pt.x, pt.y)
                    }

                    lineTo(width, usableHeight)
                    close()
                }
                drawPath(
                    path = rightTailPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            primaryColor.copy(alpha = 0.12f)
                        ),
                        startY = inspectedY,
                        endY = usableHeight
                    )
                )

                // 3. 전체 PDF 곡선 라인 그리기
                val curvePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                }
                drawPath(
                    path = curvePath,
                    color = primaryColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 4. 대한민국 중위값 (상위 50%) 기준선 (은은한 회색 점선)
                val p50X = getCanvasX(p50Value)
                if (p50X in 0f..width) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.4f),
                        start = Offset(p50X, 0f),
                        end = Offset(p50X, usableHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // 5. 사용자의 현재 순자산 기준 마커 (만약 사용자가 다른 자산을 탐색 중일 때 위치 보존 표시)
                if (abs(inspectedOk - currentNetWorthOk) >= 0.05) {
                    val myX = getCanvasX(currentNetWorthOk)
                    val myPdf = LogNormalDistribution.pdf(currentNetWorthOk, safeMu, safeSigma)
                    val myY = getCanvasY(myPdf)

                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(myX, 0f),
                        end = Offset(myX, usableHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.5f),
                        radius = 4.dp.toPx(),
                        center = Offset(myX, myY)
                    )
                }

                // 6. 현재 선택/탐색 중인 위치 마커 (수직선 + 강조 포인트)
                drawLine(
                    color = primaryColor,
                    start = Offset(inspectedX, 0f),
                    end = Offset(inspectedX, usableHeight),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )

                // 마커 포인트 (외부 원 + 내부 원)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 9.dp.toPx(),
                    center = Offset(inspectedX, inspectedY)
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(inspectedX, inspectedY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(inspectedX, inspectedY)
                )

                // 7. 하단 기준선
                drawLine(
                    color = outlineColor.copy(alpha = 0.6f),
                    start = Offset(0f, usableHeight),
                    end = Offset(width, usableHeight),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // 5. X축 눈금 라벨 (0억, 5억, 10억, 20억, 30억 등 동적 눈금)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = when {
                maxX <= 50.0 -> 10.0
                maxX <= 100.0 -> 20.0
                else -> 50.0
            }
            Text(text = "0억", fontSize = 10.sp, color = textVariantColor, maxLines = 1, softWrap = false)

            // 중위수 표기 (약 2.7억)
            if (maxX >= 15.0) {
                Text(
                    text = "중위(${String.format(Locale.US, "%.1f", p50Value)}억)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = textVariantColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    softWrap = false
                )
            }

            var tick = step
            while (tick < maxX) {
                Text(text = "${tick.toInt()}억", fontSize = 10.sp, color = textVariantColor, maxLines = 1, softWrap = false)
                tick += step
            }
            Text(text = "${maxX.toInt()}억+", fontSize = 10.sp, color = textVariantColor, maxLines = 1, softWrap = false)
        }

        HorizontalDivider(color = outlineColor.copy(alpha = 0.4f))

        // 6. 직접 자산 금액 입력 필드 & 벤치마크 칩
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "직접 금액 입력 및 주요 분위수 바로보기",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoSelectOutlinedTextField(
                    value = inputStr,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            inputStr = filtered
                            val parsed = filtered.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                inspectedOk = parsed
                            }
                        }
                    },
                    label = { Text("확인할 순자산 (단위: 억원)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    trailingIcon = {
                        Text(
                            text = "억원",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    delay(200)
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                )

                Button(
                    onClick = {
                        inspectedOk = currentNetWorthOk
                        inputStr = formatAssetNumber(currentNetWorthOk)
                        focusManager.clearFocus()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("내 자산", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 분위수 빠른 선택 칩
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BenchmarkChip(
                    label = "중위 50%",
                    valueOk = p50Value,
                    selected = abs(inspectedOk - p50Value) < 0.1,
                    onClick = {
                        inspectedOk = round(p50Value * 10.0) / 10.0
                        inputStr = formatAssetNumber(inspectedOk)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f)
                )
                BenchmarkChip(
                    label = "상위 20%",
                    valueOk = p20Value,
                    selected = abs(inspectedOk - p20Value) < 0.1,
                    onClick = {
                        inspectedOk = round(p20Value * 10.0) / 10.0
                        inputStr = formatAssetNumber(inspectedOk)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f)
                )
                BenchmarkChip(
                    label = "상위 10%",
                    valueOk = p10Value,
                    selected = abs(inspectedOk - p10Value) < 0.1,
                    onClick = {
                        inspectedOk = round(p10Value * 10.0) / 10.0
                        inputStr = formatAssetNumber(inspectedOk)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f)
                )
                BenchmarkChip(
                    label = "상위 1%",
                    valueOk = p1Value,
                    selected = abs(inspectedOk - p1Value) < 0.1,
                    onClick = {
                        inspectedOk = round(p1Value * 10.0) / 10.0
                        inputStr = formatAssetNumber(inspectedOk)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BenchmarkChip(
    label: String,
    valueOk: Double,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) primaryColor.copy(alpha = 0.18f) else surfaceColor,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, primaryColor) else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", valueOk)}억",
                fontSize = 9.sp,
                color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 자산 수치를 자연스럽게 포맷팅 (정수는 소수점 없이, 소수점은 불필요한 뒷자리 0 제거)
 */
private fun formatAssetNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}
