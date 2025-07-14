package com.example.catapulta

import OpenMeteoApi
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    fun translateWeatherType(code: Int) : String{
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Showers"
            95 -> "Thunderstorm"
            else -> "Unknown"
        }
    }

    private fun getCurrentLocation(context: Context) : Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            return networkLocation
        }

        return null
    }

    private suspend fun getIpLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ipinfo.io/json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            Log.i("Weather", "IPInfo response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val streamReader = InputStreamReader(connection.inputStream)
                val buffered = BufferedReader(streamReader)
                val response = buffered.readText()
                buffered.close()
                streamReader.close()
                connection.disconnect()

                Log.i("Weather", "IPInfo response: $response")

                val json = JSONObject(response)
                val loc = json.optString("loc", null) // e.g. "37.3860,-122.0840"
                if (loc != null && loc.contains(",")) {
                    val (latStr, lonStr) = loc.split(",")
                    val lat = latStr.toDoubleOrNull()
                    val lon = lonStr.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        return@withContext Pair(lat, lon)
                    }
                }
            } else {
                Log.e("Weather", "IPInfo HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("Weather", "Error querying IPInfo", e)
        }
        null
    }


    private fun generateWeather(){
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenMeteoApi::class.java)

        lifecycleScope.launch {
            var location = getCurrentLocation(this@MainActivity)

            if (location == null) {
                Log.i("Weather", "Location from Android API is null, trying IP geolocation fallback...")
                val ipLocation = getIpLocation()
                if (ipLocation != null) {
                    // Create a fake Location object to keep API consistent
                    location = Location("ip-api").apply {
                        latitude = ipLocation.first
                        longitude = ipLocation.second
                    }
                }
            }

            if (location != null) {
                try {
                    val response = api.getWeather(location.latitude, location.longitude)
                    val temp = response.current.temperature
                    val condition = translateWeatherType(response.current.weatherCode)
                    Log.d("Weather", "Temperature: $temp°C, Condition: $condition")
                } catch (e: Exception) {
                    Log.e("Weather", "Error: ${e.message}")
                }
            } else {
                Log.i("Weather", "Location still null after fallback. Cannot get weather.")
            }
        }

        val backgroundImage = findViewById<ImageView>(R.id.background_image)
        backgroundImage.setImageResource(R.drawable.wallpaper_nasa)
    }

    private fun retrieveAppList() : List<App> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PackageManager.ResolveInfoFlags.of(
            PackageManager.MATCH_ALL.toLong())
        val activities: List<ResolveInfo> =
            this.packageManager.queryIntentActivities(intent, flags)

        val installedApps = activities.map { resolveInfo ->
            App(
                name = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }

        return installedApps
    }

    private fun setupAppDrawer(installedApps : List<App>) {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = Adapter(installedApps) { app ->
            app.launch(this)
        }
        recyclerView.adapter = adapter
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Location access is required for weather.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestCoarseLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        requestCoarseLocationPermission()

        val installedApps = retrieveAppList()
        setupAppDrawer(installedApps)
        generateWeather()
    }
}
