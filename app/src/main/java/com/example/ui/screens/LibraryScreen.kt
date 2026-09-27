package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.runtime.LaunchedEffect
import com.example.model.AppThemePalette
import com.example.model.LibraryCategory
import com.example.model.LocalAppThemePalette
import com.example.model.Song
import com.example.model.SongSortOption
import com.example.ui.components.EqualizerBars
import com.example.ui.theme.CardDark
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeon

@Composable
fun LibraryScreen(
    songs: List<Song>,
    filteredSongs: List<Song>,
    sortedSongs: List<Song> = songs,
    simpleSongs: List<Song> = emptyList(),
    currentSong: Song?,
    isPlaying: Boolean,
    isScanning: Boolean,
    searchQuery: String,
    hasStoragePermission: Boolean,
    favoriteIds: Set<Long> = emptySet(),
    activeCategory: LibraryCategory = LibraryCategory.SONGS,
    categoryEventId: Long = 0L,
    sortOption: SongSortOption = SongSortOption.DATE_ADDED_DESC,
    onSortOptionSelected: (SongSortOption) -> Unit = {},
    onSelectCategory: (LibraryCategory) -> Unit = {},
    onSearchChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onShuffleAllClick: () -> Unit,
    onRescanClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onToggleFavorite: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<LibraryCategory?>(activeCategory) }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var hasScrolledToActiveTrack by rememberSaveable { mutableStateOf(false) }
    val palette = LocalAppThemePalette.current

    // Reset scroll flag on navigation exit (e.g. leaving Library to Now Playing)
    DisposableEffect(Unit) {
        onDispose {
            hasScrolledToActiveTrack = false
        }
    }

    LaunchedEffect(selectedCategory, selectedGroupFilter) {
        hasScrolledToActiveTrack = false
    }

    // Sync active category selected from Navigation Drawer or external action
    LaunchedEffect(categoryEventId) {
        if (categoryEventId > 0L) {
            selectedCategory = activeCategory
            selectedGroupFilter = null
        }
    }

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null && selectedCategory == null) {
            selectedCategory = LibraryCategory.SONGS
        }
    }

    BackHandler(enabled = selectedCategory != null || selectedGroupFilter != null) {
        if (selectedGroupFilter != null) {
            selectedGroupFilter = null
        } else {
            selectedCategory = null
        }
    }

    val currentCat = selectedCategory ?: LibraryCategory.SONGS

    val filteredSimple = remember(simpleSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            simpleSongs
        } else {
            simpleSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Artist list calculation
    val artistsList = remember(filteredSongs) {
        filteredSongs.groupBy { it.artist.ifBlank { "Unknown Artist" } }.map { (artist, songList) ->
            artist to songList
        }.sortedByDescending { it.second.size }
    }

    // Album list calculation
    val albumsList = remember(filteredSongs) {
        filteredSongs.groupBy { it.album.ifBlank { "Unknown Album" } }.map { (album, songList) ->
            album to songList
        }.sortedByDescending { it.second.size }
    }

    // Folders calculation
    val foldersList = remember(filteredSongs) {
        val cached = com.example.audio.AudioScanner.cachedFolders
        if (cached.isNotEmpty()) {
            cached.map { (folder, songList) ->
                folder to songList
            }.sortedBy { it.first.lowercase() }
        } else {
            filteredSongs.groupBy { it.folder.ifBlank { "Music" } }.map { (folder, songList) ->
                folder to songList
            }.sortedByDescending { it.second.size }
        }
    }

    // Genres calculation
    val genresList = remember(filteredSongs) {
        filteredSongs.groupBy { it.genre.ifBlank { "General" } }.map { (genre, songList) ->
            genre to songList
        }.sortedByDescending { it.second.size }
    }

    // Album Artists calculation
    val albumArtistsList = remember(filteredSongs) {
        filteredSongs.groupBy { it.albumArtist.ifBlank { it.artist.ifBlank { "Unknown Artist" } } }.map { (aa, songList) ->
            aa to songList
        }.sortedByDescending { it.second.size }
    }

    // Composers calculation
    val composersList = remember(filteredSongs) {
        filteredSongs.groupBy { it.composer.ifBlank { "Unknown Composer" } }.map { (comp, songList) ->
            comp to songList
        }.sortedByDescending { it.second.size }
    }

    // Displayed songs based on category and sub-filters (memoized to avoid re-filtering on scroll frames)
    val effectiveSongs = if (filteredSongs.isNotEmpty() || searchQuery.isNotBlank()) filteredSongs else sortedSongs
    val displaySongs = remember(selectedCategory, effectiveSongs, favoriteIds, selectedGroupFilter, searchQuery) {
        if (searchQuery.isNotBlank() && selectedCategory == null) {
            effectiveSongs
        } else {
            when (selectedCategory) {
                LibraryCategory.SONGS -> effectiveSongs
                LibraryCategory.PLAYLISTS -> effectiveSongs.filter { favoriteIds.contains(it.id) }
                LibraryCategory.ARTISTS -> if (selectedGroupFilter != null) effectiveSongs.filter { it.artist.ifBlank { "Unknown Artist" } == selectedGroupFilter } else emptyList()
                LibraryCategory.ALBUMS -> if (selectedGroupFilter != null) effectiveSongs.filter { it.album.ifBlank { "Unknown Album" } == selectedGroupFilter } else emptyList()
                LibraryCategory.FOLDERS -> if (selectedGroupFilter != null) com.example.audio.AudioScanner.cachedFolders[selectedGroupFilter] ?: effectiveSongs.filter { it.folder.ifBlank { "Music" } == selectedGroupFilter } else emptyList()
                LibraryCategory.GENRE -> if (selectedGroupFilter != null) effectiveSongs.filter { it.genre.ifBlank { "General" } == selectedGroupFilter } else emptyList()
                LibraryCategory.ALBUM_ARTISTS -> if (selectedGroupFilter != null) effectiveSongs.filter { it.albumArtist.ifBlank { it.artist.ifBlank { "Unknown Artist" } } == selectedGroupFilter } else emptyList()
                LibraryCategory.COMPILATIONS -> if (selectedGroupFilter != null) effectiveSongs.filter { it.album.ifBlank { "Unknown Album" } == selectedGroupFilter } else emptyList()
                LibraryCategory.COMPOSERS -> if (selectedGroupFilter != null) effectiveSongs.filter { it.composer.ifBlank { "Unknown Composer" } == selectedGroupFilter } else emptyList()
                null -> emptyList()
            }
        }
    }

    val songListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("search_bar_input"),
            placeholder = { Text("Search songs, artists, albums...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Main Landing Page vs Drilled-down Category View
        if (selectedCategory == null && searchQuery.isBlank()) {
            // Subheader: Quick Action row (Total tracks, Shuffle All, Rescan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${songs.size} Device Audio Files",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Action Button & DropdownMenu
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("library_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Songs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        SongSortDropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            selectedOption = sortOption,
                            onOptionSelected = onSortOptionSelected,
                            palette = palette
                        )
                    }

                    if (songs.isNotEmpty()) {
                        IconButton(
                            onClick = onShuffleAllClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("library_shuffle_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle All",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRescanClick,
                        enabled = !isScanning,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("library_rescan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Media",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "LIBRARY VIEWS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            // Primary Landing Page: Vertical List of Library Category Views
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_views_landing_list"),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categoryItems = listOf(
                    LibraryCategory.SONGS to ("${filteredSongs.size} tracks • All audio files" to (Icons.Default.Audiotrack to CyanNeon)),
                    LibraryCategory.PLAYLISTS to ("${favoriteIds.size} tracks • Liked & favorites" to (Icons.Default.Favorite to Color(0xFFEF4444))),
                    LibraryCategory.ARTISTS to ("${artistsList.size} artists • Grouped by performers" to (Icons.Default.Person to PurpleNeon)),
                    LibraryCategory.ALBUMS to ("${albumsList.size} albums • Grouped by releases" to (Icons.Default.Album to Color(0xFF38BDF8))),
                    LibraryCategory.FOLDERS to ("${foldersList.size} folders • Directory browser" to (Icons.Default.Folder to Color(0xFF10B981))),
                    LibraryCategory.GENRE to ("${genresList.size} genres • Musical styles" to (Icons.Default.Category to Color(0xFFF59E0B))),
                    LibraryCategory.ALBUM_ARTISTS to ("${albumArtistsList.size} album artists" to (Icons.Default.People to Color(0xFFA855F7))),
                    LibraryCategory.COMPILATIONS to ("${albumsList.size} compilations & releases" to (Icons.Default.Album to Color(0xFFEC4899))),
                    LibraryCategory.COMPOSERS to ("${composersList.size} composers" to (Icons.Default.AutoStories to Color(0xFF06B6D4)))
                )

                items(categoryItems, key = { it.first.name }) { (cat, info) ->
                    val (subtitle, iconPair) = info
                    val (icon, accentColor) = iconPair

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategory = cat
                                selectedGroupFilter = null
                                onSelectCategory(cat)
                            }
                            .testTag("lib_category_${cat.name.lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Sub-Screen: Category or Search Results with Back Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedGroupFilter != null) {
                                selectedGroupFilter = null
                            } else if (searchQuery.isNotEmpty()) {
                                onSearchChange("")
                            } else {
                                selectedCategory = null
                            }
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = when {
                                selectedGroupFilter != null -> selectedGroupFilter!!
                                searchQuery.isNotEmpty() && selectedCategory == null -> "Search Results"
                                selectedCategory != null -> selectedCategory!!.title
                                else -> "Library Views"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedGroupFilter != null) "Back to ${selectedCategory?.title}" else "Back to Library Views",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort Action Button & DropdownMenu
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("library_sub_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Songs",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        SongSortDropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            selectedOption = sortOption,
                            onOptionSelected = onSortOptionSelected,
                            palette = palette
                        )
                    }

                    if (filteredSongs.isNotEmpty()) {
                        IconButton(
                            onClick = onShuffleAllClick,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("library_shuffle_all")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle All",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRescanClick,
                        enabled = !isScanning,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("library_rescan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Media",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            val isGroupedCategory = selectedCategory != null &&
                selectedCategory != LibraryCategory.SONGS &&
                selectedCategory != LibraryCategory.PLAYLISTS
            val activeGroupList: List<Pair<String, List<Song>>> = when (selectedCategory) {
                LibraryCategory.ARTISTS -> artistsList
                LibraryCategory.ALBUMS -> albumsList
                LibraryCategory.FOLDERS -> foldersList
                LibraryCategory.GENRE -> genresList
                LibraryCategory.ALBUM_ARTISTS -> albumArtistsList
                LibraryCategory.COMPILATIONS -> albumsList
                LibraryCategory.COMPOSERS -> composersList
                else -> emptyList()
            }

        // View 1: Grouped Category Browsing (Artists, Albums, Folders, Genres, etc.)
        if (isGroupedCategory && selectedGroupFilter == null) {
            if (activeGroupList.isEmpty()) {
                EmptyPlaceholder(
                    message = "No ${currentCat.title.lowercase()} available",
                    subMessage = "Scan your audio library to organize music"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("grouped_list_${currentCat.name.lowercase()}"),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeGroupList, key = { it.first }) { (groupName, songList) ->
                        val groupIcon = when (currentCat) {
                            LibraryCategory.ARTISTS -> Icons.Default.Person
                            LibraryCategory.ALBUMS -> Icons.Default.Album
                            LibraryCategory.FOLDERS -> Icons.Default.Folder
                            LibraryCategory.GENRE -> Icons.Default.Category
                            LibraryCategory.ALBUM_ARTISTS -> Icons.Default.People
                            LibraryCategory.COMPILATIONS -> Icons.Default.Album
                            LibraryCategory.COMPOSERS -> Icons.Default.AutoStories
                            else -> Icons.Default.Audiotrack
                        }
                        val groupColor = when (currentCat) {
                            LibraryCategory.ARTISTS -> PurpleNeon
                            LibraryCategory.ALBUMS -> Color(0xFF38BDF8)
                            LibraryCategory.FOLDERS -> Color(0xFF10B981)
                            LibraryCategory.GENRE -> Color(0xFFF59E0B)
                            LibraryCategory.ALBUM_ARTISTS -> Color(0xFFA855F7)
                            LibraryCategory.COMPILATIONS -> Color(0xFFEC4899)
                            LibraryCategory.COMPOSERS -> Color(0xFF06B6D4)
                            else -> CyanNeon
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                                .clickable { selectedGroupFilter = groupName }
                                .testTag("${currentCat.name.lowercase()}_item_$groupName"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(groupColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = groupIcon,
                                        contentDescription = null,
                                        tint = groupColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${songList.size} ${if (songList.size == 1) "track" else "tracks"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        // View 2: Song Lists (Songs, Favorites, or drilled-down Group Filter)
        else {
            val currentPlayingSongId = currentSong?.id
            val targetIndex = remember(displaySongs, currentPlayingSongId) {
                if (currentPlayingSongId == null) -1 else displaySongs.indexOfFirst { it.id == currentPlayingSongId }
            }

            // High-Performance Two-Stage Auto-Scroll: Proximity Snap + Micro Settle
            LaunchedEffect(targetIndex) {
                if (targetIndex >= 0 && !hasScrolledToActiveTrack) {
                    val visibleIndices = songListState.layoutInfo.visibleItemsInfo.map { it.index }

                    // If item is already visible on screen, do nothing
                    if (targetIndex !in visibleIndices) {
                        // Instant jump close to target to prevent measuring intermediate items
                        val snapIndex = (targetIndex - 2).coerceAtLeast(0)
                        songListState.scrollToItem(index = snapIndex)

                        // Micro smooth scroll to perfectly center the track
                        songListState.animateScrollToItem(
                            index = (targetIndex - 1).coerceAtLeast(0),
                            scrollOffset = -40 // Clean breathing room from top header
                        )
                    }
                    hasScrolledToActiveTrack = true
                }
            }

            if (displaySongs.isEmpty()) {
                EmptyPlaceholder(
                    message = when {
                        currentCat == LibraryCategory.PLAYLISTS -> "No favorite songs yet"
                        searchQuery.isNotEmpty() -> "No results found for '$searchQuery'"
                        else -> "No songs found"
                    },
                    subMessage = when {
                        currentCat == LibraryCategory.PLAYLISTS -> "Tap the heart icon on any song to save it here"
                        !hasStoragePermission -> "Grant permission to index music from your device"
                        else -> "Check audio files on your device"
                    },
                    hasPermissionButton = !hasStoragePermission,
                    onRequestPermission = onRequestPermissionClick
                )
            } else {
                LazyColumn(
                    state = songListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("song_list_view"),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = displaySongs,
                        key = { it.id },
                        contentType = { "song_row" }
                    ) { song ->
                        val isCurrent = currentSong?.id == song.id
                        val isPlayingCurrent = isPlaying && isCurrent
                        val isFav = favoriteIds.contains(song.id)

                        SongItemRow(
                            song = song,
                            isCurrentSong = isCurrent,
                            isPlaying = isPlayingCurrent,
                            isFavorite = isFav,
                            onSongClick = { onSongClick(song) },
                            onToggleFavorite = { onToggleFavorite(song.id) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun CategoryChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF080B14) else activeColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor,
            selectedLabelColor = Color.Black,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = activeColor
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun EmptyPlaceholder(
    message: String,
    subMessage: String,
    hasPermissionButton: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderSpecial,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hasPermissionButton) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grant Storage Permission",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SongItemRow(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onSongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSongClick)
            .testTag("song_item_${song.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSong) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrentSong) 1.5.dp else 1.dp,
            color = if (isCurrentSong) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Art Icon or Equalizer animation
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    EqualizerBars(isPlaying = true, barColor = MaterialTheme.colorScheme.primary, maxHeight = 18.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Track",
                        tint = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Title & Artist (Direct content column without any inner background box)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Heart Favorite Icon Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("fav_btn_${song.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Unlike" else "Like",
                    tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SongSortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedOption: SongSortOption,
    onOptionSelected: (SongSortOption) -> Unit,
    palette: AppThemePalette,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .testTag("sort_dropdown_menu")
    ) {
        SongSortOption.values().forEach { option ->
            val isSelected = option == selectedOption
            DropdownMenuItem(
                text = {
                    Text(
                        text = option.displayName,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                onClick = {
                    onOptionSelected(option)
                    onDismissRequest()
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null,
                modifier = Modifier.testTag("sort_option_${option.name}")
            )
        }
    }
}

