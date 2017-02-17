package com.teocci.p2pchatroom;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;

public class ReceiverHandler extends HandlerThread {
	 private final static String TAG = "ReceiverThread";
	 private final static int REC = 1;
	 Handler mHandler = null;
	 BufferedReader netInput = null;
	 HandlerCallbacks handlerCallbacks = null;
     
     public interface HandlerCallbacks {
    	 void hadReceivedNewMessage(final String bundle, final String message);
		 void onReceiverReadingError(final String uid);
     }
		
	    //main worker method
	    public void postNewMessage(final ChatNetResourceBundle msg) {
	    	while (mHandler == null) {
	    		Log.e(TAG, "we are blocked here waiting for handler to become available !!!");
	    		try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	    	}
	    	mHandler.obtainMessage(REC, msg).sendToTarget();
	    }
	    
		public ReceiverHandler(HandlerCallbacks hcb) {
			super(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
			handlerCallbacks = hcb;
		}
		@Override
		protected void onLooperPrepared() {
			mHandler = new Handler() {
			    @Override
			    public void handleMessage(Message msg) {
			    	final ChatNetResourceBundle message = (ChatNetResourceBundle)msg.obj;
			    	handleRecEvent(message);
			    }

				private void handleRecEvent(final ChatNetResourceBundle msg) {
					
					new Thread( new Runnable() {
						//blocking statement below
						@Override 
						public void run() {
							try {
								String message = msg.getInStream().readLine();
								//sending messages to mainthread
								if (message != null){
									handlerCallbacks.hadReceivedNewMessage(msg.getSocket().toString(), new String(message));
								}
							} catch (IOException e) {
								Log.e(TAG, "reading from stream error out!!!");
								e.printStackTrace();
								handlerCallbacks.onReceiverReadingError(msg.getUID());
								return;
							}
							//schedule another event
							mHandler.obtainMessage(REC, (Object)msg.clone(msg)).sendToTarget();;
						}
					}).start();
					
				}
		    };
		    //handler.sendEmptyMessage(0);
		}
		
		public void cleanUp() {
			//remove message, close socket and outputstream.
			if (mHandler != null) {
				mHandler.removeMessages(REC);
			}
			this.quit();
		}
}
