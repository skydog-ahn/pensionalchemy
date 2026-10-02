package com.pension.alchemy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * 자산 및 연금/소득의 기준일(입력일자)을 편리하게 선택/입력할 수 있는 통합 컴포넌트
 * - FastDatePickerDialog 연동으로 연도/월 원클릭 빠른 점프 지원
 */
@Composable
fun BaseDateInputField(
    baseDate: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "입력/기준 일자",
    includeTime: Boolean = false,
    helperText: String? = "⏱️ 기준일 00시부터 현재까지 초당 자산 증가가 실시간으로 가산됩니다."
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val normalizedDate = if (baseDate.length >= 10) baseDate.take(10) else baseDate

    if (showDatePickerDialog) {
        FastDatePickerDialog(
            initialDateStr = normalizedDate,
            includeTime = false,
            title = "$label 선택",
            onDismissRequest = { showDatePickerDialog = false },
            onConfirm = { selected ->
                onDateChange(if (selected.length >= 10) selected.take(10) else selected)
                showDatePickerDialog = false
            }
        )
    }

    Column(modifier = modifier) {
        AutoSelectOutlinedTextField(
            value = normalizedDate,
            onValueChange = onDateChange,
            label = { Text(label) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            onDateChange(LocalDate.now().toString())
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "오늘",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "달력에서 날짜 선택 (연/월 빠른 이동)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            placeholder = {
                Text("YYYY-MM-DD (예: ${LocalDate.now()})")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (helperText != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = helperText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
