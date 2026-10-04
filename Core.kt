package com.anandpand3y.pdfmania.tools
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.anandpand3y.pdfmania.util.OutputSaver
import com.tom_roush.pdfbox.pdmodel.*
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.pdf.PdfRenderer
import java.io.*

data class Tool(val id:String,val title:String,val emoji:String,val subtitle:String,val category:String,val mime:String="application/pdf",val multi:Boolean=false,val run:suspend(Context,List<Uri>,Map<String,String>)->Result)
data class Result(val message:String,val text:String?=null,val uri:Uri?=null)
object PDF {
 fun input(c:Context,u:Uri)=c.contentResolver.openInputStream(u)?:error("Cannot open file")
 fun name(c:Context,u:Uri):String{var n=u.lastPathSegment? :"file"; c.contentResolver.query(u,null,null,null,null)?.use{x->val i=x.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&x.moveToFirst())n=x.getString(i)?:n};return n}
 fun pages(s:String,total:Int):List<Int>{if(s.trim().isEmpty()||s.equals("all",true))return(1..total).toList();val r=LinkedHashSet<Int>();s.split(',').forEach{p->if(p.contains('-')){val a=p.split('-',limit=2);val x=a[0].trim().toIntOrNull();val y=a.getOrNull(1)?.trim()?.toIntOrNull();if(x!=null&&y!=null)for(i in minOf(x,y)..maxOf(x,y))if(i in 1..total)r.add(i)}else p.trim().toIntOrNull()?.let{if(it in 1..total)r.add(it)}};return r.toList()}
 fun textPdf(c:Context,text:String,size:Float,out:OutputStream){val d=PDDocument();val font=PDType1Font.HELVETICA;var page=PDPage(PDRectangle.A4);d.addPage(page);var cs=PDPageContentStream(d,page);var y=760f;val lines=text.replace("\r","").split('\n').flatMap{wrap(it,font,size,480f)};for(line in lines){if(y<50){cs.close();page=PDPage(PDRectangle.A4);d.addPage(page);cs=PDPageContentStream(d,page);y=760f};cs.beginText();cs.setFont(font,size);cs.newLineAtOffset(56f,y);cs.showText(line.map{if(it.code in 32..255)it else '?'} .joinToString(""));cs.endText();y-=size*1.45f};cs.close();d.save(out);d.close()}
 private fun wrap(s:String,f:PDType1Font,size:Float,max:Float):List<String>{if(s.isEmpty())return listOf("");val out=mutableListOf<String>();var cur="";for(w in s.split(Regex("\\s+"))){val t=if(cur.isEmpty())w else "$cur $w";if(f.getStringWidth(t)/1000f*size<=max)cur=t else{if(cur.isNotEmpty())out.add(cur);cur=w}};if(cur.isNotEmpty())out.add(cur);return out}
 fun merge(c:Context,us:List<Uri>,out:OutputStream){val d=PDDocument();us.forEach{val s=PDDocument.load(input(c,it));for(p in s.pages)d.importPage(p);s.close()};d.save(out);d.close()}
 fun extract(c:Context,u:Uri,spec:String,out:OutputStream){val s=PDDocument.load(input(c,u));val d=PDDocument();pages(spec,s.numberOfPages).forEach{d.importPage(s.getPage(it-1))};d.save(out);d.close();s.close()}
 fun rotate(c:Context,u:Uri,a:Int,out:OutputStream){val d=PDDocument.load(input(c,u));for(p in d.pages)p.rotation=(p.rotation+a)%360;d.save(out);d.close()}
 fun info(c:Context,u:Uri):String{val d=PDDocument.load(input(c,u));val x=d.documentInformation;val t=runCatching{PDFTextStripper().getText(d)}.getOrDefault("");return "Pages: ${d.numberOfPages}\nPDF version: ${d.version}\nEncrypted: ${d.isEncrypted}\nTitle: ${x.title?:"—"}\nAuthor: ${x.author?:"—"}\n\nText preview:\n${t.take(1200)}".also{d.close()}}
 fun toImages(c:Context,u:Uri,outPrefix:String){val f=File.createTempFile("p",".pdf",c.cacheDir);input(c,u).use{f.outputStream().use{x->it.copyTo(x)}};val p=ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);val r=PdfRenderer(p);for(i in 0 until r.pageCount){val pg=r.openPage(i);val b=Bitmap.createBitmap(pg.width*2,pg.height*2,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);pg.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);OutputSaver.savePng(c,"${outPrefix}_page${i+1}.png"){b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle();pg.close()};r.close();p.close();f.delete()}
}
object Tools{
 val all=listOf(
 Tool("text","Text → PDF","📄","Create a PDF from text","Create", "",false){c,_,o->withContext(Dispatchers.IO){val t=o["text"].orEmpty();if(t.isBlank())error("Enter text first");val n="PDF_MANIA_text.pdf";val u=OutputSaver.savePdf(c,n){PDF.textPdf(c,t,11f,it)};Result("Created $n",uri=u)}},
 Tool("images","Images → PDF","🖼️","Combine images into one PDF","Create","image/*",true){c,us,o->withContext(Dispatchers.IO){val d=PDDocument();us.forEach{val b=BitmapFactory.decodeStream(PDF.input(c,it))?:error("Invalid image");val page=PDPage(PDRectangle.A4);d.addPage(page);val baos=ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,90,baos);val img=PDImageXObject.createFromByteArray(d,baos.toByteArray(),"img");val sc=minOf(500f/b.width,700f/b.height);PDPageContentStream(d,page).use{x->x.drawImage(img,56f,70f,b.width*sc,b.height*sc)};b.recycle()};val u=OutputSaver.savePdf(c,"PDF_MANIA_images.pdf"){d.save(it)};d.close();Result("Created image PDF",uri=u)}},
 Tool("merge","Merge PDF","🧩","Join multiple PDFs","Organize",multi=true){c,us,_->withContext(Dispatchers.IO){if(us.size<2)error("Select at least two PDFs");val u=OutputSaver.savePdf(c,"PDF_MANIA_merged.pdf"){PDF.merge(c,us,it)};Result("Merged ${us.size} PDFs",uri=u)}},
 Tool("split","Split PDF","✂️","Extract selected pages","Organize"){c,us,o->withContext(Dispatchers.IO){val u=OutputSaver.savePdf(c,"PDF_MANIA_split.pdf"){PDF.extract(c,us[0],o["pages"].orEmpty().ifBlank{"all"},it)};Result("Pages extracted",uri=u)}},
 Tool("rotate","Rotate PDF","🔄","Rotate all pages","Edit"){c,us,o->withContext(Dispatchers.IO){val a=o["angle"]?.toIntOrNull()?:90;val u=OutputSaver.savePdf(c,"PDF_MANIA_rotated.pdf"){PDF.rotate(c,us[0],a,it)};Result("Rotated PDF",uri=u)}},
 Tool("images_out","PDF → Images","📸","Export pages as PNG","Convert"){c,us,_->withContext(Dispatchers.IO){PDF.toImages(c,us[0],"PDF_MANIA");Result("Pages exported to Pictures/PDF MANIA")}},
 Tool("text_out","Extract Text","🔤","Extract the PDF text layer","Convert"){c,us,_->withContext(Dispatchers.IO){val d=PDDocument.load(PDF.input(c,us[0]));val t=PDFTextStripper().getText(d);d.close();Result("Extracted text",t)}},
 Tool("info","PDF Information","ℹ️","View pages and metadata","Read"){c,us,_->withContext(Dispatchers.IO){Result("PDF information",PDF.info(c,us[0]))}}
 )
}
