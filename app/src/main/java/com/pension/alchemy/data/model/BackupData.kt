package com.pension.alchemy.data.model

import kotlinx.serialization.Serializable

/**
 * 앱의 모든 사용자 데이터(자산, 연금, 소득, 프로필, 세법 정책 상수, 계산기 설정값)를
 * 하나로 묶어 파일로 내보내거나 가져오기 위한 통합 백업 데이터 모델
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val appName: String = "PensionAlchemy",
    val backupDate: String, // "yyyy-MM-dd HH:mm:ss"
    val userProfile: UserProfile,
    val pensions: List<Pension>,
    val assets: List<Asset>,
    val incomes: List<Income>,
    val calculatorSettings: CalculatorSettings = CalculatorSettings()
)
