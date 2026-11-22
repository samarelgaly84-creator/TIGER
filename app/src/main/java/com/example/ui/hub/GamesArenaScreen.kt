package com.example.ui.hub

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.TigerStrings
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.example.viewmodel.DataState
import com.example.viewmodel.TigerViewModel
import kotlinx.coroutines.delay

private enum class MiniGameId(val id: String, val emoji: String, val titleEn: String, val titleAr: String) {
    MEMORY_MATCH("memory_match", "🃏", "Memory Match", "تطابق الذاكرة"),
    REACTION_REFLEX("reaction_reflex", "⚡", "Reaction Reflex", "سرعة البديهة"),
    TIGER_TAPPER("tiger_tapper", "🐯", "Tiger Tapper", "نقرات النمر"),
    WORD_SCRAMBLE("word_scramble", "🔤", "Word Scramble", "فك الكلمات"),
    TRIVIA_QUIZ("trivia_quiz", "🧠", "Trivia Quiz", "مسابقة معلومات"),
    COLOR_RUSH("color_rush", "🎨", "Color Rush", "اندفاع الألوان"),
    EMOJI_CATCH("emoji_catch", "🎯", "Emoji Catch", "التقاط الرموز"),
    TIC_TAC_TOE("tic_tac_toe", "❌", "Tic-Tac-Toe", "إكس أو الذكية"),
    MATH_SPRINT("math_sprint", "➕", "Math Sprint", "سباق الحساب")
}

