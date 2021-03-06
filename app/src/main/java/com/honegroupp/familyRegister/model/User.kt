package com.honegroupp.familyRegister.model

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.PropertyName
import com.honegroupp.familyRegister.backend.DatabaseManager.FirebaseDatabaseManager
import com.honegroupp.familyRegister.view.home.CategoryAdapter
import kotlinx.android.synthetic.main.fragment_categories.view.*

/**
 * This class is responsible for storing data and business logic for User
 *
 * @author Renjie Meng
 * */

data class User(
    @set:PropertyName("username")
    @get:PropertyName("username")
    var username: String = "",
    @set:PropertyName("familyId")
    @get:PropertyName("familyId")
    var familyId: String = "",
    @set:PropertyName("imageUrl")
    @get:PropertyName("imageUrl")
    var imageUrl: String = "",
    @set:PropertyName("isFamilyOwner")
    @get:PropertyName("isFamilyOwner")
    var isFamilyOwner: Boolean = false
) {

    /*This constructor has no parameter, which is used to create CategoryUpload while retrieve data from database*/
    constructor() : this("")

    fun store(mActivity: AppCompatActivity, uid: String) {
        FirebaseDatabaseManager.uploadUser(mActivity, uid, this)

    }

    /**
     * This method is responsible for check whether the user has a family or not
     *
     * */
    fun hasFamily(): Boolean {
        return this.familyId != ""
    }

    companion object {

        /**
         * This method is responsible for showing all categories belongs to the User's family
         *
         * */
        fun showCategories(
            uid: String,
            view: View,
            mActivity: AppCompatActivity
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieveLive(rootPath) { d: DataSnapshot ->
                callbackShowCategories(
                    uid,
                    view,
                    mActivity,
                    d
                )
            }
        }

        /**
         * This callbackShowCategories method is responsible for helping show all categories belongs to the User's family
         *
         * */
        private fun callbackShowCategories(
            uid: String,
            view: View,
            mActivity: AppCompatActivity,
            dataSnapshot: DataSnapshot
        ) {
            // get User by UID
            val currUser =
                    dataSnapshot.child(FirebaseDatabaseManager.USER_PATH).child(
                        uid).getValue(User::class.java) as User

            // get family by Family ID
            val familyId = currUser?.familyId

            // Check whether user has family
            if (!currUser!!.hasFamily()) {
                Toast.makeText(
                    mActivity,
                    "Please join family First",
                    Toast.LENGTH_SHORT).show()
            } else {
                // get family
                val family =
                        dataSnapshot.child(FirebaseDatabaseManager.FAMILY_PATH).child(
                            familyId).getValue(Family::class.java) as Family

                // get categories from Family
                val categories = family.categories

                // Bind it to adapter of the recycler view
                val categoryAdapter =
                        CategoryAdapter(uid, categories, mActivity)
                // It would update recycler after loading image from firebase storage
                categoryAdapter.notifyDataSetChanged()

                // Make the progress bar invisible
                view.progress_circular_category.visibility = View.INVISIBLE


                // set it into the adapter
                view.category_recycler_view.layoutManager =
                        GridLayoutManager(mActivity, 2)
                view.category_recycler_view.adapter = categoryAdapter
            }


        }

    }

}
