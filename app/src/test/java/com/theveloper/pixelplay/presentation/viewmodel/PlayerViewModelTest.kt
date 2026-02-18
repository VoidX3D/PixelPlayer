package com.theveloper.pixelplay.presentation.viewmodel

import android.content.Context
import app.cash.turbine.test
import com.theveloper.pixelplay.data.database.AlbumArtThemeDao
import com.google.common.util.concurrent.ListenableFuture
import com.theveloper.pixelplay.data.model.SearchFilterType
import com.theveloper.pixelplay.data.model.SearchHistoryItem
import com.theveloper.pixelplay.data.model.SearchResultItem
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.model.SortOption
import com.theveloper.pixelplay.data.model.StorageFilter
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import com.theveloper.pixelplay.data.repository.MusicRepository
import com.theveloper.pixelplay.data.repository.PlaybackStatRepository
import io.mockk.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import com.theveloper.pixelplay.MainCoroutineExtension
import com.theveloper.pixelplay.data.service.player.DualPlayerEngine
import com.theveloper.pixelplay.data.telegram.TelegramCacheManager
import com.theveloper.pixelplay.data.worker.SyncManager
import com.theveloper.pixelplay.utils.AppShortcutManager
import com.theveloper.pixelplay.presentation.viewmodel.*
import app.cash.turbine.Turbine

import androidx.core.content.ContextCompat

@ExperimentalCoroutinesApi
@ExtendWith(MainCoroutineExtension::class)
class PlayerViewModelTest {

