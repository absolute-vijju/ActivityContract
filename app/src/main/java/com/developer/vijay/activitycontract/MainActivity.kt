package com.developer.vijay.activitycontract

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.developer.vijay.activitycontract.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var resultUri: Uri? = null
    private lateinit var mBinding: ActivityMainBinding
    private var isRecordingVideo = false
    private lateinit var permissionContract: ActivityResultLauncher<Array<String>>
    private lateinit var recordVideoContract: ActivityResultLauncher<Uri>
    private lateinit var takePhotoContract: ActivityResultLauncher<Uri>
    private lateinit var pickVideoContract: ActivityResultLauncher<String>
    private lateinit var pickImageContract: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        init()

        mBinding.btnCaptureImage.setOnClickListener {
            isRecordingVideo = false
            showDialog()
        }

        mBinding.btnRecordVideo.setOnClickListener {
            isRecordingVideo = true
            showDialog()
        }

    }

    private fun init() {    // Register contract in OnCreate()
        recordVideoContract =
            registerForActivityResult(ActivityResultContracts.TakeVideo()) { bmp ->
                resultUri?.let {
                    loadVideo(it)
                }
            }

        takePhotoContract =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { status ->
                if (status) {
                    Toast.makeText(this, "Image saved successfully.", Toast.LENGTH_SHORT).show()
                    resultUri?.let {
                        loadImage(it)
                    }
                } else
                    Toast.makeText(this, "Error occurred.", Toast.LENGTH_SHORT).show()
            }

        permissionContract =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultMap ->
                resultMap.entries.forEach { entry ->
                    if (entry.value) {
                        if (isRecordingVideo) {
                            createVideoURI()?.let { uri ->
                                recordVideoContract(uri)
                            }
                        } else {
                            createImageURI()?.let { uri ->
                                captureImageContract(uri)
                            }
                        }

                    }
                }
            }

        pickVideoContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
            loadVideo(it)
        }

        pickImageContract = registerForActivityResult(ActivityResultContracts.GetContent()) {
            loadImage(it)
        }

    }

    private fun pickVideoContract() {
        pickVideoContract.launch("video/*")
    }

    private fun pickImageContract() {
        pickImageContract.launch("image/*")
    }

    private fun recordVideoContract(uri: Uri) {
        recordVideoContract.launch(uri)
    }

    private fun captureImageContract(uri: Uri) {
        takePhotoContract.launch(uri)
    }

    private fun permissionContract() {
        permissionContract.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun showDialog() {
        try {
            val imageItems = arrayOf<CharSequence>(
                getString(R.string.take_picture),
                getString(R.string.choose_from_gallery),
                getString(R.string.cancel)
            )
            val videoItems = arrayOf<CharSequence>(
                getString(R.string.record_video),
                getString(R.string.choose_from_gallery),
                getString(R.string.cancel)
            )
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.select_one))
            if (isRecordingVideo) {
                builder.setItems(videoItems) { dialog, item ->
                    if (videoItems[item] == getString(R.string.record_video)) {
                        dialog.dismiss()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            createVideoURI()?.let { uri ->
                                recordVideoContract(uri)
                            }
                        } else
                            permissionContract()
                    } else if (videoItems[item] == getString(R.string.choose_from_gallery)) {
                        dialog.dismiss()
                        pickVideoContract()
                    } else if (videoItems[item] == getString(R.string.cancel)) {
                        dialog.dismiss()
                    }
                }
            } else {
                builder.setItems(imageItems) { dialog, item ->
                    if (imageItems[item] == getString(R.string.take_picture)) {
                        dialog.dismiss()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            createImageURI()?.let { uri ->
                                captureImageContract(uri)
                            }
                        } else
                            permissionContract()
                    } else if (imageItems[item] == getString(R.string.choose_from_gallery)) {
                        dialog.dismiss()
                        pickImageContract()
                    } else if (imageItems[item] == getString(R.string.cancel)) {
                        dialog.dismiss()
                    }
                }
            }
            builder.show()
        } catch (e: NotFoundException) {
            e.printStackTrace()
        }
    }

    private fun createImageURI(): Uri? {

        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val imageName = System.currentTimeMillis()

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageName")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val finalURI = contentResolver.insert(imageCollection, contentValues)
        resultUri = finalURI
        return finalURI
    }

    private fun createVideoURI(): Uri? {

        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val imageName = System.currentTimeMillis()

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$imageName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/MPEG-4")
        }

        val finalURI = contentResolver.insert(imageCollection, contentValues)
        resultUri = finalURI
        return finalURI
    }

    private fun loadVideo(videoUri: Uri) {
        mBinding.vvResult.setVideoURI(videoUri)
        mBinding.vvResult.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.start()
            mediaPlayer.isLooping = true
        }
        refreshGallery()
    }

    private fun loadImage(uri: Uri) {
        Glide.with(applicationContext).asBitmap().error(R.drawable.loader)
            .placeholder(R.drawable.loader).load(uri)
            .into(object : CustomTarget<Bitmap>(mBinding.ivResult.width, mBinding.ivResult.height) {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    mBinding.ivResult.setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    //
                }
            })
        refreshGallery()
    }

    private fun refreshGallery() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            mediaScanIntent.data = resultUri
            sendBroadcast(mediaScanIntent)
        }
    }
}