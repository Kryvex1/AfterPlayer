package dev.anilbeesetti.nextplayer.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.storagePermission
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun OnboardingScreen(
    isPermissionInitiallyGranted: Boolean,
    onOnboardingComplete: () -> Unit,
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var permissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            currentStep = 2
        } else {
            permissionDeniedOnce = true
        }
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 1 && isPermissionInitiallyGranted) {
            currentStep = 2
        } else if (currentStep == 1 && !permissionDeniedOnce) {
            permissionLauncher.launch(storagePermission)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top step indicator pills
            StepProgressIndicator(
                currentStep = currentStep,
                totalSteps = 3,
                modifier = Modifier.padding(top = 12.dp),
            )

            // Center animated hero content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val isForward = targetState >= initialState
                        (slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { width -> if (isForward) width / 2 else -width / 2 },
                        ) + fadeIn(tween(260)) + scaleIn(
                            initialScale = 0.90f,
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                        )) togetherWith (slideOutHorizontally(
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                            targetOffsetX = { width -> if (isForward) -width / 3 else width / 3 },
                        ) + fadeOut(tween(200)))
                    },
                    label = "onboarding_step_content",
                ) { step ->
                    when (step) {
                        0 -> WelcomeStepContent()
                        1 -> PermissionStepContent(permissionDeniedOnce = permissionDeniedOnce)
                        else -> AllSetStepContent()
                    }
                }
            }

            // Bottom action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (currentStep) {
                    0 -> {
                        Button(
                            onClick = {
                                if (isPermissionInitiallyGranted) {
                                    currentStep = 2
                                } else {
                                    currentStep = 1
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("onboarding_get_started_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_get_started),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                                .height(56.dp)
                                .testTag("onboarding_grant_permission_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(
                                imageVector = NextIcons.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.onboarding_allow_permission),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (permissionDeniedOnce) {
                            OutlinedButton(
                                onClick = {
                                    (context as? Activity)?.finishAffinity()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("onboarding_exit_app_button"),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.exit),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = onOnboardingComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("onboarding_finish_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2EAE5B),
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_open_app),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = NextIcons.ArrowForward,
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
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until totalSteps) {
            val isSelected = i == currentStep
            val isCompleted = i < currentStep
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 10.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "step_dot_$i",
            )
            val dotColor = when {
                currentStep == 2 -> Color(0xFF2EAE5B)
                isSelected || isCompleted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
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
}

@Composable
private fun WelcomeStepContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring_rotation",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Animated ambient halo
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.28f),
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.minDimension / 2f,
                )
            }

            // Rotating orbital ring
            Canvas(modifier = Modifier.size(160.dp)) {
                rotate(ringRotation) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.45f),
                        startAngle = 0f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Center Play Icon Badge
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        scaleX = 1f + (pulseScale - 1f) * 0.35f
                        scaleY = 1f + (pulseScale - 1f) * 0.35f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryColor,
                                primaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = NextIcons.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun PermissionStepContent(permissionDeniedOnce: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "permission_pulse")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_progress",
    )
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "icon_float",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Expanding radar ripple waves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = size.minDimension / 2f
                drawCircle(
                    color = primaryColor.copy(alpha = (1f - waveProgress) * 0.30f),
                    radius = maxRadius * (0.45f + waveProgress * 0.55f),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
                val secondWave = (waveProgress + 0.5f) % 1f
                drawCircle(
                    color = primaryColor.copy(alpha = (1f - secondWave) * 0.25f),
                    radius = maxRadius * (0.45f + secondWave * 0.55f),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }

            // Floating folder/media badge
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        translationY = iconFloat
                    }
                    .clip(RoundedCornerShape(34.dp))
                    .background(secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = NextIcons.Folder,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(56.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_permission_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (permissionDeniedOnce) {
                stringResource(R.string.permission_info, storagePermission.substringAfterLast('.'))
            } else {
                stringResource(R.string.onboarding_permission_subtitle)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun AllSetStepContent() {
    val sweepAnim = remember { Animatable(0f) }
    val checkAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    LaunchedEffect(Unit) {
        // Spinning ring sweep to 360 degrees
        sweepAnim.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
        // Smooth green checkmark stroke draw
        checkAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )
    }

    val greenAccent = Color(0xFF2EAE5B)
    val greenGlow = Color(0xFF2EAE5B).copy(alpha = 0.18f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f

                // Soft glowing green backdrop circle
                drawCircle(
                    color = greenGlow,
                    radius = radius * 1.15f,
                )

                // Filled circle as check completes
                if (checkAnim.value > 0f) {
                    drawCircle(
                        color = greenAccent.copy(alpha = 0.15f * checkAnim.value),
                        radius = radius,
                    )
                }

                // Spinning circular arc that completes a full green circle
                drawArc(
                    color = greenAccent,
                    startAngle = -90f + (1f - sweepAnim.value / 360f) * 180f,
                    sweepAngle = sweepAnim.value,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                // Animated green checkmark path
                if (checkAnim.value > 0f) {
                    val progress = checkAnim.value
                    val startX = size.width * 0.28f
                    val startY = size.height * 0.52f
                    val midX = size.width * 0.44f
                    val midY = size.height * 0.67f
                    val endX = size.width * 0.73f
                    val endY = size.height * 0.36f

                    val path = Path().apply {
                        moveTo(startX, startY)
                        if (progress <= 0.4f) {
                            val segmentProgress = progress / 0.4f
                            lineTo(
                                startX + (midX - startX) * segmentProgress,
                                startY + (midY - startY) * segmentProgress,
                            )
                        } else {
                            lineTo(midX, midY)
                            val segmentProgress = (progress - 0.4f) / 0.6f
                            lineTo(
                                midX + (endX - midX) * segmentProgress,
                                midY + (endY - midY) * segmentProgress,
                            )
                        }
                    }

                    drawPath(
                        path = path,
                        color = greenAccent,
                        style = Stroke(
                            width = 7.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_all_set_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.onboarding_all_set_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
