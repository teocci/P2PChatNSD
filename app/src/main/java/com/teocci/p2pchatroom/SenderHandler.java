package com.teocci.p2pchatroom;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.PrintWriter;

public class SenderHandler extends HandlerThread
{
    private final static String TAG = "SenderThread";
    private final static int SEND = 0;
    static Handler handler = null;
    PrintWriter netOutput = null;
    HandlerCallbacks handlerCallback = null;

    /**
     * Main worker method
     *
     * @param msg ChatNetResourceBundle
     */
    public void postNewMessage(final ChatNetResourceBundle msg)
    {
        handler.obtainMessage(SEND, msg).sendToTarget();
    }

//	public synchronized void setmSocket(Socket mSock) {
//		if (mSocket != null) { //we previously had a connection do cleanup first
//			try {
//				//mark outputstream also
//				netOutput.close();
//				netOutput = null;
//				mSocket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			if (handler != null) {
//				handler.removeMessages(SEND);
//			}
//		}
//		this.mSocket = mSock;
//	}

    public interface HandlerCallbacks
    {
        void doneSendingMessage(final String msg);
    }

    public SenderHandler(HandlerCallbacks hcb)
    {
        super(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerCallback = hcb;
    }

    @Override
    protected void onLooperPrepared()
    {
        handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if (msg.what == SEND) {
                    Log.i(TAG, "Before handling SEND event");
                    final ChatNetResourceBundle message = (ChatNetResourceBundle) msg.obj;
                    handleSendEvent(message);
                }
            }

            private void handleSendEvent(ChatNetResourceBundle msg)
            {
                msg.getOutStream().println(msg.getMessage());
                handlerCallback.doneSendingMessage(msg.getMessage() + " to " + msg.getSocket().toString());
            }
        };
    }

    public void cleanUp()
    {
        if (handler != null) {
            handler.removeMessages(SEND);
        }
        this.quit();
    }
}
