package com.pension.alchemy.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 백업 JSON 파일 저장을 위한 ActivityResultContract
 * 사용자가 저장 위치(Document, Download, Drive 등)와 파일명을 지정할 수 있도록 하며,
 * Android API 26+ 기기에서는 기본 저장 위치를 제안합니다.
 */
class CreateBackupDocumentContract : ActivityResultContracts.CreateDocument("application/json") {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = super.createIntent(context, input)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val initialUri = DocumentsContract.buildRootUri(
                    "com.android.externalstorage.documents",
                    "primary"
                )
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            }
        }
        return intent
    }
}
