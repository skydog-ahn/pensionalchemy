package com.pension.alchemy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.theme.EmeraldPrimary
import com.pension.alchemy.util.CurrencyFormatter

/**
 * 만원 단위를 기본 큰 숫자로, 타이머에 의해 가산되는 원 단위를 작은 숫자로 표현하는 실시간 금액 표시 컴포저블
 */
@Composable
fun RealTimeCurrencyText(
    amount: Long,
    modifier: Modifier = Modifier,
    mainTextStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp
    ),
    subTextSize: TextUnit = 15.sp,
    mainColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    timerColor: Color = EmeraldPrimary,
    alwaysShowWon: Boolean = true
) {
    val formatted = CurrencyFormatter.splitManAndWon(amount, alwaysIncludeWon = alwaysShowWon)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        if (formatted.manPart.isNotEmpty()) {
            Text(
                text = formatted.manPart,
                style = mainTextStyle,
                color = mainColor,
                maxLines = 1,
                softWrap = false
            )
        }
        if (formatted.wonPart.isNotEmpty()) {
            if (formatted.manPart.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = formatted.wonPart,
                style = mainTextStyle.copy(
                    fontSize = subTextSize,
                    fontWeight = FontWeight.Bold
                ),
                color = timerColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = (subTextSize.value * 0.12f).dp)
            )
        }
    }
}

/**
 * 리스트 아이템 및 소형 카드용 컴팩트 실시간 금액 컴포저블
 */
@Composable
fun CompactRealTimeCurrencyText(
    amount: Long,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    fontSize: TextUnit = 13.sp,
    mainColor: Color = MaterialTheme.colorScheme.onSurface,
    timerColor: Color = EmeraldPrimary,
    alwaysShowWon: Boolean = true
) {
    val formatted = CurrencyFormatter.splitManAndWon(amount, alwaysIncludeWon = alwaysShowWon)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = mainColor,
                maxLines = 1,
                softWrap = false
            )
        }
        if (formatted.manPart.isNotEmpty()) {
            Text(
                text = formatted.manPart,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = mainColor,
                maxLines = 1,
                softWrap = false
            )
        }
        if (formatted.wonPart.isNotEmpty()) {
            if (formatted.manPart.isNotEmpty()) {
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = formatted.wonPart,
                fontSize = (fontSize.value * 0.85f).sp,
                fontWeight = FontWeight.SemiBold,
                color = timerColor,
                maxLines = 1,
                softWrap = false
            )
        }
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = mainColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
