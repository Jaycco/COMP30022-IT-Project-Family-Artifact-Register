package com.honegroupp.familyRegister.view.item

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.honegroupp.familyRegister.R
import com.honegroupp.familyRegister.backend.FirebaseDatabaseManager
import com.honegroupp.familyRegister.controller.ItemController.Companion.editItem
import com.honegroupp.familyRegister.model.Item
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_edit.*
import java.text.SimpleDateFormat
import java.util.*

class ItemEdit : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        // get extra from Item Detail(DetailSlide)
        val itemKey = intent.getStringExtra("ItemKey")
        val currFamilyId = intent.getStringExtra("FamilyId")

        // retrieve Item
        lateinit var currItem: Item
        val rootPath = "/"
        val databaseRef = FirebaseDatabase.getInstance().getReference(rootPath)
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //Don't ignore errors!
                Log.d("TAG", p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                currItem =
                    p0
                        .child(FirebaseDatabaseManager.FAMILY_PATH)
                        .child(currFamilyId)
                        .child("items")
                        .child(itemKey)
                        .getValue(Item::class.java) as Item

                // set current item to view
                Picasso.get()
                    .load(currItem.imageURLs[0])
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(editItemImage)
                findViewById<EditText>(R.id.editName).setText(currItem.itemName)
                findViewById<EditText>(R.id.editDescription).setText(currItem.itemDescription)
                findViewById<TextView>(R.id.editItemDate).setText(currItem.date)

                // set Date picker
                setDatePicker(editItemDate)

                // set up the spinner (select public and privacy)
                val spinner: Spinner = findViewById(R.id.edit_privacy_spinner)

                // Create an ArrayAdapter using the string array and a default spinner layout
                ArrayAdapter.createFromResource(
                    this@ItemEdit,
                    R.array.privacy_options,
                    android.R.layout.simple_spinner_item
                ).also { adapter ->
                    // Specify the layout to use when the list of choices appears
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    // Apply the adapter to the spinner
                    spinner.adapter = adapter
                }

                // set on click listener
                editConfirm.setOnClickListener {
                    if (editName.text.toString() == "") {
                        Toast.makeText(
                            this@ItemEdit,
                            "Item name should not leave blank",
                            Toast.LENGTH_SHORT
                        ).show()
//                    }else if(numberOfImages == 0) {
//                        Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show()
//                    }else if(numberOfImages != imagePathList.size){
//                        Toast.makeText(this, "Please wait for uploading image", Toast.LENGTH_SHORT).show()
                    } else {

                        // create item to upload to Firebase
                        val updatedItem = Item(
                            itemName = editName.text.toString(),
                            itemDescription = editDescription.text.toString(),
                            itemOwnerUID = currItem.itemOwnerUID,
                            imageURLs = currItem.imageURLs,
                            isPublic = spinner.selectedItemPosition == 0,
                            date = editItemDate.text.toString()
                        )

                        // upload to Firebase
                        val itemPath =
                            FirebaseDatabaseManager.FAMILY_PATH + currFamilyId + "/" + "items/" + itemKey
                        val databaseRef = FirebaseDatabase.getInstance().getReference(itemPath)

                        databaseRef.child("").setValue(updatedItem)

                        // Go back to the previous activity
                        this@ItemEdit.finish()
                    }
                }
            }
        })
    }

    /**
     * This method is responsible for setting date picker.
     * */
    private fun setDatePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val sdf = SimpleDateFormat("dd/M/yyyy")
                textView.text = sdf.format(cal.time)

            }

        textView.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }
}