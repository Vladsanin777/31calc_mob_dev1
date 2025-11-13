package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.example.myapplication.databinding.ActivityMainBinding;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.content.Context;

import android.graphics.Color;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
import android.util.Log;
import android.view.KeyEvent;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Used to load the 'myapplication' library on application startup.
    static {
        System.loadLibrary("myapplication");
    }

    private TextView m_rt;
    private TextView m_headsOrTrails;
    private TextView m_rng;
    private ActivityMainBinding m_binding;
    private SensorManager m_sensorManager;
    private Sensor m_accelerometer;
    private final Object m_playLock = new Object();
    private long m_lastShakeTime = 0;
    private static final long SHAKE_COOLDOWN_MS = 1000;
    private boolean m_volumeUpPressed = false;
    private boolean m_volumeDownPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(m_binding.getRoot());

        // Example of a call to a native method
        m_rt = m_binding.isRoot;
        m_headsOrTrails = m_binding.headsOrTails;
        m_rng = m_binding.isRNG;
        m_rt.setText(cheakRoot());
        m_rng.setText(cheakRNG());
        m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        m_accelerometer = m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        m_sensorManager.registerListener(this, m_accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                eventPlayHeadsOrTails();
                return true;
            }
        });
        m_headsOrTrails.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (acceleration > 4) {
            eventPlayHeadsOrTails();
        }
    }
    private void eventPlayHeadsOrTails() {
        synchronized (m_playLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - m_lastShakeTime > SHAKE_COOLDOWN_MS) {
                m_lastShakeTime = currentTime;
                boolean isHeads;
                if (m_volumeUpPressed) {
                    isHeads = true;
                } else if (m_volumeDownPressed) {
                    isHeads = false;
                } else {
                    isHeads = playHeadsOrTails();
                }
                String resultText = isHeads ? "Орёл" : "Решка";

                m_headsOrTrails.post(() -> {
                    m_headsOrTrails.setText(resultText);

                    m_headsOrTrails.setScaleX(0.5f);
                    m_headsOrTrails.setScaleY(0.5f);
                    float rotationAngle = (Math.random() < 0.5 ? -90f : 90f);
                    m_headsOrTrails.setRotation(rotationAngle);
                    m_headsOrTrails.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .rotation(0f)
                            .setDuration(300)
                            .start();

                    int colorFrom = Color.parseColor("#FF9B00FF");
                    int colorTo = Color.parseColor("#9C27B0");
                    ValueAnimator colorAnim = ValueAnimator.ofObject(
                            new ArgbEvaluator(), colorFrom, colorTo, colorFrom);
                    colorAnim.setDuration(2000);
                    colorAnim.addUpdateListener(animator ->
                            m_headsOrTrails.setTextColor((int) animator.getAnimatedValue()));
                    colorAnim.start();
                });
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                m_volumeUpPressed = true;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                m_volumeDownPressed = true;
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                m_volumeUpPressed = false;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                m_volumeDownPressed = false;
                break;
        }
        return true;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private native String cheakRoot();
    private native String cheakRNG();
    private native boolean playHeadsOrTails();

}
