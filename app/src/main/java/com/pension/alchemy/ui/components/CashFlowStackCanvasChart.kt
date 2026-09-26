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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.data.model.YearlySimulationResult
import com.pension.alchemy.theme.*

@Composable
fun CashFlowStackCanvasChart(
    results: List<YearlySimulationResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    // 5년 단위 샘플링 (은퇴 60세 ~ 90세 구간 중점)
    val sampled = results.filter { it.age in 55..85 && (it.age % 5 == 0) }
    if (sampled.isEmpty()) return

    val maxAmount = (sampled.maxOfOrNull { it.monthlyPensionIncome.coerceAtLeast(it.monthlyExpenses) } ?: 5_000_000L).coerceAtLeast(1_000_000L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "3층 연금 월 수령액 vs 지출 스택",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 범례
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LegendItem(color = Tier1NationalColor, label = "국민")
            LegendItem(color = Tier2RetirementColor, label = "퇴직")
            LegendItem(color = Tier3PersonalColor, label = "연금저축")
            LegendItem(color = CyanInfo, label = "연금보험")
            LegendItem(color = RoseDanger, label = "월 지출선")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val count = sampled.size
                val barWidth = (width / count) * 0.45f

                sampled.forEachIndexed { i, item ->
                    val centerX = (width / count) * i + (width / count) / 2f
                    val left = centerX - barWidth / 2f

                    var currentBottom = height

                    fun drawSegment(amount: Long, color: Color) {
                        if (amount <= 0) return
                        val segmentHeight = (amount.toFloat() / maxAmount.toFloat()) * height
                        val top = currentBottom - segmentHeight
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, segmentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        currentBottom = top
                    }

                    // 1층, 2층, 3층 순서로 적재
                    drawSegment(item.monthlyNationalPension, Tier1NationalColor)
                    drawSegment(item.monthlyRetirementPension, Tier2RetirementColor)
                    drawSegment(item.monthlyPersonalPension, Tier3PersonalColor)
                    drawSegment(item.monthlyAnnuityInsurancePension, CyanInfo)
                    drawSegment(item.monthlyHousingPension, TierHousingColor)

                    // 지출선 가로 마커
                    val expenseY = height - ((item.monthlyExpenses.toFloat() / maxAmount.toFloat()) * height)
                    drawLine(
                        color = RoseDanger,
                        start = Offset(left - 4.dp.toPx(), expenseY),
                        end = Offset(left + barWidth + 4.dp.toPx(), expenseY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        }

        // X축 나이 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            sampled.forEach {
                Text(text = "${it.age}세", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}