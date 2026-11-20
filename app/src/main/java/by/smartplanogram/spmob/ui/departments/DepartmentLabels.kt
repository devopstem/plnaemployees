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
fun departmentTitle(departmentId: String): String = when (departmentId.uppercase()) {
    "WJ" -> stringResource(R.string.dept_wj)
    "WE" -> stringResource(R.string.dept_we)
    "ML" -> stringResource(R.string.dept_ml)
    "GEN" -> stringResource(R.string.dept_gen)
    else -> departmentId
}

fun departmentIcon(departmentId: String): ImageVector = when (departmentId.uppercase()) {
    "WJ" -> Icons.Outlined.LocalDrink
    "WE" -> Icons.Outlined.WaterDrop
    "ML" -> Icons.Outlined.AcUnit
    else -> Icons.Outlined.Category
}
