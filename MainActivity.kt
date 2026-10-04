package com.anandpand3y.pdfmania
import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anandpand3y.pdfmania.tools.*
import kotlinx.coroutines.launch

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
@Composable fun App(){var dark by remember{mutableStateOf(false)};MaterialTheme(colorScheme=if(dark)darkColorScheme() else lightColorScheme()){Surface(Modifier.fillMaxSize()){var selected by remember{mutableStateOf<Tool?>(null)};if(selected==null)Home({dark=!dark}){selected=it}else ToolPage(selected!!){selected=null}}}}
@Composable fun Home(toggle:()->Unit,open:(Tool)->Unit){Scaffold(topBar={TopAppBar(title={Text("PDF MANIA")},actions={IconButton(toggle){Icon(Icons.Default.DarkMode,"Theme")}})}){pad->LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Offline PDF toolkit",style=MaterialTheme.typography.headlineSmall);Text("Tools run on your device.",color=MaterialTheme.colorScheme.onSurfaceVariant)};items(Tools.all){t->ElevatedCard(onClick={open(t)},modifier=Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)){Text(t.emoji,style=MaterialTheme.typography.headlineMedium);Column{Text(t.title,style=MaterialTheme.typography.titleMedium);Text(t.subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}}
@Composable fun ToolPage(tool:Tool,back:()->Unit){val ctx=LocalContext.current;var uris by remember{mutableStateOf<List<Uri>>(emptyList())};var result by remember{mutableStateOf<Result?>(null)};var busy by remember{mutableStateOf(false)};var text by remember{mutableStateOf("")};var pages by remember{mutableStateOf("all")};var angle by remember{mutableStateOf("90")};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){if(it.isNotEmpty())uris=it};Scaffold(topBar={TopAppBar(title={Text(tool.title)},navigationIcon={IconButton(back){Icon(Icons.Default.ArrowBack,"Back")}})}){p->Column(Modifier.padding(p).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(tool.subtitle);if(tool.id=="text"){OutlinedTextField(text,{text=it},Modifier.fillMaxWidth().height(180.dp),label={Text("Text")})};if(tool.id=="split")OutlinedTextField(pages,{pages=it},label={Text("Pages (e.g. 1-3,5 or all")});if(tool.id=="rotate")OutlinedTextField(angle,{angle=it},label={Text("Angle (90/180/270)")});Button(onClick={launcher.launch(arrayOf(if(tool.mime.isBlank())"text/plain" else tool.mime))}){Icon(Icons.Default.AttachFile,null);Spacer(Modifier.width(6.dp));Text(if(tool.multi)"Choose files" else "Choose file")};Text(if(uris.isEmpty())"No file selected" else "Selected: ${uris.size}");Button(enabled=!busy,onClick={scope.launch{busy=true;result=null;result=runCatching{tool.run(ctx,uris,mapOf("text" to text,"pages" to pages,"angle" to angle))}.getOrElse{Result("Error: ${it.message}")};busy=false}},modifier=Modifier.fillMaxWidth()){Text(if(busy)"Working…" else "Run tool")};result?.let{r->HorizontalDivider();Text(r.message,style=MaterialTheme.typography.titleMedium);r.text?.let{Text(it,Modifier.fillMaxWidth())}}}}}
