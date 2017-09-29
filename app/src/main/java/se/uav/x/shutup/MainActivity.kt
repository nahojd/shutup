package se.uav.x.shutup

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

	private val TAG = "Main"

	private lateinit var sensorManager: SensorManager
	private var accelerometer: Sensor? = null

	private var dndOn = false
	private var faceDownSince: Long? = null
	private var faceUpSince: Long? = null
	private var lastInclination: Long = 90 //Let's start with straight up

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
	}

	override fun onResume() {
		super.onResume()
		sensorManager.registerListener(this, accelerometer, 1 * 1000 * 1000) //En gång i sekunden?

	}

	override fun onPause() {
		super.onPause()
		sensorManager.unregisterListener(this)
	}

	private fun turnOnDND() {
		Log.i(TAG, "Would have turned ON do not disturb")
		dndOn = true
		faceUpSince = null
	}

	private fun turnOffDND() {
		Log.i(TAG, "Would have turned OFF do not disturb")
		dndOn = false
		faceDownSince = null
	}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {

			val g = event.values.clone()
			val norm_of_g = Math.sqrt(g[0].toDouble() * g[0] + g[1] * g[1] + g[2] * g[2])
			g[0] = g[0] / norm_of_g.toFloat()
			g[1] = g[1] / norm_of_g.toFloat()
			g[2] = g[2] / norm_of_g.toFloat()

			val inclination = Math.round(Math.toDegrees(Math.acos(g[2].toDouble())))

			if (inclination > 170) { //Face down, and not DND yet
				val downSince = faceDownSince;
				if (downSince == null) { //Har inte tidigare varit facedown
					faceDownSince = Date().time
				}
				else if (Math.abs(lastInclination - inclination) > 1) { //Om den ändrat sig mer än 1 grad är den inte stilla på ett bord
					faceDownSince = Date().time //Reset
				}
				else if (!dndOn && Date().time - downSince > 5000) {
					turnOnDND()
				}

			}
			else if (dndOn) { //Not face down enough, and DND on
				val upSince = faceUpSince;
				if (upSince == null) {
					faceUpSince = Date().time
				}
				else if (Date().time - upSince > 2000) {
					turnOffDND()
				}
			}

			lastInclination = inclination
			runOnUiThread {
				textViewInclination.text = "DND: ${if (dndOn) "ON" else "OFF"}"
			}
			return
		}

	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

	}
}
