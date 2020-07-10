package com.example.handover

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.toolbar.*

class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNavigation: BottomNavigationView = findViewById(R.id.nav_view)
        val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_share -> {
                    openFragment(ShareFragment())
                    supportActionBar!!.setTitle(R.string.share)
                    Toast.makeText(this, R.string.share, Toast.LENGTH_SHORT).show()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_group -> {
                    openFragment(GroupFragment())
                    supportActionBar!!.setTitle(R.string.room)
                    Toast.makeText(this, R.string.room, Toast.LENGTH_SHORT).show()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    openFragment(ProfileFragment())
                    supportActionBar!!.setTitle(R.string.profile)
                    Toast.makeText(this, R.string.profile, Toast.LENGTH_SHORT).show()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        if (savedInstanceState == null) {
            val fragment: Fragment = ShareFragment()
            supportFragmentManager.beginTransaction().replace(R.id.container_a, fragment).commit()
            supportActionBar!!.setTitle(R.string.share)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mItem_connect_to_pc) {
            Toast.makeText(this, R.string.connect_to_pc, Toast.LENGTH_SHORT).show()
        }

        if (item.itemId == R.id.mItem_scan_to_connect) {
            Toast.makeText(this, R.string.scan_to_connect, Toast.LENGTH_SHORT).show()
        }

        if (item.itemId == R.id.mItem_invite) {
            Toast.makeText(this, R.string.invite, Toast.LENGTH_SHORT).show()
        }

        return super.onOptionsItemSelected(item)
    }

    fun openFragment(fragment: Fragment?) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container_a, fragment!!)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}