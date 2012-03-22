package br.usp.ime.dspbenchmarking;

import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/************************************************************************
 * SystemWatchThread Monitors system parameters and sends messages to the
 * main thread.
 ***********************************************************************/
public class SystemWatchThread extends Thread {

	// Class constants defining state of the thread
	private boolean isRunning = false;

	Handler mHandler;
	int total;
	private int cpuUsage;
	

	// Constructor with an argument that specifies Handler on main thread
	// to which messages will be sent by this thread.
	SystemWatchThread(Handler h) {
		mHandler = h;
	}

	// This is called when the thread starts. It monitors device parameters
	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			// The method Thread.sleep throws an InterruptedException if
			// Thread.interrupt()
			// were to be issued while thread is sleeping; the exception
			// must be caught.
			float usage = readUsage();
			cpuUsage = (int) (usage * 100.0);
			if (cpuUsage >= 100)
				cpuUsage = 99;

			try {
				// Control speed of update (but precision of delay not
				// guaranteed)
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.e("ERROR", "Thread was Interrupted");
			}

			// Send message (with current value of total as data) to Handler
			// on UI thread
			// so that it can update the progress bar.

			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("total", total);
			msg.setData(b);
			mHandler.sendMessage(msg);

			total--; // Count down
		}
	}

	
	public int getCpuUsage() {
		return cpuUsage;
	}
	
	// Set current state of thread (use state=ProgressThread.DONE to stop
	// thread)
	public boolean stopRunning() {
		if (isRunning == false)
			return false;
		isRunning = false;
		return true;
	}
	
	/************************************************************************
	 * Gets CPU usage from /proc/stat.
	 ***********************************************************************/
	private float readUsage() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			try {
				Thread.sleep(360);
			} catch (Exception e) {
				e.printStackTrace();
			}

			reader.seek(0);
			load = reader.readLine();
			reader.close();

			toks = load.split(" ");

			long idle2 = Long.parseLong(toks[5]);
			long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			return (float) (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return 0;
	}
}