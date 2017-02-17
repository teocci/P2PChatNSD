package com.teocci.p2pchatroom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import android.util.Log;

/**
 * ChatNetResourceBundle class is used to store all shared/cached resources
 * for persistent TCP connection
 */
public class ChatNetResourceBundle
{
    private static final String TAG = "ChatNetResourceBundle";
    // UID will be used as HashMap's key
    private String UID = "NULL";

    private Socket socket = null;
    private BufferedReader inStream = null;
    private PrintWriter outStream = null;
    // String will only be not null when creating instance to pass to EventLooper
    private String message = null;

    private boolean isDirty = false;
    public boolean isDirty()
    {
        return isDirty;
    }

    public void setDirty(boolean isDirty)
    {
        this.isDirty = isDirty;
    }

    public Socket getSocket()
    {
        return socket;
    }

    public BufferedReader getInStream()
    {
        return inStream;
    }

    public void setInStream(BufferedReader inStream)
    {
        this.inStream = inStream;
    }

    public PrintWriter getOutStream()
    {
        return outStream;
    }

    public void setOutStream(PrintWriter outStream)
    {
        this.outStream = outStream;
    }

    public void setMessage(String s)
    {
        message = s;
    }

    public String getMessage()
    {
        return message;
    }

    public String getUID()
    {
        return UID;
    }

    /**
     * Public constructor-- we need to two versions:
     * 1) initializes the Socket and Streams and then caches them
     *
     * @param sock Socket
     * @throws IOException
     */
    public ChatNetResourceBundle(Socket sock) throws IOException
    {
        socket = sock;
        UID = new String(sock.getInetAddress().getHostAddress());
        if (!openInStream()) {

            throw new IOException();
        }

        if (!openOutStream()) {

            throw new IOException();
        }
    }

    /**
     * 2) create new instance of this class to pass to Event Looper at that time we already have socket and streams cached
     *
     * @param sock Socket
     * @param in BufferedReader
     * @param out PrintWriter
     * @param msg String
     * @param uid String
     */
    public ChatNetResourceBundle(Socket sock, BufferedReader in, PrintWriter out, String msg, String uid)
    {
        socket = sock;
        inStream = in;
        outStream = out;
        message = msg;
        UID = uid;
    }

    /**
     * Return an exact copy of itself
     *
     * @param o ChatNetResourceBundle
     * @return
     */
    public static ChatNetResourceBundle clone(ChatNetResourceBundle o)
    {
        return new ChatNetResourceBundle(o.getSocket(), o.getInStream(), o.getOutStream(), o.getMessage(), o.getUID());
    }

    /**
     * methods to open and cache In/Out NetStream object
     * @return
     */
    public boolean openInStream()
    {
        try {
            inStream = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            Log.e(TAG, "InputStream - failed to open network stream!!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean openOutStream()
    {
        try {
            outStream = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
        } catch (IOException e) {
            Log.e(TAG, "OutputStream - failed to open network stream!!");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void cleanUp()
    {
        if (inStream != null) {
            try {
                inStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outStream != null) {
            outStream.close();
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
