package br.usp.ime.dspbenchmarking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StressActivity extends DspActivity {

	
	// Test config
	protected static final int MAX_DSP_CYCLES = 100;

	// Views
	protected ToggleButton toggleTests = null;
	protected ProgressBar workingBar = null;
	protected ProgressBar progressBar = null;
	protected TextView algorithmName = null;
	protected TextView blockSizeView = null;

	// Output file specifications
	final String dirName = "DspBenchmarking";
	final String fileName = "/dsp-benchmark-results-";
	final String dateFormat = "yyyy-MM-dd_HH-mm-ss";
	OutputStream os;

	// External storage state
	BroadcastReceiver mExternalStorageReceiver;
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;

	// DSP stuff
	InputStream is;

	// Test limits
	static int startAlgorithm = 0;
	static int endAlgorithm = 2;
	int blockSize = startBlockSize;
	int algorithm = startAlgorithm;
	
	// local stuff
	private int filterSize;
	static int startBlockSize = (int) Math.pow(2,5);
	static int endBlockSize = (int) Math.pow(2,14);

	
	/**
	 * 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Set the view
		setContentView(R.layout.stress);
		super.onCreate(savedInstanceState);

		// Finds toggle button
		toggleTests = (ToggleButton) findViewById(R.id.toggleTests);
		toggleTests.setTextOff("start");
		toggleTests.setTextOn("stressing device...");

		// Find working bar
		workingBar = (ProgressBar) findViewById(R.id.workingBar);
		workingBar.setVisibility(ProgressBar.INVISIBLE);

		// Find progress bar
		progressBar = (ProgressBar) findViewById(R.id.progressBar);

		// Find algorithm and block info
		algorithmName = (TextView) findViewById(R.id.algorithmName);
		blockSizeView = (TextView) findViewById(R.id.blockSize);
	}
	
	/**
	 * 
	 */
	protected void turnOff() {
		workingBar.setVisibility(ProgressBar.INVISIBLE);
		toggleTests.toggle();
		toggleTests.setClickable(true);
	}
	
	public void toggleTests(View v) {
		if (toggleTests.isChecked()) {
			toggleTests.setClickable(false);
			workingBar.setVisibility(ProgressBar.VISIBLE);
			initTests();
		} else {
			turnOff();
		}
	}
	

	/**
	 * 
	 * @return
	 */
	protected OutputStream getOutputStream() {
		// Get info on external storage
		try {
			updateExternalStorageState();
		} catch (IOException e) {
			Log.e("TestActivity", "No writeable media found.");
			e.printStackTrace();
		}
		File prefix = Environment.getExternalStorageDirectory();
		// Create dir
		File dir = new File(prefix, dirName);
		dir.mkdirs();
		// Create output file
		File outputFile = new File(dir, getFileName());
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// Writes device info on file
		try {
			outputStream.write(getBuildInfo().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outputStream;
	}
	

	/**
	 * 
	 * @return
	 */
	private String getBuildInfo() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("# board: " + Build.BOARD + "\n");
		// sbuf.append("# bootloader: "+Build.BOOTLOADER+"\n");
		sbuf.append("# brand: " + Build.BRAND + "\n");
		sbuf.append("# cpu_abi: " + Build.CPU_ABI + "\n");
		// sbuf.append("# cpu_abi2: "+Build.CPU_ABI2+"\n");
		sbuf.append("# device: " + Build.DEVICE + "\n");
		sbuf.append("# display: " + Build.DISPLAY + "\n");
		sbuf.append("# fingerprint: " + Build.FINGERPRINT + "\n");
		// sbuf.append("# hardware: "+Build.HARDWARE+"\n");
		sbuf.append("# host: " + Build.HOST + "\n");
		sbuf.append("# id: " + Build.ID + "\n");
		sbuf.append("# manufacturer: " + Build.MANUFACTURER + "\n");
		sbuf.append("# model: " + Build.MODEL + "\n");
		sbuf.append("# product: " + Build.PRODUCT + "\n");
		// sbuf.append("# serial: "+Build.SERIAL+"\n");
		sbuf.append("# tags: " + Build.TAGS + "\n");
		sbuf.append("# time: " + Build.TIME + "\n");
		sbuf.append("# type: " + Build.TYPE + "\n");
		sbuf.append("# user: " + Build.USER + "\n\n");
		sbuf.append("# bsize time  cbt readt sampread sampwrit blockper cbperiod perftime calltime\n");
		return sbuf.toString();
	}


	/**
	 * Generates a byte array with statistics from the DSP thread
	 * 
	 * @return
	 */
	private byte[] getDspThreadInfo(int algorithm) {
		String output = "" + algorithm + " "; 								// 1  - alg
		output += String.format("%5d ", dt.getBlockSize()); 				// 2  - bsize
		output += String.format("%5.0f ", dt.getElapsedTime()); 			// 3  - time
		output += String.format("%3d ", dt.getCallbackTicks());				// 4  - cbt
		output += String.format("%5d ", dt.getReadTicks());					// 5  - read tics
		output += String.format("%8.4f ", dt.getSampleReadMeanTime());		// 6  - sample read
		output += String.format("%8.4f ", dt.getSampleWriteMeanTime());		// 7  - sample write
		output += String.format("%8.4f ", dt.getBlockPeriod());				// 8  - block period (calculated)
		output += String.format("%8.4f ", dt.getCallbackPeriodMeanTime());	// 9  - callback period
		output += String.format("%8.4f ", dt.getDspPerformMeanTime());		// 10 - perform time
		output += String.format("%8.4f \n", dt.getDspCallbackMeanTime());	// 11 - callback time
		return output.getBytes();
	}
	
	/**
	 * 
	 * @return
	 */
	private String getFileName() {
		// Generate file name
		StringBuffer sbuf = new StringBuffer(fileName);
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		sdf.format(new Date(), sbuf, new FieldPosition(0));
		sbuf.append(".txt");
		return sbuf.toString();
	}

	/**
	 * 
	 * @throws IOException
	 */
	void updateExternalStorageState() throws IOException {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (!mExternalStorageWriteable)
			throw new IOException();
	}

	/**
	 *
	 */
	void updateScreenInfo(int bsize, int algorithm) {
		if (algorithm == 0)
			algorithmName.setText("Loopback:  ");
		else if (algorithm == 1)
			algorithmName.setText("Reverb:  ");
		else if (algorithm == 2)
			algorithmName.setText("FFT:  ");
		else if (algorithm == 4)
			algorithmName.setText("Stress:  ");
		blockSizeView.setText(String.valueOf(bsize));
	}

	
	/**
	 * 
	 */
	private void initTests() {
		// Opens results file
		os = getOutputStream();
		StressThread mt = new StressThread(mHandler);
		mt.start();
	}
	
	/**
	 * 
	 * @param bSize
	 * @param alg
	 */
	private void launchTest() {
		try {
			is = getResources().openRawResourceFd(
					R.raw.alien_orifice).createInputStream();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dt = new DspThread(blockSize, algorithm, is, MAX_DSP_CYCLES, filterSize);
		dt.setParams(0.5);
		dt.start();
	}


	/************************************************************************
	 * message handler.
	 ***********************************************************************/
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String action = msg.getData().getString("action");
			if (action.equals("finish-tests"))
				finishTests();
			else if (action.equals("launch-test")) {
				algorithm = msg.getData().getInt("algorithm");
				blockSize = msg.getData().getInt("block-size");
				filterSize = msg.getData().getInt("filter-size");
				updateScreenInfo(blockSize, algorithm);
				Log.i("launchTest", "launching with filterSize="+filterSize);
				launchTest();
			} else if (action.equals("release-test")) {
				releaseTest();
			}
		}
	};
	

	/**
	 * 
	 */
	private void releaseTest() {
		// write results
		try {
			os.write(getDspThreadInfo(algorithm));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// close the input stream
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		is = null;
		releaseDspThread();
	}

	/**
	 * 
	 */
	private void finishTests() {
		// Close the output file
		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		turnOff();
	}
	
	/**
	 * 
	 */
	private void releaseDspThread() {
		if (dt != null) {
			dt.releaseIO();
			dt.stopRunning();
			dt = null;
		}
	}


	/**
	 * 
	 * @author andre
	 * 
	 */
	public class StressThread extends Thread {

		private Handler mHandler;

		public StressThread(Handler handler) {
			mHandler = handler;
			algorithm = 4;
		}

		@Override
		public void run() {

			try {
				// iterate through block sizes
				for (blockSize = startBlockSize; blockSize <= endBlockSize; blockSize *= 2) {

					int m = 1; // 2 ** 5
					int M = (int) Math.pow(2,14); 			// 2 ** 14
					int filterSize = 1;
					boolean performedWell = true;
					boolean reachedPeak = false;

					// invariant: the device performs well on filters with m coefficients
					// and bad with filters with M coefficients
					while(m < M-1) {
						Log.e("***INFO***", "m="+m);
						Log.e("***INFO***", "filterSize="+filterSize);
						Log.e("***IFO***", "M="+M);
						//===============================================
						// calc new block size
						//===============================================
						if (performedWell == true)
							if (!reachedPeak)
								filterSize *= 2;
							else
								filterSize = (int) (m + ((double) (M-m) / 2));
						else
							filterSize = (int) (M - ((M-m) / 2));

						//===============================================
						// run test
						//===============================================
						// launch test
						Message msg = mHandler.obtainMessage();
						Bundle b = new Bundle();
						b.putString("action", "launch-test");
						b.putInt("block-size", blockSize);
						b.putInt("algorithm", 4);
						b.putInt("filter-size", filterSize);
						msg.setData(b);
						mHandler.sendMessage(msg);

						// wait for test to start
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}

						// wait for test to end
						while (dt.isRunning())
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								Log.e("ERROR", "Thread was Interrupted");
							}

						//===============================================
						// get performance results
						//===============================================
						if (dt.getCallbackPeriodMeanTime() < dt.getBlockPeriod()) {
							performedWell = true;
							m = filterSize;
							Log.e("***K***", "com filterSize="+m+" rola!");
						}
						else {
							reachedPeak = true;
							performedWell = false;
							M = filterSize;
							Log.e("***K***", "com filterSize="+M+" ***NAO*** rola!");
						}
						

						//===============================================
						// clean trash before running next test
						//===============================================
						// release test
						msg = mHandler.obtainMessage();
						b = new Bundle();
						b.putString("action", "release-test");
						msg.setData(b);
						mHandler.sendMessage(msg);
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}
						System.gc();
						// wait for garbage collector
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							Log.e("ERROR", "Thread was Interrupted");
						}
					}
					
					// increase status bar
					progressBar
					.setProgress((int) ((Math.log(blockSize) / Math.log(2))-4) * 10);
					continue;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// Turn off when done.
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putString("action", "finish-tests");
			msg.setData(b);
			mHandler.sendMessage(msg);
		}



	}

}