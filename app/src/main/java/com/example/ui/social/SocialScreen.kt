package com.example.ui.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.core.TigerStrings
import com.example.data.firestore.SocialPost
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.StoryItem
import com.example.viewmodel.TigerViewModel
import kotlinx.coroutines.delay

private val FEED_CATEGORIES = listOf(
    "All", "General", "Tech", "Gaming", "Education", "Commerce", "Sports", "Entertainment"
)

private val STORY_GRADIENTS = listOf(
    listOf(Color(0xFFF97316), Color(0xFF9A3412)),
    listOf(Color(0xFF3B82F6), Color(0xFF1E3A8A)),
    listOf(Color(0xFF10B981), Color(0xFF065F46)),
    listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95))
)

private data class ReelSample(
    val id: String,
    val creator: String,
    val category: String,
    val titleAr: String,
    val titleEn: String,
    val likes: Int,
    val streamUrl: String
)

private val REEL_ITEMS = listOf(
    ReelSample(
        "r1", "@tiger_labs", "Tech",
        "تشفير المحادثات الفوري ECDH P-256 في تايجر كونكت",
        "Instant E2EE messaging with ECDH P-256 on Tiger Connect",
        1420,
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    ),
    ReelSample(
        "r2", "@riyadh_creators", "Entertainment",
        "جولة داخل استوديو البث المباشر الذكي",
        "Inside the smart neon creator Live Studio",
        980,
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    ),
    ReelSample(
        "r3", "@gamer_oasis", "Gaming",
        "تحدي 9 ألعاب مصغرة وكسب نقاط XP مضاعفة",
        "9 Mini-Games Arena speedrun for double XP & Coins",
        2310,
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onNavigateToAi: () -> Unit
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val liveComments by viewModel.liveComments.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showComposer by remember { mutableStateOf(false) }
    var showStoryCreator by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<StoryItem?>(null) }
    var sharingPost by remember { mutableStateOf<SocialPost?>(null) }
    var commentingPost by remember { mutableStateOf<SocialPost?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text(strings.tabFeed) },
                    modifier = Modifier.testTag("social_tab_feed")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text(strings.tabReels) },
                    modifier = Modifier.testTag("social_tab_reels")
                )
                Tab(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    text = { Text(strings.tabLive) },
                    modifier = Modifier.testTag("social_tab_live")
                )
            }

            when (selectedSubTab) {
                0 -> FeedTabContent(
                    postsState = postsState,
                    stories = stories,
                    bookmarks = bookmarks,
                    selectedCategory = selectedCategory,
                    strings = strings,
                    onSelectCategory = { selectedCategory = it },
                    onAddStory = { showStoryCreator = true },
                    onViewStory = { viewingStory = it },
                    onLikePost = { viewModel.likePost(it) },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onCommentPost = { commentingPost = it },
                    onSharePost = { sharingPost = it },
                    onReportPost = { postId ->
                        viewModel.submitReport("post", postId, "User flagged post for review")
                    }
                )
                1 -> ReelsPlayerTab(strings = strings, onShareReel = { title ->
                    viewModel.shareTextToFirstOrNewChat("Reel: $title", "shared_post")
                })
                2 -> LiveStreamTab(
                    strings = strings,
                    comments = liveComments,
                    onSendComment = { viewModel.addLiveComment(it) }
                )
            }
        }

        if (selectedSubTab == 0) {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_new_post")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.newPost)
            }
        }
    }

    if (showComposer) {
        PostComposerDialog(
            strings = strings,
            onDismiss = { showComposer = false },
            onPublish = { cat, body, mediaType, mediaLabel ->
                viewModel.createPost(cat, body, mediaType, mediaLabel)
                showComposer = false
            }
        )
    }

    if (showStoryCreator) {
        StoryCreatorDialog(
            strings = strings,
            onDismiss = { showStoryCreator = false },
            onPublish = { caption, gradIdx ->
                viewModel.addStory(caption, gradIdx)
                showStoryCreator = false
            }
        )
    }

    viewingStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onDismiss = { viewingStory = null }
        )
    }

    sharingPost?.let { post ->
        ModalBottomSheet(onDismissRequest = { sharingPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.pick("مشاركة عبر وحدات تايجر كونكت", "Cross-Module Share Sheet"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        viewModel.createPost(post.category, "Repost from ${post.authorName}:\n${post.content}")
                        sharingPost = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_to_feed_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareToFeed)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.addStory("${post.authorName}: ${post.content.take(120)}", 0)
                        sharingPost = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_to_story_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareToStory)
                }
                OutlinedButton(
                    onClick = {
                        viewModel.shareTextToFirstOrNewChat("${post.authorName}: ${post.content}", "shared_post")
                        sharingPost = null
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_to_chat_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareToChat)
                }
                Button(
                    onClick = {
                        viewModel.sendToAiFromShare("Analyze and summarize this post:\n${post.content}")
                        sharingPost = null
                        onNavigateToAi()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("share_to_ai_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.shareToAi)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    commentingPost?.let { post ->
        NestedCommentsDialog(
            post = post,
            strings = strings,
            onDismiss = { commentingPost = null }
        )
    }
}

@Composable
private fun FeedTabContent(
    postsState: DataState<List<SocialPost>>,
    stories: List<StoryItem>,
    bookmarks: Set<String>,
    selectedCategory: String,
    strings: TigerStrings,
    onSelectCategory: (String) -> Unit,
    onAddStory: () -> Unit,
    onViewStory: (StoryItem) -> Unit,
    onLikePost: (String) -> Unit,
    onToggleBookmark: (String) -> Unit,
    onCommentPost: (SocialPost) -> Unit,
    onSharePost: (SocialPost) -> Unit,
    onReportPost: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 24h Stories Rail
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.storiesTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(onClick = onAddStory)
                                .testTag("add_story_circle")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = strings.addStory, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(strings.addStory, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    items(stories, key = { it.id }) { story ->
                        val colors = STORY_GRADIENTS[story.gradientIndex % STORY_GRADIENTS.size]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onViewStory(story) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, Brush.linearGradient(listOf(TigerOrange, TigerGold)), CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(colors)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = story.author.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = story.author.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FEED_CATEGORIES) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                        label = {
                            Text(
                                if (category == "All") strings.pick("الكل", "All")
                                else strings.categoryLabel(category)
                            )
                        }
                    )
                }
            }
        }

        when (postsState) {
            is DataState.Loading -> item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            is DataState.Error -> item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(postsState.message, modifier = Modifier.padding(16.dp))
                }
            }
            is DataState.Success -> {
                val filtered = postsState.data.filter {
                    selectedCategory == "All" || it.category == selectedCategory
                }
                if (filtered.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Text(
                                    text = strings.pick("لا توجد منشورات بعد في هذا التصنيف", "No posts in this category yet"),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = strings.pick("اضغط على زر (+) لإنشاء أول منشور سحابي", "Tap (+) below to publish the first cloud post"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            bookmarked = post.id in bookmarks,
                            strings = strings,
                            onLike = { onLikePost(post.id) },
                            onBookmark = { onToggleBookmark(post.id) },
                            onComment = { onCommentPost(post) },
                            onShare = { onSharePost(post) },
                            onReport = { onReportPost(post.id) }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun PostCard(
    post: SocialPost,
    bookmarked: Boolean,
    strings: TigerStrings,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(post.authorName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${strings.categoryLabel(post.category)} • ${strings.formatDate((post.createdAt?.seconds ?: (System.currentTimeMillis() / 1000)) * 1000)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onReport) {
                    Icon(Icons.Default.Flag, contentDescription = "Report", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Text(post.content, style = MaterialTheme.typography.bodyLarge)

            if (!post.mediaLabel.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(post.mediaLabel, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onLike) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.formatNumber(post.likesCount))
                    }
                    TextButton(onClick = onComment) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Comments", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.formatNumber(post.commentsCount))
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
                IconButton(onClick = onBookmark) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelsPlayerTab(
    strings: TigerStrings,
    onShareReel: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }
    val filteredReels = remember(selectedFilter) {
        REEL_ITEMS.filter { selectedFilter == "All" || it.category == selectedFilter }
    }
    val pagerState = rememberPagerState(pageCount = { filteredReels.size.coerceAtLeast(1) })
    val likedIds = remember { mutableStateListOf<String>() }
    var uploadedReelUri by remember { mutableStateOf<Uri?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uploadedReelUri = uri }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(pagerState.currentPage, uploadedReelUri, filteredReels) {
        val current = filteredReels.getOrNull(pagerState.currentPage)
        val uriToPlay = uploadedReelUri?.toString() ?: current?.streamUrl
        if (!uriToPlay.isNullOrBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uriToPlay))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Tech", "Gaming", "Entertainment")) { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(if (cat == "All") strings.pick("الكل", "All") else strings.categoryLabel(cat)) }
                    )
                }
            }
            TextButton(onClick = {
                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.pick("رفع ريل", "Upload"))
            }
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)
        ) { page ->
            val reel = filteredReels[page]
            val liked = reel.id in likedIds
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0F1E))) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(0.78f)
                    ) {
                        Surface(color = TigerOrange, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = strings.categoryLabel(reel.category),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(reel.creator, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (strings.isArabic) reel.titleAr else reel.titleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                        if (uploadedReelUri != null) {
                            Text(
                                text = strings.pick("يتم تشغيل الفيديو المحلي عبر ExoPlayer Media3", "Playing local upload via Media3 ExoPlayer"),
                                style = MaterialTheme.typography.labelSmall,
                                color = TigerGold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = {
                            if (liked) likedIds.remove(reel.id) else likedIds.add(reel.id)
                        }) {
                            Icon(
                                imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Like Reel",
                                tint = if (liked) CrimsonDanger else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(strings.formatNumber(reel.likes + if (liked) 1 else 0), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = {
                            onShareReel(if (strings.isArabic) reel.titleAr else reel.titleEn)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Reel", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStreamTab(
    strings: TigerStrings,
    comments: List<com.example.viewmodel.LiveComment>,
    onSendComment: (String) -> Unit
) {
    var isBroadcasting by remember { mutableStateOf(false) }
    var viewerCount by remember { mutableIntStateOf(1284) }
    var chatInput by remember { mutableStateOf("") }

    LaunchedEffect(isBroadcasting) {
        while (true) {
            delay(3500L)
            viewerCount += if (isBroadcasting) (3..12).random() else (-2..5).random()
            if (viewerCount < 50) viewerCount = 120
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().height(230.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_live_stage_1795365391088),
                    contentDescription = "Live Stage Backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isBroadcasting) CrimsonDanger else Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isBroadcasting) strings.pick("بث مباشر الآن", "ON AIR • LIVE") else strings.pick("استوديو جاهز", "STUDIO READY"),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = TigerGold, modifier = Modifier.size(16.dp))
                            Text(strings.formatNumber(viewerCount), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Button(
                    onClick = { isBroadcasting = !isBroadcasting },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).testTag("toggle_live_broadcast_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBroadcasting) CrimsonDanger else EmeraldSuccess
                    )
                ) {
                    Icon(if (isBroadcasting) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isBroadcasting) strings.pick("إنهاء البث المباشر", "End Broadcast")
                        else strings.pick("ابدأ البث المباشر الآن", "Go Live Now")
                    )
                }
            }
        }

        Text(
            text = strings.pick("دردشة البث المباشر", "Real-Time Live Chat"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(comments, key = { it.id }) { comment ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(comment.author, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text(strings.pick("أضف تعليقاً مباشراً...", "Drop a live message...")) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        onSendComment(chatInput)
                        chatInput = ""
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Live Comment", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PostComposerDialog(
    strings: TigerStrings,
    onDismiss: () -> Unit,
    onPublish: (String, String, String, String?) -> Unit
) {
    var category by remember { mutableStateOf("General") }
    var text by remember { mutableStateOf("") }
    var pickedLabel by remember { mutableStateOf<String?>(null) }
    var pickedType by remember { mutableStateOf("none") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedLabel = uri.lastPathSegment ?: "Selected Media"
            pickedType = "image"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newPost) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FEED_CATEGORIES.drop(1)) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.categoryLabel(cat)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(strings.pick("بماذا تفكر؟", "What's on your mind?")) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("post_content_input")
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(pickedLabel ?: strings.pick("إرفاق صورة أو فيديو (Photo Picker)", "Attach Photo/Video (Photo Picker)"))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onPublish(category, text, pickedType, pickedLabel) },
                modifier = Modifier.testTag("publish_post_btn")
            ) {
                Text(strings.pick("نشر", "Publish"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.pick("إلغاء", "Cancel")) }
        }
    )
}

@Composable
private fun StoryCreatorDialog(
    strings: TigerStrings,
    onDismiss: () -> Unit,
    onPublish: (String, Int) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var gradIdx by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addStory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(STORY_GRADIENTS[gradIdx])),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = caption.ifBlank { strings.pick("معاينة قصتك لـ 24 ساعة", "24h Story Preview") },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    STORY_GRADIENTS.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(STORY_GRADIENTS[idx]))
                                .border(if (gradIdx == idx) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { gradIdx = idx }
                        )
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(strings.pick("نص القصة", "Story Caption")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (caption.isNotBlank()) onPublish(caption, gradIdx) }) {
                Text(strings.pick("نشر القصة", "Share Story"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.pick("إلغاء", "Cancel")) }
        }
    )
}

@Composable
private fun StoryViewerDialog(story: StoryItem, onDismiss: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 5000),
        label = "story_progress"
    )

    LaunchedEffect(story.id) {
        started = true
        delay(5200L)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text(story.author, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(STORY_GRADIENTS[story.gradientIndex % STORY_GRADIENTS.size])),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    )
}

@Composable
private fun NestedCommentsDialog(
    post: SocialPost,
    strings: TigerStrings,
    onDismiss: () -> Unit
) {
    val comments = remember(post.id) {
        mutableStateListOf(
            "Rami" to listOf("Great update on ${post.category}!", "↳ Totally agree!"),
            "Lina" to listOf("Sharing this to my encrypted group now.")
        )
    }
    var newComment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.pick("التعليقات المتداخلة", "Nested Comments")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                comments.forEach { (author, lines) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            lines.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    placeholder = { Text(strings.pick("أضف رداً...", "Write a reply...")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newComment.isNotBlank()) {
                    comments.add("You" to listOf(newComment.trim()))
                    newComment = ""
                }
            }) {
                Text(strings.pick("إرسال", "Reply"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.pick("إغلاق", "Close")) }
        }
    )
}
