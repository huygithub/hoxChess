/**
 *  Copyright 2014 Huy Phan <huyphan@playxiangqi.com>
 * 
 *  This file is part of HOXChess.
 * 
 *  HOXChess is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  HOXChess is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.playxiangqi.hoxchess;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * A network player
 * 
 * References:
 *   http://tutorials.jenkov.com/java-nio/index.html (Java NIO Tutorial)
 *   http://developer.android.com/reference/java/nio/channels/SocketChannel.html
 *   http://developer.android.com/reference/java/nio/channels/Selector.html
 */
class NetworkPlayer extends Thread {
    
    private static final String TAG = "NetworkPlayer";
    
    private static final String APP_VERSION = "AOXChess-1.0";
    
    private Selector selector_;
    private SocketChannel socketChannel_;
    private boolean disconnectionRequested_ = false;

    private String pid_ = "_THE_PLAYER_ID_"; // FIXME: ...
    private String password_ = "_THE_PLAYER_PASSWORD_"; // FIXME: ....

    // -------------------------------------------------------------------------------------
    private enum ConnectionState {
        CONNECTION_STATE_NONE,
        CONNECTION_STATE_CONNECTING,
        CONNECTION_STATE_CONNECTED,
        CONNECTION_STATE_LOGIN,
        //CONNECTION_STATE_LOGOUT,
        //CONNECTION_STATE_DISCONNECTING
    }
    private ConnectionState connectionState_ = ConnectionState.CONNECTION_STATE_NONE;
    
    // -------------------------------------------------------------------------------------
    private Handler handler_;

    private static final int MSG_NETWORK_CONNECT_TO_SERVER = 1;
    private static final int MSG_NETWORK_DISCONNECT_FROM_SERVER = 2;
    private static final int MSG_NETWORK_CHECK_FOR_WORK = 3;
    
