package by.smartplanogram.spmob.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.ui.graphics.vector.ImageVector
import by.smartplanogram.spmob.R

enum class SpDestination(
    val labelRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.nav_home, Icons.Outlined.Home),
    Departments(R.string.nav_departments, Icons.Outlined.ViewModule),
    Search(R.string.nav_search, Icons.Outlined.Search),
    Profile(R.string.nav_profile, Icons.Outlined.Person),
}
