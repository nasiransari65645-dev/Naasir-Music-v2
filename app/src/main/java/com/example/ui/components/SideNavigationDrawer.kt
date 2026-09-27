package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LibraryCategory
import com.example.model.LocalThemePalette

data class DrawerMenuItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val category: LibraryCategory? = null,
    val testTag: String
)

@Composable
fun SideNavigationDrawerContent(
    totalSongsCount: Int,
    activeCategory: LibraryCategory,
    onSelectCategory: (LibraryCategory) -> Unit,
    onNavigateSettings: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalThemePalette.current
    var showNetworkDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    val navItems = listOf(
        DrawerMenuItem("Artists", "Grouped by performers", Icons.Default.Person, LibraryCategory.ARTISTS, "drawer_item_artists"),
        DrawerMenuItem("Albums", "Releases & compilations", Icons.Default.Album, LibraryCategory.ALBUMS, "drawer_item_albums"),
        DrawerMenuItem("Songs", "All audio files", Icons.Default.MusicNote, LibraryCategory.SONGS, "drawer_item_songs"),
        DrawerMenuItem("Folder", "Browse directories", Icons.Default.Folder, LibraryCategory.FOLDERS, "drawer_item_folders"),
        DrawerMenuItem("Playlists", "Favorites & playlists", Icons.Default.QueueMusic, LibraryCategory.PLAYLISTS, "drawer_item_playlists"),
        DrawerMenuItem("Genre", "Musical genres", Icons.Default.Category, LibraryCategory.GENRE, "drawer_item_genre"),
        DrawerMenuItem("Album Artists", "Primary album artists", Icons.Default.People, LibraryCategory.ALBUM_ARTISTS, "drawer_item_album_artists"),
        DrawerMenuItem("Compilations", "Various artists releases", Icons.Default.LibraryMusic, LibraryCategory.COMPILATIONS, "drawer_item_compilations"),
        DrawerMenuItem("Composers", "Track composers", Icons.Default.AutoStories, LibraryCategory.COMPOSERS, "drawer_item_composers")
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("side_navigation_drawer"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Header: App icon and App Name at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NaasirMusicLogo(
                        size = 52.dp,
                        animatedWavePulse = true,
                        glowIntensity = 0.6f,
                        onClick = { /* Interactive click zoom */ }
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Naasir Music",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(palette.primaryAccent)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = MaterialTheme.colorScheme.surface,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (totalSongsCount > 0) "$totalSongsCount local tracks" else "Hi-Res Audio Player",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items Section
            Text(
                text = "LIBRARY VIEWS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            navItems.forEach { item ->
                val isSelected = item.category != null && item.category == activeCategory
                DrawerNavigationRow(
                    title = item.title,
                    icon = item.icon,
                    isSelected = isSelected,
                    testTag = item.testTag,
                    onClick = {
                        item.category?.let { cat ->
                            onSelectCategory(cat)
                            onCloseDrawer()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom / System Options (separated by a divider)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Text(
                text = "MEDIA & SYSTEM",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Network / Cloud
            DrawerNavigationRow(
                title = "Network / Cloud",
                icon = Icons.Default.Cloud,
                iconTint = palette.primaryAccent,
                isSelected = false,
                testTag = "drawer_item_network_cloud",
                onClick = {
                    showNetworkDialog = true
                }
            )

            // Settings / Preferences
            DrawerNavigationRow(
                title = "Settings / Preferences",
                icon = Icons.Default.Settings,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                isSelected = false,
                testTag = "drawer_item_settings",
                onClick = {
                    onNavigateSettings()
                    onCloseDrawer()
                }
            )

            // Pro / Purchase (with a highlighted gift icon)
            ProDrawerRow(
                onClick = {
                    showProDialog = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Network / Cloud Dialog
    if (showNetworkDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = palette.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Network & Cloud Streaming", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Stream high-resolution lossless audio directly across your local network and cloud services:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    NetworkFeatureRow(title = "UPnP / DLNA Media Server", status = "Ready for casting")
                    NetworkFeatureRow(title = "WebDAV Local Storage", status = "Connected")
                    NetworkFeatureRow(title = "SMB / Windows Network Share", status = "Auto-discover LAN")
                    NetworkFeatureRow(title = "Google Drive / OneDrive Cloud", status = "OAuth Sync Available")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNetworkDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryAccent)
                ) {
                    Text("Close", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Pro / Purchase Dialog
    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null,
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Naasir Music Pro", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Unlock the complete studio listening experience with lifetime Pro license:",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )

                    ProFeatureBenefit(title = "13-Band Hardware Studio Equalizer")
                    ProFeatureBenefit(title = "Lossless 32-bit Float Audio Pipeline")
                    ProFeatureBenefit(title = "Custom Ambient Edge Lighting Presets")
                    ProFeatureBenefit(title = "Cloud & LAN High-Res Streaming")
                    ProFeatureBenefit(title = "100% Ad-Free Listening Forever")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Special Gift: 50% Early Adopter Discount Applied!",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showProDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Claim Pro Gift", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProDialog = false }) {
                    Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun DrawerNavigationRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    iconTint: Color? = null,
    onClick: () -> Unit
) {
    val palette = LocalThemePalette.current
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val actualIconTint = iconTint ?: if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = actualIconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent)
            )
        }
    }
}

@Composable
private fun ProDrawerRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3B2506),
                        Color(0xFF281702)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("drawer_item_pro_purchase"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Highlighted Gift Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB703).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = "Pro Gift",
                tint = Color(0xFFFFB703),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pro / Purchase",
                color = Color(0xFFFFD166),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Special Gift • Lifetime Access",
                color = Color(0xFFFBBF24),
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFFB703))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "50% OFF",
                color = Color(0xFF080B14),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun NetworkFeatureRow(title: String, status: String) {
    val palette = LocalThemePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = palette.primaryAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ProFeatureBenefit(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFFFB703),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
    }
}