@Composable
fun GamesArenaScreen(
    viewModel: TigerViewModel,
    strings: TigerStrings,
    onBack: () -> Unit
) {
    var activeGame by remember { mutableStateOf<MiniGameId?>(null) }
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val scoresState by viewModel.gameScoresState.collectAsStateWithLifecycle()

    BackHandler(onBack = {
        if (activeGame != null) activeGame = null else onBack()
    })

    if (activeGame != null) {
        val game = activeGame!!
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { activeGame = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Games") }
                Text("${game.emoji} ${if (strings.isArabic) game.titleAr else game.titleEn}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            val submit = { score: Int -> viewModel.submitMiniGameScore(game.id, score) }
            when (game) {
                MiniGameId.MEMORY_MATCH -> MemoryMatchGame(strings, submit)
                MiniGameId.REACTION_REFLEX -> ReactionReflexGame(strings, submit)
                MiniGameId.TIGER_TAPPER -> TigerTapperGame(strings, submit)
                MiniGameId.WORD_SCRAMBLE -> WordScrambleGame(strings, submit)
                MiniGameId.TRIVIA_QUIZ -> TriviaQuizGame(strings, submit)
                MiniGameId.COLOR_RUSH -> ColorRushGame(strings, submit)
                MiniGameId.EMOJI_CATCH -> EmojiCatchGame(strings, submit)
                MiniGameId.TIC_TAC_TOE -> TicTacToeGame(strings, submit)
                MiniGameId.MATH_SPRINT -> MathSprintGame(strings, submit)
            }
        }
        return
    }

    val profile = (profileState as? DataState.Success)?.data
    val scores = (scoresState as? DataState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubGamesArena, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // XP & Coins Summary Header
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TigerOrange)
                    Text("${strings.formatNumber(profile?.xp ?: 0L)} XP", fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = TigerGold)
                    Text("${strings.formatNumber(profile?.coins ?: 0L)} Coins", fontWeight = FontWeight.Bold, color = TigerGold)
                }
            }
        }

        // 9 Mini-Games Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(MiniGameId.entries) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clickable { activeGame = game }
                        .testTag("play_game_${game.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(game.emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (strings.isArabic) game.titleAr else game.titleEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Leaderboard Snapshot
        Text(strings.pick("لوحة الصدارة (أعلى النتائج)", "Personal & Arena Leaderboard"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.height(120.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (scores.isEmpty()) {
                item {
                    Text(strings.pick("العب أي لعبة لتسجيل نتيجتك الأولى وكسب عملات تايجر!", "Play any of the 9 games to post your first score!"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                itemsIndexed(scores.take(5)) { idx, rec ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${idx + 1} ${rec.playerName} • ${rec.gameId}", style = MaterialTheme.typography.labelMedium)
                            Text("${rec.score} pts (+${rec.xpEarned} XP)", color = TigerGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// 1. Memory Match (4x3 pairs)
@Composable
private fun MemoryMatchGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    val emojis = remember { listOf("🐯", "🔥", "⚡", "👑", "💎", "🚀").flatMap { listOf(it, it) }.shuffled() }
    val matched = remember { mutableStateListOf<Int>() }
    val revealed = remember { mutableStateListOf<Int>() }
    var moves by remember { mutableIntStateOf(0) }

    LaunchedEffect(revealed.size) {
        if (revealed.size == 2) {
            delay(500L)
            if (emojis[revealed[0]] == emojis[revealed[1]]) {
                matched.addAll(revealed)
            }
            revealed.clear()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Matches: ${matched.size / 2} / 6 • Moves: $moves", fontWeight = FontWeight.Bold)
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until 4) {
                    val idx = row * 4 + col
                    val isOpen = idx in matched || idx in revealed
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isOpen) TigerOrange.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (!isOpen && revealed.size < 2) {
                                    revealed.add(idx)
                                    moves++
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isOpen) emojis[idx] else "?", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        if (matched.size == emojis.size) {
            val score = (200 - moves * 5).coerceAtLeast(50)
            Button(onClick = { onSubmitScore(score) }) {
                Text(strings.pick("حفظ النتيجة ($score)", "Claim $score pts & XP"))
            }
        }
    }
}

// 2. Reaction Reflex
@Composable
private fun ReactionReflexGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    var phase by remember { mutableStateOf("idle") } // idle, waiting, go, done
    var goTime by remember { mutableLongStateOf(0L) }
    var reactionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(phase) {
        if (phase == "waiting") {
            delay((1200L..2800L).random())
            goTime = System.currentTimeMillis()
            phase = "go"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    when (phase) {
                        "waiting" -> CrimsonDanger
                        "go" -> EmeraldSuccess
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable {
                    when (phase) {
                        "idle", "done" -> phase = "waiting"
                        "go" -> {
                            reactionMs = (System.currentTimeMillis() - goTime).coerceAtLeast(80L)
                            phase = "done"
                        }
                        "waiting" -> phase = "idle"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (phase) {
                    "idle" -> strings.pick("اضغط للبدء", "Tap to Start")
                    "waiting" -> strings.pick("انتظر اللون الأخضر...", "Wait for GREEN...")
                    "go" -> strings.pick("اضغط الآن!!", "TAP NOW!!")
                    else -> "${reactionMs} ms"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        if (phase == "done") {
            val score = (600 - reactionMs.toInt()).coerceIn(40, 500)
            Button(onClick = { onSubmitScore(score) }) {
                Text("Submit Score ($score pts)")
            }
        }
    }
}

// 3. Tiger Tapper (10-second sprint)
@Composable
private fun TigerTapperGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(10) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (running) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            running = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Time: ${timeLeft}s • Taps: $taps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(
            onClick = {
                if (!running && timeLeft == 10) running = true
                if (running) taps++
            },
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(80.dp)
        ) {
            Text("🐯\nTAP!", textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)
        }
        if (timeLeft == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSubmitScore(taps * 4) }) { Text("Submit ${taps * 4} pts") }
                OutlinedButton(onClick = { taps = 0; timeLeft = 10 }) { Text(strings.pick("إعادة", "Replay")) }
            }
        }
    }
}

// 4. Word Scramble
@Composable
private fun WordScrambleGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    val words = remember { listOf("TIGER" to "GRETI", "CRYPTO" to "PYTROC", "STUDIO" to "DUOIST", "GALAXY" to "LAXAGY") }
    var idx by remember { mutableIntStateOf(0) }
    var guess by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Unscramble: ${words[idx].second}", style = MaterialTheme.typography.headlineMedium, color = TigerGold, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = guess, onValueChange = { guess = it.uppercase() }, label = { Text("Your Answer") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (guess.trim() == words[idx].first) {
                    score += 50
                    idx = (idx + 1) % words.size
                    guess = ""
                }
            }) { Text("Check") }
            if (score > 0) {
                OutlinedButton(onClick = { onSubmitScore(score) }) { Text("Submit $score pts") }
            }
        }
        Text("Score: $score", fontWeight = FontWeight.Bold)
    }
}

// 5. Trivia Quiz
@Composable
private fun TriviaQuizGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    val questions = remember {
        listOf(
            Triple("Which curve powers Tiger Connect E2EE?", listOf("P-256", "MD5", "DES"), 0),
            Triple("What is the PRO tier monthly AI limit?", listOf("10,000", "100,000", "10,000,000"), 2),
            Triple("Which language builds native Compose UI?", listOf("Kotlin", "Ruby", "PHP"), 0)
        )
    }
    var qIdx by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (!finished) {
            val q = questions[qIdx]
            Text(q.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            q.second.forEachIndexed { idx, opt ->
                OutlinedButton(
                    onClick = {
                        if (idx == q.third) score += 60
                        if (qIdx + 1 < questions.size) qIdx++ else finished = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(opt) }
            }
        } else {
            Text("Quiz Complete! Score: $score", style = MaterialTheme.typography.headlineSmall, color = EmeraldSuccess)
            Button(onClick = { onSubmitScore(score.coerceAtLeast(30)) }) { Text("Submit Score") }
        }
    }
}

// 6. Color Rush (Stroop Challenge)
@Composable
private fun ColorRushGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    val palette = listOf("ORANGE" to TigerOrange, "GREEN" to EmeraldSuccess, "BLUE" to CobaltInfo, "RED" to CrimsonDanger)
    var wordIdx by remember { mutableIntStateOf(0) }
    var inkIdx by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(strings.pick("اضغط على لون الحبر وليس الكلمة!", "Tap the INK COLOR, not the word!"))
        Text(palette[wordIdx].first, style = MaterialTheme.typography.displaySmall, color = palette[inkIdx].second, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            palette.forEachIndexed { idx, pair ->
                Button(
                    onClick = {
                        if (idx == inkIdx) score += 30 else score = (score - 10).coerceAtLeast(0)
                        wordIdx = palette.indices.random()
                        inkIdx = palette.indices.random()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pair.second)
                ) { Text(pair.first.take(3), color = Color.White) }
            }
        }
        Text("Score: $score", fontWeight = FontWeight.Bold)
        if (score >= 30) Button(onClick = { onSubmitScore(score) }) { Text("Claim $score pts") }
    }
}

