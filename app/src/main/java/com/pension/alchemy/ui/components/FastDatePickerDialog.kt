package com.pension.alchemy.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pension.alchemy.domain.engine.RealTimeGrowthCalculator
import com.pension.alchemy.theme.RoseDanger
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class DatePickerViewMode {
    DAY, YEAR, MONTH
}

/**
     * 연도, 월, 일, (선택적으로 시간)을 빠르고 직관적으로 선택할 수 있는 캘린더 다이얼로그
     * - 연도 빠른 선택: 연도 헤더 클릭 시 1940년~2060년 그리드에서 한 번에 선택, 또는 << >> 버튼으로 1년 단위 이동
     * - 월 빠른 선택: 월 헤더 클릭 시 1월~12월 그리드에서 한 번에 선택, 또는 < > 버튼으로 1달 단위 이동
     * - 오늘/현재 바로가기 버튼 제공
     */
@Composable
fun FastDatePickerDialog(
    initialDateStr: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
    includeTime: Boolean = false,
    title: String = "날짜 선택"
) {
    val initialDateTime = remember(initialDateStr) {
        RealTimeGrowthCalculator.parseDateTimeSafely(initialDateStr)
    }

    var selectedYear by remember { mutableIntStateOf(initialDateTime.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDateTime.monthValue) }
    var selectedDay by remember { mutableIntStateOf(initialDateTime.dayOfMonth) }
    var selectedHour by remember { mutableIntStateOf(initialDateTime.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialDateTime.minute) }

    var viewMode by remember { mutableStateOf(DatePickerViewMode.DAY) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
    }

    // 날짜 유효성 자동 조정 (예: 31일 선택 상태에서 2월로 변경 시 28일/29일로 조정)
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    val selectedLocalDate = remember(selectedYear, selectedMonth, selectedDay) {
        LocalDate.of(selectedYear, selectedMonth, selectedDay.coerceIn(1, daysInMonth))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. 다이얼로그 상단 헤더: 타이틀 & 선택된 날짜 미리보기
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val dayOfWeekStr = selectedLocalDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREA)
                        val formattedDate = if (includeTime) {
                            String.format(Locale.KOREA, "%04d년 %02d월 %02d일 (%s) %02d:%02d",
                                selectedYear, selectedMonth, selectedDay, dayOfWeekStr, selectedHour, selectedMinute)
                        } else {
                            String.format(Locale.KOREA, "%04d년 %02d월 %02d일 (%s)",
                                selectedYear, selectedMonth, selectedDay, dayOfWeekStr)
                        }
                        Text(
                            text = formattedDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // 2. 모드별 컨텐츠 (연도 그리드 / 월 그리드 / 기본 달력)
                AnimatedContent(
                    targetState = viewMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DatePickerModeTransition"
                ) { mode ->
                    when (mode) {
                        DatePickerViewMode.YEAR -> {
                            YearSelectorView(
                                currentYear = selectedYear,
                                onSelectYear = {
                                    selectedYear = it
                                    viewMode = DatePickerViewMode.DAY
                                },
                                onCancel = { viewMode = DatePickerViewMode.DAY }
                            )
                        }
                        DatePickerViewMode.MONTH -> {
                            MonthSelectorView(
                                currentMonth = selectedMonth,
                                onSelectMonth = {
                                    selectedMonth = it
                                    viewMode = DatePickerViewMode.DAY
                                },
                                onCancel = { viewMode = DatePickerViewMode.DAY }
                            )
                        }
                        DatePickerViewMode.DAY -> {
                            DayCalendarView(
                                year = selectedYear,
                                month = selectedMonth,
                                selectedDay = selectedDay,
                                onYearClick = { viewMode = DatePickerViewMode.YEAR },
                                onMonthClick = { viewMode = DatePickerViewMode.MONTH },
                                onPrevYear = { selectedYear -= 1 },
                                onNextYear = { selectedYear += 1 },
                                onPrevMonth = {
                                    if (selectedMonth == 1) {
                                        selectedYear -= 1
                                        selectedMonth = 12
                                    } else {
                                        selectedMonth -= 1
                                    }
                                },
                                onNextMonth = {
                                    if (selectedMonth == 12) {
                                        selectedYear += 1
                                        selectedMonth = 1
                                    } else {
                                        selectedMonth += 1
                                    }
                                },
                                onSelectDay = { selectedDay = it }
                            )
                        }
                    }
                }

                // 3. 시간 선택 영역 (includeTime == true 일 때 표시)
                if (includeTime && viewMode == DatePickerViewMode.DAY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ 기준 시각",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TimeSpinSelector(
                                    value = selectedHour,
                                    maxValue = 23,
                                    label = "시",
                                    onValueChange = { selectedHour = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                TimeSpinSelector(
                                    value = selectedMinute,
                                    maxValue = 59,
                                    label = "분",
                                    onValueChange = { selectedMinute = it }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 하단 버튼 액션: [오늘/현재], [취소], [적용]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val now = LocalDateTime.now()
                            selectedYear = now.year
                            selectedMonth = now.monthValue
                            selectedDay = now.dayOfMonth
                            selectedHour = now.hour
                            selectedMinute = now.minute
                            viewMode = DatePickerViewMode.DAY
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (includeTime) "현재 시각" else "오늘 날짜", fontSize = 13.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismissRequest,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                val resultStr = if (includeTime) {
                                    String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                                        selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                                } else {
                                    String.format(Locale.US, "%04d-%02d-%02d",
                                        selectedYear, selectedMonth, selectedDay)
                                }
                                onConfirm(resultStr)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("선택 완료", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 기본 DAY 달력 뷰: 상단에 연/월 빠른 이동 헤더 + 요일 헤더 + 날짜 그리드
 */
@Composable
private fun DayCalendarView(
    year: Int,
    month: Int,
    selectedDay: Int,
    onYearClick: () -> Unit,
    onMonthClick: () -> Unit,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (Int) -> Unit
) {
    val yearMonth = YearMonth.of(year, month)
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7 // 0: 일요일, 1: 월요일, ...
    val daysInMonth = yearMonth.lengthOfMonth()
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {
        // 상단 네비게이션: << (1년전) < (1달전) [ 2026년 ▾ ] [ 10월 ▾ ] > (1달후) >> (1년후)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevYear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowLeft,
                        contentDescription = "1년 전",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "1달 전",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 연도 & 월 빠른 선택 드롭다운 버튼 (누르면 원클릭 그리드 모드 전환)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onYearClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${year}년 ▾",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    onClick = onMonthClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${month}월 ▾",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "1달 후",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onNextYear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowRight,
                        contentDescription = "1년 후",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 요일 헤더 (일 ~ 토)
        val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEachIndexed { index, dayName ->
                val textColor = when (index) {
                    0 -> RoseDanger
                    6 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = dayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 날짜 그리드
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val isSelected = (dayNumber == selectedDay)
                            val isToday = (today.year == year && today.monthValue == month && today.dayOfMonth == dayNumber)
                            val dayOfWeek = c // 0: 일요일, 6: 토요일

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .then(
                                        if (isToday && !isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onSelectDay(dayNumber) },
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    dayOfWeek == 0 -> RoseDanger
                                    dayOfWeek == 6 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 연도 빠른 선택 뷰 (1940년 ~ 2060년 그리드)
 */
@Composable
private fun YearSelectorView(
    currentYear: Int,
    onSelectYear: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val years = remember { (1940..2060).toList() }
    val initialIndex = remember { (currentYear - 1940).coerceIn(0, years.size - 1) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = (initialIndex - 6).coerceAtLeast(0))

    Column(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 연도 선택",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onCancel) {
                Text("달력으로 돌아가기", fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(years) { year ->
                val isSelected = year == currentYear
                Surface(
                    onClick = { onSelectYear(year) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${year}년",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 월 빠른 선택 뷰 (1월 ~ 12월 그리드)
 */
@Composable
private fun MonthSelectorView(
    currentMonth: Int,
    onSelectMonth: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val months = remember { (1..12).toList() }

    Column(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🗓️ 월 선택",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onCancel) {
                Text("달력으로 돌아가기", fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(months) { month ->
                val isSelected = month == currentMonth
                Surface(
                    onClick = { onSelectMonth(month) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${month}월",
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 시/분 스핀 증감 셀렉터
 */
@Composable
private fun TimeSpinSelector(
    value: Int,
    maxValue: Int,
    label: String,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", value),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Column {
            Text(
                text = "▲",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onValueChange(if (value >= maxValue) 0 else value + 1) }
                    .padding(2.dp)
            )
            Text(
                text = "▼",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onValueChange(if (value <= 0) maxValue else value - 1) }
                    .padding(2.dp)
            )
        }
    }
}
