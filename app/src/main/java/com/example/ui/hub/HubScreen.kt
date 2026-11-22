package com.example.ui.hub

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TigerStrings
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.ui.theme.VioletLive
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel

private enum class HubDestination {
    DASHBOARD,
    AVATAR_STUDIO,
    GAMES_ARENA,
    DRIVE_VAULT,
    TIGER_MAP,
    PLANS,
    ADMIN,
    SETTINGS
}

private data class HubTile(
    val dest: HubDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val tag: String
)

@Composable
fun HubScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onSignOut: () -> Unit
) {
    var currentDest by remember { mutableStateOf(HubDestination.DASHBOARD) }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = (profileState as? DataState.Success)?.data

    when (currentDest) {
        HubDestination.AVATAR_STUDIO -> AvatarStudioScreen(
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD },
            onExportAvatar = { json, bmp -> viewModel.saveAvatar(json, bmp) }
        )
        HubDestination.GAMES_ARENA -> GamesArenaScreen(
            viewModel = viewModel,
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD }
        )
        HubDestination.DRIVE_VAULT -> DriveVaultScreen(
            viewModel = viewModel,
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD }
        )
        HubDestination.TIGER_MAP -> TigerMapScreen(
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD }
        )
        HubDestination.PLANS -> PlansScreen(
            viewModel = viewModel,
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD }
        )
        HubDestination.ADMIN -> AdminPanelScreen(
            viewModel = viewModel,
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD }
        )
        HubDestination.SETTINGS -> ThemeAndSecuritySettingsScreen(
            viewModel = viewModel,
            strings = strings,
            onBack = { currentDest = HubDestination.DASHBOARD },
            onSignOut = onSignOut
        )
        HubDestination.DASHBOARD -> {
            val tiles = listOf(
                HubTile(HubDestination.AVATAR_STUDIO, strings.hubAvatarStudio, strings.pick("بناء أفاتار + تصدير PNG", "Bitmoji Builder & PNG"), Icons.Default.Face, TigerOrange, "hub_tile_avatar"),
                HubTile(HubDestination.GAMES_ARENA, strings.hubGamesArena, strings.pick("9 ألعاب • XP وعملات", "9 Games • XP & Coins"), Icons.Default.SportsEsports, VioletLive, "hub_tile_games"),
                HubTile(HubDestination.DRIVE_VAULT, strings.hubDriveVault, strings.pick("سحابة + خزينة مقفلة", "Cloud + PIN Vault"), Icons.Default.Cloud, CobaltInfo, "hub_tile_drive"),
                HubTile(HubDestination.TIGER_MAP, strings.hubTigerMap, strings.pick("GPS مباشر + أصدقاء", "Live GPS & Places"), Icons.Default.Map, EmeraldSuccess, "hub_tile_map"),
                HubTile(HubDestination.PLANS, strings.hubPlans, "${profile?.planTier ?: "FREE"} • 10K–10M AI", Icons.Default.WorkspacePremium, TigerGold, "hub_tile_plans"),
                HubTile(HubDestination.ADMIN, strings.hubAdmin, strings.pick("المشرفون والبلاغات", "Moderation & Audits"), Icons.Default.AdminPanelSettings, CrimsonDanger, "hub_tile_admin"),
                HubTile(HubDestination.SETTINGS, strings.hubSettings, strings.pick("RTL/LTR • الألوان • البصمة", "Theme, Layout & PIN"), Icons.Default.Palette, TigerOrange, "hub_tile_settings")
            )

            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // User Profile Summary Banner
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (profile?.displayName ?: "TC").take(2).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile?.displayName ?: "Tiger Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${profile?.username ?: "tiger_user"} • ${profile?.planTier ?: "FREE"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(color = TigerGold.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = "${strings.formatNumber(profile?.xp ?: 0L)} XP",
                                color = TigerGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 152.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tiles) { tile ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clickable { currentDest = tile.dest }
                                .testTag(tile.tag),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tile.tint.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tile.icon, contentDescription = tile.title, tint = tile.tint)
                                }
                                Column {
                                    Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(tile.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
