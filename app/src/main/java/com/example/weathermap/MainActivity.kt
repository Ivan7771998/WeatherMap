package com.example.weathermap

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.weathermap.common.Common
import com.example.weathermap.common.Helper
import com.example.weathermap.models.OpenWeatherMap
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {
    //Const
    private val PERMISSION_REQUEST_CODE = 1001
    private val PLAY_SERVICE_RESOLUTION_REQUEST = 1000

    //Variables
    internal var openWeatherMap = OpenWeatherMap()
    private var mGoogleApiClient: GoogleApiClient? = null
    var mLocationRequest: LocationRequest? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission();
        if (checkPlayService()) {
            buildGoogleApiClient()
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayService()) {
                        buildGoogleApiClient()
                    }
                }
            }
        }
    }


    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API).build()
    }

    private fun checkPlayService(): Boolean {
        var resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    PLAY_SERVICE_RESOLUTION_REQUEST
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Это устройство не поддерживает",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            return false
        }
        return true
    }


    private inner class GetWeather : AsyncTask<String, Void, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progress.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: String?): String {
            var stream: String? = null
            var urlString = params[0]

            val http = Helper()
            stream = http.getHTTPData(urlString)
            return stream

        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result!!.contains("Error: Not found city")) {
                progress.visibility = View.GONE
                return
            }

            val gson = Gson()
            val type = object : TypeToken<OpenWeatherMap>() {}.type

            openWeatherMap = gson.fromJson<OpenWeatherMap>(result, type)
            progress.visibility = View.GONE

            txtCity.text = "${openWeatherMap.name},${openWeatherMap.sys!!.country}"
            txtLastUpdate.text = "Last Updated: ${Common.dateNow}"
            txtDescription.text = "${openWeatherMap.weather!![0].description}"
            txtTime.text =
                "${Common.unixTimeStampToDateTime(openWeatherMap.sys!!.sunrise)}/${Common.unixTimeStampToDateTime(
                    openWeatherMap.sys!!.sunset
                )}"
            txtHumidity.text = "${openWeatherMap.main?.humidity}"
            txtCelsius.text = "${openWeatherMap.main?.temp} ℃"
            Glide.with(this@MainActivity)
                .load(Common.getImage(openWeatherMap.weather!![0].icon!!))
                .into(imageView)
        }


    }

    override fun onConnected(p0: Bundle?) {
        createLocationRequest()
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 10000 //10 sec
        mLocationRequest!!.fastestInterval = 5000 //5 sec
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
            mGoogleApiClient,
            mLocationRequest,
            this
        )
    }

    override fun onConnectionSuspended(p0: Int) {
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.i("Error", "Connection failed: " + p0.errorCode)
    }

    override fun onLocationChanged(location: Location?) {
        Log.d("Location", "LocationChanged: ${location?.latitude} // ${location?.longitude}")
        GetWeather().execute(
            Common.apiRequest(
                location?.latitude.toString(),
                location?.longitude.toString()
            )
        )
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mGoogleApiClient!!.disconnect()
    }

    override fun onResume() {
        super.onResume()
        checkPlayService()
    }
}
