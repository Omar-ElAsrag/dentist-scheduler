package com.example.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.R

fun getProcedureNameResourceId(englishName: String): Int? {
    return when (englishName) {
        "Endodontic TT" -> R.string.proc_endodontic_tt
        "Fixed Prosthodontic TT" -> R.string.proc_fixed_prosthodontic_tt
        "Removable Prosthodontic TT" -> R.string.proc_removable_prosthodontic_tt
        "Oral Surgery" -> R.string.proc_oral_surgery
        "Periodontic TT" -> R.string.proc_periodontic_tt
        "Operative TT" -> R.string.proc_operative_tt
        "Pedodontics TT" -> R.string.proc_pedodontics_tt
        "General Examination" -> R.string.proc_general_examination
        else -> null
    }
}

@Composable
fun localizedProcedureName(englishName: String): String {
    val resId = getProcedureNameResourceId(englishName)
    return if (resId != null) stringResource(resId) else englishName
}

fun getMaterialFee(materialFees: String?, material: String?): Double? {
    if (materialFees == null || material == null) return null
    return materialFees.split(",").firstNotNullOfOrNull { entry ->
        val parts = entry.trim().split(":")
        if (parts.size >= 2 && parts[0].trim() == material) parts[1].trim().toDoubleOrNull()
        else null
    }
}

fun getMaterialLabFee(materialFees: String?, material: String?): Double? {
    if (materialFees == null || material == null) return null
    return materialFees.split(",").firstNotNullOfOrNull { entry ->
        val parts = entry.trim().split(":")
        if (parts.size >= 3 && parts[0].trim() == material) parts[2].trim().toDoubleOrNull()
        else null
    }
}
