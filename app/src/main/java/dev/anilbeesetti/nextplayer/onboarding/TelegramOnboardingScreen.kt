package dev.anilbeesetti.nextplayer.onboarding

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.anilbeesetti.nextplayer.core.common.storagePermission
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object OnboardingManager {
    private const val PREFS_NAME = "afterplayer_onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed_v1"

    fun shouldShowOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completed = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            storagePermission,
        ) == PackageManager.PERMISSION_GRANTED
        return !completed || !hasPermission
    }

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }
}

@Composable
fun TelegramOnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val initialGranted = remember {
        ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
    }

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var permissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            permissionDeniedOnce = false
            currentStep = 2
        } else {
            permissionDeniedOnce = true
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top step indicator pills (3 steps)
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val isSelected = index == currentStep
                    val isDone = index < currentStep
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 8.dp,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy,
                        ),
                        label = "step_dot_$index",
                    )
                    val dotColor = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }

            // Animated Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val isForward = targetState >= initialState
                        (slideInHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                            initialOffsetX = { if (isForward) it / 2 else -it / 2 },
                        ) + fadeIn(tween(240)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            targetOffsetX = { if (isForward) -it / 3 else it / 3 },
                        ) + fadeOut(tween(180)) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                        ))
                    },
                    label = "onboarding_step_transition",
                ) { step ->
                    when (step) {
                        0 -> WelcomeStepContent()
                        1 -> PermissionStepContent(permissionDeniedOnce = permissionDeniedOnce)
                        2 -> AllSetStepContent()
                    }
                }
            }

            // Bottom Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (currentStep) {
                    0 -> {
                        Button(
                            onClick = {
                                val alreadyGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    storagePermission,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (alreadyGranted || initialGranted) {
                                    currentStep = 2
                                } else {
                                    currentStep = 1
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = NextIcons.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    1 -> {
                        Button(
                            onClick = {
                                permissionLauncher.launch(storagePermission)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = NextIcons.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.allow_access),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        if (permissionDeniedOnce) {
                            OutlinedButton(
                                onClick = {
                                    activity?.finishAffinity()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(
                                    imageVector = NextIcons.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.close),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }

                    2 -> {
                        val greenColor = Color(0xFF16A34A)
                        Button(
                            onClick = {
                                OnboardingManager.markCompleted(context)
                                onFinished()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = greenColor,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "Start Watching",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = NextIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome_float",
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "welcome_scale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    translationY = floatOffset
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = scalePulse
                        scaleY = scalePulse
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            ) {}

            Surface(
                modifier = Modifier.size(104.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = NextIcons.Play,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(54.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ultra-smooth 4K playback, hardware acceleration, smart gestures, and customizable subtitles — crafted for speed.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun PermissionStepContent(permissionDeniedOnce: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "perm_anim")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "perm_scale",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(148.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ) {}

            Surface(
                modifier = Modifier.size(104.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = NextIcons.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(50.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.storage_permission_required),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (permissionDeniedOnce) {
                "Video access is needed to find and play videos on your phone. Tap \"Allow Access\" below to grant permission."
            } else {
                stringResource(R.string.storage_permission_description)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun AllSetStepContent() {
    val sweepProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val popScale = remember { Animatable(0.75f) }

    LaunchedEffect(Unit) {
        launch {
            popScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        )
        delay(40)
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        )
    }

    val greenMain = Color(0xFF16A34A)
    val greenSoft = Color(0xFF16A34A).copy(alpha = 0.16f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    scaleX = popScale.value
                    scaleY = popScale.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(132.dp),
                shape = CircleShape,
                color = greenSoft,
            ) {}

            Canvas(modifier = Modifier.size(104.dp)) {
                val strokeWidth = 6.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Fill green circle once ring completes
                if (sweepProgress.value > 0.5f) {
                    val fillAlpha = ((sweepProgress.value - 0.5f) * 2f).coerceIn(0f, 1f)
                    drawCircle(
                        color = greenMain.copy(alpha = fillAlpha),
                        radius = (size.minDimension - strokeWidth) / 2f,
                    )
                }

                // Rotating/Sweeping green circle ring
                drawArc(
                    color = greenMain,
                    startAngle = -90f,
                    sweepAngle = 360f * sweepProgress.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // Animated Checkmark inside the circle
                val checkVal = checkProgress.value
                if (checkVal > 0f) {
                    val w = size.width
                    val h = size.height
                    val p1 = Offset(w * 0.29f, h * 0.52f)
                    val p2 = Offset(w * 0.45f, h * 0.67f)
                    val p3 = Offset(w * 0.72f, h * 0.36f)

                    val firstSegmentProgress = (checkVal / 0.45f).coerceIn(0f, 1f)
                    val currentMid = Offset(
                        x = p1.x + (p2.x - p1.x) * firstSegmentProgress,
                        y = p1.y + (p2.y - p1.y) * firstSegmentProgress,
                    )
                    drawLine(
                        color = Color.White,
                        start = p1,
                        end = currentMid,
                        strokeWidth = 5.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    if (checkVal > 0.45f) {
                        val secondSegmentProgress = ((checkVal - 0.45f) / 0.55f).coerceIn(0f, 1f)
                        val currentEnd = Offset(
                            x = p2.x + (p3.x - p2.x) * secondSegmentProgress,
                            y = p2.y + (p3.y - p2.y) * secondSegmentProgress,
                        )
                        drawLine(
                            color = Color.White,
                            start = p2,
                            end = currentEnd,
                            strokeWidth = 5.5.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "All Set!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Permission granted and your media library is ready. Enjoy a fast, lag-free video experience!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}