// 7. Emoji Catch
@Composable
private fun EmojiCatchGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    var lane by remember { mutableIntStateOf(1) }
    var targetLane by remember { mutableIntStateOf(0) }
    var caught by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        repeat(15) {
            delay(850L)
            if (lane == targetLane) caught += 25
            targetLane = (0..2).random()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Caught Score: $caught", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            (0..2).forEach { col ->
                Surface(
                    color = if (lane == col) TigerOrange.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(92.dp).clickable { lane = col }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(8.dp)) {
                        Text(if (targetLane == col) "🐯" else "", style = MaterialTheme.typography.headlineSmall)
                        Text(if (lane == col) "🧺" else "", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Button(onClick = { onSubmitScore(caught.coerceAtLeast(25)) }) { Text("Submit $caught pts") }
    }
}

// 8. Tic-Tac-Toe vs Smart AI
@Composable
private fun TicTacToeGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    val board = remember { mutableStateListOf("", "", "", "", "", "", "", "", "") }
    var status by remember { mutableStateOf("Your Turn (X)") }
    var winner by remember { mutableStateOf<String?>(null) }

    fun checkWinner(): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (l in lines) {
            if (board[l[0]].isNotEmpty() && board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]]) return board[l[0]]
        }
        return if (board.none { it.isEmpty() }) "DRAW" else null
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(status, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        for (r in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0..2) {
                    val idx = r * 3 + c
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (winner == null && board[idx].isEmpty()) {
                                    board[idx] = "X"
                                    winner = checkWinner()
                                    if (winner == null) {
                                        val empty = board.indices.filter { board[it].isEmpty() }
                                        if (empty.isNotEmpty()) {
                                            val aiMove = if (4 in empty) 4 else empty.random()
                                            board[aiMove] = "O"
                                            winner = checkWinner()
                                        }
                                    }
                                    status = when (winner) {
                                        "X" -> "You Win! (+120 pts)"
                                        "O" -> "Tiger Bot Wins!"
                                        "DRAW" -> "Draw! (+60 pts)"
                                        else -> "Your Turn (X)"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(board[idx], style = MaterialTheme.typography.headlineMedium, color = if (board[idx] == "X") TigerOrange else TigerGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (winner != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSubmitScore(if (winner == "X") 120 else 60) }) { Text("Claim XP") }
                OutlinedButton(onClick = { for (i in 0..8) board[i] = ""; winner = null; status = "Your Turn (X)" }) { Text("Reset") }
            }
        }
    }
}

// 9. Math Sprint
@Composable
private fun MathSprintGame(strings: TigerStrings, onSubmitScore: (Int) -> Unit) {
    var a by remember { mutableIntStateOf(7) }
    var b by remember { mutableIntStateOf(8) }
    var answerInput by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("$a + $b = ?", style = MaterialTheme.typography.displaySmall, color = TigerOrange, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = answerInput, onValueChange = { answerInput = it }, label = { Text("Answer") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (answerInput.trim().toIntOrNull() == a + b) {
                    score += 35
                    a = (3..25).random()
                    b = (4..25).random()
                    answerInput = ""
                }
            }) { Text("Submit") }
            if (score > 0) OutlinedButton(onClick = { onSubmitScore(score) }) { Text("Save $score pts") }
        }
        Text("Score: $score", fontWeight = FontWeight.Bold)
    }
}
