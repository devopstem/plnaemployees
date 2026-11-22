package by.smartplanogram.spmob.ui.departments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.data.repo.DepartmentInfo
import by.smartplanogram.spmob.ui.rack.RackCarouselScreen
import by.smartplanogram.spmob.ui.rack.RackCarouselViewModel
import kotlinx.coroutines.flow.first

@Composable
fun DepartmentsScreen(wide: Boolean = false) {
    var selectedDepartmentId by rememberSaveable { mutableStateOf<String?>(null) }
    var departments by remember { mutableStateOf<List<DepartmentInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        SpmobApp.instance.seedReadyFlow.first { it }
        departments = SpmobApp.instance.catalog.departments()
        loading = false
    }

    val deptId = selectedDepartmentId
    // System back must close planogram, not the whole app
    BackHandler(enabled = deptId != null) {
        selectedDepartmentId = null
    }

    if (deptId != null) {
        val selected = departments.find { it.id == deptId }
        val title = departmentTitle(deptId, selected?.name.orEmpty())
        val vm: RackCarouselViewModel = viewModel(
            key = "dept-$deptId",
            factory = RackCarouselViewModel.Factory(SpmobApp.instance.db),
        )
        LaunchedEffect(deptId) {
            val storeId = SpmobApp.instance.catalog.currentStore()?.id.orEmpty()
            val firstRack = SpmobApp.instance.db.racks()
                .byDepartment(storeId, deptId)
                .firstOrNull()
            vm.loadRacks(
                departmentId = deptId,
                targetRackId = firstRack?.id.orEmpty(),
            )
        }
        RackCarouselScreen(
            viewModel = vm,
            onClose = { selectedDepartmentId = null },
            titleOverride = title,
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMaxWidth = maxWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide || layoutMaxWidth >= 600.dp) 40.dp else 16.dp)
                .padding(top = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_departments),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.departments_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                departments.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.departments_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(departments, key = { it.id }) { dept ->
                            DepartmentCard(
                                title = departmentTitle(dept.id, dept.name),
                                subtitle = stringResource(
                                    R.string.departments_racks_count,
                                    dept.rackCount,
                                ),
                                icon = departmentIcon(dept.id),
                                onClick = { selectedDepartmentId = dept.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
