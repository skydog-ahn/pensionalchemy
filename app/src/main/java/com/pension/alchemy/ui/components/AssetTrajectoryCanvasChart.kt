package com.pension.alchemy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.YearlySimulationResult
import com.pension.alchemy.theme.AmberWarning
import com.pension.alchemy.theme.EmeraldPrimary
import com.pension.alchemy.theme.RoseDanger
import com.pension.alchemy.util.CurrencyFormatter

@Composable
fun AssetTrajectoryCanvasChart(
    results: List<YearlySimulationResult>,
    selectedAge: Int,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    val maxAsset = (results.maxOfOrNull { it.netAssetValue } ?: 100_000_000L).coerceAtLeast(10_000_000L)
    val minAsset = results.minOfOrNull { it.netAssetValue } ?: 0L
    val hasNegativeNetWorth = minAsset < 0L

    val minAge = results.first().age
    val maxAge = results.last().age
    val ageRange = (maxAge - minAge).coerceAtLeast(1)

    // 동적 Y축 스케일링 범위 설정 (마이너스 순자산 지원)
    val effectiveMin = if (hasNegativeNetWorth) (minAsset * 1.15).toLong() else 0L
    val effectiveMax = (maxAsset * 1.10).toLong().coerceAtLeast(10_000_000L)
    val valueRange = (effectiveMax - effectiveMin).coerceAtLeast(1L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "생애 순자산 시뮬레이션 궤적",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasNegativeNetWorth) {
                    Text(
                        text = "노후 적자(순자산 음수) 구간 주의",
                        fontSize = 11.sp,
                        color = RoseDanger,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "최고 자산: ${CurrencyFormatter.formatKoreanWon(maxAsset, isShort = true)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                if (hasNegativeNetWorth) {
                    Text(
                        text = "최저: ${CurrencyFormatter.formatKoreanWon(minAsset, isShort = true)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = RoseDanger
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20.dp.toPx()
                val usableHeight = height - paddingBottom

                // 좌표 계산 함수
                fun getX(age: Int): Float {
                    return ((age - minAge).toFloat() / ageRange) * width
                }

                fun getY(value: Long): Float {
                    val ratio = ((value - effectiveMin).toFloat() / valueRange.toFloat()).coerceIn(0f, 1f)
                    return usableHeight - (ratio * usableHeight)
                }

                val zeroY = getY(0L)

                // 1. 적자 위험 영역 배경 틴트 (0원 기준선 아래)
                if (hasNegativeNetWorth && zeroY < usableHeight) {
                    drawRect(
                        color = RoseDanger.copy(alpha = 0.08f),
                        topLeft = Offset(0f, zeroY),
                        size = Size(width, usableHeight - zeroY)
                    )
                }

                // 2. 그리드 배경 가로선
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = (usableHeight / gridLines) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 3. 0원 기준선 (손익분기선) 강조 표시
                if (hasNegativeNetWorth && zeroY in 0f..usableHeight) {
                    drawLine(
                        color = RoseDanger.copy(alpha = 0.7f),
                        start = Offset(0f, zeroY),
                        end = Offset(width, zeroY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }

                // 4. 자산 곡선 패스 & 면적 채우기 패스 생성
                val path = Path()
                val fillPath = Path()

                results.forEachIndexed { index, res ->
                    val x = getX(res.age)
                    val y = getY(res.netAssetValue)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, zeroY.coerceIn(0f, usableHeight))
                        fillPath.lineTo(x, y)
                    } else {
                        val prev = results[index - 1]
                        val prevX = getX(prev.age)
                        val prevY = getY(prev.netAssetValue)
                        val cX = (prevX + x) / 2f
                        path.cubicTo(cX, prevY, cX, y, x, y)
                        fillPath.cubicTo(cX, prevY, cX, y, x, y)
                    }
                }

                val lastX = getX(results.last().age)
                fillPath.lineTo(lastX, zeroY.coerceIn(0f, usableHeight))
                fillPath.close()

                // 5. 그라데이션 채우기 (0원 기준 양수와 음수에 맞춤)
                val fillBrush = if (hasNegativeNetWorth) {
                    val zeroRatio = (zeroY / usableHeight).coerceIn(0f, 1f)
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to primaryColor.copy(alpha = 0.35f),
                            zeroRatio * 0.9f to primaryColor.copy(alpha = 0.05f),
                            zeroRatio to Color.Transparent,
                            zeroRatio + (1f - zeroRatio) * 0.2f to RoseDanger.copy(alpha = 0.08f),
                            1.0f to RoseDanger.copy(alpha = 0.30f)
                        ),
                        startY = 0f,
                        endY = usableHeight
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = usableHeight
                    )
                }

                drawPath(path = fillPath, brush = fillBrush)

                // 6. 자산 곡선 선 그리기 (양수 -> 음수 시 색상 점진적 전환)
                val strokeBrush = if (hasNegativeNetWorth) {
                    val zeroRatio = (zeroY / usableHeight).coerceIn(0f, 1f)
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to primaryColor,
                            (zeroRatio * 0.85f).coerceIn(0f, 1f) to primaryColor,
                            zeroRatio to AmberWarning,
                            1.0f to RoseDanger
                        ),
                        startY = 0f,
                        endY = usableHeight
                    )
                } else {
                    SolidColor(primaryColor)
                }

                drawPath(
                    path = path,
                    brush = strokeBrush,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 7. 선택된 나이 인디케이터 (수직선 & 포인트)
                val selectedResult = results.firstOrNull { it.age == selectedAge }
                if (selectedResult != null) {
                    val selX = getX(selectedAge)
                    val selY = getY(selectedResult.netAssetValue)
                    val isDeficit = selectedResult.netAssetValue < 0L
                    val pointColor = if (isDeficit) RoseDanger else primaryColor

                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, usableHeight),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    drawCircle(
                        color = pointColor,
                        radius = 7.dp.toPx(),
                        center = Offset(selX, selY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5.dp.toPx(),
                        center = Offset(selX, selY)
                    )
                }
            }
        }

        // X축 나이 눈금 라벨 (동적 생성)
        val step1 = minAge + (ageRange * 0.33f).toInt()
        val step2 = minAge + (ageRange * 0.66f).toInt()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${minAge}세", fontSize = 11.sp, color = textColor)
            Text(text = "${step1}세", fontSize = 11.sp, color = textColor)
            Text(text = "${step2}세", fontSize = 11.sp, color = textColor)
            Text(text = "${maxAge}세", fontSize = 11.sp, color = textColor)
        }
    }
}