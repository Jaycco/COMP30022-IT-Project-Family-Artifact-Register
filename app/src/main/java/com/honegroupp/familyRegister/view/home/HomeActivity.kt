package com.honegroupp.familyRegister.view.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

import com.honegroupp.familyRegister.R
import kotlinx.android.synthetic.main.activity_home.*
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import com.google.android.material.tabs.TabLayout
import com.honegroupp.familyRegister.IDoubleClickToExit
import com.honegroupp.familyRegister.controller.AuthenticationController

import android.widget.TextView
import com.google.firebase.database.*
import com.honegroupp.familyRegister.backend.FirebaseDatabaseManager
import com.honegroupp.familyRegister.controller.HomeController
import com.honegroupp.familyRegister.model.Item
import com.honegroupp.familyRegister.model.User
import com.honegroupp.familyRegister.view.item.DetailSlide

import com.honegroupp.familyRegister.view.search.SearchActivity
import com.squareup.picasso.Picasso


@Suppress("DEPRECATION")
class HomeActivity : ContainerActivity(), IDoubleClickToExit {

    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    lateinit var userID: String

    var sortOrderAll = SORT_DEFAULT
    var sortOrderShow = SORT_DEFAULT


    // set items adapter for show page
    private val showItems = ArrayList<Item>()
    val showTabAdapter =
            ContainerAdapter(showItems, this, ContainerAdapter.SHOWPAGE)

    // set items adapter for show page
    private val allItems = ArrayList<Item>()
    val allTabAdapter = ContainerAdapter(allItems, this, ContainerAdapter.ALL)


    override fun onItemClick(position: Int) {
        if (viewPager.currentItem == 0) {
            categoryName = DetailSlide.ALL_PAGE_SIGNAL.toString()
        } else if (viewPager.currentItem == 2) {
            categoryName = DetailSlide.SHOW_PAGE_SIGNAL.toString()
        }


        val intent = Intent(this, DetailSlide::class.java)
        intent.putExtra("UserID", uid)
        intent.putExtra("FamilyId", familyId)
        intent.putExtra("PositionList", position.toString())
        intent.putExtra("CategoryNameList", categoryName)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Press  key to navigate to navigation drawer
        btn_home_sort.setOnClickListener {
            drawer_layout.openDrawer(GravityCompat.END)
        }

        //get User ID
        userID = intent.getStringExtra("UserID")
        val username = intent.getStringExtra("UserName") as String
        Log.d("Username", username)

        // Configure the toolbar setting
        toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.home_page)
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewPager = findViewById(R.id.viewpager)
        val viewPagerAdapter = setupViewPager(viewPager)

        // set the sort logic for both show and all page
        val allTabFragment = viewPagerAdapter.getItem(0) as AllTabFragment
        HomeController
            .sortItem(allTabFragment, allTabAdapter, showTabAdapter, this)

        // set when the btn_home_button should be displayed
        viewPager
            .addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    // so nothing
                }

                override fun onPageSelected(position: Int) {
                    when (position) {
                        0 -> btn_home_sort.visibility = View.VISIBLE
                        1 -> btn_home_sort.visibility = View.INVISIBLE
                        2 -> btn_home_sort.visibility = View.VISIBLE
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {

                }
            })

        tabLayout = findViewById(R.id.tabs)
        tabLayout.setupWithViewPager(viewPager)


        // Searching Feature
        var search = findViewById<TextView>(R.id.search_icon)
        search.layoutParams = Toolbar.LayoutParams(Gravity.RIGHT)

        search_icon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra("UserID", userID)
            intent.putExtra("Category", DetailSlide.ALL_PAGE_SIGNAL.toString())
            startActivity(intent)
            true
        }

        // Press Hamburger key to navigate to navigation drawer
        toolbar.setNavigationOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }


        //        //display User information
        displayUserInfo(uid)


        // Interaction with menuitems contained in the navigation drawer
        nav_view.menu.findItem(R.id.nav_account).setOnMenuItemClickListener {
            val intent = Intent(this, AccountActivity::class.java)
            intent.putExtra("UserID", userID)
            startActivity(intent)
            true
        }

        nav_view.menu.findItem(R.id.nav_view_family)
            .setOnMenuItemClickListener {
                val intent = Intent(this, ViewFamilyActivity::class.java)
                intent.putExtra("UserID", userID)
                startActivity(intent)
                true
            }
        nav_view.menu.findItem(R.id.help_and_feedback)
            .setOnMenuItemClickListener {
                val intent = Intent(this, HelpFeedbackActivity::class.java)
                intent.putExtra("UserID", userID)
                startActivity(intent)
                true
            }
        nav_view.menu.findItem(R.id.about).setOnMenuItemClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            intent.putExtra("UserID", userID)
            startActivity(intent)
            true
        }
        // Log out
        AuthenticationController.logout(btn_log_out, this)

        //click can see user detail (image and info)
        val headerView = nav_view.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.nav_imageView)
        imageView.setOnClickListener {
            val intent = Intent(this, UserDetailActivity::class.java)
            intent.putExtra("UserID", uid)
            startActivity(intent)

        }


    }

    private fun setupViewPager(viewPager: ViewPager): ViewPagerAdapter {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter
            .addFragment(AllTabFragment(allTabAdapter), getString(R.string.all))
        adapter
            .addFragment(CategoriesTabFragment(), getString(R.string.category))
        adapter.addFragment(
            ShowTabFragment(showTabAdapter),
            getString(R.string.show))
        viewPager.adapter = adapter
        return adapter
    }

    internal inner class ViewPagerAdapter(manager: FragmentManager) :
        FragmentPagerAdapter(manager) {
        private val mFragmentList = mutableListOf<Fragment>()
        private val mFragmentTitleList = mutableListOf<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitleList[position]
        }
    }

    /**
     * Click back button twice to exit app.
     * */
    override fun onBackPressed() {
        doubleClickToExit(this)
    }

    /*
    Display the user name, email address, and the user image
     */
    private fun displayUserInfo(uid: String) {
        // retrieve User
        lateinit var currUser: User
        val rootPath = "/"

        var databaseRef = FirebaseDatabase.getInstance().getReference(rootPath)
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //Don't ignore errors!
                Log.d("TAG", p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val t = object : GenericTypeIndicator<ArrayList<String>>() {}

                // get current Item from data snap shot
                currUser = p0
                    .child(FirebaseDatabaseManager.USER_PATH)
                    .child(uid)
                    .getValue(User::class.java) as User

                //display User Name on the header
                val headerView = nav_view.getHeaderView(0)
                val navUsername =
                        headerView.findViewById(R.id.nav_userName) as TextView
                navUsername.text = currUser.username

                // display the user id on the header
                val navUserEmail =
                        headerView.findViewById(R.id.nav_userEmail) as TextView
                navUserEmail.text = userID.replace("=", ".")

                //get the image URL
                val imageView =
                        headerView.findViewById<ImageView>(R.id.nav_imageView)
                val imageUrl = currUser.imageUrl

                // Load image to ImageView via its URL from Firebase Storage
                if (imageUrl != "") {
                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.mipmap.loading_jewellery)
                        .fit()
                        .centerCrop()
                        .into(imageView)
                }
            }
        })
    }
}