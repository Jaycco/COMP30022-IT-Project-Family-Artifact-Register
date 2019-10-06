package com.honegroupp.familyRegister.backend

import android.net.Uri
import android.provider.MediaStore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.honegroupp.familyRegister.utility.CompressionUtil
import com.honegroupp.familyRegister.view.item.ItemUploadActivity

class FirebaseStorageManager{
    companion object {
        private const val uploadPath = " "

        fun uploadToFirebase(allImageUri: ArrayList<Uri>, categoryName:String,activity:ItemUploadActivity) {
            if (allImageUri.size == 0){

                //Create Item And Upload
                activity.uploadItem(categoryName)

            }else {
                //upload the progress bar
                activity.displayProgress()

                //get firebase storage reference
                val ref =
                    FirebaseStorage.getInstance()
                        .reference.child(uploadPath + System.currentTimeMillis())

                //convert first image in list to bitmap
                val uri = allImageUri[0]
                val bitmap = MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)

                //decrease the resolution
                val scaledBitmap = CompressionUtil.scaleDown(bitmap, true)

                //compress the image
                val data = CompressionUtil.compressImage(scaledBitmap)

                //upload the compressed image
                var uploadTask: StorageTask<UploadTask.TaskSnapshot>? = ref.putBytes(data)
                    .addOnSuccessListener {

                        //remove the uploaded Item
                        allImageUri.removeAt(0)

                        //add item logic
                        ref.downloadUrl.addOnCompleteListener() { taskSnapshot ->
                            var url = taskSnapshot.result
                            activity.imagePathList.add(url.toString())

                            //recursively upload next image
                            this.uploadToFirebase(allImageUri,categoryName,activity)
                        }
                    }
            }
        }
    }
}