package com.redvirtualcreations.locmock

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.redvirtualcreations.locmock.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import java.lang.IllegalStateException

const val REQUEST_CODE_LOCATION = 478

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapClickListener,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationProvider: FusedLocationProviderClient
    private var marker: Marker? = null
    private var mockLocation: LatLng? = null
    private lateinit var updateLocButton: Button
    private lateinit var connectButton: Button
    private lateinit var settingsButton: Button
    lateinit var sharedPref: SharedPreferences
    private var mockNetwork: MockLocationProvider? = null
    private var mockGPS: MockLocationProvider? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        updateLocButton = findViewById(R.id.LocUpdate)
        connectButton = findViewById(R.id.ConnectButton)
        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val settingsintent = Intent(this, SettingsActivity::class.java)
            this.startActivity(settingsintent)
        }
        updateLocButton.setOnClickListener {
            if (hasMockPermission()) {
                showPopUp(it, true)
            } else {
                showPopUp(it, false)
//                val fragment = NeedMockDialog()
//                fragment.show(supportFragmentManager, "MockNeeded")
            }
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap ?: return
        enableMyLocation()

        with(mMap.uiSettings) {
            isMyLocationButtonEnabled = hasLocationPermission()
            isRotateGesturesEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
        }
        mMap.setOnMapClickListener(this)
    }

    override fun onMapClick(loc: LatLng) {
        marker?.remove()
        marker = mMap.addMarker(MarkerOptions().position(loc))
    }

    @SuppressLint("MissingPermission")
    @AfterPermissionGranted(REQUEST_CODE_LOCATION)
    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            mMap.isMyLocationEnabled = true
            locationProvider.lastLocation.addOnSuccessListener { loc ->
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                )
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Location permission is used to show your location",
                REQUEST_CODE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) || EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasMockPermission(): Boolean {
        val appOpsManager: AppOpsManager =
            this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                applicationInfo.uid,
                applicationInfo.packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                applicationInfo.uid,
                applicationInfo.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private fun isLocationSelected(): Boolean {
        return marker != null
    }

    private fun startMocking(onPhone: Boolean, onWatch: Boolean) {
        if (isLocationSelected()) {
            mockLocation = marker!!.position
            if (onPhone) {
                if (sharedPref.getBoolean("mocknetwork", true)) {
                    mockNetwork = MockLocationProvider(LocationManager.NETWORK_PROVIDER, this)
                    mockNetwork!!.pushLocation(mockLocation!!.latitude, mockLocation!!.longitude)
                }
                mockGPS = MockLocationProvider(LocationManager.GPS_PROVIDER, this)
                mockGPS!!.pushLocation(mockLocation!!.latitude, mockLocation!!.longitude)
            }
            if (onWatch) {
                //TODO Send startmock with location data thru datalayer
            }
            updateLocButton.text = "Stop mocking"
            updateLocButton.setOnClickListener { stopMocking() }
        } else {
            Toast.makeText(this, "No location selected! Select one please", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun stopMocking() {
        mockNetwork?.shutdown()
        mockGPS?.shutdown()
        updateLocButton.setText(R.string.locupdate)
        updateLocButton.setOnClickListener {
            if (hasMockPermission()) {
                showPopUp(it, true)
            } else {
                showPopUp(it, false)
//                val fragment = NeedMockDialog()
//                fragment.show(supportFragmentManager, "MockNeeded")
            }
        }
    }

    private fun showPopUp(v: View, hasMock: Boolean) {
        val popup = PopupMenu(this, v)
        popup.setOnMenuItemClickListener(this)
        popup.inflate(R.menu.mockmenu)
        if (!hasMock) {
            popup.menu.removeItem(R.id.mockPhone)
            popup.menu.removeItem(R.id.mockAll)
        }
        popup.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onMenuItemClick(p0: MenuItem?): Boolean {
        if (p0 != null) {
            when (p0.itemId) {
                R.id.mockPhone -> {
                    startMocking(true, onWatch = false)
                }

                R.id.mockWatch -> {
                    startMocking(false, onWatch = true)
                }

                R.id.mockAll -> {
                    startMocking(true, onWatch = true)
                }
            }
        }
        return false
    }
}

class NeedMockDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.MockNeeded).setPositiveButton(
                "Open settings"
            ) { _, _ ->
                val intent =
                    Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            }.setTitle("Need mock access").create()
        } ?: throw IllegalStateException("Missing activity")
    }
}