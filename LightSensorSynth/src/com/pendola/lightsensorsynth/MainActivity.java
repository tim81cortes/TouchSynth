package com.pendola.lightsensorsynth;

import com.mango.lightsens.R;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener{

	public SensorManager mSensorManager;
	public Sensor mLightSensor;
	public float mLux = 0.0f;
	Thread t;
	int sr = 44100;
	boolean isRunning = true;
	public double lightSensVal;
	double pentatonic[] = new double[] { 220, 261.63, 293.66,
			329.63, 392.00, 440.00, 523.25, 587.33, 659.26, 783.99,
			880.00, 1046.50, 1174.66, 1318.51, 1567.98, 1760.00 };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		//start an new thread to synthesise audio
				t = new Thread() {
					public void run(){
						//set process priority
						setPriority(Thread.MAX_PRIORITY);
						//set buffer size
						int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				        // create an audiotrack object
				        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffsize, AudioTrack.MODE_STREAM);
						short samples1st[] = new short[buffsize];
						short samples8ve[] = new short[buffsize];
						short samplesSum[] = new short[buffsize];
						short samplesDif[] = new short[buffsize];
						short samplesFin[] = new short[buffsize];

						//TODO initialize other oscillators 
						int amp1st = 10000;
						double amp8ve = amp1st * 0.75;
						int scaleNote = 0;
						double twopi = 8.*Math.atan(1.);
						double fr1st = 440.f;
						double fr8ve = 440.f;
						double frSum = 440.f;
						double frDif = 440.f;
						double ph1st = 0.0;
						double ph8ve = 0.0;
						double phSum = 0.0;
						double phDif = 0.0;
						
						//start audio
						audioTrack.play();
						
						//synthesis loop
						while(isRunning){
							scaleNote = (int) Math.round(lightSensVal / 30);
							if (scaleNote >= 0 && scaleNote < 15)
							{
								fr1st = pentatonic[scaleNote];
							}
							else if (scaleNote > 15)
							{	
								fr1st = pentatonic[15];
							}	
							amp8ve = amp1st * 0.75;
							fr8ve = fr1st * 2;
							frSum = fr1st + fr8ve;
							frDif = fr1st - fr8ve;
							for(int i = 0; i < buffsize; i++){
								samples1st[i] = (short) (amp1st*Math.sin(ph1st));
								samples8ve[i] = (short) (amp8ve * Math.sin(ph8ve));
								samplesSum[i] = (short) ((amp8ve* 0.2) * Math.sin(phSum));
								samplesDif[i] = (short) ((amp8ve * 0.2) * Math.sin(phDif));
								samplesFin[i] = (short) (samples1st[i] + samples8ve[i]
										+ samplesSum[i] + samplesDif[i] / 4);
								
								ph1st += twopi * fr1st / sr;
								ph8ve += twopi * fr8ve / sr;
								phSum += twopi * frSum / sr;
								phDif += twopi * frDif / sr;
							}
							audioTrack.write(samplesFin, 0, buffsize);
						}
						audioTrack.stop();
						audioTrack.release();
					}
					};
					t.start();
	}
	@Override
	protected void onResume() {
		mSensorManager.registerListener(this,mLightSensor,SensorManager.SENSOR_DELAY_FASTEST);
		super.onResume();
		
	}

	@Override
	protected void onPause() {
		mSensorManager.unregisterListener(this);
		super.onPause();
		
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if(sensor.getType() == Sensor.TYPE_LIGHT)
		{
			
		}
		}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LIGHT){
			mLux = event.values[0];
			
			String luxStr = String.valueOf(mLux);
			lightSensVal = mLux;
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setText(luxStr);
			
			Log.d("LUXTAG","Lux value: " + event.values[0]);
			//Log.i("Sensor Changed", "onSensor Change :" + event.values[0]);
		}

	}
	protected void onDestroy() {
		super.onDestroy();
		isRunning = false;
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		t = null;

	}

}
