package com.virueditx.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.virueditx.app.R
import com.virueditx.app.engine.Converters
import com.virueditx.app.engine.ImageFormat
import com.virueditx.app.engine.ImagesToPdfOptions
import com.virueditx.app.engine.OutputResult
import com.virueditx.app.engine.PdfOrientation
import com.virueditx.app.engine.PdfPageSize
import com.virueditx.app.engine.VerifiedOutput
import com.virueditx.app.engine.ViruEngine
import com.virueditx.app.data.ConversionRecord
import com.virueditx.app.data.HistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================================================
//  VirueditX UI — design rules (from Virendra): CLEAN and USER-FRIENDLY.
//  Lots of space, one clear main action per screen, big labelled buttons,
//  simple everyday words, sensible defaults, plain-language messages.
// ==========================================================================

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

fun formatTime(millis: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(millis))

fun mimeOf(file: File): String = when (file.extension.lowercase()) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    "bmp" -> "image/bmp"
    "txt" -> "text/plain"
    else -> "*/*"
}

fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeOf(file)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
}

fun openFileExternally(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeOf(file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

/** Plain, friendly error text — never raw exception jargon. */
fun friendlyError(e: Exception): String {
    val msg = e.message ?: ""
    return when {
        msg.contains("password", ignoreCase = true) -> "This file is locked with a password, so it can't be opened. Try another file."
        msg.contains("corrupt", ignoreCase = true) || msg.contains("could not be read", ignoreCase = true) ->
            "This file seems damaged or isn't really the type it says. Try another file."
        msg.contains("decoded", ignoreCase = true) -> "This photo type can't be opened on this phone. Try a JPG or PNG photo."
        msg.isNotBlank() -> msg
        else -> "Something went wrong. Please try again."
    }
}

@Composable
fun rememberBitmapFromUri(uri: Uri?, maxDim: Int = 400): Bitmap? {
    val context = LocalContext.current
    val state = produceState<Bitmap?>(initialValue = null, uri, maxDim) {
        value = if (uri == null) null else try {
            Converters.decodeBitmap(context, uri, maxDim = maxDim)
        } catch (e: Exception) {
            null
        }
    }
    return state.value
}

@Composable
fun rememberBitmapFromFile(file: File?, maxDim: Int = 400): Bitmap? {
    val state = produceState<Bitmap?>(initialValue = null, file?.absolutePath, maxDim) {
        value = if (file == null || !file.exists()) null else withContext(Dispatchers.IO) {
            try {
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
                var sample = 1
                while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) sample *= 2
                android.graphics.BitmapFactory.decodeFile(
                    file.absolutePath,
                    android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    return state.value
}

// ---------------------------------------------------------------- tools list

data class ToolDef(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
)

val TOOLS = listOf(
    ToolDef("tool/img2pdf", "Image to PDF", "Photos into one PDF", Icons.Filled.PictureAsPdf, Color(0xFFEEF2FF)),
    ToolDef("tool/pdf2img", "PDF to Images", "Each page as a photo", Icons.Filled.Image, Color(0xFFFDF2F8)),
    ToolDef("tool/convert", "Image Converter", "JPG, PNG, WEBP, BMP", Icons.Filled.Transform, Color(0xFFECFDF5)),
    ToolDef("tool/compress", "PDF Compressor", "Make a PDF smaller", Icons.Filled.Compress, Color(0xFFFFFBEB)),
    ToolDef("tool/merge", "Merge PDFs", "Join PDFs into one", Icons.Filled.MergeType, Color(0xFFEFF6FF)),
    ToolDef("tool/split", "Split PDF", "Take out some pages", Icons.Filled.CallSplit, Color(0xFFF5F3FF)),
    ToolDef("tool/scan", "Scan to PDF", "Use your camera", Icons.Filled.CameraAlt, Color(0xFFFEF2F2)),
    ToolDef("tool/doc", "Document Converter", "Text files and more", Icons.Filled.Description, Color(0xFFF0FDFA))
)

data class SuccessInfo(
    val heading: String,
    val fileName: String,
    val sizeBytes: Long,
    val pages: Int?,
    val filePath: String,
    val note: String = ""
)

// ---------------------------------------------------------------- app shell

@Composable
fun VirueditXApp(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val mainTabs = listOf("home", "tools", "files", "settings")

    // Images picked on Home travel into the Image-to-PDF studio.
    val sharedImages = remember { mutableStateListOf<Uri>() }
    var success by remember { mutableStateOf<SuccessInfo?>(null) }

    fun showSuccess(info: SuccessInfo) {
        success = info
        nav.navigate("success")
    }

    Scaffold(
        bottomBar = {
            if (route in mainTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "home",
                        onClick = { nav.navigate("home") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = route == "tools",
                        onClick = { nav.navigate("tools") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Apps, contentDescription = "Tools") },
                        label = { Text("Tools") }
                    )
                    NavigationBarItem(
                        selected = route == "files",
                        onClick = { nav.navigate("files") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Folder, contentDescription = "Files") },
                        label = { Text("Files") }
                    )
                    NavigationBarItem(
                        selected = route == "settings",
                        onClick = { nav.navigate("settings") { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    nav = nav,
                    sharedImages = sharedImages
                )
            }
            composable("tools") { ToolsScreen(nav) }
            composable("files") { FilesScreen(nav) }
            composable("settings") {
                SettingsScreen(themeMode = themeMode, onThemeModeChange = onThemeModeChange, nav = nav)
            }
            composable("tool/img2pdf") {
                ImageToPdfStudio(nav = nav, initialImages = sharedImages.toList(), autoCamera = false) { showSuccess(it) }
            }
            composable("tool/scan") {
                ImageToPdfStudio(nav = nav, initialImages = emptyList(), autoCamera = true) { showSuccess(it) }
            }
            composable("tool/pdf2img") { PdfToImagesScreen(nav) { showSuccess(it) } }
            composable("tool/convert") { ImageConvertScreen(nav) { showSuccess(it) } }
            composable("tool/compress") { CompressScreen(nav) { showSuccess(it) } }
            composable("tool/merge") { MergeScreen(nav) { showSuccess(it) } }
            composable("tool/split") { SplitScreen(nav) { showSuccess(it) } }
            composable("tool/doc") { DocumentScreen(nav) }
            composable("tool/txt2pdf") { TxtToPdfScreen(nav) { showSuccess(it) } }
            composable("success") {
                val s = success
                if (s == null) {
                    LaunchedEffect(Unit) { nav.navigate("home") { launchSingleTop = true } }
                } else {
                    SuccessScreen(
                        info = s,
                        onDone = { nav.navigate("home") { launchSingleTop = true } },
                        onConvertMore = { nav.navigate("tools") { launchSingleTop = true } }
                    )
                }
            }
            composable(
                route = "preview?path={path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType })
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                PreviewScreen(nav = nav, file = File(path))
            }
        }
    }
}

// ---------------------------------------------------------------- shared bits

@Composable
fun BigButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StepText(step: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Text(
                text = step.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(title: String, nav: NavHostController, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = content
    )
}

@Composable
fun WorkingRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorText(text: String) {
    Surface(
        color = Color(0xFFFDECEA),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text,
            color = Color(0xFF7A1E17),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun ToolCard(tool: ToolDef, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = tool.tint) {
                Icon(
                    tool.icon,
                    contentDescription = tool.title,
                    tint = Color(0xFF3730A3),
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(tool.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                tool.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ToolsGrid(nav: NavHostController, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        itemsIndexed(TOOLS, key = { _, t -> t.route }) { _, tool ->
            ToolCard(tool) { nav.navigate(tool.route) }
        }
    }
}

// ---------------------------------------------------------------- home

@Composable
fun HomeScreen(nav: NavHostController, sharedImages: androidx.compose.runtime.snapshots.SnapshotStateList<Uri>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recents by remember { mutableStateOf<List<File>>(emptyList()) }
    LaunchedEffect(Unit) {
        recents = withContext(Dispatchers.IO) {
            Converters.outputsDir(context).listFiles()?.sortedByDescending { it.lastModified() }?.take(5) ?: emptyList()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            sharedImages.clear()
            sharedImages.addAll(uris)
            nav.navigate("tool/img2pdf")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        Icons.Filled.PictureAsPdf, contentDescription = "VirueditX",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("VirueditX", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = "Your profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { nav.navigate("settings") }
                )
            }
        }
        item {
            Column {
                Text("Your files,\nbeautifully transformed.", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Convert images, create PDFs, and manage your documents — all in one seamless workspace.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                BigButton(text = "Select Files") {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Text(
                    "Pick photos to make a PDF — or choose a tool below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            Text("Tools", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            ToolsGrid(nav = nav, modifier = Modifier.height(512.dp))
        }
        item {
            Text("Recent Files", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (recents.isEmpty()) {
                Text(
                    "Nothing here yet. Convert something and it will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recents.forEach { file -> RecentRow(file, nav) }
                }
            }
        }
    }
}

@Composable
fun RecentRow(file: File, nav: NavHostController) {
    val context = LocalContext.current
    Card(
        onClick = {
            val ext = file.extension.lowercase()
            if (ext == "pdf" || ext in listOf("jpg", "jpeg", "png", "webp", "bmp")) {
                nav.navigate("preview?path=" + Uri.encode(file.absolutePath))
            } else {
                openFileExternally(context, file)
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                if (file.extension.equals("pdf", true)) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                contentDescription = file.name,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${file.extension.uppercase()} · ${formatSize(file.length())} · ${formatTime(file.lastModified())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ToolsScreen(nav: NavHostController) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Tools", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick what you want to do.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        ToolsGrid(nav = nav, modifier = Modifier.fillMaxSize())
    }
}

// ---------------------------------------------------------------- image -> PDF studio

private data class StudioItem(val uri: Uri, val rotation: Int = 0, val selected: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfStudio(
    nav: NavHostController,
    initialImages: List<Uri>,
    autoCamera: Boolean,
    onSuccess: (SuccessInfo) -> Unit
) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val items = remember { mutableStateListOf<StudioItem>() }
    var outputName by remember { mutableStateOf("My PDF") }
    var pageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var orientation by remember { mutableStateOf(PdfOrientation.AUTO) }
    var margin by remember { mutableStateOf(36f) }
    var quality by remember { mutableStateOf(90f) }
    var showSettings by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    fun addUris(uris: List<Uri>) {
        uris.forEach { u ->
            if (items.none { it.uri == u }) items.add(StudioItem(u))
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris -> addUris(uris) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = captureFile
        val u = captureUri
        if (ok && f != null && u != null && f.exists() && f.length() > 0) {
            addUris(listOf(u))
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val f = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val u = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        captureFile = f
        captureUri = u
        camera.launch(u)
    }

    LaunchedEffect(Unit) {
        if (items.isEmpty() && initialImages.isNotEmpty()) addUris(initialImages)
        if (autoCamera && items.isEmpty()) launchCamera()
    }

    val selectedItems = items.filter { it.selected }
    val selectedBytes by produceState(0L, selectedItems.map { it.uri.toString() }.joinToString()) {
        value = withContext(Dispatchers.IO) {
            selectedItems.sumOf { item ->
                try {
                    context.contentResolver.openAssetFileDescriptor(item.uri, "r")?.use { it.length } ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        }
    }

    ToolScaffold(title = "Image to PDF", nav = nav) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            StepText(1, "Add your photos")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Add photos")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Photos")
                }
                OutlinedButton(onClick = { launchCamera() }) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Take a photo")
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }

            if (items.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "No photos yet. Tap “Add Photos” to begin.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${selectedItems.size} of ${items.size} photos will be in the PDF",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val all = items.all { it.selected }
                        for (i in items.indices) items[i] = items[i].copy(selected = !all)
                    }) { Text(if (items.all { it.selected }) "Select none" else "Select all") }
                }
                if (selectedBytes > 0) {
                    Text(
                        "Photos size: ${formatSize(selectedBytes)} · PDF will be about the same",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(items, key = { _, item -> item.uri.toString() }) { index, item ->
                        StudioThumb(
                            item = item,
                            onToggle = { items[index] = item.copy(selected = !item.selected) },
                            onRotate = { items[index] = item.copy(rotation = (item.rotation + 90) % 360) },
                            onRemove = { items.removeAt(index) },
                            onMove = { dir ->
                                val target = index + dir
                                if (target in items.indices) {
                                    val tmp = items[target]
                                    items[target] = item
                                    items[index] = tmp
                                }
                            },
                            onDragSwap = { dir ->
                                val target = index + dir
                                if (target in items.indices) {
                                    val tmp = items[target]
                                    items[target] = item
                                    items[index] = tmp
                                }
                            }
                        )
                    }
                }
            }

            StepText(2, "Name your PDF")
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { showSettings = true }) { Text("More settings (page size, quality) — optional") }

            if (vm.errorText != null) ErrorText(vm.errorText!!)
            if (vm.working) {
                WorkingRow(vm.progressText.ifBlank { "Making your PDF…" })
                TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
            } else {
                StepText(3, "Make it")
                BigButton(text = "Create PDF", enabled = selectedItems.isNotEmpty()) {
                    val opts = ImagesToPdfOptions(
                        pageSize = pageSize,
                        orientation = orientation,
                        marginPt = margin.toInt(),
                        quality = quality.toInt(),
                        outputName = outputName
                    )
                    val uris = selectedItems.map { it.uri }
                    val rotations = selectedItems.map { it.rotation }
                    vm.run(
                        block = { report ->
                            com.virueditx.app.engine.ViruEngine.imagesToPdf(context, uris, rotations, opts) { d, t ->
                                report("Making page $d of $t…")
                            }
                        },
                        onSuccess = { v ->
                            HistoryStore.add(
                                context,
                                ConversionRecord(
                                    id = v.file.absolutePath + v.sizeBytes,
                                    tool = "Image to PDF",
                                    inputName = "${uris.size} photos",
                                    outputName = v.name,
                                    outputPath = v.file.absolutePath,
                                    success = true,
                                    message = "",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            onSuccess(SuccessInfo("Your PDF is ready", v.name, v.sizeBytes, v.pageCount, v.file.absolutePath))
                        }
                    )
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("PDF settings", style = MaterialTheme.typography.titleLarge)
                Text("Page size", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfPageSize.values().forEach { size ->
                        FilterChip(
                            selected = pageSize == size,
                            onClick = { pageSize = size },
                            label = { Text(size.label) }
                        )
                    }
                }
                Text("Direction", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfOrientation.values().forEach { o ->
                        FilterChip(
                            selected = orientation == o,
                            onClick = { orientation = o },
                            label = { Text(o.label) }
                        )
                    }
                }
                Text("Margin: ${margin.toInt()} pt", style = MaterialTheme.typography.titleMedium)
                Slider(value = margin, onValueChange = { margin = it }, valueRange = 0f..72f)
                Text("Quality: ${quality.toInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 40f..100f)
                BigButton(text = "Done") { showSettings = false }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StudioThumb(
    item: StudioItem,
    onToggle: () -> Unit,
    onRotate: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    onDragSwap: (Int) -> Unit
) {
    val bmp = rememberBitmapFromUri(item.uri, maxDim = 300)
    var dragTotal by remember { mutableStateOf(0f) }
    var dragOffset by remember { mutableStateOf(0f) }
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset }
            .pointerInput(item.uri) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragTotal += amount.y
                        dragOffset = dragTotal.coerceIn(-140f, 140f)
                        if (dragTotal > 130f) {
                            onDragSwap(3) // next row in a 3-column grid
                            dragTotal = 0f
                            dragOffset = 0f
                        } else if (dragTotal < -130f) {
                            onDragSwap(-3)
                            dragTotal = 0f
                            dragOffset = 0f
                        }
                    }
                )
            }
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = item.rotation.toFloat() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Image, contentDescription = "Photo") }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                ) {
                    Checkbox(checked = item.selected, onCheckedChange = { onToggle() }, modifier = Modifier.size(30.dp))
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(30.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove photo", modifier = Modifier.size(18.dp))
                }
                if (!item.selected) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onMove(-1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Move earlier", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRotate, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.RotateRight, contentDescription = "Turn photo", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onMove(1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Move later", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- single-file tools

private fun recordHistory(context: Context, tool: String, inputName: String, v: VerifiedOutput) {
    HistoryStore.add(
        context,
        ConversionRecord(
            id = v.file.absolutePath + v.sizeBytes,
            tool = tool,
            inputName = inputName,
            outputName = v.name,
            outputPath = v.file.absolutePath,
            success = true,
            message = "",
            timestamp = System.currentTimeMillis()
        )
    )
}

@Composable
private fun PickedFileCard(name: String, detail: String = "") {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Icon(Icons.Filled.Description, contentDescription = "Selected file", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (detail.isNotBlank()) {
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImagesScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf<Int?>(null) }
    var format by remember { mutableStateOf(ImageFormat.PNG) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pdfUri = uri
            pdfName = Converters.displayName(context, uri)
            pageCount = null
        }
    }
    LaunchedEffect(pdfUri) {
        val u = pdfUri ?: return@LaunchedEffect
        when (val r = ViruEngine.pdfPageCount(context, u)) {
            is com.virueditx.app.engine.EngineResult.Success -> pageCount = r.value
            is com.virueditx.app.engine.EngineResult.Failure -> pageCount = null
        }
    }

    ToolScaffold(title = "PDF to Images", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick your PDF")
            BigButton(text = "Select PDF") { picker.launch(arrayOf("application/pdf")) }
            if (pdfUri != null) {
                Spacer(Modifier.height(12.dp))
                PickedFileCard(pdfName, if (pageCount != null) "$pageCount pages" else "")
                Spacer(Modifier.height(16.dp))
                StepText(2, "Choose photo type")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ImageFormat.JPG, ImageFormat.PNG, ImageFormat.WEBP).forEach { f ->
                        FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                StepText(3, "Make the photos")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Working…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = "Convert") {
                        val u = pdfUri!!
                        vm.run(
                            block = { report ->
                                ViruEngine.pdfToImages(context, u, format) { d, t -> report("Page $d of $t…") }
                            },
                            onSuccess = { list ->
                                val first = list.first()
                                recordHistory(context, "PDF to Images", pdfName, first)
                                onSuccess(
                                    SuccessInfo(
                                        heading = "Your photos are ready",
                                        fileName = first.name,
                                        sizeBytes = list.sumOf { it.sizeBytes },
                                        pages = list.size,
                                        filePath = first.file.absolutePath,
                                        note = if (list.size > 1) "${list.size} photos saved — find them all in Files." else ""
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageConvertScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageName by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(ImageFormat.JPG) }
    var under1mb by remember { mutableStateOf(false) }
    val preview = rememberBitmapFromUri(imageUri, maxDim = 700)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            imageName = Converters.displayName(context, uri)
        }
    }

    ToolScaffold(title = "Image Converter", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick your photo")
            BigButton(text = "Select Photo") { picker.launch("image/*") }
            if (imageUri != null) {
                Spacer(Modifier.height(12.dp))
                PickedFileCard(imageName, "HEIC photos work on most newer phones.")
                if (preview != null) {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Your photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(Modifier.height(16.dp))
                StepText(2, "Choose what you need")
                // Prominent "Under 1 MB" target for forms and college submissions.
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (under1mb) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { under1mb = !under1mb }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Checkbox(checked = under1mb, onCheckedChange = { under1mb = it })
                        Column {
                            Text("Make it under 1 MB", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Best for forms and college uploads. Saved as JPG.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (!under1mb) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageFormat.values().forEach { f ->
                            FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) })
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                StepText(3, "Make it")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Working…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = if (under1mb) "Make it under 1 MB" else "Convert") {
                        val u = imageUri!!
                        if (under1mb) {
                            vm.run(
                                block = { ViruEngine.compressImageUnder1Mb(context, u) },
                                onSuccess = { v ->
                                    recordHistory(context, "Image under 1 MB", imageName, v)
                                    onSuccess(
                                        SuccessInfo(
                                            heading = "Your photo is ready",
                                            fileName = v.name,
                                            sizeBytes = v.sizeBytes,
                                            pages = null,
                                            filePath = v.file.absolutePath,
                                            note = if (v.note == "UNDER_1MB") "✓ Under 1 MB"
                                            else "Smallest possible: ${formatSize(v.sizeBytes)} — could not reach 1 MB"
                                        )
                                    )
                                }
                            )
                        } else {
                            vm.run(
                                block = { ViruEngine.convertImage(context, u, format) },
                                onSuccess = { v ->
                                    recordHistory(context, "Image Converter", imageName, v)
                                    onSuccess(SuccessInfo("Your photo is ready", v.name, v.sizeBytes, null, v.file.absolutePath))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pdfSize by remember { mutableStateOf(0L) }
    var under1mb by remember { mutableStateOf(true) } // the common need comes first
    var outputName by remember { mutableStateOf("Smaller PDF") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pdfUri = uri
            pdfName = Converters.displayName(context, uri)
            outputName = pdfName.substringBeforeLast('.').ifBlank { "Smaller PDF" } + " small"
            pdfSize = try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    ToolScaffold(title = "PDF Compressor", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick your PDF")
            BigButton(text = "Select PDF") { picker.launch(arrayOf("application/pdf")) }
            if (pdfUri != null) {
                Spacer(Modifier.height(12.dp))
                PickedFileCard(pdfName, if (pdfSize > 0) "Now: ${formatSize(pdfSize)}" else "")
                Spacer(Modifier.height(16.dp))
                StepText(2, "How small?")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (under1mb) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { under1mb = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        RadioButton(selected = under1mb, onClick = { under1mb = true })
                        Column {
                            Text("Make it under 1 MB", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Best for forms and college uploads.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().clickable { under1mb = false }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        RadioButton(selected = !under1mb, onClick = { under1mb = false })
                        Column {
                            Text("Just make it smaller", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Keeps quality as high as possible.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                StepText(3, "Make it")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Making it smaller…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = if (under1mb) "Make it under 1 MB" else "Compress PDF") {
                        val u = pdfUri!!
                        if (under1mb) {
                            vm.run(
                                block = { ViruEngine.compressPdfUnder1Mb(context, u, outputName) },
                                onSuccess = { v ->
                                    recordHistory(context, "PDF under 1 MB", pdfName, v)
                                    onSuccess(
                                        SuccessInfo(
                                            heading = "Your smaller PDF is ready",
                                            fileName = v.name,
                                            sizeBytes = v.sizeBytes,
                                            pages = v.pageCount,
                                            filePath = v.file.absolutePath,
                                            note = if (v.note == "UNDER_1MB") "✓ Under 1 MB"
                                            else "Smallest possible: ${formatSize(v.sizeBytes)} — could not reach 1 MB"
                                        )
                                    )
                                }
                            )
                        } else {
                            vm.run(
                                block = { ViruEngine.compressPdf(context, u, outputName) },
                                onSuccess = { v ->
                                    recordHistory(context, "PDF Compressor", pdfName, v)
                                    onSuccess(
                                        SuccessInfo(
                                            heading = "Your smaller PDF is ready",
                                            fileName = v.name,
                                            sizeBytes = v.sizeBytes,
                                            pages = v.pageCount,
                                            filePath = v.file.absolutePath,
                                            note = if (pdfSize > 0) "Was ${formatSize(pdfSize)}, now ${formatSize(v.sizeBytes)}" else ""
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MergeScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val pdfs = remember { mutableStateListOf<Uri>() }
    var outputName by remember { mutableStateOf("Merged") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { if (!pdfs.contains(it)) pdfs.add(it) }
    }

    ToolScaffold(title = "Merge PDFs", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick two or more PDFs")
            BigButton(text = "Select PDFs") { picker.launch(arrayOf("application/pdf")) }
            Spacer(Modifier.height(12.dp))
            if (pdfs.isEmpty()) {
                Text(
                    "Your PDFs will join in the order shown here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(pdfs.size, key = { pdfs[it].toString() }) { index ->
                        val uri = pdfs[index]
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                                Text("${index + 1}.", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    Converters.displayName(context, uri),
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    if (index > 0) {
                                        val t = pdfs[index - 1]; pdfs[index - 1] = uri; pdfs[index] = t
                                    }
                                }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                                IconButton(onClick = {
                                    if (index < pdfs.lastIndex) {
                                        val t = pdfs[index + 1]; pdfs[index + 1] = uri; pdfs[index] = t
                                    }
                                }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                                IconButton(onClick = { pdfs.removeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                StepText(2, "Name the joined PDF")
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                StepText(3, "Make it")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Joining your PDFs…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = "Merge PDFs", enabled = pdfs.size >= 2) {
                        vm.run(
                            block = { ViruEngine.mergePdfs(context, pdfs.toList(), outputName) },
                            onSuccess = { v ->
                                recordHistory(context, "Merge PDFs", "${pdfs.size} PDFs", v)
                                onSuccess(SuccessInfo("Your PDF is ready", v.name, v.sizeBytes, v.pageCount, v.file.absolutePath))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplitScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf<Int?>(null) }
    var ranges by remember { mutableStateOf("1") }
    var outputName by remember { mutableStateOf("My Pages") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pdfUri = uri
            pdfName = Converters.displayName(context, uri)
            pageCount = null
        }
    }
    LaunchedEffect(pdfUri) {
        val u = pdfUri ?: return@LaunchedEffect
        when (val r = ViruEngine.pdfPageCount(context, u)) {
            is com.virueditx.app.engine.EngineResult.Success -> pageCount = r.value
            is com.virueditx.app.engine.EngineResult.Failure -> pageCount = null
        }
    }

    ToolScaffold(title = "Split PDF", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick your PDF")
            BigButton(text = "Select PDF") { picker.launch(arrayOf("application/pdf")) }
            if (pdfUri != null) {
                Spacer(Modifier.height(12.dp))
                PickedFileCard(pdfName, if (pageCount != null) "$pageCount pages in this PDF" else "")
                Spacer(Modifier.height(16.dp))
                StepText(2, "Which pages do you want?")
                OutlinedTextField(
                    value = ranges,
                    onValueChange = { ranges = it },
                    label = { Text("Pages, like 1,3-5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Example: 1,3-5 takes pages 1, 3, 4 and 5 into a new PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = outputName,
                    onValueChange = { outputName = it },
                    label = { Text("New file name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                StepText(3, "Make it")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Taking out your pages…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = "Make New PDF") {
                        val u = pdfUri!!
                        vm.run(
                            block = { ViruEngine.extractPages(context, u, ranges, outputName) },
                            onSuccess = { v ->
                                recordHistory(context, "Split PDF", pdfName, v)
                                onSuccess(SuccessInfo("Your PDF is ready", v.name, v.sizeBytes, v.pageCount, v.file.absolutePath))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TxtToPdfScreen(nav: NavHostController, onSuccess: (SuccessInfo) -> Unit) {
    val context = LocalContext.current
    val vm: ToolViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var txtUri by remember { mutableStateOf<Uri?>(null) }
    var txtName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("My Text") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            txtUri = uri
            txtName = Converters.displayName(context, uri)
            outputName = txtName.substringBeforeLast('.').ifBlank { "My Text" }
        }
    }

    ToolScaffold(title = "Text to PDF", nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            StepText(1, "Pick your text file")
            BigButton(text = "Select Text File") { picker.launch(arrayOf("text/plain")) }
            if (txtUri != null) {
                Spacer(Modifier.height(12.dp))
                PickedFileCard(txtName)
                Spacer(Modifier.height(16.dp))
                StepText(2, "Make it")
                if (vm.errorText != null) ErrorText(vm.errorText!!)
                if (vm.working) {
                    WorkingRow(vm.progressText.ifBlank { "Making your PDF…" })
                    TextButton(onClick = { vm.cancel() }) { Text("Cancel") }
                } else {
                    BigButton(text = "Create PDF") {
                        val u = txtUri!!
                        vm.run(
                            block = { ViruEngine.txtToPdf(context, u, outputName) },
                            onSuccess = { v ->
                                recordHistory(context, "Text to PDF", txtName, v)
                                onSuccess(SuccessInfo("Your PDF is ready", v.name, v.sizeBytes, v.pageCount, v.file.absolutePath))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentScreen(nav: NavHostController) {
    ToolScaffold(title = "Document Converter", nav = nav) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("What do you want to make?", style = MaterialTheme.typography.titleLarge)
            Card(
                onClick = { nav.navigate("tool/txt2pdf") },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.TextSnippet, contentDescription = "Text to PDF", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Text file to PDF", style = MaterialTheme.typography.titleMedium)
                        Text("Works fully on your phone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Open")
                }
            }
            NotAvailableCard(
                title = "Word (DOCX) to PDF",
                text = "Word files need a big computer program to turn into PDFs. That can't be done properly on a phone yet, so VirueditX won't pretend to do it."
            )
            NotAvailableCard(
                title = "PDF to Word (DOCX)",
                text = "Turning a PDF back into an editable Word file also needs a computer program. Not available on-device yet."
            )
        }
    }
}

@Composable
private fun NotAvailableCard(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFF3CD)) {
                    Text(
                        "Not available yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A5B00),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- success

@Composable
fun SuccessScreen(info: SuccessInfo, onDone: () -> Unit, onConvertMore: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scale by remember { mutableStateOf(0.6f) }
    LaunchedEffect(Unit) { scale = 1f }
    val file = File(info.filePath)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        Surface(shape = CircleShape, color = Color(0xFFE7F7EC)) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Done",
                tint = Color(0xFF1E9E50),
                modifier = Modifier.padding(18.dp).size(52.dp).graphicsLayer {
                    scaleX = scale; scaleY = scale
                }
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(info.heading, style = MaterialTheme.typography.headlineMedium)
        Text(
            "Checked and saved on your phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Size: ${formatSize(info.sizeBytes)}", style = MaterialTheme.typography.bodyLarge)
                if (info.pages != null) {
                    Text("Pages: ${info.pages}", style = MaterialTheme.typography.bodyLarge)
                }
                if (info.note.isNotBlank()) {
                    Text(
                        info.note,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (info.note.startsWith("✓")) Color(0xFF1E9E50) else Color(0xFFB45309)
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        BigButton(text = "Open File") { if (file.exists()) openFileExternally(context, file) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { if (file.exists()) shareFile(context, file) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Share, contentDescription = "Share")
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        if (file.exists()) {
                            val copy = Converters.uniqueFile(
                                Converters.outputsDir(context),
                                info.fileName.substringBeforeLast('.') + " copy",
                                info.fileName.substringAfterLast('.')
                            )
                            file.copyTo(copy)
                        }
                    } catch (e: Exception) {
                        // best-effort extra copy
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Save Another Copy") }
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = onConvertMore) { Text("Convert More Files") }
            TextButton(onClick = onDone) { Text("Done") }
        }
    }
}

// ---------------------------------------------------------------- files

@Composable
fun FilesScreen(nav: NavHostController) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("Newest") }
    var refresh by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(refresh) {
        files = withContext(Dispatchers.IO) {
            Converters.outputsDir(context).listFiles()?.filter { it.isFile } ?: emptyList()
        }
    }

    val shown = remember(files, query, filter, sort) {
        var list = files.filter { f ->
            (query.isBlank() || f.name.contains(query, ignoreCase = true)) &&
                when (filter) {
                    "PDF" -> f.extension.equals("pdf", true)
                    "Photos" -> f.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "bmp")
                    "Text" -> f.extension.equals("txt", true)
                    else -> true
                }
        }
        list = when (sort) {
            "Name" -> list.sortedBy { it.name.lowercase() }
            "Size" -> list.sortedByDescending { it.length() }
            else -> list.sortedByDescending { it.lastModified() }
        }
        list
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Files", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Everything you made with VirueditX lives here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search by name") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "PDF", "Photos", "Text").forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sort:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("Newest", "Name", "Size").forEach { s ->
                FilterChip(selected = sort == s, onClick = { sort = s }, label = { Text(s) })
            }
        }
        Spacer(Modifier.height(12.dp))
        if (shown.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (files.isEmpty()) "No files yet." else "No files match that search.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Make your first PDF or photo — it takes a few seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                BigButton(text = "Make Something") { nav.navigate("tools") }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(shown, key = { it.absolutePath }) { file ->
                    Card(
                        onClick = {
                            val ext = file.extension.lowercase()
                            if (ext == "pdf" || ext in listOf("jpg", "jpeg", "png", "webp", "bmp")) {
                                nav.navigate("preview?path=" + Uri.encode(file.absolutePath))
                            } else {
                                openFileExternally(context, file)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            Icon(
                                if (file.extension.equals("pdf", true)) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                                contentDescription = file.name,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${formatSize(file.length())} · ${formatTime(file.lastModified())}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { shareFile(context, file) }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share ${file.name}")
                            }
                            IconButton(onClick = {
                                renameTarget = file
                                renameText = file.nameWithoutExtension
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Rename ${file.name}")
                            }
                            IconButton(onClick = { deleteTarget = file }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${file.name}")
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename file") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameText.trim()
                    if (clean.isNotBlank()) {
                        val dest = File(target.parentFile, "$clean.${target.extension}")
                        if (!dest.exists()) {
                            target.renameTo(dest)
                        }
                    }
                    renameTarget = null
                    refresh++
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this file?") },
            text = { Text("\"${target.name}\" will be deleted from VirueditX. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    target.delete()
                    deleteTarget = null
                    refresh++
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Keep") }
            }
        )
    }
}

// ---------------------------------------------------------------- settings + history + about

@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, nav: NavHostController) {
    val context = LocalContext.current
    var history by remember { mutableStateOf<List<ConversionRecord>>(emptyList()) }
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) { HistoryStore.load(context) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Column {
                Text("Look of the app", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                listOf(
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                    ThemeMode.SYSTEM to "Use phone setting"
                ).forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeModeChange(mode) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = { onThemeModeChange(mode) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = "History")
                    Spacer(Modifier.width(8.dp))
                    Text("History", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("Clear") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (history.isEmpty()) {
                    Text(
                        "Nothing converted yet — your recent work will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.take(30).forEach { record ->
                            val outFile = File(record.outputPath)
                            val exists = outFile.exists()
                            Card(
                                onClick = {
                                    if (exists) {
                                        val ext = outFile.extension.lowercase()
                                        if (ext == "pdf" || ext in listOf("jpg", "jpeg", "png", "webp", "bmp")) {
                                            nav.navigate("preview?path=" + Uri.encode(outFile.absolutePath))
                                        } else {
                                            openFileExternally(context, outFile)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("${record.tool}: ${record.outputName}", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        if (record.success) {
                                            if (exists) formatTime(record.timestamp) else "This file was moved or deleted."
                                        } else {
                                            "Didn't finish — ${record.message}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = "Viru — the face of VirueditX",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(84.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("VirueditX", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Create. Convert. Simplify.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A strong on-device engine (PDFBox + Android PDF tools) does all the work. " +
                            "Your files never leave your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Version 1.0.0 · Works on Android 7.0 – 15", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("The list of past conversions will be cleared. Your files stay as they are.") },
            confirmButton = {
                TextButton(onClick = {
                    HistoryStore.clear(context)
                    history = emptyList()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Keep") }
            }
        )
    }
}

// ---------------------------------------------------------------- preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(nav: NavHostController, file: File) {
    val context = LocalContext.current
    val ext = file.extension.lowercase()
    ToolScaffold(title = file.name, nav = nav) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            if (!file.exists()) {
                Text(
                    "This file was moved or deleted, so it can't be shown.",
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Column
            }
            when {
                ext == "pdf" -> PdfPreview(file)
                ext in listOf("jpg", "jpeg", "png", "webp", "bmp") -> {
                    val bmp = rememberBitmapFromFile(file, maxDim = 1600)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text("This photo can't be shown here.", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${formatSize(file.length())} · ${formatTime(file.lastModified())}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text(
                        "No preview for this type of file.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { openFileExternally(context, file) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Open")
                }
                Button(onClick = { shareFile(context, file) }, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun PdfPreview(file: File) {
    val context = LocalContext.current
    var pageCount by remember { mutableStateOf<Int?>(null) }
    var page by remember { mutableStateOf(0) }
    var zoom by remember { mutableStateOf(1f) }

    LaunchedEffect(file.absolutePath) {
        pageCount = withContext(Dispatchers.IO) {
            var fd: android.os.ParcelFileDescriptor? = null
            var renderer: android.graphics.pdf.PdfRenderer? = null
            try {
                fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = android.graphics.pdf.PdfRenderer(fd)
                renderer.pageCount
            } catch (e: Exception) {
                null
            } finally {
                try { renderer?.close() } catch (e: Exception) {}
                try { fd?.close() } catch (e: Exception) {}
            }
        }
    }

    val bmpState = produceState<Bitmap?>(initialValue = null, file.absolutePath, page) {
        value = when (val r = ViruEngine.renderPdfPage(context, file, page)) {
            is com.virueditx.app.engine.EngineResult.Success -> r.value
            is com.virueditx.app.engine.EngineResult.Failure -> null
        }
    }
    val bmp = bmpState.value

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 200.dp, max = 460.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom }
                )
            } else {
                Text("Loading page…", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { if (page > 0) page-- }, enabled = page > 0) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous page")
            }
            Text(
                if (pageCount != null) "Page ${page + 1} of $pageCount" else "Page ${page + 1}",
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = { if (pageCount == null || page < (pageCount!! - 1)) page++ }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next page")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = zoom == 1f, onClick = { zoom = 1f }, label = { Text("Fit") })
            FilterChip(selected = zoom == 1.5f, onClick = { zoom = 1.5f }, label = { Text("Bigger") })
            FilterChip(selected = zoom == 2f, onClick = { zoom = 2f }, label = { Text("Biggest") })
        }
    }
}
