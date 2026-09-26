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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.theme.*
import com.pension.alchemy.util.CurrencyFormatter

@Composable
fun AssetAllocationDonutChart(
    financialAssets: Long,
    pensionAssets: Long,
    realEstateAssets: Long,
    debt: Long,
    modifier: Modifier = Modifier
) {
    val total = (financialAssets + pensionAssets + realEstateAssets).coerceAtLeast(1L)
    val financialRatio = financialAssets.toFloat() / total
    val pensionRatio = pensionAssets.toFloat() / total
    val realEstateRatio = realEstateAssets.toFloat() / total

    val netWorth = (financialAssets + pensionAssets + realEstateAssets - debt).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "포트폴리오 자산 배분 비중",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 도넛 차트
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    var startAngle = -90f

                    fun drawSlice(ratio: Float, color: Color) {
                        val sweep = ratio * 360f
                        if (sweep > 0) {
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height),
                                topLeft = Offset(0f, 0f)
                            )
                            startAngle += sweep
                        }
                    }

                    drawSlice(financialRatio, CyanInfo)
                    drawSlice(pensionRatio, EmeraldPrimary)
                    drawSlice(realEstateRatio, AmberWarning)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "순자산", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatKoreanWon(netWorth, isShort = true),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 항목별 상세 요약
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AllocationRow(color = CyanInfo, label = "금융자산", amount = financialAssets, ratio = financialRatio)
                AllocationRow(color = EmeraldPrimary, label = "연금적립금", amount = pensionAssets, ratio = pensionRatio)
                AllocationRow(color = AmberWarning, label = "부동산", amount = realEstateAssets, ratio = realEstateRatio)
                if (debt > 0) {
                    AllocationRow(color = RoseDanger, label = "부채(차감)", amount = debt, ratio = null)
                }
            }
        }
    }
}

@Composable
private fun AllocationRow(color: Color, label: String, amount: Long, ratio: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = CurrencyFormatter.formatKoreanWon(amount, isShort = true),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (ratio != null) {
                Text(
                    text = "(${(ratio * 100).toInt()}%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}