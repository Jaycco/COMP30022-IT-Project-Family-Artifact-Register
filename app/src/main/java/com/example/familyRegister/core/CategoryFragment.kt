package com.example.familyRegister.core

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.familyRegister.model.CategoryUpload
import com.example.familyRegister.model.ItemUpload
import com.example.familyRegister.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_category.view.*
import androidx.appcompat.app.AppCompatActivity

/**
 * This class handles the events happened in category fragment
 *      1. when user click on an category, it would navigate to that category page
 *      2. it would retrieve data from database and show them as category item
 *      3. (TODO) when user click on add category, he/she could add a new customized category
 * */

class CategoryFragment : Fragment() {
    companion object {
        /*This constant is used as a flag, to show there is no cover image for the category*/
        /*And, it told the adapter to use the default cover for that categoty*/
        const val PLEASE_USE_DEFAULT_COVER = "Default Cover"
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Find the toolbar view inside the activity layout
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_category)
        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        // set menu option to be true, then we could inflate our selected menu on the toolbar
        setHasOptionsMenu(true)


        // retrieve all the category data from the database
        val categories = ArrayList<CategoryUpload>()

        val databaseReference = FirebaseDatabase.getInstance().getReference(RegisterFragment.uid + "/")
        val categoryAdapter = CategoryAdapter(categories, context!!)

        databaseReference.addValueEventListener(object : ValueEventListener {

            override fun onCancelled(p0: DatabaseError) {
                toast(p0.message, Toast.LENGTH_SHORT)
                view.progress_circular_category.visibility = View.INVISIBLE
            }


            override fun onDataChange(p0: DataSnapshot) {
                // clear it before filling it
                categories.clear()

                p0.children.forEach {
                    // Retrieve data from database
                    val mValue = it.value

                    // check the data
                    var currItemUpload: CategoryUpload?
                    if (mValue == RegisterFragment.fakeInitialValue) {
                        currItemUpload = CategoryUpload(
                            it.key.toString(),
                            PLEASE_USE_DEFAULT_COVER,
                            "0"
                        )
                    } else {
                        val downloadURL = (it.children.last().getValue(ItemUpload::class.java) as ItemUpload).url
                        val count = it.childrenCount.toString()
                        currItemUpload =
                            CategoryUpload(it.key.toString(), downloadURL, count)
                    }


                    // add the freshly created object to the categories list
                    categories.add(currItemUpload)
                }

                // It would update recycler after loading image from firebase storage
                categoryAdapter.notifyDataSetChanged()
                view.progress_circular_category.visibility = View.INVISIBLE
            }
        })

        // set it into the adapter
        view.category_recycler_view.layoutManager = GridLayoutManager(activity, 2)
        view.category_recycler_view.adapter = categoryAdapter

        return view
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_category, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.add_category) {
            toast("Click!!!", Toast.LENGTH_SHORT)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun CategoryFragment.toast(msg: String, duration: Int) {
        Toast.makeText(activity, msg, duration).show()
    }
}