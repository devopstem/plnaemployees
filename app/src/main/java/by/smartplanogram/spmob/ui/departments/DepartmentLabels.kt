package by.smartplanogram.spmob.ui.departments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import by.smartplanogram.spmob.R

@Composable
fun departmentTitle(departmentId: String, displayName: String = ""): String {
    if (displayName.isNotBlank()) return displayName
    return when (departmentId.uppercase()) {
        "WJ" -> stringResource(R.string.dept_wj)
        "WE" -> stringResource(R.string.dept_we)
        "ML" -> stringResource(R.string.dept_ml)
        "GEN" -> stringResource(R.string.dept_gen)
        "WR" -> "Вода"
        "CED" -> "Газированные напитки"
        "DAIRY" -> "Молочная продукция"
        "PDC" -> "Овощи и Фрукты"
        else -> departmentId
    }
}

fun departmentIcon(departmentId: String): ImageVector = when (departmentId.uppercase()) {
    "WJ", "CED", "JES", "KSS" -> Icons.Outlined.LocalDrink
    "WE", "WR" -> Icons.Outlined.WaterDrop
    "ML", "DAIRY", "IAM" -> Icons.Outlined.AcUnit
    else -> Icons.Outlined.Category
}
