package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.AppSettingsState
import com.example.core.LayoutPreviewMode
import com.example.core.TigerStrings
import com.example.core.configureAppCheck
import com.example.di.AppContainer
import com.example.ui.ai.TigerAiScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.hub.HubScreen
import com.example.ui.messenger.MessengerScreen
import com.example.ui.social.SocialScreen
import com.example.ui.store.StoreScreen
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.TigerConnectTheme
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must execute before any Firebase, AppContainer, or Compose initialization
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val container = AppContainer.getInstance(applicationContext)
        setContent {
            TigerConnectRoot(container = container)
        }
    }
}

private data class MainNavTab(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
private fun TigerConnectRoot(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val settings by container.settingsStore.settingsFlow.collectAsStateWithLifecycle(
        initialValue = AppSettingsState()
    )
    val strings = remember(settings.isArabic) { TigerStrings(settings.isArabic) }
    val accentPreset = remember(settings.accentId) { AccentPreset.fromId(settings.accentId) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    TigerConnectTheme(
        darkTheme = settings.darkTheme,
        accentPreset = accentPreset,
        isArabic = settings.isArabic
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val uid = currentUser?.uid
            if (uid == null) {
                AuthScreen(
                    container = container,
                    strings = strings,
                    darkTheme = settings.darkTheme,
                    backendBaseUrl = settings.backendBaseUrl,
                    onToggleLanguage = {
                        scope.launch { container.settingsStore.setArabic(!settings.isArabic) }
                    },
                    onToggleTheme = {
                        scope.launch { container.settingsStore.setDarkTheme(!settings.darkTheme) }
                    },
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    }
                )
            } else {
                // Scope ViewModel strictly per signed-in UID so account switching resets cleanly
                key(uid) {
                    val viewModel: TigerViewModel = viewModel(
                        key = uid,
                        factory = TigerViewModel.Factory(container, uid)
                    )
                    TigerConnectMainShell(
                        container = container,
                        viewModel = viewModel,
                        settings = settings,
                        strings = strings
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TigerConnectMainShell(
    container: AppContainer,
    viewModel: TigerViewModel,
    settings: AppSettingsState,
    strings: TigerStrings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val chatBadgeCount = (chatsState as? DataState.Success)?.data?.size ?: 0

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val navTabs = remember(strings) {
        listOf(
            MainNavTab(0, strings.navFeed, Icons.Default.DynamicFeed, "nav_tab_social"),
            MainNavTab(1, strings.navChat, Icons.AutoMirrored.Filled.Chat, "nav_tab_messenger"),
            MainNavTab(2, strings.navAi, Icons.Default.AutoAwesome, "nav_tab_ai"),
            MainNavTab(3, strings.navStore, Icons.Default.Storefront, "nav_tab_store"),
            MainNavTab(4, strings.navHub, Icons.Default.GridView, "nav_tab_hub")
        )
    }

    val nextLayoutMode = remember(settings.layoutPreviewMode) {
        when (settings.layoutPreviewMode) {
            LayoutPreviewMode.AUTO -> LayoutPreviewMode.PHONE
            LayoutPreviewMode.PHONE -> LayoutPreviewMode.TABLET
            LayoutPreviewMode.TABLET -> LayoutPreviewMode.AUTO
        }
    }

    val layoutModeIcon = when (settings.layoutPreviewMode) {
        LayoutPreviewMode.AUTO -> Icons.Default.Devices
        LayoutPreviewMode.PHONE -> Icons.Default.PhoneAndroid
        LayoutPreviewMode.TABLET -> Icons.Default.TabletAndroid
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = when (settings.layoutPreviewMode) {
            LayoutPreviewMode.TABLET -> true
            LayoutPreviewMode.PHONE -> false
            LayoutPreviewMode.AUTO -> maxWidth >= 600.dp
        }
        val constrainPhonePreview = settings.layoutPreviewMode == LayoutPreviewMode.PHONE && maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tiger_icon_1795365393271),
                                contentDescription = strings.appTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = navTabs[selectedTab].label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        // Global Device-Preview / Adaptive Layout switcher
                        IconButton(
                            onClick = {
                                scope.launch {
                                    container.settingsStore.setLayoutPreviewMode(nextLayoutMode)
                                }
                                viewModel.showMessage(
                                    when (nextLayoutMode) {
                                        LayoutPreviewMode.AUTO -> strings.pick("وضع العرض: تلقائي متجاوب", "Layout Preview: Auto Adaptive")
                                        LayoutPreviewMode.PHONE -> strings.pick("وضع العرض: شاشة هاتف", "Layout Preview: Phone Mode")
                                        LayoutPreviewMode.TABLET -> strings.pick("وضع العرض: شاشة تابلت (شريط جانبي)", "Layout Preview: Tablet Rail Mode")
                                    }
                                )
                            },
                            modifier = Modifier.testTag("global_layout_switcher_button")
                        ) {
                            Icon(
                                imageVector = layoutModeIcon,
                                contentDescription = strings.pick("تبديل وضع الشاشة", "Switch layout preview")
                            )
                        }

                        // Instant AR / EN language toggle
                        Surface(
                            onClick = {
                                scope.launch { container.settingsStore.setArabic(!settings.isArabic) }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .testTag("global_language_toggle")
                        ) {
                            Text(
                                text = if (settings.isArabic) "EN" else "عربي",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Instant Dark / Light mode toggle
                        IconButton(
                            onClick = {
                                scope.launch { container.settingsStore.setDarkTheme(!settings.darkTheme) }
                            },
                            modifier = Modifier.testTag("global_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (settings.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = strings.pick("تبديل السمة", "Toggle theme")
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab.index,
                                onClick = { selectedTab = tab.index },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    if (tab.index == 1 && chatBadgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(strings.formatNumber(chatBadgeCount.coerceAtMost(99)))
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            if (useNavigationRail) {
                Row(
                    modifier = contentModifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.size(8.dp))
                        navTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab.index,
                                onClick = { selectedTab = tab.index },
                                modifier = Modifier.testTag(tab.tag),
                                icon = {
                                    if (tab.index == 1 && chatBadgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(strings.formatNumber(chatBadgeCount.coerceAtMost(99)))
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 960.dp)
                                .fillMaxWidth()
                        ) {
                            TigerTabContent(
                                selectedTab = selectedTab,
                                viewModel = viewModel,
                                strings = strings,
                                onNavigateToAi = { selectedTab = 2 },
                                onSignOut = {
                                    signOutUser(
                                        context = context,
                                        container = container,
                                        scope = scope
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = contentModifier,
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = if (constrainPhonePreview) {
                            Modifier
                                .width(412.dp)
                                .fillMaxHeight()
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        TigerTabContent(
                            selectedTab = selectedTab,
                            viewModel = viewModel,
                            strings = strings,
                            onNavigateToAi = { selectedTab = 2 },
                            onSignOut = {
                                signOutUser(
                                    context = context,
                                    container = container,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TigerTabContent(
    selectedTab: Int,
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onNavigateToAi: () -> Unit,
    onSignOut: () -> Unit
) {
    when (selectedTab) {
        0 -> SocialScreen(
            viewModel = viewModel,
            strings = strings,
            onNavigateToAi = onNavigateToAi
        )
        1 -> MessengerScreen(
            viewModel = viewModel,
            strings = strings
        )
        2 -> TigerAiScreen(
            viewModel = viewModel,
            strings = strings
        )
        3 -> StoreScreen(
            viewModel = viewModel,
            strings = strings
        )
        else -> HubScreen(
            viewModel = viewModel,
            strings = strings,
            onSignOut = onSignOut
        )
    }
}
