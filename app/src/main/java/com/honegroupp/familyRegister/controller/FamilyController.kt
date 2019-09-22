package com.honegroupp.familyRegister.controller

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.honegroupp.familyRegister.R
import com.honegroupp.familyRegister.backend.FirebaseDatabaseManager
import com.honegroupp.familyRegister.model.Family
import com.honegroupp.familyRegister.model.User
import com.honegroupp.familyRegister.view.home.HomeActivity
import com.honegroupp.familyRegister.view.item.ItemUploadActivity


/**
 * This class is responsible for controller the event related to family.
 * There are create, join and view a family.
 *
 * */
class FamilyController {
    companion object {

        //TODO 1 user could only create one family

        //TODO user currently in family has no right to create family

        /**
         * This method is responsible for validating user input for creating a family
         *
         * */
        fun validateCreateFamilyInput(
            mContext: Context,
            familyId: EditText,
            password: EditText,
            passwordAgain: EditText
        ): Boolean {
            // check if the family name is empty
            if (familyId.text.toString().trim() == "") {
                Toast.makeText(
                    mContext,
                    mContext.getString(R.string.type_family_name),
                    Toast.LENGTH_SHORT
                ).show()
                familyId.text = null

                return false
            }
            // check if first password entered
            else if (password.text.toString() == "") {
                Toast.makeText(mContext, "Please enter the password", Toast.LENGTH_SHORT).show()
                passwordAgain.text = null
                return false
            }

            // check if repeated password entered
            else if (passwordAgain.text.toString() == "") {
                Toast.makeText(mContext, "Please enter the password again", Toast.LENGTH_SHORT)
                    .show()
                return false
            }

            // check are these two passwords identical
            else if (password.text.toString() != passwordAgain.text.toString()) {
                Toast.makeText(mContext, "Two passwords are not same", Toast.LENGTH_SHORT).show()
                password.text = null
                passwordAgain.text = null
                return false
            }

            // Pass all the test
            return true
        }

        /**
         * This methods is responsible for creating a family and upload it to the database.
         *
         * */
        fun createFamily(
            mActivity: AppCompatActivity,
            familyId: EditText,
            password: EditText,
            uid: String
        ) {
            val family = Family(familyId.text.toString(), password.text.toString(), uid)
            family.members.add(uid)
            family.store(mActivity, uid)

            Toast.makeText(mActivity, "Family Created Successfully", Toast.LENGTH_SHORT).show()


        }


        /**
         * This methods is responsible for validating family id and its password.
         * TODO This method might not be in controller.
         * */
        fun validateJoinFamilyInput(
            mActivity: AppCompatActivity,
            familyId: EditText,
            password: EditText,
            uid: String
        ) {

            val familyIdInput = familyId.text.toString()
            val familyPasswordInput = password.text.toString()

            FirebaseDatabaseManager.retrieve(
                FirebaseDatabaseManager.FAMILY_PATH
            ) { d: DataSnapshot ->
                callbackJoinFamily(
                    mActivity,
                    uid,
                    familyIdInput,
                    familyPasswordInput,
                    mActivity,
                    d
                )
            }
        }

        private fun callbackJoinFamily(
            mActivity: AppCompatActivity,
            currUid: String,
            familyIdInput: String,
            familyPasswordInput: String,
            currActivity: AppCompatActivity,
            dataSnapshot: DataSnapshot
        ) {
            // Check whether family exist
            if (!dataSnapshot.hasChild(familyIdInput)) {
                Toast.makeText(currActivity, "Family Id is not correct!", Toast.LENGTH_SHORT).show()
            } else {
                // Get family
                val family =
                    dataSnapshot.child(familyIdInput).getValue(Family::class.java) as Family
                // Check password
                if (family.password != familyPasswordInput) {
                    Toast.makeText(currActivity, "Password is not correct!", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // Add user to family and add family to user
                    if (!family.members.contains(currUid)) {
                        family.members.add(currUid)
                        val familyPath = FirebaseDatabaseManager.FAMILY_PATH + familyIdInput + "/"
                        FirebaseDatabaseManager.update(familyPath, family)
                        // get User and add familyId to user
                        FirebaseDatabaseManager.retrieve(
                            FirebaseDatabaseManager.USER_PATH
                        ) { d: DataSnapshot ->
                            callbackAddFamilyIdToUser(
                                mActivity,
                                currUid,
                                familyIdInput,
                                d
                            )
                        }
                    }

                    Toast.makeText(currActivity, "Join family successful!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        private fun callbackAddFamilyIdToUser(
            mActivity: AppCompatActivity,
            currUid: String,
            familyIdInput: String,
            dataSnapshot: DataSnapshot
        ) {
            val currUser = dataSnapshot.child(currUid).getValue(User::class.java) as User
            currUser.familyId = familyIdInput


            // update user in the database
            val path = FirebaseDatabaseManager.USER_PATH + currUid + "/"
            FirebaseDatabaseManager.update(path, currUser)

            // Go to Home page
            val intent = Intent(mActivity, HomeActivity::class.java)
            intent.putExtra("UserID", currUid)
            mActivity.startActivity(intent)
        }

        /**
         * This method is to navigate button click from mActivity to destination
         *
         * */
        fun buttonClick(mActivity: AppCompatActivity, button: Button, destination: Class<*>, uid: String) {
            button.setOnClickListener {
                val intent = Intent(mActivity, destination)
                intent.putExtra("UserID", uid)
                mActivity.startActivity(intent)
            }
        }
    }
}