package dev.pam.shareextension
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.pam.nativeapp.modules.*
import dev.pam.nativeapp.protocol.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
class ShareExtensionModule(context:Context):NativeModule{
 private val activity=context as? Activity;private val root=File(context.filesDir,"pam-share-inbox").apply{mkdirs()}
 override fun invoke(method:String,payload:ByteArray,completion:ModuleCompletion){if(method!="drain"){completion.fail("Unknown method: $method");return};runCatching{drain()}.onSuccess{completion.ok(mapOf("json" to WireValue.Text(it.toString())))}.onFailure{completion.fail(it.message.orEmpty())}}
 @Suppress("DEPRECATION") private fun drain():JSONArray{val output=JSONArray();val intent=activity?.intent?:return output;if(intent.action!=Intent.ACTION_SEND&&intent.action!=Intent.ACTION_SEND_MULTIPLE)return output;val now=System.currentTimeMillis();intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf{it.isNotBlank()}?.let{output.put(row(if(runCatching{Uri.parse(it).scheme in listOf("http","https")}.getOrDefault(false))2 else 1,it,"text/plain",now))};val uris=linkedSetOf<Uri>();intent.clipData?.let{clip->for(i in 0 until clip.itemCount)clip.getItemAt(i).uri?.let(uris::add)};(intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let(uris::add);(intent.getParcelableArrayListExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>())?.let(uris::addAll);uris.take(32).forEach{uri->val mime=activity?.contentResolver?.getType(uri).orEmpty().ifEmpty{"application/octet-stream"};val file=File(root,UUID.randomUUID().toString());activity?.contentResolver?.openInputStream(uri).use{input->requireNotNull(input){"Cannot read shared URI."};file.outputStream().use{outputStream->input.copyTo(outputStream,bufferSize=64*1024)}};output.put(row(3,file.name,mime,now))};intent.action=null;intent.removeExtra(Intent.EXTRA_TEXT);intent.removeExtra(Intent.EXTRA_STREAM);return output}
 private fun row(kind:Int,value:String,mime:String,time:Long)=JSONObject().put("id",UUID.randomUUID().toString()).put("kind",kind).put("value",value.take(8192)).put("mimeType",mime.take(255)).put("createdAtMillis",time)
 private fun ModuleCompletion.ok(v:Map<String,WireValue>)=complete(ModuleResultStatus.SUCCESS,WireMap.encode(v));private fun ModuleCompletion.fail(m:String)=complete(ModuleResultStatus.FAILURE,m.take(1024).toByteArray())
}
