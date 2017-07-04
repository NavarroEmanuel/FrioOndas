package com.frioondas.frioondas;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class ProximityDetector implements SensorEventListener {

    private OnProximityListener mListener;

    public void setOnProximityListener(OnProximityListener listener) {
        this.mListener = listener;
    }

    public interface OnProximityListener {
        void onProximity(float count);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float valor=Float.parseFloat(String.valueOf(event.values[0]));

        if (mListener != null) {
            if (valor == 0) {
                mListener.onProximity(valor);
            } else {
                mListener.onProximity(100);
            }
        }
    }
}
