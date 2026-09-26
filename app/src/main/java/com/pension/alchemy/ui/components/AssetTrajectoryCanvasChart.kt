package com.pension.alchemy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.YearlySimulationResult
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
    val minAge = results.first().age
    val maxAge = results.last().age
    val ageRange = (maxAge - minAge).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "생애 순자산 시뮬레이션 궤적",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "최고 자산: ${CurrencyFormatter.formatKoreanWon(maxAsset, isShort = true)}",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingBottom = 24.dp.toPx()
                val usableHeight = height - paddingBottom

                // 그리드 배경선 (3개 라인)
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = (usableHeight / gridLines) * i
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 좌표 계산 함수
                fun getX(age: Int): Float {
                    return ((age - minAge).toFloat() / ageRange) * width
                }

                fun getY(value: Long): Float {
                    val ratio = (value.toFloat() / maxAsset.toFloat()).coerceIn(0f, 1f)
                    return usableHeight - (ratio * usableHeight)
                }

                // 면적 채우기 패스 & 곡선 패스 생성
                val path = Path()
                val fillPath = Path()

                results.forEachIndexed { index, res ->
                    val x = getX(res.age)
                    val y = getY(res.netAssetValue)

                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, usableHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        val prev = results[index - 1]
                        val prevX = getX(prev.age)
                        val prevY = getY(prev.netAssetValue)
                        val cX1 = (prevX + x) / 2f
                        path.cubicTo(cX1, prevY, cX1, y, x, y)
                        fillPath.cubicTo(cX1, prevY, cX1, y, x, y)
                    }
                }

                fillPath.lineTo(getX(results.last().age), usableHeight)
                fillPath.close()

                // 그래디언트 채우기
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = usableHeight
                    )
                )

                // 자산 곡선 선 그리기
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 선택된 나이 인디케이터 (수직선 & 포인트)
                val selectedResult = results.firstOrNull { it.age == selectedAge }
                if (selectedResult != null) {
                    val selX = getX(selectedAge)
                    val selY = getY(selectedResult.netAssetValue)

                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(selX, 0f),
                        end = Offset(selX, usableHeight),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    drawCircle(
                        color = primaryColor,
                        radius = 6.dp.toPx(),
                        center = Offset(selX, selY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(selX, selY)
                    )
                }
            }
        }

        // X축 나이 눈금 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${minAge}세", fontSize = 11.sp, color = textColor)
            Text(text = "60세(은퇴)", fontSize = 11.sp, color = textColor)
            Text(text = "65세(국민연금)", fontSize = 11.sp, color = textColor)
            Text(text = "${maxAge}세", fontSize = 11.sp, color = textColor)
        }
    }
}