    private lateinit var playerViewModel: PlayerViewModel
    private val mockMusicRepository: MusicRepository = mockk()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val mockAlbumArtThemeDao: AlbumArtThemeDao = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)

    // New Mocks
    private val mockSyncManager: SyncManager = mockk(relaxed = true)
    private val mockDualPlayerEngine: DualPlayerEngine = mockk(relaxed = true)
    private val mockAppShortcutManager: AppShortcutManager = mockk(relaxed = true)
    private val mockTelegramCacheManager: TelegramCacheManager = mockk(relaxed = true)
    private val mockListeningStatsTracker: ListeningStatsTracker = mockk(relaxed = true)
    private val mockDailyMixStateHolder: DailyMixStateHolder = mockk(relaxed = true)
    private val mockLyricsStateHolder: LyricsStateHolder = mockk(relaxed = true)
    private val mockCastStateHolder: CastStateHolder = mockk(relaxed = true)
    private val mockQueueStateHolder: QueueStateHolder = mockk(relaxed = true)
    private val mockPlaybackStateHolder: PlaybackStateHolder = mockk(relaxed = true)
    private val mockConnectivityStateHolder: ConnectivityStateHolder = mockk(relaxed = true)
    private val mockSleepTimerStateHolder: SleepTimerStateHolder = mockk(relaxed = true)
    private val mockSearchStateHolder: SearchStateHolder = mockk(relaxed = true)
    private val mockAiStateHolder: AiStateHolder = mockk(relaxed = true)
    private val mockLibraryStateHolder: LibraryStateHolder = mockk(relaxed = true)
    private val mockCastTransferStateHolder: CastTransferStateHolder = mockk(relaxed = true)
    private val mockMetadataEditStateHolder: MetadataEditStateHolder = mockk(relaxed = true)
    private val mockExternalMediaStateHolder: ExternalMediaStateHolder = mockk(relaxed = true)
    private val mockPlaybackStatRepository: PlaybackStatRepository = mockk(relaxed = true)
    private val mockThemeStateHolder: ThemeStateHolder = mockk(relaxed = true)
    private val mockMultiSelectionStateHolder: MultiSelectionStateHolder = mockk(relaxed = true)
    private lateinit var mockMediaControllerFactory: com.theveloper.pixelplay.data.media.MediaControllerFactory

    private val testDispatcher = StandardTestDispatcher()

    // Test Flows
    private val _allSongsFlow = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    private val _searchHistoryFlow = MutableStateFlow<ImmutableList<SearchHistoryItem>>(persistentListOf())
    private val _searchResultsFlow = MutableStateFlow<ImmutableList<SearchResultItem>>(persistentListOf())
    private val _selectedSearchFilterFlow = MutableStateFlow(SearchFilterType.ALL)
    private val _castSessionFlow = MutableStateFlow<com.google.android.gms.cast.framework.CastSession?>(null)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockKAnnotations.init(this)

        mockkStatic(ContextCompat::class)
        val directExecutor = java.util.concurrent.Executor { it.run() }
        every { ContextCompat.getMainExecutor(any()) } returns directExecutor
        every { mockTelegramCacheManager.embeddedArtUpdated } returns kotlinx.coroutines.flow.MutableSharedFlow()

        // Mock UserPreferences
        coEvery { mockUserPreferencesRepository.playerThemePreferenceFlow } returns flowOf("Global")
        coEvery { mockUserPreferencesRepository.favoriteSongIdsFlow } returns flowOf(emptySet())
        coEvery { mockUserPreferencesRepository.songsSortOptionFlow } returns flowOf("SongTitleAZ")
        coEvery { mockUserPreferencesRepository.albumsSortOptionFlow } returns flowOf("AlbumTitleAZ")
        coEvery { mockUserPreferencesRepository.artistsSortOptionFlow } returns flowOf("ArtistNameAZ")
        coEvery { mockUserPreferencesRepository.likedSongsSortOptionFlow } returns flowOf("LikedSongTitleAZ")
        coEvery { mockUserPreferencesRepository.navBarCornerRadiusFlow } returns flowOf(32)
        coEvery { mockUserPreferencesRepository.navBarStyleFlow } returns flowOf("Default")
        coEvery { mockUserPreferencesRepository.libraryNavigationModeFlow } returns flowOf("TabRow")
        coEvery { mockUserPreferencesRepository.carouselStyleFlow } returns flowOf("NoPeek")
        coEvery { mockUserPreferencesRepository.geminiApiKey } returns flowOf("")
        coEvery { mockUserPreferencesRepository.fullPlayerLoadingTweaksFlow } returns flowOf(com.theveloper.pixelplay.data.preferences.FullPlayerLoadingTweaks())
        coEvery { mockUserPreferencesRepository.tapBackgroundClosesPlayerFlow } returns flowOf(true)
        coEvery { mockUserPreferencesRepository.hapticsEnabledFlow } returns flowOf(true)
        coEvery { mockUserPreferencesRepository.foldersSortOptionFlow } returns flowOf("FolderNameAZ")
        coEvery { mockUserPreferencesRepository.persistentShuffleEnabledFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.isShuffleOnFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.preAmpFactorFlow } returns flowOf(1.0f)
        coEvery { mockUserPreferencesRepository.aiTrackingEnabledFlow } returns flowOf(true)
        coEvery { mockUserPreferencesRepository.playbackSpeedFlow } returns flowOf(1.0f)
        coEvery { mockUserPreferencesRepository.playbackPitchFlow } returns flowOf(1.0f)
        coEvery { mockUserPreferencesRepository.nightModeEnabledFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.bitPerfectEnabledFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.smartSleepTimerFinishSongFlow } returns flowOf(false)
        coEvery { mockUserPreferencesRepository.crossfadeDurationLabFlow } returns flowOf(6000)

        // Mock StateHolders Flows
        every { mockLibraryStateHolder.allSongs } returns _allSongsFlow
        every { mockLibraryStateHolder.isLoadingLibrary } returns MutableStateFlow(false)
        every { mockLibraryStateHolder.isLoadingCategories } returns MutableStateFlow(false)
        every { mockLibraryStateHolder.genres } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.albums } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.artists } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.musicFolders } returns MutableStateFlow(persistentListOf())
        every { mockLibraryStateHolder.currentSongSortOption } returns MutableStateFlow<SortOption>(SortOption.SongTitleAZ)
        every { mockLibraryStateHolder.currentAlbumSortOption } returns MutableStateFlow<SortOption>(SortOption.AlbumTitleAZ)
        every { mockLibraryStateHolder.currentArtistSortOption } returns MutableStateFlow<SortOption>(SortOption.ArtistNameAZ)
        every { mockLibraryStateHolder.currentFolderSortOption } returns MutableStateFlow<SortOption>(SortOption.FolderNameAZ)
        every { mockLibraryStateHolder.currentFavoriteSortOption } returns MutableStateFlow<SortOption>(SortOption.LikedSongTitleAZ)
        every { mockLibraryStateHolder.currentStorageFilter } returns MutableStateFlow(StorageFilter.ALL)

        every { mockSearchStateHolder.searchHistory } returns _searchHistoryFlow
        every { mockSearchStateHolder.searchResults } returns _searchResultsFlow
        every { mockSearchStateHolder.selectedSearchFilter } returns _selectedSearchFilterFlow
        every { mockSearchStateHolder.loadSearchHistory(any()) } just runs
        every { mockSearchStateHolder.clearSearchHistory() } just runs
        every { mockSearchStateHolder.deleteSearchHistoryItem(any()) } just runs
        every { mockSearchStateHolder.updateSearchFilter(any()) } just runs
        every { mockSearchStateHolder.initialize(any()) } just runs

        every { mockAiStateHolder.showAiPlaylistSheet } returns MutableStateFlow(false)
        every { mockAiStateHolder.isGeneratingAiPlaylist } returns MutableStateFlow(false)
        every { mockAiStateHolder.aiError } returns MutableStateFlow<String?>(null)
        every { mockAiStateHolder.isGeneratingMetadata } returns MutableStateFlow(false)
        every { mockAiStateHolder.initialize(any(), any(), any(), any(), any(), any()) } just runs
 
        every { mockCastStateHolder.castSession } returns _castSessionFlow
        every { mockCastStateHolder.startDiscovery() } just runs
        every { mockCastStateHolder.selectedRoute } returns MutableStateFlow<androidx.mediarouter.media.MediaRouter.RouteInfo?>(null)

        every { mockConnectivityStateHolder.initialize() } just runs
        every { mockConnectivityStateHolder.offlinePlaybackBlocked } returns MutableSharedFlow()

        val stablePlayerState = MutableStateFlow(StablePlayerState(currentSong = null))
        every { mockPlaybackStateHolder.stablePlayerState } returns stablePlayerState
        every { mockPlaybackStateHolder.setMediaController(any()) } just runs

        every { mockSleepTimerStateHolder.initialize(any(), any(), any(), any(), any()) } just runs
        every { mockSleepTimerStateHolder.isEndOfTrackTimerActive } returns MutableStateFlow(false)
        every { mockSleepTimerStateHolder.activeTimerValueDisplay } returns MutableStateFlow(null)
        every { mockSleepTimerStateHolder.playCount } returns MutableStateFlow(0f)

        every { mockLibraryStateHolder.initialize(any()) } just runs
        every { mockCastTransferStateHolder.initialize(any(), any(), any(), any(), any(), any(), any(), any(), any()) } just runs
        
        every { mockPlaybackStatRepository.getRecentStats() } returns flowOf(emptyList())
        coEvery { mockPlaybackStatRepository.getTopGenre() } returns "Rock"

        // Mock MusicRepository
        every { mockMusicRepository.getPaginatedSongs(any(), any()) } returns flowOf(androidx.paging.PagingData.empty())
        every { mockMusicRepository.getAudioFiles() } returns flowOf(emptyList())
        coEvery { mockMusicRepository.getFavoriteSongIdsOnce() } returns emptySet()
        every { mockMusicRepository.telegramRepository } returns mockk(relaxed = true)
        every { mockLyricsStateHolder.songUpdates } returns MutableSharedFlow()

        // Initialize PlayerViewModel
        val sessionToken = mockk<SessionToken>(relaxed = true)
        mockMediaControllerFactory = mockk(relaxed = true)
        
        val mockController = mockk<MediaController>(relaxed = true)
        val mockFuture = mockk<ListenableFuture<MediaController>>(relaxed = true)
        every { mockFuture.get() } returns mockController
        every { mockFuture.addListener(any(), any()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
        }
        every { mockMediaControllerFactory.create(any(), any(), any()) } returns mockFuture
        
        playerViewModel = PlayerViewModel(
            mockContext,
            mockMusicRepository,
            mockUserPreferencesRepository,
            mockAlbumArtThemeDao,
            mockSyncManager,
            mockDualPlayerEngine,
            mockAppShortcutManager,
            mockTelegramCacheManager,
            mockListeningStatsTracker,
            mockDailyMixStateHolder,
            mockLyricsStateHolder,
            mockCastStateHolder,
            mockQueueStateHolder,
            mockPlaybackStateHolder,
            mockConnectivityStateHolder,
            mockSleepTimerStateHolder,
            mockSearchStateHolder,
            mockAiStateHolder,
            mockLibraryStateHolder,
            mockCastTransferStateHolder,
            mockMetadataEditStateHolder,
            mockExternalMediaStateHolder,
            mockPlaybackStatRepository,
            mockThemeStateHolder,
            mockMultiSelectionStateHolder,
            sessionToken,
            mockMediaControllerFactory
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Nested
    @DisplayName("GetSongUrisForGenre Tests")
    inner class GetSongUrisForGenreTests {

        private val song1 = Song(id = "1", title = "Song 1", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover1.png", artistId = 1L, albumId = 1L, contentUriString = "content://dummy/1", duration = 180000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")
        private val song3 = Song(id = "3", title = "Song 3", artist = "Artist A", genre = "Rock", albumArtUriString = "rock_cover2.png", artistId = 3L, albumId = 3L, contentUriString = "content://dummy/3", duration = 190000L, bitrate = null, sampleRate = null, album = "Album", path = "path", mimeType = "audio/mpeg")

        private fun setupViewModelWithSongs(songs: List<Song>) {
            _allSongsFlow.value = songs.toImmutableList()
            testDispatcher.scheduler.advanceUntilIdle()
            
            val genreSlot = slot<String>()
            every { mockMusicRepository.getMusicByGenre(capture(genreSlot)) } answers {
                val genre = genreSlot.captured
                val filtered = songs.filter { it.genre.equals(genre, ignoreCase = true) }
                flowOf(filtered)
            }
        }

        @Test
        fun `single song with genre returns its cover`() = runTest {
            val testSongs = listOf(song1)
            setupViewModelWithSongs(testSongs)

            val uris = playerViewModel.getSongUrisForGenre("Rock").first()
            assertEquals(listOf("rock_cover1.png"), uris)
        }
    }
}
