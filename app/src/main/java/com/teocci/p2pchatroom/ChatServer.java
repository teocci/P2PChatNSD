package com.teocci.p2pchatroom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.util.Log;

public class ChatServer {
	protected ReceiverHandler rh;
	protected SenderHandler sh;
	protected final UICallbacks cbs; 
    private final String TAG = "ChatServer";
    private Thread mLoopThread = null;
    public Thread getmLoopThread() {
		return mLoopThread;
	}

	private Socket clientSock = null;
	//use this map to maintain globally available client lists, must be synchronized
    protected Map<String, ChatNetResourceBundle> mChatClientResources;
	private ServerSocket sSock = null;
    
	public ChatServer(final UICallbacks calls) {
		cbs = calls;
		mChatClientResources = Collections.synchronizedMap(new HashMap<String, ChatNetResourceBundle>());
        initLoopers();
        
	}
	
	protected void initLoopers() {
		if (sh != null &&sh.isAlive()) return;
		if (rh != null &&rh.isAlive()) return;
		sh = new SenderHandler(new SenderHandler.HandlerCallbacks() {
			@Override
			public void doneSendingMessage(final String msg) {
				cbs.sendMessageToUI("Me said: "+msg);	
			}
		});
		//After received new client immediately start listening to it by posting new Handler Message
		rh = new ReceiverHandler(new ReceiverHandler.HandlerCallbacks() {
			@Override
			public void hadReceivedNewMessage(final String bundle, final String message) {
				cbs.sendMessageToUI(bundle+" said : "+message);
			}
			@Override
			public void onReceiverReadingError(final String uid) {
				//the pipe is broken, need to remove
				if (mChatClientResources.containsKey(uid)) {
					mChatClientResources.get(String.valueOf(uid)).cleanUp();
					mChatClientResources.remove(String.valueOf(uid));
					//cbs.notifyErrors(1);
				}
			}
		});
		sh.start();
		rh.start();
		sh.getLooper();
		rh.getLooper();
		Log.i(TAG, "Server successfully created!!");	
	}

	protected void init() throws IOException{
		//initiates server socket accepting loop:
		if (sSock != null) {
			try {
				sSock.close();
				//only wait for three seconds until it dies
				mLoopThread.join(3000);
	    	} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
		sSock  = new ServerSocket(ChatActivity.PORT);
		
		runLoop(sSock);
		Log.i(TAG, "Server successfully initialized!!");
	}
	@SuppressWarnings("resource")
    void runLoop(final ServerSocket sSock) {
//		    if (mLoopThread != null&& mLoopThread.isAlive()) {
//		    	try {
//					sSock.close();
//					//only wait for three seconds until it dies
//					mLoopThread.join(3000);
//		    	} catch (IOException e) {
//					e.printStackTrace();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}	
//		    }
			mLoopThread = new Thread(new Runnable() {
				@Override
				public void run() {
					
					try {
						do {//run loop for server
							clientSock = sSock.accept();
							//first check if clientSocket already exists in list
							//if client exits it will be replaced with the new one so there will be no duplicates for same client
							//if (mChatClientResources.containsKey(clientSock.getInetAddress().getHostAddress()))
							//	continue; this two lines commmented coz our HashMap can replace duplicate on IP address
							//after we have a new client socket initialize tcp resource
							final ChatNetResourceBundle cnrb = new ChatNetResourceBundle(clientSock); 

							mChatClientResources.put(new String(cnrb.getUID()), cnrb);
							if (cbs != null) //there are chances that cbs is null we cannot avoid it
								cbs.sendMessageToUI("New client connected!! ["+clientSock.getInetAddress().toString()+"]");
							//for each client to maintain a place in event loop, the first message must be fired to trigger subsequent messages.
							//rh.postNewMessage(cnrb);
						}while (true);
					} catch (IOException e) { //if we have exception we are out!!
							Log.e(TAG, "having exception in accepting Loops now we are out!!!");
							e.printStackTrace();
					} finally{//do we really can notify error here? it can trigger another request for server
							cbs.notifyErrors(1);
					}
				}
			});	
			mLoopThread.start();
	}
	public interface UICallbacks {
		public void sendMessageToUI(final String msg);

		public void notifyErrors(int errCode);
	}
	public void cleanUp() {
		//clean up Server Loop, Receiver, Sender Looper thread
		try {
			if (sSock!=null) sSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//mLoopThread.interrupt();
		sh.cleanUp();
		rh.cleanUp();
		//clean up cache
		Set<String> keySet = mChatClientResources.keySet();
		for (Iterator<String> i = keySet.iterator(); i.hasNext(); ) {
			mChatClientResources.get(i.next()).cleanUp();
		}
		mChatClientResources.clear();
	}
	
	public void sendMessages( final String msg) {
		//Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
		if (sh != null) {
		   //Loop through all available clients and sending message for each one
		  Log.i(TAG, "before sending message "+msg+" from "+this.getClass().toString());
		  Set<String> keySet = mChatClientResources.keySet();
		  for (Iterator<String> i = keySet.iterator(); i.hasNext(); ) {
			  //here need to create new copy to make sure no synchronization issue
			  ChatNetResourceBundle bundle = ChatNetResourceBundle.clone(mChatClientResources.get(i.next()));
			  bundle.setMessage(msg);
			  sh.postNewMessage(bundle);
		  }
		}
	}
	
	private void debugMessage(final String msg) {
		String className = ChatServer.this.toString();
		cbs.sendMessageToUI("Debug from ["+className.substring(className.indexOf("Chat"))+"]: "+msg);
	}

	public boolean needToInitThread() {
		// TODO Auto-generated method stub
		if (mLoopThread == null) return true;
		else
			return !mLoopThread.isAlive();
	}
}