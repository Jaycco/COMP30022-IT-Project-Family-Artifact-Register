package com.honegroupp.familyRegister.model

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.PropertyName
import com.honegroupp.familyRegister.R
import com.honegroupp.familyRegister.backend.FirebaseDatabaseManager
import com.honegroupp.familyRegister.utility.EmailPathSwitch
import com.honegroupp.familyRegister.utility.Hash
import com.honegroupp.familyRegister.view.home.*
import com.honegroupp.familyRegister.view.item.ItemUploadActivity
import com.honegroupp.familyRegister.view.itemList.ItemListActivity

/**
 * This class is responsible for storing data and business logic for Family
 *
 * @param familyId is the id of the family, this is generated by FirebaseAuthetication.
 * @param members is an ArrayList of UIDs of the member of the familyK
 *
 * @author Tianyi Mo
 * */

data class Family(
    @set:PropertyName("familyName")
    @get:PropertyName("familyName")
    var familyName: String = "",
    @set:PropertyName("password")
    @get:PropertyName("password")
    var password: String = "",
    @set:PropertyName("familyId")
    @get:PropertyName("familyId")
    var familyId: String = "",
    @set:PropertyName("members")
    @get:PropertyName("members")
    var members: ArrayList<String> = ArrayList(),
    @set:PropertyName("categories")
    @get:PropertyName("categories")
    var categories: ArrayList<Category> = ArrayList(),
    @set:PropertyName("items")
    @get:PropertyName("items")
    var items: HashMap<String, Item> = HashMap()
) {
    /*This constructor has no parameter, which is used to create CategoryUpload while retrieve data from database*/
    constructor() : this(
        "",
        "",
        "",
        ArrayList(),
        ArrayList(),
        HashMap<String, Item>()
    )

    /**
     * This method is responsible for storing Family to the database.
     *
     * */
    fun store(mActivity: AppCompatActivity, uid: String, username: String) {
        // upload family first
        FirebaseDatabaseManager.uploadFamily(this, uid)

        // update User
        val userPath = FirebaseDatabaseManager.USER_PATH + uid + "/"
        FirebaseDatabaseManager.retrieve(
            userPath
        ) { d: DataSnapshot ->
            callbackAddFamilyToUser(
                this,
                mActivity,
                uid,
                username,
                userPath,
                d
            )
        }
    }

    /**
     * This method is the callback for add family ID to teh given user and update user on the Database.
     *
     * */
    private fun callbackAddFamilyToUser(
        family: Family,
        mActivity: AppCompatActivity,
        uid: String,
        username: String,
        userPath: String,
        dataSnapshot: DataSnapshot
    ) {
        // get user
        val user = dataSnapshot.child("").getValue(User::class.java) as User

        // set family id
        user.familyId = this.familyId

        // family ID is the owner's id, so if the uid is same as the family id
        // This user is the owner of the family, otherwise not.
        if (family.familyId == uid) {
            user.isFamilyOwner = true
        }

        // update user in the database
        FirebaseDatabaseManager.update(userPath, user)

        // Go to Home page
        val intent = Intent(mActivity, HomeActivity::class.java)
        intent.putExtra("UserID", uid)
        intent.putExtra("UserName", username)
        mActivity.startActivity(intent)
    }

    companion object {
        /**
         * This method is responsible for joining the user to the given family
         * */
        fun joinFamily(
            mActivity: AppCompatActivity,
            familyIdInput: String,
            familyPasswordInput: String,
            uid: String,
            username: String
        ) {
            FirebaseDatabaseManager.retrieve(
                FirebaseDatabaseManager.FAMILY_PATH
            ) { d: DataSnapshot ->
                callbackJoinFamily(
                    mActivity,
                    uid,
                    username,
                    familyIdInput,
                    familyPasswordInput,
                    mActivity,
                    d
                )
            }
        }

        /**
         * This method is responsible for joining the User to the family.
         * */
        private fun callbackJoinFamily(
            mActivity: AppCompatActivity,
            currUid: String,
            username: String,
            familyIdInput: String,
            familyPasswordInput: String,
            currActivity: AppCompatActivity,
            dataSnapshot: DataSnapshot
        ) {

            // Convert familyiD to th format it stored on firebase
            val familyIdInputModified = familyIdInput.replace(".", "=", true)

            // Check whether family exist
            if (!dataSnapshot.hasChild(familyIdInputModified) || familyIdInput.trim() == "") {
                Toast.makeText(
                    currActivity,
                    mActivity.getString(R.string.family_id_not_exist),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Get family
                val family =
                        dataSnapshot.child(familyIdInputModified).getValue(
                            Family::class.java
                        ) as Family
                // Check password
                if (family.password != Hash.applyHash(familyPasswordInput)) {
                    Toast.makeText(
                        currActivity,
                        mActivity.getString(R.string.password_is_incorrect),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    // Add user to family and add family to user
                    if (!family.members.contains(currUid)) {
                        family.members.add(currUid)
                        family.store(mActivity, currUid, username)
                    }

                    Toast.makeText(
                        currActivity,
                        mActivity.getString(R.string.join_family_successfully),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        /**
         * This method is responsible for showing the items in the item list
         *
         * */
        fun addItem(
            uid: String,
            categoryName: String,
            mActivity: AppCompatActivity
        ) {
            // go to upload new item
            val addButton =
                    mActivity.findViewById<FloatingActionButton>(R.id.btn_add)
            addButton.setOnClickListener {
                val intent = Intent(mActivity, ItemUploadActivity::class.java)
                intent.putExtra("UserID", uid)
                intent.putExtra("categoryPath", categoryName)
                mActivity.startActivity(intent)
            }
        }


        /**
         * This function is responsible for showing all the items in the category
         *
         * */
        fun showItems(
            uid: String,
            items: ArrayList<Item>,
            itemListAdapter: ContainerAdapter,
            categoryName: String,
            mActivity: ItemListActivity
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieveLive(rootPath) { d: DataSnapshot ->
                callbackShowItems(
                    uid,
                    items,
                    itemListAdapter,
                    categoryName,
                    mActivity,
                    d
                )
            }
        }

        /**
         * This method is the callback for the show items
         * */
        private fun callbackShowItems(
            uid: String,
            items: ArrayList<Item>,
            itemListAdapter: ContainerAdapter,
            categoryName: String,
            mActivity: ItemListActivity,
            dataSnapshot: DataSnapshot
        ) {


            // get user's family ID
            val currFamilyId =
                    dataSnapshot.child(FirebaseDatabaseManager.USER_PATH).child(
                        uid
                    ).child("familyId").getValue(
                        String::class.java
                    ) as String

            // set familyID
            mActivity.familyId = currFamilyId

            // get item keys for the given category as an ArrayList
            val itemKeysSnapshot =
                    dataSnapshot.child(FirebaseDatabaseManager.FAMILY_PATH)
                        .child(currFamilyId)
                        .child("categories").child(categoryName)
                        .child("itemKeys")

            var itemKeys = if (!itemKeysSnapshot.hasChildren()) {
                // the item keys is empty
                ArrayList()
            } else {
                itemKeysSnapshot.value as ArrayList<String>
            }

            // remove null from the itemkeys
            itemKeys = itemKeys.filterNotNull() as ArrayList<String>

            // clear items once retrieve item from the database
            items.clear()
            for (key in itemKeys) {
                // get item by each key
                val currItem =
                        dataSnapshot
                            .child(FirebaseDatabaseManager.FAMILY_PATH)
                            .child(currFamilyId)
                            .child("items")
                            .child(key)
                            .getValue(Item::class.java) as Item

                currItem.key = key

                // add it to the items, check item is visible, if not check user is owner
                if (currItem.isPublic) {
                    items.add(currItem)
                } else if (currItem.itemOwnerUID == mActivity.uid) {
                    items.add(currItem)
                }
            }

            // sort the current item according to current app's sort order
            sortItems(mActivity.sortOrder, items)

            // update the UI
            updateUI(
                itemListAdapter, items, mActivity,
                R.id.progress_circular,
                R.id.item_list_empty
            )
        }

        /**
         * Delete item from family
         * */
        fun deleteItem(
            uid: String,
            categoryName: String,
            itemId: String,
            mActivity: AppCompatActivity
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieve(rootPath) { d: DataSnapshot ->
                callbackDeleteItem(uid, categoryName, itemId, mActivity, d)
            }
        }

        /**
         * This is the callback for deleteItem.
         * This method defines the logic of deleting an item.
         * */
        @SuppressLint("RestrictedApi")
        private fun callbackDeleteItem(
            uid: String,
            categoryName: String,
            itemId: String,
            mActivity: AppCompatActivity,
            dataSnapshot: DataSnapshot
        ) {
            // get user's family ID
            val currFamilyId =
                    dataSnapshot.child(FirebaseDatabaseManager.USER_PATH).child(
                        uid
                    ).child("familyId").getValue(
                        String::class.java
                    ) as String

            // get the item first
            val item = dataSnapshot
                .child(FirebaseDatabaseManager.FAMILY_PATH)
                .child(currFamilyId)
                .child("items")
                .child(itemId)
                .getValue(Item::class.java) as Item

            // if the user is the item owner, delete
            if (item.itemOwnerUID == uid) {
                // find the category
                val itemKeys = dataSnapshot
                    .child(FirebaseDatabaseManager.FAMILY_PATH)
                    .child(currFamilyId)
                    .child("categories")
                    .child(categoryName)
                    .child("itemKeys")
                    .children


                for (itemKey in itemKeys) {
                    // remove item from category only.
                    if (itemKey.value == itemId) {
                        itemKey.ref.setValue(null)
                        break
                    }
                }


                // update itemkeys index first
                val itemKeysPath =
                        "${FirebaseDatabaseManager.FAMILY_PATH}$currFamilyId/categories/$categoryName/itemKeys/"
                var categoryItemKeys = dataSnapshot
                    .child(FirebaseDatabaseManager.FAMILY_PATH)
                    .child(currFamilyId)
                    .child("categories")
                    .child(categoryName)
                    .child("itemKeys")
                    .value as ArrayList<String>

                categoryItemKeys =
                        categoryItemKeys.filterNotNull() as ArrayList<String>
                categoryItemKeys.remove(itemId)

                FirebaseDatabaseManager.update(itemKeysPath, categoryItemKeys)

                // remove item
                FirebaseDatabase.getInstance()
                    .getReference("${FirebaseDatabaseManager.FAMILY_PATH}$currFamilyId/items/")
                    .child(itemId)
                    .removeValue()

                // update the item count
                val itemCount = dataSnapshot
                    .child(FirebaseDatabaseManager.FAMILY_PATH)
                    .child(currFamilyId)
                    .child("categories")
                    .child(categoryName)
                    .child("itemKeys")
                    .childrenCount
                val countReference =
                        "${FirebaseDatabaseManager.FAMILY_PATH}$currFamilyId/categories/$categoryName/count"
                FirebaseDatabase.getInstance().getReference(countReference)
                    .setValue(itemCount - 1)

            }
            // otherwise, show the warning dialog
            else {
                val familyNameChangeDialog = FamilyNameChangeDialog(uid)
                familyNameChangeDialog.show(
                    mActivity.supportFragmentManager,
                    "Location Change Dialog")
            }


        }


        /**
         * This method is responsible for showing all items in the show page
         *
         * */
        fun displayShowPage(
            mActivity: HomeActivity,
            showTabAdapter: ContainerAdapter,
            uid: String,
            currFrag: Fragment
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieveLive(rootPath) { d: DataSnapshot ->
                callbackDisplayShowPage(
                    uid,
                    mActivity,
                    currFrag,
                    showTabAdapter,
                    d
                )
            }
        }

        /**
         * This method is the callback for showing all items in the show page
         * */
        private fun callbackDisplayShowPage(
            uid: String,
            mActivity: HomeActivity,
            currFrag: Fragment,
            allTabAdapter: ContainerAdapter,
            dataSnapshot: DataSnapshot
        ) {

            if (currFrag != null && currFrag.isVisible) {
                // get user's family ID
                val currFamilyId = FirebaseDatabaseManager
                    .getFamilyIDByUID(uid, dataSnapshot)
                mActivity.familyId = currFamilyId

                // get items from the family
                val allItems =
                        dataSnapshot
                            .child(FirebaseDatabaseManager.FAMILY_PATH)
                            .child(currFamilyId)
                            .child("items")
                            .children


                // clear items once retrieve item from the database
                allTabAdapter.items.clear()

                allItems.forEach {
                    val item = it.getValue(Item::class.java) as Item

                    // add it to the items, check item is visible, if not check user is owner
                    if (item.showPageUids.contains(uid)) {
                        item.key = it.key.toString()
                        allTabAdapter.items.add(item)
                    }
                }

                // sort the current item according to current app's sort order
                sortItems(mActivity.sortOrderShow, allTabAdapter.items)

                // update the UI
                updateUI(
                    allTabAdapter,
                    allTabAdapter.items,
                    mActivity,
                    R.id.progress_circular,
                    R.id.item_list_empty
                )
            }
        }


        fun showAll(
            uid: String,
            showTabAdapter: ContainerAdapter,
            mActivity: HomeActivity,
            currFrag: Fragment
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieveLive(rootPath) { d: DataSnapshot ->
                callbackShowAll(
                    uid,
                    showTabAdapter,
                    mActivity,
                    currFrag,
                    d
                )
            }
        }


        private fun callbackShowAll(
            uid: String,
            showTabAdapter: ContainerAdapter,
            mActivity: HomeActivity,
            currFrag: Fragment,
            dataSnapshot: DataSnapshot
        ) {
            if (currFrag != null && currFrag.isVisible) {

                // get user's family ID
                val currFamilyId = FirebaseDatabaseManager
                    .getFamilyIDByUID(uid, dataSnapshot)
                mActivity.familyId = currFamilyId

                // get items from the family
                val allItems =
                        dataSnapshot
                            .child(FirebaseDatabaseManager.FAMILY_PATH)
                            .child(currFamilyId)
                            .child("items")
                            .children


                // clear items once retrieve item from the database
                showTabAdapter.items.clear()

                allItems.forEach {
                    val item = it.getValue(Item::class.java) as Item

                    item.key = it.key.toString()
                    showTabAdapter.items.add(item)
                }
                // sort the current item according to current app's sort order
                sortItems(mActivity.sortOrderAll, showTabAdapter.items)

                // update the UI
                updateUI(
                    showTabAdapter,
                    showTabAdapter.items,
                    mActivity,
                    R.id.all_progress_circular,
                    R.id.all_text_view_empty
                )
            }
        }

        /**
         * This method is responsible for showing all the family member in the user's family
         *
         * */
        fun showAllMembersAndInfo(
            uid: String, adapter: ViewFamilyAdapter,
            mActivity: ViewFamilyActivity,
            users: ArrayList<User>
        ) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieveLive(rootPath) { d: DataSnapshot ->
                callbackShowAllMembersAndInfo(uid, adapter, users, mActivity, d)
            }
        }

        /**
         * This method is responsible for interacting with the database to showi all the family
         * member in the user's family
         * */
        private fun callbackShowAllMembersAndInfo(
            uid: String,
            adapter: ViewFamilyAdapter,
            users: ArrayList<User>,
            mActivity: ViewFamilyActivity,
            dataSnapshot: DataSnapshot
        ) {
            val familyId =
                    FirebaseDatabaseManager.getFamilyIDByUID(uid, dataSnapshot)

            // get family name
            val familyName =
                    dataSnapshot.child(FirebaseDatabaseManager.FAMILY_PATH).child(
                        familyId
                    ).child("familyName").value as String

            // set the value of textview
            val familyIdView: TextView = mActivity.findViewById(R.id.family_id)
            val familyNameView: TextView =
                    mActivity.findViewById(R.id.family_name)

            // if the user id is the same as the family id then it is the owner of the family, he/she has the right to modify the family
            if (uid == familyId) {
                mActivity.findViewById<TextView>(R.id.btn_family_setting)
                    .visibility = View.VISIBLE

            }

            familyIdView.text =
                    "${mActivity.getString(R.string.family_id_show)}  ${EmailPathSwitch.pathToEmail(
                        familyId
                    )}"
            familyNameView.text =
                    "${mActivity.getString(R.string.family_name_show)}  $familyName"

            // retrieve user's uids in current family
            val path =
                    "${FirebaseDatabaseManager.FAMILY_PATH}$familyId/members/"

            val currUserUids =
                    dataSnapshot.child(path).value as ArrayList<String>

            // retrieve user and add it to a list
            users.clear()
            for (uid in currUserUids) {
                val currUser =
                        dataSnapshot.child(FirebaseDatabaseManager.USER_PATH).child(
                            uid).getValue(User::class.java) as User
                users.add(currUser)
            }

            // set this user to the adapter
            adapter.notifyDataSetChanged()
        }

        /**
         * This method is responsible for change the family name
         *
         * */
        fun changeName(uid: String, newFamilyName: String) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieve(rootPath) { d: DataSnapshot ->
                callbackChangeName(uid, newFamilyName, d)
            }
        }

        /**
         * This method the callback for change the family name
         *
         * */
        private fun callbackChangeName(
            uid: String,
            newFamilyName: String,
            dataSnapshot: DataSnapshot
        ) {
            val familyId =
                    FirebaseDatabaseManager.getFamilyIDByUID(uid, dataSnapshot)
            val familyNamePath =
                    "${FirebaseDatabaseManager.FAMILY_PATH}$familyId/familyName"

            FirebaseDatabase.getInstance().getReference(familyNamePath)
                .setValue(newFamilyName)
        }


        /**
         * This method is responsible for change the family password
         *
         * */
        fun changePassword(uid: String, newFamilyPassword: String) {
            val rootPath = "/"
            FirebaseDatabaseManager.retrieve(rootPath) { d: DataSnapshot ->
                callbackChangePassword(uid, newFamilyPassword, d)
            }
        }

        /**
         * This method the callback for change the family password
         *
         * */
        private fun callbackChangePassword(
            uid: String,
            newFamilyPassword: String,
            dataSnapshot: DataSnapshot
        ) {
            val familyId =
                    FirebaseDatabaseManager.getFamilyIDByUID(uid, dataSnapshot)
            val familyNamePath =
                    "${FirebaseDatabaseManager.FAMILY_PATH}$familyId/password"

            FirebaseDatabase.getInstance().getReference(familyNamePath)
                .setValue(Hash.applyHash(newFamilyPassword))
        }

        /**
         * This method is responsible for sort the items according the the sortorder
         *
         * */
        private fun sortItems(sortOrder: String, items: ArrayList<Item>) {
            when (sortOrder) {
                ContainerActivity.NAME_ASCENDING -> //sort by name ascending
                    items.sortBy { it.itemName }
                ContainerActivity.NAME_DESCENDING -> //sort by name descending
                    items.sortByDescending { it.itemName }
                ContainerActivity.TIME_ASCENDING -> //sort by time ascending
                    items.sortBy { it.date }
                ContainerActivity.TIME_DESCENDING -> //sort by time descending
                    items.sortByDescending { it.date }
            }
        }

        /**
         * This method is responsible for updating the UI layer after retrieving the data from
         * the Firebase Database.
         *
         * */

        private fun updateUI(
            adapter: ContainerAdapter,
            items: ArrayList<Item>,
            mActivity: ContainerActivity,
            progressBarId: Int,
            emptyTextViewId: Int
        ) {
            // notify the adapter to update
            adapter.notifyDataSetChanged()

            // update the UI according to the state of items, whether it is empty.
            if (items.isEmpty()) {
                // Make the progress bar invisible
                mActivity.findViewById<ProgressBar>(progressBarId).visibility =
                        View.INVISIBLE

                mActivity.findViewById<TextView>(emptyTextViewId).visibility =
                        View.VISIBLE
            } else {
                // Make the progress bar invisible
                mActivity.findViewById<ProgressBar>(progressBarId).visibility =
                        View.INVISIBLE
                mActivity.findViewById<TextView>(emptyTextViewId).visibility =
                        View.INVISIBLE
            }
        }

    }
}
