package com.anandpand3y.pdfmania.util
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import java.io.OutputStream
object OutputSaver{
 fun savePdf(c:Context,n:String,w:(OutputStream)->Unit):Uri=save(c,n,"application/pdf",Environment.DIRECTORY_DOWNLOADS,w)
 fun savePng(c:Context,n:String,w:(OutputStream)->Unit):Uri=save(c,n,"image/png",Environment.DIRECTORY_PICTURES,w)
 private fun save(c:Context,n:String,m:String,d:String,w:(OutputStream)->Unit):Uri{if(Build.VERSION.SDK_INT>=29){val v=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,n);put(MediaStore.MediaColumns.MIME_TYPE,m);put(MediaStore.MediaColumns.RELATIVE_PATH,"$d/PDF MANIA");put(MediaStore.MediaColumns.IS_PENDING,1)};val col=if(d==Environment.DIRECTORY_DOWNLOADS)MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI;val u=c.contentResolver.insert(col,v)?:error("Cannot create output");c.contentResolver.openOutputStream(u)?.use(w)?:error("Cannot open output");v.clear();v.put(MediaStore.MediaColumns.IS_PENDING,0);c.contentResolver.update(u,v,null,null);return u};val f=java.io.File(c.getExternalFilesDir(null),"PDF MANIA").apply{mkdirs()};val x=java.io.File(f,n);x.outputStream().use(w);return Uri.fromFile(x)}
}
