package com.example.FamilyRegister

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.fragment_register.view.*

class RegisterFragment : Fragment() {
    companion object {
        val defaultCategories = listOf("Letter", "Instrument", "Furniture", "Photos")
        val fakeInitialValue = "fakeInitialValue"
        lateinit var uid: String
    }

    var database = FirebaseDatabase.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        val view = inflater.inflate(R.layout.fragment_register, container, false)

        // Navigate to the category fragment
        view.btn_register.setOnClickListener {
            // uid is the user id entered
            uid = view.edit_text_user_id.text.toString()
            // initialize default categories in user's database
            val path = "$uid/"
            val databaseReference = database.getReference(path)
            defaultCategories.forEach {
                databaseReference.child(it).setValue(fakeInitialValue)
            }

            (activity as NavigationHost).navigateTo(CategoryFragment(), false)
        }
        return view
    }

}