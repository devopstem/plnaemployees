package by.smartplanogram.spmob.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.smartplanogram.spmob.R
import by.smartplanogram.spmob.SpmobApp
import by.smartplanogram.spmob.data.prefs.RecentRack
import by.smartplanogram.spmob.data.repo.DepartmentInfo
import by.smartplanogram.spmob.data.sync.SyncPhase
import by.smartplanogram.spmob.ui.departments.departmentIcon
import by.smartplanogram.spmob.ui.departments.departmentTitle
import by.smartplanogram.spmob.ui.rack.RackCarouselActivity
import by.smartplanogram.spmob.ui.scanner.ScannerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    wide: Boolean = false,
    onOpenProfile: () -> Unit,
    onOpenDepartments: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    var userName by remember { mutableStateOf<String?>(null) }
    var storeName by remember { mutableStateOf<String?>(null) }
    var departments by remember { mutableStateOf<List<DepartmentInfo>>(emptyList()) }
    val recentRacks by SpmobApp.instance.settings.recentRacks.collectAsStateWithLifecycle()
    val syncState by SpmobApp.instance.sync.state.collectAsStateWithLifecycle()
    val lastSyncAt by SpmobApp.instance.settings.lastSyncAt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SpmobApp.instance.seedReadyFlow.first { it }
        userName = SpmobApp.instance.catalog.currentUser()?.name
        storeName = SpmobApp.instance.catalog.currentStore()?.name
        departments = SpmobApp.instance.catalog.departments()
    }

    // Refresh department list after cloud update
    LaunchedEffect(syncState.phase, syncState.localVersion) {
        if (syncState.phase == SyncPhase.Updated && SpmobApp.instance.seedReady) {
            departments = SpmobApp.instance.catalog.departments()
            storeName = SpmobApp.instance.catalog.currentStore()?.name
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layoutMaxWidth = maxWidth
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide || layoutMaxWidth >= 600.dp) 40.dp else 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.nav_profile),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }

            Spacer(Modifier.size(20.dp))
            Text(
                text = userName?.let { stringResource(R.string.home_greeting_named, it) }
                    ?: stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = storeName ?: stringResource(R.string.home_store_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )

            SyncStatusCard(
                message = syncState.message.ifBlank {
                    when (syncState.phase) {
                        SyncPhase.Offline -> stringResource(R.string.sync_offline)
                        SyncPhase.Checking -> stringResource(R.string.sync_checking)
                        SyncPhase.UpToDate -> stringResource(R.string.sync_uptodate)
                        SyncPhase.Updated -> stringResource(R.string.sync_updated)
                        SyncPhase.Error -> syncState.message
                    }
                },
                phase = syncState.phase,
                lastSyncAt = lastSyncAt,
                onRetry = {
                    scope.launch { SpmobApp.instance.sync.syncNow(force = true) }
                },
            )

            Spacer(Modifier.size(16.dp))

            Card(
                onClick = { context.startActivity(ScannerActivity.intent(context)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_scan_open),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeActionCard(
                    title = stringResource(R.string.nav_departments),
                    subtitle = stringResource(R.string.home_departments_sub),
                    icon = Icons.Outlined.ViewModule,
                    onClick = onOpenDepartments,
                    modifier = Modifier.weight(1f),
                )
                HomeActionCard(
                    title = stringResource(R.string.nav_search),
                    subtitle = stringResource(R.string.home_search_sub),
                    icon = Icons.Outlined.Search,
                    onClick = onOpenSearch,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.size(28.dp))
            Text(
                text = stringResource(R.string.home_recent_racks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(10.dp))
            if (recentRacks.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_recent_racks_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentRacks.forEach { rack ->
                        RecentRackRow(
                            rack = rack,
                            onClick = {
                                context.startActivity(
                                    RackCarouselActivity.intent(
                                        context = context,
                                        departmentId = rack.departmentId,
                                        targetRackId = rack.id,
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.size(28.dp))
            Text(
                text = stringResource(R.string.home_favorites),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                departments.take(4).forEach { dept ->
                    FavoriteDeptRow(
                        departmentId = dept.id,
                        rackCount = dept.rackCount,
                        onClick = {
                            scope.launch {
                                val storeId = withContext(Dispatchers.IO) {
                                    SpmobApp.instance.catalog.currentStore()?.id
                                } ?: return@launch
                                val firstRack = withContext(Dispatchers.IO) {
                                    SpmobApp.instance.db.racks()
                                        .byDepartment(storeId, dept.id)
                                        .firstOrNull()
                                } ?: return@launch
                                context.startActivity(
                                    RackCarouselActivity.intent(
                                        context = context,
                                        departmentId = dept.id,
                                        targetRackId = firstRack.id,
                                        storeId = storeId,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    message: String,
    phase: SyncPhase,
    lastSyncAt: Long,
    onRetry: () -> Unit,
) {
    val icon = when (phase) {
        SyncPhase.Offline -> Icons.Outlined.CloudOff
        SyncPhase.Checking -> Icons.Outlined.CloudSync
        SyncPhase.UpToDate, SyncPhase.Updated -> Icons.Outlined.CloudDone
        SyncPhase.Error -> Icons.Outlined.CloudOff
    }
    val timeLabel = if (lastSyncAt > 0L) {
        stringResource(R.string.sync_updated_at, formatSyncClock(lastSyncAt))
    } else {
        stringResource(R.string.sync_never)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.sync_status),
                tint = if (phase == SyncPhase.Offline || phase == SyncPhase.Error) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.sync_retry))
            }
        }
    }
}

private fun formatSyncClock(millis: Long): String {
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "dd.MM HH:mm"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(millis)
}

@Composable
private fun RecentRackRow(
    rack: RecentRack,
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warehouse,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = rack.code,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = departmentTitle(rack.departmentId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FavoriteDeptRow(
    departmentId: String,
    rackCount: Int,
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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = departmentIcon(departmentId),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = departmentTitle(departmentId),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.departments_racks_count, rackCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
