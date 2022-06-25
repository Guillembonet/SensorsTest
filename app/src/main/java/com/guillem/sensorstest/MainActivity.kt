package com.guillem.sensorstest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

private const val SAMPLING_AVERAGE = 2
private const val RECORD_SIZE = 10

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager : SensorManager
    private var mAccelerometer : Sensor ?= null
    private var mGyroscope : Sensor ?= null

    private var accCount : Int = 0
    private var accAverage : Triple<Float, Float, Float> = Triple(0.0F, 0.0F, 0.0F)

    private var gyroCount : Int = 0
    private var gyroAverage : Triple<Float, Float, Float> = Triple(0.0F, 0.0F, 0.0F)
    private var lastGyroAverage : Triple<Float, Float, Float> = Triple(0.0F, 0.0F, 0.0F)

    private var recordedValues1 : MutableList<Pair<Triple<Float, Float, Float>, Triple<Float, Float, Float>>> = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}
    private var recordedValues2 : MutableList<Pair<Triple<Float, Float, Float>, Triple<Float, Float, Float>>> = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}
    private var compareValues : MutableList<Pair<Triple<Float, Float, Float>, Triple<Float, Float, Float>>> = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}

    private var recordMode : Int = 0
    private var recorded1 : Boolean = false
    private var recorded2 : Boolean = false
    private var recordCount : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordedValues1 = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}
        recordedValues2 = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}
        compareValues = MutableList(RECORD_SIZE){Pair(Triple(0.0F, 0.0F, 0.0F), Triple(0.0F, 0.0F, 0.0F))}

        accCount = 0
        accAverage = Triple(0.0F, 0.0F, 0.0F)

        gyroCount = 0
        gyroAverage = Triple(0.0F, 0.0F, 0.0F)
        lastGyroAverage = Triple(0.0F, 0.0F, 0.0F)

        recordMode = 0
        recorded1 = false
        recorded2 = false
        recordCount = 0

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // get reference to button
        findViewById<Button>(R.id.record1).setOnClickListener {
            recordMode = 1
            recordCount = 0
        }
        findViewById<Button>(R.id.record2).setOnClickListener {
            recordMode = 2
            recordCount = 0
        }
        findViewById<Button>(R.id.compare).setOnClickListener {
            recordMode = 3
            recordCount = 0
        }
        findViewById<TextView>(R.id.sensor_data).text = "Waiting..."
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                accAverage = Triple(accAverage.first+event.values[0], accAverage.second+event.values[1], accAverage.third+event.values[2])
                accCount += 1
                if (accCount == SAMPLING_AVERAGE) {
                    val values = Triple(accAverage.first/SAMPLING_AVERAGE, accAverage.second/SAMPLING_AVERAGE, accAverage.third/SAMPLING_AVERAGE)
                    if (recordMode == 1) {
                        findViewById<TextView>(R.id.sensor_data).text = "Recording first..."
                        recordSensorValue(values, lastGyroAverage)
                        recordCount += 1
                        if (recordCount >= RECORD_SIZE) {
                            recordMode = 0
                            recorded1 = true
                            recordCount = 0
                            findViewById<TextView>(R.id.sensor_data).text = "Recorded"
                        }
                    } else if (recordMode == 2) {
                        findViewById<TextView>(R.id.sensor_data).text = "Recording second..."
                        recordSensorValue(values, lastGyroAverage)
                        recordCount += 1
                        if (recordCount >= RECORD_SIZE) {
                            recordMode = 0
                            recorded2 = true
                            recordCount = 0
                            findViewById<TextView>(R.id.sensor_data).text = "Recorded"
                        }
                    } else if (recordMode == 3) {
                        findViewById<TextView>(R.id.sensor_data).text = "Comparing..."
                        recordSensorValue(values, lastGyroAverage)
                        recordCount += 1
                        if (recordCount >= RECORD_SIZE) {
                            recordMode = 0
                            recordCount = 0
                            val errs = compareSensorValues()
                            if (errs.first > errs.second) {
                                findViewById<TextView>(R.id.sensor_data).text = "Second"
                            } else {
                                findViewById<TextView>(R.id.sensor_data).text = "First"
                            }
                            //findViewById<TextView>(R.id.sensor_data).text = "Recorded"
                        }
                    }
                    accCount = 0
                    accAverage = Triple(0.0F, 0.0F, 0.0F)
                }
            } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                gyroAverage = Triple(gyroAverage.first+event.values[0], gyroAverage.second+event.values[1], gyroAverage.third+event.values[2])
                gyroCount += 1
                if (gyroCount == SAMPLING_AVERAGE) {
                    lastGyroAverage = gyroAverage
                }
                gyroCount = 0
                gyroAverage = Triple(0.0F, 0.0F, 0.0F)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    private fun recordSensorValue(accValues: Triple<Float,Float,Float>, gyroValues: Triple<Float,Float,Float>) {
        if (recordMode == 1) {
            recordedValues1.add(Pair(accValues, gyroValues))
            recordedValues1.removeFirst()
        } else if (recordMode == 2) {
            recordedValues2.add(Pair(accValues, gyroValues))
            recordedValues2.removeFirst()
        } else if (recordMode == 3) {
            compareValues.add(Pair(accValues, gyroValues))
            compareValues.removeFirst()
        }
        //findViewById<TextView>(R.id.sensor_data).text =
        //    values.first.toString()+","+values.second.toString()+","+values.third.toString()
    }

    private fun compareSensorValues(): Pair<Float, Float> {
        var totalErr1 = 0.0F
        var totalErr2 = 0.0F
        for (i in 0 until RECORD_SIZE) {
            var firstDiff1 = recordedValues1[i].first.first - compareValues[i].first.first
            var secondDiff1 = recordedValues1[i].first.second - compareValues[i].first.second
            var thirdDiff1 = recordedValues1[i].first.third - compareValues[i].first.third
            totalErr1 += firstDiff1*firstDiff1 + secondDiff1*secondDiff1 + thirdDiff1*thirdDiff1
            firstDiff1 = recordedValues1[i].second.first - compareValues[i].second.first
            secondDiff1 = recordedValues1[i].second.second - compareValues[i].second.second
            thirdDiff1 = recordedValues1[i].second.third - compareValues[i].second.third
            totalErr1 += firstDiff1*firstDiff1 + secondDiff1*secondDiff1 + thirdDiff1*thirdDiff1

            var firstDiff2 = recordedValues2[i].first.first - compareValues[i].first.first
            var secondDiff2 = recordedValues2[i].first.second - compareValues[i].first.second
            var thirdDiff2 = recordedValues2[i].first.third - compareValues[i].first.third
            totalErr2 += firstDiff2*firstDiff2 + secondDiff2*secondDiff2 + thirdDiff2*thirdDiff2
            firstDiff2 = recordedValues2[i].second.first - recordedValues2[i].second.first
            secondDiff2 = recordedValues2[i].second.second - recordedValues2[i].second.second
            thirdDiff2 = recordedValues2[i].second.third - recordedValues2[i].second.third
            totalErr2 += firstDiff2*firstDiff2 + secondDiff2*secondDiff2 + thirdDiff2*thirdDiff2
        }
        totalErr1 /= RECORD_SIZE
        totalErr2 /= RECORD_SIZE
        return Pair(totalErr1, totalErr2)
    }
}