package com.pension.alchemy.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.pension.alchemy.data.model.*
import com.pension.alchemy.util.SampleDataGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PensionAlchemyRepository(private val context: Context) {
    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true 
        encodeDefaults = true 
    }
    
    private val scope = CoroutineScope(Dispatchers.IO)

    private val pensionsFile get() = File(context.filesDir, "pensions.json")
    private val assetsFile get() = File(context.filesDir, "assets.json")
    private val incomesFile get() = File(context.filesDir, "incomes.json")
    private val profileFile get() = File(context.filesDir, "user_profile.json")
    private val calcSettingsFile get() = File(context.filesDir, "calculator_settings.json")

    private val _pensions = MutableStateFlow<List<Pension>>(emptyList())
    val pensions: StateFlow<List<Pension>> = _pensions.asStateFlow()

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _incomes = MutableStateFlow<List<Income>>(emptyList())
    val incomes: StateFlow<List<Income>> = _incomes.asStateFlow()

    private val _userProfile = MutableStateFlow(SampleDataGenerator.createDefaultProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _calculatorSettings = MutableStateFlow(CalculatorSettings())
    val calculatorSettings: StateFlow<CalculatorSettings> = _calculatorSettings.asStateFlow()

    init {
        scope.launch {
            loadAllData()
        }
    }

    suspend fun loadAllData() = withContext(Dispatchers.IO) {
        val loadedProfile = loadProfileInternal()
        val loadedPensions = loadPensionsInternal()
        val loadedAssets = loadAssetsInternal()
        val loadedIncomes = loadIncomesInternal()
        val loadedCalcSettings = loadCalculatorSettingsInternal()

        // 첫 실행이거나 비어있을 때 기본 샘플 데이터 시딩
        if (loadedPensions.isEmpty() && loadedAssets.isEmpty()) {
            val defaultProfile = SampleDataGenerator.createDefaultProfile()
            val defaultPensions = SampleDataGenerator.createDefaultPensions()
            val defaultAssets = SampleDataGenerator.createDefaultAssets()
            val defaultIncomes = SampleDataGenerator.createDefaultIncomes()
            val defaultCalcSettings = CalculatorSettings()

            saveProfileInternal(defaultProfile)
            savePensionsInternal(defaultPensions)
            saveAssetsInternal(defaultAssets)
            saveIncomesInternal(defaultIncomes)
            saveCalculatorSettingsInternal(defaultCalcSettings)

            _userProfile.value = defaultProfile
            _pensions.value = defaultPensions
            _assets.value = defaultAssets
            _incomes.value = defaultIncomes
            _calculatorSettings.value = defaultCalcSettings
        } else {
            _userProfile.value = loadedProfile ?: SampleDataGenerator.createDefaultProfile()
            _pensions.value = loadedPensions
            _assets.value = loadedAssets
            _incomes.value = loadedIncomes
            _calculatorSettings.value = loadedCalcSettings ?: CalculatorSettings()
        }
    }

    // --- Pension CRUD ---
    suspend fun savePension(pension: Pension) = withContext(Dispatchers.IO) {
        val current = _pensions.value.toMutableList()
        val index = current.indexOfFirst { it.id == pension.id }
        if (index >= 0) {
            current[index] = pension
        } else {
            current.add(pension)
        }
        _pensions.value = current
        savePensionsInternal(current)
    }

    suspend fun deletePension(id: String) = withContext(Dispatchers.IO) {
        val updated = _pensions.value.filterNot { it.id == id }
        _pensions.value = updated
        savePensionsInternal(updated)
    }

    // --- Asset CRUD ---
    suspend fun saveAsset(asset: Asset) = withContext(Dispatchers.IO) {
        val current = _assets.value.toMutableList()
        val index = current.indexOfFirst { it.id == asset.id }
        if (index >= 0) {
            current[index] = asset
        } else {
            current.add(asset)
        }
        _assets.value = current
        saveAssetsInternal(current)
    }

    suspend fun deleteAsset(id: String) = withContext(Dispatchers.IO) {
        val updated = _assets.value.filterNot { it.id == id }
        _assets.value = updated
        saveAssetsInternal(updated)
    }

    // --- Income CRUD ---
    suspend fun saveIncome(income: Income) = withContext(Dispatchers.IO) {
        val current = _incomes.value.toMutableList()
        val index = current.indexOfFirst { it.id == income.id }
        if (index >= 0) {
            current[index] = income
        } else {
            current.add(income)
        }
        _incomes.value = current
        saveIncomesInternal(current)
    }

    suspend fun deleteIncome(id: String) = withContext(Dispatchers.IO) {
        val updated = _incomes.value.filterNot { it.id == id }
        _incomes.value = updated
        saveIncomesInternal(updated)
    }

    suspend fun reorderAssets(orderedList: List<Asset>) = withContext(Dispatchers.IO) {
        _assets.value = orderedList
        saveAssetsInternal(orderedList)
    }

    suspend fun reorderIncomes(orderedList: List<Income>) = withContext(Dispatchers.IO) {
        _incomes.value = orderedList
        saveIncomesInternal(orderedList)
    }

    // --- User Profile ---
    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        _userProfile.value = profile
        saveProfileInternal(profile)
    }

    // --- Calculator Settings ---
    suspend fun updateCalculatorSettings(settings: CalculatorSettings) = withContext(Dispatchers.IO) {
        _calculatorSettings.value = settings
        saveCalculatorSettingsInternal(settings)
    }

    // --- Presets & Reset ---
    suspend fun applyPreset(presetType: Int) = withContext(Dispatchers.IO) {
        val (profile, pensions, assetsAndIncomes) = when (presetType) {
            20 -> SampleDataGenerator.create20sPreset()
            30 -> SampleDataGenerator.create30sPreset()
            40 -> SampleDataGenerator.create40sPreset()
            50 -> SampleDataGenerator.create50sPreset()
            60 -> SampleDataGenerator.create60sPreset()
            else -> SampleDataGenerator.create40sPreset()
        }
        val (assets, incomes) = assetsAndIncomes

        saveProfileInternal(profile)
        savePensionsInternal(pensions)
        saveAssetsInternal(assets)
        saveIncomesInternal(incomes)

        _userProfile.value = profile
        _pensions.value = pensions
        _assets.value = assets
        _incomes.value = incomes
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        applyPreset(40) // 40대 기본 프리셋으로 초기화
        val defaultCalc = CalculatorSettings()
        saveCalculatorSettingsInternal(defaultCalc)
        _calculatorSettings.value = defaultCalc
    }

    // ──────────────────────────────────────────────
    // 데이터 백업 및 복구 (Backup & Restore)
    // ──────────────────────────────────────────────

    fun generateBackupFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "PensionAlchemy_Backup_$timeStamp.json"
    }

    suspend fun createBackupData(calculatorSettings: CalculatorSettings? = null): BackupData {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return BackupData(
            version = 1,
            appName = "PensionAlchemy",
            backupDate = timeStamp,
            userProfile = _userProfile.value,
            pensions = _pensions.value,
            assets = _assets.value,
            incomes = _incomes.value,
            calculatorSettings = calculatorSettings ?: _calculatorSettings.value
        )
    }

    suspend fun exportBackupJson(calculatorSettings: CalculatorSettings? = null): String = withContext(Dispatchers.IO) {
        val backupData = createBackupData(calculatorSettings)
        json.encodeToString(backupData)
    }

    /**
     * 사용자가 선택한 Uri (Documents, Downloads, Google Drive 등)로 백업 파일 저장
     */
    suspend fun exportBackupToUri(uri: Uri, calculatorSettings: CalculatorSettings? = null): Result<BackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val backupData = createBackupData(calculatorSettings)
            val jsonString = json.encodeToString(backupData)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: throw IOException("파일을 열어 데이터를 저장할 수 없습니다.")
            backupData
        }
    }

    /**
     * 기본 Download 폴더로 빠른 백업 수행
     */
    suspend fun exportBackupToDownloads(calculatorSettings: CalculatorSettings? = null): Result<Pair<String, BackupData>> = withContext(Dispatchers.IO) {
        runCatching {
            val backupData = createBackupData(calculatorSettings)
            val jsonString = json.encodeToString(backupData)
            val fileName = generateBackupFileName()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("Downloads 폴더에 파일을 생성할 수 없습니다.")
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                    os.flush()
                } ?: throw IOException("파일에 데이터를 쓸 수 없습니다.")
                Pair("Download/$fileName", backupData)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = File(downloadsDir, fileName)
                targetFile.writeText(jsonString, Charsets.UTF_8)
                Pair(targetFile.absolutePath, backupData)
            }
        }
    }

    /**
     * 백업 파일(Uri)로부터 복구 전 미리보기 정보 파싱 및 유효성 검증
     */
    suspend fun readBackupPreviewFromUri(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IOException("파일을 읽을 수 없습니다.")
            json.decodeFromString<BackupData>(jsonString)
        }
    }

    /**
     * 파싱된 BackupData를 시스템에 적용 및 영속화
     */
    suspend fun restoreFromBackupData(backup: BackupData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 프로필 저장
            saveProfileInternal(backup.userProfile)
            _userProfile.value = backup.userProfile

            // 연금 저장
            savePensionsInternal(backup.pensions)
            _pensions.value = backup.pensions

            // 자산 저장
            saveAssetsInternal(backup.assets)
            _assets.value = backup.assets

            // 소득 저장
            saveIncomesInternal(backup.incomes)
            _incomes.value = backup.incomes

            // 계산기 설정 저장
            saveCalculatorSettingsInternal(backup.calculatorSettings)
            _calculatorSettings.value = backup.calculatorSettings
        }
    }

    /**
     * 사용자가 선택한 백업 파일(Uri)로부터 전체 데이터 복구
     */
    suspend fun restoreBackupFromUri(uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val backup = readBackupPreviewFromUri(uri).getOrThrow()
            restoreFromBackupData(backup).getOrThrow()
            backup
        }
    }

    // --- Internal I/O ---
    private fun loadPensionsInternal(): List<Pension> {
        return runCatching {
            if (!pensionsFile.exists()) emptyList()
            else json.decodeFromString<List<Pension>>(pensionsFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun savePensionsInternal(list: List<Pension>) {
        runCatching {
            pensionsFile.writeText(json.encodeToString(list))
        }
    }

    private fun loadAssetsInternal(): List<Asset> {
        return runCatching {
            if (!assetsFile.exists()) emptyList()
            else json.decodeFromString<List<Asset>>(assetsFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun saveAssetsInternal(list: List<Asset>) {
        runCatching {
            assetsFile.writeText(json.encodeToString(list))
        }
    }

    private fun loadIncomesInternal(): List<Income> {
        return runCatching {
            if (!incomesFile.exists()) emptyList()
            else json.decodeFromString<List<Income>>(incomesFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun saveIncomesInternal(list: List<Income>) {
        runCatching {
            incomesFile.writeText(json.encodeToString(list))
        }
    }

    private fun loadProfileInternal(): UserProfile? {
        return runCatching {
            if (!profileFile.exists()) null
            else json.decodeFromString<UserProfile>(profileFile.readText())
        }.getOrNull()
    }

    private fun saveProfileInternal(profile: UserProfile) {
        runCatching {
            profileFile.writeText(json.encodeToString(profile))
        }
    }

    private fun loadCalculatorSettingsInternal(): CalculatorSettings? {
        return runCatching {
            if (!calcSettingsFile.exists()) null
            else json.decodeFromString<CalculatorSettings>(calcSettingsFile.readText())
        }.getOrNull()
    }

    private fun saveCalculatorSettingsInternal(settings: CalculatorSettings) {
        runCatching {
            calcSettingsFile.writeText(json.encodeToString(settings))
        }
    }

    companion object {
        @Volatile
        private var instance: PensionAlchemyRepository? = null

        fun getInstance(context: Context): PensionAlchemyRepository {
            return instance ?: synchronized(this) {
                instance ?: PensionAlchemyRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}