    @SuppressLint("HandlerLeak") @Override
    public void run() {
        Log.d(TAG, "Running...");
        Looper.prepare();

        handler_ = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case MSG_NETWORK_CONNECT_TO_SERVER:
                            Log.d(TAG, "(Handler) Got the request to 'Connect to server'...");
                            handleConnectToServer();
                            break;
                            
                        case MSG_NETWORK_DISCONNECT_FROM_SERVER:
                            Log.d(TAG, "(Handler) Got the request to 'Disconnect from server'...");
                            handleDisconnectFromServer();
                            break;
                            
    
                        case MSG_NETWORK_CHECK_FOR_WORK:
                            Log.d(TAG, "(Handler) Got the request to 'Check for work'...");
                            handleCheckForWork();
                            break;
                            
                        default:
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        };
        Looper.loop();
    }
    
    public void connectToServer() {
        Log.d(TAG, "Connect to server...");
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_CONNECT_TO_SERVER));
    }

    public void disconnectFromServer() {
        Log.d(TAG, "Disconnect from server...");
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_DISCONNECT_FROM_SERVER));
    }
    
    private void handleConnectToServer() throws IOException {
        Log.i(TAG, "Handle 'Connect to server'...");
        
        if (connectionState_ != ConnectionState.CONNECTION_STATE_NONE) {
            return;
        }
        
        connectionState_ = ConnectionState.CONNECTION_STATE_CONNECTING;
        disconnectionRequested_ = false;
        
        selector_ = Selector.open();
        
        socketChannel_ = SocketChannel.open();
        socketChannel_.configureBlocking(false);
        
        /*SelectionKey selectionKey =*/ socketChannel_.register(selector_,
                SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        
        socketChannel_.connect(new InetSocketAddress("games.playxiangqi.com", 80));
        
        Log.d(TAG, "... Continue with connecting....");
        
        while( (! socketChannel_.finishConnect()) || disconnectionRequested_ ) {
            try { sleep(1000); } catch (InterruptedException e) { }
        }
        connectionState_ = ConnectionState.CONNECTION_STATE_CONNECTED;
        Log.d(TAG, "... Connection established!");
        
        handler_.sendMessageDelayed(handler_.obtainMessage(MSG_NETWORK_CHECK_FOR_WORK), 1000);
        
        Log.i(TAG, "Handle 'Connect to server'... DONE *****");
    }
    
    private void handleCheckForWork() throws IOException {
        Log.i(TAG, "Handle 'Check for work'...");
            
        for (int tries = 1; tries <= 1 /* HACK */; tries++) {
            Log.d(TAG, "........ tries = " + tries);
            try { sleep(1000); } catch (InterruptedException e) { }
            
            int readyChannels = selector_.select(1000); // 1-second interval
            if (readyChannels == 0) {
                continue;
            }
            
            Set<SelectionKey> selectedKeys = selector_.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isConnectable()) {
                    Log.d(TAG, "a connection was established with a remote server.");

                } else if (key.isReadable()) {
                    Log.d(TAG, "a channel is ready for reading");
                    readIncomingData();

                } else if (key.isWritable()) {
                    Log.d(TAG, "a channel is ready for writing");
                    if (connectionState_ == ConnectionState.CONNECTION_STATE_CONNECTED) {
                        send_LOGIN();
                    } else if (connectionState_ == ConnectionState.CONNECTION_STATE_LOGIN
                            && disconnectionRequested_) {
                        send_LOGOUT();
                    }
                }

                keyIterator.remove();
            }
        }
        
        if (disconnectionRequested_) {
            Log.i(TAG, "Handle 'Connect to server'... Handle disconnection");
            if (socketChannel_ != null) {
                Log.i(TAG, "Handle 'Connect to server'... Closing the channel!");
                socketChannel_.close();
                socketChannel_ = null;
                connectionState_ = ConnectionState.CONNECTION_STATE_NONE;
            }
            disconnectionRequested_ = false;
        } else {
            handler_.sendMessageDelayed(handler_.obtainMessage(MSG_NETWORK_CHECK_FOR_WORK), 1000);
        }
        
        Log.i(TAG, "Handle 'Check for work'... DONE *****");
    }
    
    private void handleDisconnectFromServer() {
        Log.i(TAG, "Handle 'Disconnect from server'...");

        if (!disconnectionRequested_) {
            disconnectionRequested_ = true;
        }
        
        Log.i(TAG, "Handle 'Disconnect from server'... DONE *****");
    }
    
    private void send_LOGIN() throws IOException {
        Log.i(TAG, "LOGIN: Enter");
        
        String request = "op=LOGIN&version=" + APP_VERSION +
                "&pid=" + pid_ + "&password=" + password_;
        sendRequest(request);
        
        connectionState_ = ConnectionState.CONNECTION_STATE_LOGIN;
        Log.i(TAG, "LOGIN: End");
    }
    
    private void send_LOGOUT() throws IOException {
        Log.i(TAG, "LOGOUT: Enter");
        
        String request = "op=LOGOUT&pid=" + pid_;
        sendRequest(request);
        
        connectionState_ = ConnectionState.CONNECTION_STATE_CONNECTED;
        Log.i(TAG, "LOGOUT: End");
    }
    
    /**
     * A helper to send a request to the server.
     */
    private void sendRequest(String request) throws IOException {
        Log.d(TAG, "Send request: Enter");

        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.clear();
        
        final String fullRequest = request + "\n";
        buf.put(fullRequest.getBytes());

        buf.flip();

        while (buf.hasRemaining()) {
            int bytesWritten = socketChannel_.write(buf);
            Log.v(TAG, " ... bytesWritten = " + bytesWritten);
        }
        
        Log.d(TAG, "Send request: End");
    }
    
    private void readIncomingData() throws IOException {
        Log.i(TAG, "READ (data): Enter");
        
        StringBuffer inData = new StringBuffer(10*1024);
        
        ByteBuffer buf = ByteBuffer.allocate(1*1024);
        int bytesRead = socketChannel_.read(buf); // read into buffer.
        Log.d(TAG, "READ (data): .... bytesRead = " + bytesRead);
        while (bytesRead > 0) {
            buf.flip();  // make buffer ready for read

            while (buf.hasRemaining()) {
                inData.append((char) buf.get()); // read 1 byte at a time
            }

            buf.clear(); // make buffer ready for writing
            bytesRead = socketChannel_.read(buf);
            Log.d(TAG, "READ (*data): .... bytesRead = " + bytesRead);
        }
        
        Log.i(TAG, "READ (data): End. inData = " + inData.toString());
    }
}
