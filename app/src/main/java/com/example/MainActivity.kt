package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firestore.AttendanceRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.QrStatus
import com.example.data.model.Student
import com.example.data.model.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.CredentialAuthHelper
import com.example.ui.components.ManualAttendanceSheet
import com.example.ui.components.StudentFormDialog
import com.example.ui.components.StudentQrCardSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.theme.AbsenTarunaTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  DASHBOARD("dashboard", R.string.nav_dashboard, Icons.Filled.Home, Icons.Outlined.Home),
  SCAN("scan", R.string.nav_scan, Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
  STUDENTS("students", R.string.nav_students, Icons.Filled.Groups, Icons.Outlined.Groups),
  SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AbsenTarunaTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
  } else {
    key(user.uid) {
      MainAppScaffold(user = user)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val uid = user.uid

  val viewModel: AttendanceViewModel =
    viewModel(
      key = uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            AttendanceViewModel(
              repository = AttendanceRepository(db),
              currentUserId = uid,
              displayName = user.displayName,
            )
          }
        },
    )

  val studentsState by viewModel.studentsState.collectAsStateWithLifecycle()
  val attendanceState by viewModel.attendanceState.collectAsStateWithLifecycle()
  val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
  val settings by viewModel.settingsState.collectAsStateWithLifecycle()
  val scanMode by viewModel.scanMode.collectAsStateWithLifecycle()
  val scanBanner by viewModel.scanBanner.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
  val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(actionMessage) {
    val msg = actionMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  // Modal states
  var showStudentForm by remember { mutableStateOf(false) }
  var editingStudent by remember { mutableStateOf<Student?>(null) }
  var activeQrStudentId by remember { mutableStateOf<String?>(null) }
  var manualAttendanceTarget by remember { mutableStateOf<Pair<Student, AttendanceRecord?>?>(null) }

  val studentsList = (studentsState as? UiState.Success)?.data ?: emptyList()
  val attendanceList = (attendanceState as? UiState.Success)?.data ?: emptyList()

  // Keep active QR sheet synced with live Firestore updates (e.g. right after issuing QR!)
  val activeQrStudent =
    remember(activeQrStudentId, studentsList) {
      activeQrStudentId?.let { id -> studentsList.firstOrNull { it.id == id } }
    }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: AppDestination.DASHBOARD.route

  val navigateToTab: (AppDestination) -> Unit = { dest ->
    navController.navigate(dest.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Text(
              text = stringResource(id = R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
            )
          },
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.primary,
            ),
        )
      },
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            AppDestination.entries.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(id = dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(dest) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${dest.route}"),
              )
            }
          }
        }
      },
      containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            AppDestination.entries.forEach { dest ->
              val selected = currentRoute == dest.route
              val label = stringResource(id = dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(dest) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("rail_${dest.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = AppDestination.DASHBOARD.route,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(AppDestination.DASHBOARD.route) {
            DashboardScreen(
              studentsState = studentsState,
              attendanceState = attendanceState,
              selectedDateKey = selectedDateKey,
              settings = settings,
              onSelectDate = viewModel::setSelectedDate,
              onNavigateToScanner = { navigateToTab(AppDestination.SCAN) },
              onNavigateToStudents = { navigateToTab(AppDestination.STUDENTS) },
              onOpenStudentQr = { student -> activeQrStudentId = student.id },
              onOpenManualAttendance = { student, rec -> manualAttendanceTarget = student to rec },
            )
          }
          composable(AppDestination.SCAN.route) {
            ScannerScreen(
              scanMode = scanMode,
              scanBanner = scanBanner,
              isMutating = isMutating,
              onSelectScanMode = viewModel::setScanMode,
              onQrScanned = viewModel::processScannedQr,
              onClearBanner = viewModel::clearScanBanner,
            )
          }
          composable(AppDestination.STUDENTS.route) {
            StudentsScreen(
              studentsState = studentsState,
              isMutating = isMutating,
              onAddStudent = {
                editingStudent = null
                showStudentForm = true
              },
              onEditStudent = { student ->
                editingStudent = student
                showStudentForm = true
              },
              onDeleteStudent = viewModel::deleteStudent,
              onIssueOrOpenQr = { student ->
                if (student.qrStatusEnum != QrStatus.ACTIVE) {
                  viewModel.issueOrReissueQr(student) {
                    activeQrStudentId = student.id
                  }
                } else {
                  activeQrStudentId = student.id
                }
              },
              onBatchIssuePendingQr = viewModel::batchIssuePendingQr,
              onOpenManualAttendance = { student ->
                val rec = attendanceList.firstOrNull { it.studentId == student.id }
                manualAttendanceTarget = student to rec
              },
            )
          }
          composable(AppDestination.SETTINGS.route) {
            SettingsScreen(
              userEmail = user.email,
              userDisplayName = user.displayName,
              settings = settings,
              isMutating = isMutating,
              onSaveSettings = viewModel::updateSettings,
              onSignOut = {
                CredentialAuthHelper.signOut(credentialManager, scope)
              },
            )
          }
        }
      }
    }

    if (showStudentForm) {
      StudentFormDialog(
        initialStudent = editingStudent,
        isMutating = isMutating,
        onDismiss = {
          showStudentForm = false
          editingStudent = null
        },
        onConfirm = { nis, fullName, className, gender, issueQr ->
          val existing = editingStudent
          if (existing == null) {
            viewModel.addStudent(nis, fullName, className, gender, issueQr) { created ->
              if (issueQr) {
                activeQrStudentId = created.id
              }
            }
          } else {
            viewModel.updateStudent(existing.id, nis, fullName, className, gender)
          }
          showStudentForm = false
          editingStudent = null
        },
      )
    }

    if (activeQrStudent != null) {
      StudentQrCardSheet(
        student = activeQrStudent,
        isMutating = isMutating,
        onDismiss = { activeQrStudentId = null },
        onIssueOrReissueQr = { student -> viewModel.issueOrReissueQr(student) },
        onRevokeQr = viewModel::revokeQr,
      )
    }

    if (manualAttendanceTarget != null) {
      val (targetStudent, existingRec) = manualAttendanceTarget!!
      ManualAttendanceSheet(
        student = targetStudent,
        currentStatus = existingRec?.statusEnum,
        currentNote = existingRec?.note ?: "",
        isMutating = isMutating,
        onDismiss = { manualAttendanceTarget = null },
        onSave = { status, note ->
          viewModel.setManualAttendance(targetStudent, status, note)
          manualAttendanceTarget = null
        },
      )
    }
  }
}
