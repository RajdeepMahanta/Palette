package com.rajdeep.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround: ImageView = findViewById(R.id.iv_background)
                imageBackGround.setImageURI(result.data?.data)
            }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()

                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setBrushSize(8.toFloat())

        val linearLayoutColors = findViewById<LinearLayout>(R.id.ll_colors)

        mImageButtonCurrentPaint = linearLayoutColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palette_selected)
        )

//        val ib_save : ImageButton = findViewById(R.id.ib_save)
//        ib_save.setOnClickListener {
//            val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
//            saveBitmap(flDrawingView)
//        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener{
            showProgressDialog()
            val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
            saveBitmap(flDrawingView)
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.onUndoClick()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onRedoClick()
        }

        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeDialog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

    }

    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setBrushSize(5.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setBrushSize(8.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setBrushSize(12.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view:View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palette_selected)
            )
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palette_normal)
            )
            mImageButtonCurrentPaint = view
        }
    }

    private fun isReadStorageAllowed() : Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

//    private fun requestStoragePermission(){
//        if(ActivityCompat.shouldShowRequestPermissionRationale(
//            this,
//            Manifest.permission.READ_EXTERNAL_STORAGE)
//            ){
//            showRationaleDialog("Palette", "Palette needs" + "needs to access your external storage")
//        }else{
//            requestPermission.launch(
//                arrayOf(
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                )
//            )
//        }
//    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission already granted
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            } else {
                // Permission not granted, request it
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                Toast.makeText(this, "Please grant permission to access external storage.", Toast.LENGTH_LONG).show()
            }
        } else {
            // Android 10 or lower, request the permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                showRationaleDialog("Drawing App", "Drawing App needs to Access your External Storage")
            } else {
                requestPermission.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "Palette_"
                            + System.currentTimeMillis() / 1000
                            + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully: $result", Toast.LENGTH_SHORT).show()
                            val ibShare: ImageButton = findViewById(R.id.ib_share)
                            ibShare.setOnClickListener {
                                shareImage(result)
                            }
                        }
                        else{
                            Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()

                        }
                    }
                }catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun saveBitmap(view : View) {
        lifecycleScope.launch {
            val bitmap = getBitmapFromView(view) // replace with your view

            // save bitmap to gallery based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = applicationContext.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "Palette_${System.currentTimeMillis() / 1000}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                }

                var imageUri: Uri? = null
                resolver.run {
                    cancelProgressDialog()
                    imageUri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    imageUri?.let {
                        openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                        }
                    }
                }

                if (imageUri != null) {
                    Toast.makeText(this@MainActivity, "File saved successfully: $imageUri", Toast.LENGTH_SHORT).show()
                    val ibShare: ImageButton = findViewById(R.id.ib_share)
                    ibShare.setOnClickListener {
                        shareImage(imageUri.toString())
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                }

            } else {
                val filePath = saveBitmapFile(bitmap)
                if (filePath.isNotEmpty()) {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val file = File(filePath)
                    val contentUri = Uri.fromFile(file)
                    mediaScanIntent.data = contentUri
                    sendBroadcast(mediaScanIntent)
                    Toast.makeText(this@MainActivity, "File saved successfully: $filePath", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showRationaleDialog(title:String,message:String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

}