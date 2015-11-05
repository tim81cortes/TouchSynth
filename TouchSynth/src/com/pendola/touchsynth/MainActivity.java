package com.pendola.touchsynth;


import uk.org.mangotsfieldschool.t.aylott.butonsound.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

@SuppressLint("ClickableViewAccessibility")
public class MainActivity extends Activity {
	Thread t;
	int sr = 44100;
	boolean isRunning = true;
	double surfTouchYVal;
	double surfTouchXVal;
	double tmpX = 0;
	double tmpY = 0;
	double surfTouchW = 0;
	double surfTouchH = 0;
	int lux;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		View touchView = (SurfaceView) findViewById(R.id.sV1);
		surfTouchW = touchView.getWidth();
		surfTouchH = touchView.getHeight();
		touchView.setOnTouchListener(new View.OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				final int action = event.getAction();
				double tmpX = event.getX();
				double tmpY = event.getY();
				switch (action & MotionEvent.ACTION_MASK) {

				case MotionEvent.ACTION_DOWN: {
					if (tmpX < surfTouchH) {
						surfTouchXVal = tmpX;
					}
					if (tmpY < surfTouchH) {
						surfTouchYVal = tmpY;
					}
					break;
				}

				case MotionEvent.ACTION_MOVE: {
					surfTouchXVal = event.getX();
					surfTouchYVal = event.getY();
					
					if (tmpX < surfTouchW) {
						surfTouchXVal = tmpX;
					}
					if (tmpY < surfTouchH) {
						surfTouchYVal = tmpY;
					}

					break;
				}
				}
				return true;

			}

		});

		t = new Thread() {
			public void run() {
				// set process priority
				setPriority(Thread.MAX_PRIORITY);
				// instantiate audio track object
				int buffsize = AudioTrack.getMinBufferSize(sr,
						AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				// CREATE AN AUDIO TRACK OBJECT
				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, sr,
						AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, buffsize,
						AudioTrack.MODE_STREAM);

				short samples1st[] = new short[buffsize];
				short samples5th[] = new short[buffsize];
				short samplesSum[] = new short[buffsize];
				short samplesDif[] = new short[buffsize];
				short samplesFin[] = new short[buffsize];

				int scaleNote = 0;
				double pentatonic[] = new double[] { 220, 261.63, 293.66,
						329.63, 392.00, 440.00, 523.25, 587.33, 659.26, 783.99,
						880.00, 1046.50, 1174.66, 1318.51, 1567.98, 1760.00 };
				int amp1st = 10000;
				double amp5th = amp1st * 0.75;
				double twopi = 8. * Math.atan(1.);
				double fr1st = 440.f;
				double fr5th = 440.f;
				double frSum = 440.f;
				double frDif = 440.f;
				double ph1st = 0.0;
				double ph5th = 0.0;
				double phSum = 0.0;
				double phDif = 0.0;

				// start audio track
				audioTrack.play();
				// synthesis loop
				while (isRunning) {
					// fr1st = 220 + 880*surfTouchYVal/500;
					scaleNote = (int) Math.round(surfTouchYVal / 50);
					if (scaleNote >= 0 && scaleNote < 16)
					{
						fr1st = pentatonic[scaleNote];
					}
					// fr5th = 440 + 1760*surfTouchYVal/500;
					fr5th = fr1st * 2;
					frSum = fr1st + fr5th;
					frDif = fr1st - fr5th;
					for (int i = 0; i < buffsize; i++) {
						samples1st[i] = (short) (amp1st * Math.sin(ph1st));
						samples5th[i] = (short) ((amp5th * surfTouchXVal / 300) * Math.sin(ph5th));
						samplesSum[i] = (short) (((amp5th * surfTouchXVal / 300) * 0.5) * Math.sin(phSum));
						samplesDif[i] = (short) (((amp5th * surfTouchXVal / 300) * 0.5) * Math.sin(phDif));
						samplesFin[i] = (short) (samples1st[i] + samples5th[i]
								+ samplesSum[i] + samplesDif[i] / 4);
						ph1st += twopi * fr1st / sr;
						ph5th += twopi * fr5th / sr;
						phSum += twopi * frSum / sr;
						phDif += twopi * frDif / sr;
					}
					audioTrack.write(samplesFin, 0, buffsize);
					// audioTrack.write(samples1st, 0, buffsize);
				}
				audioTrack.stop();
				audioTrack.release();
			}
		};
		t.start();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
