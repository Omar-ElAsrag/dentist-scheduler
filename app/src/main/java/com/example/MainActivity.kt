package com.example

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import coil.compose.AsyncImage
import com.example.data.LocalePreferences
import com.example.data.ThemePreferences
import com.example.ui.DentistViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ClinicsScreen
import com.example.ui.screens.PatientsScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProceduresLibraryDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val lang = newBase?.let { base ->
            runBlocking { LocalePreferences(base).languageFlow.first() }
        } ?: "en"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase?.resources?.configuration)
        config?.setLocale(locale)
        super.attachBaseContext(newBase?.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val appThemeMode by ThemePreferences(this).themeModeFlow.collectAsState(initial = ThemePreferences.DEFAULT_MODE)
            MyApplicationTheme(themeMode = appThemeMode) {
                MainAppScreen(
                    localePreferences = LocalePreferences(this),
                    themePreferences = ThemePreferences(this)
                )
            }
        }
    }
}

sealed class NavigationItem(val route: String, val titleResId: Int, val activeIcon: androidx.compose.ui.graphics.vector.ImageVector, val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Patients : NavigationItem("patients", R.string.nav_patients, Icons.Filled.People, Icons.Outlined.People)
    object Schedule : NavigationItem("schedule", R.string.nav_schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Analytics : NavigationItem("analytics", R.string.nav_analytics, Icons.Filled.Analytics, Icons.Outlined.Analytics)
    object Clinics : NavigationItem("clinics", R.string.nav_associates, Icons.Filled.HomeWork, Icons.Outlined.HomeWork)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    localePreferences: LocalePreferences,
    themePreferences: ThemePreferences
) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onSplashComplete = { showSplash = false })
        return
    }

    val navController = rememberNavController()
    val viewModel: DentistViewModel = viewModel()
    var showLibraryDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dentistName by viewModel.dentistName.collectAsState()
    val dentistBio by viewModel.dentistBio.collectAsState()
    val photoPath by viewModel.dentistPhotoPath.collectAsState()

    val currentLanguage by localePreferences.languageFlow.collectAsState(initial = "en")

    val navItems = listOf(
        NavigationItem.Schedule,
        NavigationItem.Patients,
        NavigationItem.Analytics,
        NavigationItem.Clinics
    )

    if (showLibraryDialog) {
        ProceduresLibraryDialog(
            viewModel = viewModel,
            onDismiss = { showLibraryDialog = false }
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerRoutes = listOf("profile", "settings")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 280.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_profile)) },
                    selected = currentRoute == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        },
        gesturesEnabled = currentRoute !in drawerRoutes
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentRoute !in drawerRoutes) {
                    TopClinicalHeader(
                        dentistName = dentistName,
                        dentistBio = dentistBio,
                        photoPath = photoPath,
                        onOpenLibrary = { showLibraryDialog = true },
                        onOpenMenu = {
                            scope.launch { drawerState.open() }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentRoute !in drawerRoutes) {
                    NavigationBar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("app_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                        contentDescription = stringResource(item.titleResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.titleResId),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("nav_item_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = NavigationItem.Schedule.route,
                modifier = Modifier.padding(if (currentRoute in drawerRoutes) PaddingValues(0.dp) else innerPadding)
            ) {
                composable(
                    route = NavigationItem.Patients.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    PatientsScreen(viewModel = viewModel)
                }
                composable(
                    route = NavigationItem.Schedule.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    ScheduleScreen(viewModel = viewModel)
                }
                composable(
                    route = NavigationItem.Analytics.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    AnalyticsScreen(viewModel = viewModel)
                }
                composable(
                    route = NavigationItem.Clinics.route,
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    ClinicsScreen(viewModel = viewModel)
                }
                composable("profile") {
                    ProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    val activity = LocalContext.current as? Activity
                    val currentThemeMode by themePreferences.themeModeFlow.collectAsState(initial = ThemePreferences.DEFAULT_MODE)
                    SettingsScreen(
                        currentLanguage = currentLanguage,
                        onLanguageChange = { code ->
                            scope.launch {
                                localePreferences.setLanguage(code)
                                activity?.recreate()
                            }
                        },
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = { mode ->
                            scope.launch {
                                themePreferences.setThemeMode(mode)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun TopClinicalHeader(
    dentistName: String,
    dentistBio: String,
    photoPath: String,
    onOpenLibrary: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenMenu() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .testTag("top_bar_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            photoPath.isNotBlank() -> {
                                AsyncImage(
                                    model = File(photoPath),
                                    contentDescription = stringResource(R.string.cd_profile_photo),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dentistName.isNotBlank()) dentistName.take(1).uppercase() else "D",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.widthIn(max = 200.dp)) {
                        Text(
                            text = dentistName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (dentistBio.isNotBlank()) {
                            Text(
                                text = dentistBio,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onOpenLibrary,
                        modifier = Modifier.testTag("top_bar_library_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = stringResource(R.string.cd_templates_library),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.templates_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.cd_main_menu),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}
