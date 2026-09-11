package com.example.landguard.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SatelliteAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.landguard.ui.components.LandGuardCard
import com.example.landguard.ui.theme.BgDeep
import com.example.landguard.ui.theme.BrandContainer
import com.example.landguard.ui.theme.BrandPrimary
import com.example.landguard.ui.theme.TextPrimary
import com.example.landguard.ui.theme.TextSecondary

@Composable
fun MoreScreen(
    onOpenProfile: () -> Unit = {},
    onOpenSatellite: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 100.dp)
    ) {

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        Text(
            text = "More",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Manage your LandGuard workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ---------------------------------------------------------
        // PROFILE CARD
        // ---------------------------------------------------------

        LandGuardCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenProfile() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(BrandContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "LandGuard Explorer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Environmental monitoring workspace",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open profile",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // MONITORING
        // ---------------------------------------------------------

        SectionTitle("Monitoring")

        Spacer(modifier = Modifier.height(10.dp))

        MoreMenuItem(
            icon = Icons.Outlined.SatelliteAlt,
            title = "Satellite Data",
            description = "ALOS-4 and deformation observations",
            onClick = onOpenSatellite
        )

        Spacer(modifier = Modifier.height(10.dp))

        MoreMenuItem(
            icon = Icons.Outlined.Assessment,
            title = "Reports",
            description = "View generated land-risk reports",
            onClick = onOpenReports
        )

        Spacer(modifier = Modifier.height(10.dp))

        MoreMenuItem(
            icon = Icons.Outlined.Storage,
            title = "Data Sources",
            description = "Satellite, weather and risk data",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // PREFERENCES
        // ---------------------------------------------------------

        SectionTitle("Preferences")

        Spacer(modifier = Modifier.height(10.dp))

        LandGuardCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            MoreToggleItem(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                description = "Receive critical land-risk alerts",
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                }
            )

            MenuDivider()

            MoreMenuItem(
                icon = Icons.Outlined.Settings,
                title = "Settings",
                description = "App preferences and configuration",
                onClick = onOpenSettings,
                showCard = false
            )

            MenuDivider()

            MoreMenuItem(
                icon = Icons.Outlined.Language,
                title = "Language",
                description = "English",
                onClick = {},
                showCard = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // SYSTEM STATUS
        // ---------------------------------------------------------

        SectionTitle("System")

        Spacer(modifier = Modifier.height(10.dp))

        LandGuardCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                SystemStatusRow(
                    title = "LandGuard Engine",
                    status = "Connected",
                    connected = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                SystemStatusRow(
                    title = "Satellite Data",
                    status = "Available",
                    connected = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                SystemStatusRow(
                    title = "Alert Service",
                    status = "Active",
                    connected = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------------------------------------------------------
        // ABOUT
        // ---------------------------------------------------------

        SectionTitle("About")

        Spacer(modifier = Modifier.height(10.dp))

        LandGuardCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            MoreMenuItem(
                icon = Icons.Outlined.Info,
                title = "About LandGuard",
                description = "Environmental risk monitoring platform",
                onClick = {},
                showCard = false
            )

            MenuDivider()

            MoreMenuItem(
                icon = Icons.Outlined.Policy,
                title = "Privacy & Data",
                description = "How monitoring data is handled",
                onClick = {},
                showCard = false
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "LANDGUARD",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = BrandPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Environmental intelligence • v1.0",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SectionTitle(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun MoreMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    showCard: Boolean = true
) {
    if (showCard) {
        LandGuardCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            MoreMenuContent(
                icon = icon,
                title = title,
                description = description,
                onClick = onClick
            )
        }
    } else {
        MoreMenuContent(
            icon = icon,
            title = title,
            description = description,
            onClick = onClick
        )
    }
}

@Composable
private fun MoreMenuContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(BrandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MoreToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(BrandContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant
            )
    )
}

@Composable
private fun SystemStatusRow(
    title: String,
    status: String,
    connected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(
                    if (connected) {
                        BrandPrimary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = if (connected) {
                BrandPrimary
            } else {
                TextSecondary
            },
            fontWeight = FontWeight.SemiBold
        )
    }
}