package com.shekarmudaliyar.social_share

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.content.FileProvider
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.widget.ShareDialog

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class SocialSharePlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var activeContext: Context? = null
    private var context: Context? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "social_share")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        activeContext = if (activity != null) activity!!.applicationContext else context!!
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "shareInstagramStory") {
            //share on instagram story
            val stickerImage: String? = call.argument("stickerImage")
            val backgroundImage: String? = call.argument("backgroundImage")

            val backgroundTopColor: String? = call.argument("backgroundTopColor")
            val backgroundBottomColor: String? = call.argument("backgroundBottomColor")
            val attributionURL: String? = call.argument("attributionURL")

            val file = File(activeContext!!.cacheDir, stickerImage!!)
            val stickerImageFile = FileProvider.getUriForFile(
                activeContext!!,
                activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share",
                file
            )

            val intent = Intent("com.instagram.share.ADD_TO_STORY")
            intent.type = "*/*"
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("interactive_asset_uri", stickerImageFile)
            if (backgroundImage != null) {
                //check if background image is also provided
                val backfile = File(activeContext!!.cacheDir, backgroundImage)
                val backgroundImageFile = FileProvider.getUriForFile(
                    activeContext!!,
                    activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share",
                    backfile
                )
                intent.setDataAndType(backgroundImageFile, "*/*")
            }
            intent.putExtra("content_url", attributionURL)
            intent.putExtra("top_background_color", backgroundTopColor)
            intent.putExtra("bottom_background_color", backgroundBottomColor)
            Log.d("", activity!!.toString())
            // Instantiate activity and verify it will resolve implicit intent
            activity!!.grantUriPermission(
                "com.instagram.android",
                stickerImageFile,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            if (activity!!.packageManager.resolveActivity(intent, 0) != null) {
                activeContext!!.startActivity(intent)
                result.success("success")
            } else {
                result.success("error")
            }
        } else if (call.method == "shareFacebookStory") {
            //share on facebook story
            val stickerImage: String? = call.argument("stickerImage")
            val backgroundTopColor: String? = call.argument("backgroundTopColor")
            val backgroundBottomColor: String? = call.argument("backgroundBottomColor")
            val attributionURL: String? = call.argument("attributionURL")
            val appId: String? = call.argument("appId")

            val file = File(activeContext!!.cacheDir, stickerImage)
            val stickerImageFile = FileProvider.getUriForFile(
                activeContext!!,
                activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share",
                file
            )
            val intent = Intent("com.facebook.stories.ADD_TO_STORY")
            intent.type = "*/*"
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("com.facebook.platform.extra.APPLICATION_ID", appId)
            intent.putExtra("interactive_asset_uri", stickerImageFile)
            intent.putExtra("content_url", attributionURL)
            intent.putExtra("top_background_color", backgroundTopColor)
            intent.putExtra("bottom_background_color", backgroundBottomColor)
            Log.d("", activity!!.toString())
            // Instantiate activity and verify it will resolve implicit intent
            activity!!.grantUriPermission(
                "com.facebook.katana",
                stickerImageFile,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            if (activity!!.packageManager.resolveActivity(intent, 0) != null) {
                activeContext!!.startActivity(intent)
                result.success("success")
            } else {
                result.success("error")
            }
        } else if (call.method == "shareOptions") {
            //native share options
            val content: String? = call.argument("content")
            val image: String? = call.argument("image")
            val intent = Intent()
            intent.action = Intent.ACTION_SEND

            if (image != null) {
                //check if  image is also provided
                val imagefile = File(activeContext!!.cacheDir, image)
                val imageFileUri = FileProvider.getUriForFile(
                    activeContext!!,
                    activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share",
                    imagefile
                )
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_STREAM, imageFileUri)
            } else {
                intent.type = "text/plain";
            }

            intent.putExtra(Intent.EXTRA_TEXT, content)

            //create chooser intent to launch intent
            //source: "share" package by flutter (https://github.com/flutter/plugins/blob/master/packages/share/)
            val chooserIntent: Intent =
                Intent.createChooser(intent, null /* dialog title optional */)
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            activeContext!!.startActivity(chooserIntent)
            result.success(true)

        } else if (call.method == "copyToClipboard") {
            //copies content onto the clipboard
            val content: String? = call.argument("content")
            val clipboard =
                context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("", content)
            clipboard.setPrimaryClip(clip)
            result.success(true)
        } else if (call.method == "shareWhatsapp") {
            //shares content on WhatsApp
            val content: String? = call.argument("content")
            val whatsappIntent = Intent(Intent.ACTION_SEND)
            whatsappIntent.type = "text/plain"
            whatsappIntent.setPackage("com.whatsapp")
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, content)
            try {
                activity!!.startActivity(whatsappIntent)
                result.success("true")
            } catch (ex: ActivityNotFoundException) {
                result.success("false")
            }
        } else if (call.method == "shareSms") {
            //shares content on sms
            val content: String? = call.argument("message")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.type = "vnd.android-dir/mms-sms"
            intent.data = Uri.parse("sms:")
            intent.putExtra("sms_body", content)
            try {
                activity!!.startActivity(intent)
                result.success("true")
            } catch (ex: ActivityNotFoundException) {
                result.success("false")
            }
        } else if (call.method == "shareTwitter") {
            //shares content on twitter
            val text: String? = call.argument("captionText")
            val url: String? = call.argument("url")
            val trailingText: String? = call.argument("trailingText")
            val urlScheme = "http://www.twitter.com/intent/tweet?text=$text$url$trailingText"
            Log.d("log", urlScheme)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(urlScheme)
            try {
                activity!!.startActivity(intent)
                result.success("true")
            } catch (ex: ActivityNotFoundException) {
                result.success("false")
            }
        } else if (call.method == "shareTelegram") {
            //shares content on Telegram
            val content: String? = call.argument("content")
            val telegramIntent = Intent(Intent.ACTION_SEND)
            telegramIntent.type = "text/plain"
            telegramIntent.setPackage("org.telegram.messenger")
            telegramIntent.putExtra(Intent.EXTRA_TEXT, content)
            try {
                activity!!.startActivity(telegramIntent)
                result.success("true")
            } catch (ex: ActivityNotFoundException) {
                result.success("false")
            }
        } else if (call.method == "checkInstalledApps") {
            //check if the apps exists
            //creating a mutable map of apps
            val apps: MutableMap<String, Boolean> = mutableMapOf()
            //assigning package manager
            val pm: PackageManager = context!!.packageManager
            //get a list of installed apps.
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            //intent to check sms app exists
            val intent = Intent(Intent.ACTION_SENDTO).addCategory(Intent.CATEGORY_DEFAULT)
            intent.type = "vnd.android-dir/mms-sms"
            intent.data = Uri.parse("sms:")
            val resolvedActivities: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            //if sms app exists
            apps["sms"] = resolvedActivities.isNotEmpty()
            //if other app exists
            apps["instagram"] =
                packages.any { it.packageName.toString().contentEquals("com.instagram.android") }
            apps["facebook"] =
                packages.any { it.packageName.toString().contentEquals("com.facebook.katana") }
            apps["twitter"] =
                packages.any { it.packageName.toString().contentEquals("com.twitter.android") }
            apps["whatsapp"] =
                packages.any { it.packageName.toString().contentEquals("com.whatsapp") }
            apps["telegram"] =
                packages.any { it.packageName.toString().contentEquals("org.telegram.messenger") }

            result.success(apps)
        } else if (call.method == "getFacebookUser") {
            getFacebookUser(result)
        }   else if (call.method == "shareOnFeedFacebook") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val message: String? = args["message"] as? String?
            shareOnFeedFacebook(url, message, result)
        } else if (call.method == "shareStoryOnInstagram") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            shareStoryOnInstagram(url, result)
        } else if (call.method == "shareStoryOnFacebook") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val facebookId = args["facebookId"] as String
            shareStoryOnFacebook(url, facebookId, result)
        } else if (call.method == "shareOnFeedInstagram") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val message: String? = args["message"] as? String?
            shareOnFeedInstagram(url, message, result)
        } else if (call.method == "shareOnWhatsApp") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val message: String? = args["message"] as? String?
            shareOnWhatsApp(url, message, result, false)
        } else if (call.method == "shareOnWhatsAppBusiness") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val message: String? = args["message"] as? String?
            shareOnWhatsApp(url, message, result, true)
        } else if (call.method == "openAppOnStore") {
            val args = call.arguments as Map<*, *>
            val appUrl: String? = args["appUrl"] as? String?
            openAppOnStore(appUrl)
        } else if (call.method == "shareOnNative") {
            val args = call.arguments as Map<*, *>
            val url: String? = args["url"] as? String?
            val message: String? = args["message"] as? String?
            shareOnNative(url, message, result)
        } else if (call.method == "shareLinkOnWhatsApp") {
            val args = call.arguments as Map<*, *>
            val link: String? = args["link"] as? String?
            shareLinkOnWhatsApp(link, result, false)
        } else if (call.method == "shareLinkOnWhatsAppBusiness") {
            val args = call.arguments as Map<*, *>
            val link: String? = args["link"] as? String?
            shareLinkOnWhatsApp(link, result, true)
        } else if (call.method == "shareOnGallery") {
            val image = call.argument<ByteArray>("imageBytes") ?: return
            shareOnGallery(BitmapFactory.decodeByteArray(image, 0, image.size))
        } else if (call.method == "checkPermissionToPublish") {
            checkPermissionToPublish(result)
        } else {
            result.notImplemented()
        }
    }

    private fun getFacebookUser(result: Result) {
        if (AccessToken.getCurrentAccessToken() != null) {
            val parameters = Bundle()
            parameters.putString("fields", "id,name")
            GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me",
                parameters,
                HttpMethod.GET,
                  GraphRequest.Callback() {
                    fun onCompleted(response: GraphResponse) {
                        if (response.jsonObject != null) {
                            val obj = response.jsonObject
                            val map = HashMap<String, String>()
                            if (obj != null) {
                                map["id"] = obj.optString("id")
                            }
                            if (obj != null) {
                                map["name"] = obj.optString("name")
                            }
                            result.success(map)
                        } else {
                            result.error("FAIL_TO_GET_FB_USER", "Response is null", "FACEBOOK_APP")
                        }
                    }
                }
            )   .executeAsync()
        }
    }


    private fun shareOnFeedFacebook(url: String?, message: String?, result: Result) {
        try {
            val imgFile = File(url!!)
            if (imgFile.exists()) {
//                val activity = activity.get()!!
                val bitmapUri =
                    FileProvider.getUriForFile(activeContext!!,
                        activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)
                val content = if (bitmapUri != null) {
                    val photo = SharePhoto.Builder().setCaption("$message #Postou").setImageUrl(bitmapUri).build()
                    SharePhotoContent.Builder().addPhoto(photo).build()
                } else {
                    ShareLinkContent.Builder().setQuote(message).build()
                }
                val shareDialog = ShareDialog(activity)
                if (ShareDialog.canShow(SharePhotoContent::class.java)) {
                    shareDialog.show(content)
                    result.success("POST_SENT")
                } else result.error("APP_NOT_FOUND", "Facebook app not found", "FACEBOOK_APP")
            } else {
                result.error("FAIL_TO_POST", "$url not found", "FACEBOOK_APP")
            }
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "FACEBOOK_APP")
        }
    }

    private fun shareStoryOnInstagram(url: String?, result: Result) {
        try {
            if (isInstalled("com.instagram.android")) {
                val imgFile = File(url!!)
                if (imgFile.exists()) {
//                    val activity = activity.get()!!
                    val bitmapUri =
                        FileProvider.getUriForFile(activeContext!!,
                            activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)
                    val storiesIntent = Intent("com.instagram.share.ADD_TO_STORY")
                    storiesIntent.setDataAndType(bitmapUri,
                        activity?.contentResolver?.getType(bitmapUri))
                    storiesIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    activity?.startActivity(storiesIntent)
                    result.success("POST_SENT")
                } else {
                    result.error("FAIL_TO_POST", "$url not found", "INSTAGRAM_STORY_APP")
                }
            } else {
                result.error("APP_NOT_FOUND", "Instagram app not found", "INSTAGRAM_STORY_APP")
            }
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "INSTAGRAM_STORY_APP")
        }
    }

    private fun shareStoryOnFacebook(url: String?, facebookId: String, result: Result){

        try {
            if (isInstalled("com.facebook.katana")) {
                val imgFile = File(url!!)

                if (imgFile.exists()) {


                    val bitmapUri =
                        FileProvider.getUriForFile(activeContext!!,
                            activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)

                    val intent = Intent("com.facebook.stories.ADD_TO_STORY")
                    intent.type = "*/*"
                    intent.putExtra("com.facebook.platform.extra.APPLICATION_ID", facebookId)
                    intent.putExtra("interactive_asset_uri", bitmapUri)
                    intent.putExtra("top_background_color", "#26242e")
                    intent.putExtra("bottom_background_color", "#26242e")

                    activeContext!!.grantUriPermission(
                        "com.facebook.katana", bitmapUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (activeContext!!.packageManager.resolveActivity(intent, 0) != null) {
                        activity?.startActivityForResult(intent, 0)
                    }
                    result.success("POST_SENT")
                }  else {
                    result.error("FAIL_TO_POST", "$url not found", "FACEBOOK_POST_APP")
                }
            } else {
                result.error("APP_NOT_FOUND", "App do Facebook não encontrado", "FACEBOOK_POST_APP")
            }
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "FACEBOOK_POST_APP")
        }
    }

    private fun shareOnFeedInstagram(url: String?, msg: String?, result: Result) {
        try {
            if (isInstalled("com.instagram.android")) {
                val imgFile = File(url!!)
                if (imgFile.exists()) {
                     val bitmapUri =
                        FileProvider.getUriForFile(activeContext!!,
                            activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)
                    val feedIntent = Intent(Intent.ACTION_SEND)
                    feedIntent.type = "*/*"
                    feedIntent.putExtra(Intent.EXTRA_TEXT, msg)
                    feedIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    feedIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
                    feedIntent.setPackage("com.instagram.android")
                    activity?.startActivity(feedIntent)
                    result.success("POST_SENT")
                } else {
                    result.error("FAIL_TO_POST", "$url not found", "INSTAGRAM_POST_APP")
                }
            } else {
                result.error("APP_NOT_FOUND", "App do Instagram não encontrado", "INSTAGRAM_POST_APP")
            }
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "INSTAGRAM_POST_APP")
        }
    }

    private fun shareOnWhatsApp(url: String?, msg: String?, result: Result,
                                shareToWhatsAppBiz: Boolean) {
        val app = if (shareToWhatsAppBiz) "com.whatsapp.w4b" else "com.whatsapp"
        try {
            if (isInstalled(app)) {
                val whatsappIntent = Intent(Intent.ACTION_SEND)
                whatsappIntent.setPackage(app)
                whatsappIntent.putExtra(Intent.EXTRA_TEXT, msg)
                val imgFile = File(url!!)
                if (imgFile.exists()) {

                    val bitmapUri =
                        FileProvider.getUriForFile(activeContext!!,
                            activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)
                    whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    whatsappIntent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
                    val cr = activity?.contentResolver
                    whatsappIntent.type = cr?.getType(bitmapUri)
                    activity?.startActivity(whatsappIntent)
                    result.success("POST_SENT")
                } else {
                    result.error("FAIL_TO_POST", "$url not found", app)
                }
            } else {
                result.error("APP_NOT_FOUND", "App do WhatsApp não foi encontrado", app)
            }
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), app)
        }
    }

    private fun shareLinkOnWhatsApp(link: String?, result: Result, shareToWhatsAppBiz: Boolean) {
        val app = if (shareToWhatsAppBiz) "com.whatsapp.w4b" else "com.whatsapp"
        if (isInstalled(app)) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.setPackage(app)
            intent.putExtra(Intent.EXTRA_TEXT, link)
            activeContext!!.startActivity(intent)
        } else {
            result.error("APP_NOT_FOUND", "App do WhatsApp não foi encontrado", app)
        }
    }

    private fun shareOnNative(url: String?, msg: String?, result: Result) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, msg)
            if (url != null) {
                val imgFile = File(url)
                if (imgFile.exists()) {
                    val bitmapUri =
                        FileProvider.getUriForFile(activeContext!!,
                            activeContext!!.applicationContext.packageName + ".com.shekarmudaliyar.social_share", imgFile)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
                    val cr = activity?.contentResolver
                    intent.type = cr?.getType(bitmapUri)
                } else {
                    result.error("FAIL_TO_POST", "$url not found", "NATIVE")
                }
                activity?.startActivity(Intent.createChooser(intent, "Enviar post..."))
            } else {
                intent.type = "text/plain"
                activity?.startActivity(Intent.createChooser(intent, "Enviar mensagem..."))
            }


            result.success("POST_SENT")

        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "NATIVE")
        }
    }

    private fun checkPermissionToPublish(result: Result) {
        result.success(AccessToken.getCurrentAccessToken() != null)
    }

    private fun shareLinkOnFacebook(link: String?, result: Result) {
        try {
            val content = ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(link))
                .build()
            val shareDialog = ShareDialog(activity!!)
            if (ShareDialog.canShow(ShareLinkContent::class.java)) {
                shareDialog.show(content)
                result.success("POST_SENT")
            } else result.error("APP_NOT_FOUND", "App do Facebook não foi encontrado", "FACEBOOK_APP")
        } catch (e: Exception) {
            result.error("FAIL_TO_POST", e.toString(), "FACEBOOK_APP")
        }
    }

    private fun openAppOnStore(packageName: String?) {

        try {
            val playStoreUri = Uri.parse("market://details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, playStoreUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activeContext!!.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val playStoreUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            val intent = Intent(Intent.ACTION_VIEW, playStoreUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activeContext!!.startActivity(intent)
        }
    }

    private fun isInstalled(packageName: String): Boolean {
        val packageManager = activeContext!!.packageManager
        return try {
            packageManager.getApplicationInfo(packageName, 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun shareOnGallery(bmp: Bitmap): Uri? {



        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/test_pictures")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "img_${SystemClock.uptimeMillis()}")

            val uri: Uri? =
                activeContext!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                saveImageToStream(bmp, activeContext!!.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                activeContext!!.contentResolver.update(uri, values, null, null)
                return uri
            }
        } else {
            val directory =
                File(activeContext!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() + File.separator + "MediaSocialShare")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName =  "img_${SystemClock.uptimeMillis()}"+ ".jpeg"
            val file = File(directory, fileName)
            saveImageToStream(bmp, FileOutputStream(file))
            val values = ContentValues()
            values.put(MediaStore.Images.Media._ID, file.absolutePath)
            // .DATA is deprecated in API 29
            activeContext!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            return Uri.fromFile(file)
        }
        return null
    }

    fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }




    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
//        activity = WeakReference(binding.activity)
        activity = binding.getActivity()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}