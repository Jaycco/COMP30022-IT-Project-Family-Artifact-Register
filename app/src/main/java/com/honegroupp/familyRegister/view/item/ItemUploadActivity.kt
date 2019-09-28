package com.honegroupp.familyRegister.view.item

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.honegroupp.familyRegister.R
import kotlinx.android.synthetic.main.item_upload_page.*
import android.app.Activity
import android.net.Uri
import android.view.MotionEvent
import android.widget.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.honegroupp.familyRegister.controller.ItemController.Companion.createItem
import com.honegroupp.familyRegister.view.itemList.ItemGridAdapter
import java.util.*
import kotlin.collections.ArrayList


class ItemUploadActivity : AppCompatActivity(){
    val GALLERY_REQUEST_CODE = 123
    var imagePathList = ArrayList<String>()
    var allImageUri= ArrayList<Uri>()
    var numberOfImages = 0
    lateinit var uid :String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_upload_page)

        uid = intent.getStringExtra("UserID")
        val categoryName = intent.getStringExtra("categoryPath").toString()

        //set up the spinner (select public and privacy)
        val spinner: Spinner = findViewById(R.id.privacy_spinner)

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.privacy_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }


        itemChooseImage.setOnClickListener {
            selectImageInAlbum()
        }

        addItemConfirm.setOnClickListener{

//            need to CHECK empty logic
            if(item_name_input.text.toString() == ""){
                Toast.makeText(this,"Item name should not leave blank",Toast.LENGTH_SHORT).show()
            }else if(numberOfImages == 0) {
                Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
            }else if(numberOfImages != imagePathList.size){
//                Toast.makeText(this, numberOfImages.toString() +" " + imagePathList.size.toString(),Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "Please wait for uploading image", Toast.LENGTH_SHORT).show()
            }else {
                createItem(this, item_name_input,item_description_input, uid, categoryName, imagePathList, spinner.selectedItemPosition == 0)
            }
        }
    }



    //use the phone API to get thr image from the album
    private fun selectImageInAlbum() {

        //reset the image url list
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        // ask for multiple images picker
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    @SuppressLint("ResourceType")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {

//                    adding multiple image
                    if (data != null) {
                        if (data.getClipData() != null) {

                            //handle multiple images
                            val count = data.getClipData()!!.getItemCount()
                            numberOfImages += count

                            var allUris : ArrayList<Uri> = arrayListOf()
                            for (i in 0 until count) {
                                val uri = data.getClipData()!!.getItemAt(i).uri
                                if (uri != null) {

                                    //add into Uri List
                                    allImageUri.add(uri)

                                    //upload uri to firebase
                                    uploadtofirebase(uri)
                                }
                            }


                            //selecting single image from album
                        } else if (data.getData() != null) {

                            numberOfImages += 1

                            val uri = data.getData()
                            if (uri != null) {

                                //upload to firebase
                                uploadtofirebase(uri)

                                //add into Uri List
                                allImageUri.add(uri)
                            }

                        }

                        // Get an instance of base adapter
                        val adapter = ItemGridAdapter(this,allImageUri)

                        // Set the grid view adapter
                        itemGridView.adapter = adapter

                        Toast.makeText(this,numberOfImages.toString(),Toast.LENGTH_LONG).show()
                    }else{
                        Toast.makeText(this,"Error",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun uploadtofirebase(selectedImage: Uri) {
        val uploadPath = " "
        val ref =
            FirebaseStorage.getInstance().reference.child(uploadPath + System.currentTimeMillis())
        var uploadTask: StorageTask<UploadTask.TaskSnapshot>? = ref.putFile(selectedImage!!)
            .addOnSuccessListener {
                //add item logic

                ref.downloadUrl.addOnCompleteListener() { taskSnapshot ->

                    var url = taskSnapshot.result

                    this.imagePathList.add(url.toString())

                }
            }
    }
}