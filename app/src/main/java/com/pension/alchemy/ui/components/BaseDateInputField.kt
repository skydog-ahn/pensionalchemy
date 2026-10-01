package com.pension.alchemy.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import java.time.LocalDate

/**
 * 자산 및 연금/소득의 기준일(입력일자)을 편리하게 선택/입력할 수 있는 통합 컴포넌트
 */
@Composable
fun BaseDateInputField(
    baseDate: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "입력/기준 일자"
) {
    val context = LocalContext.current
    val parsedDate = remember(baseDate) {
        RealTimeGrowthCalculator.parseDateSafely(baseDate)
    }

    val datePickerDialog = remember(context, parsedDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                onDateChange(selected.toString())
            },
            parsedDate.year,
            parsedDate.monthValue - 1,
            parsedDate.dayOfMonth
        )
    }

    Column(modifier = modifier) {
        AutoSelectOutlinedTextField(
            value = baseDate,
            onValueChange = onDateChange,
            label = { Text(label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onDateChange(LocalDate.now().toString()) },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "오늘",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "달력에서 날짜 선택",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            placeholder = { Text("YYYY-MM-DD (예: ${LocalDate.now()})") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "⏱️ 기준일 00시부터 현재까지 초당 자산 증가가 실시간으로 가산됩니다.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
