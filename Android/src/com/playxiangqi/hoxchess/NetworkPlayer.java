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
import java.nio.channels.UnresolvedAddressException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    
    private String pid_;
    private String password_;

    private StringBuilder inData_ = new StringBuilder(10*1024);
    
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
    
    // Network error codes.
    public static final int NETWORK_CODE_CONNECTED = 1;
    public static final int NETWORK_CODE_UNRESOLVED_ADDRESS = 2;
    public static final int NETWORK_CODE_IO_EXCEPTION = 3;
    
    // -------------------------------------------------------------------------------------
    private Handler handler_;

    private static final int MSG_NETWORK_CONNECT_TO_SERVER = 1;
    private static final int MSG_NETWORK_DISCONNECT_FROM_SERVER = 2;
    private static final int MSG_NETWORK_CHECK_FOR_WORK = 3;
    private static final int MSG_NETWORK_SEND_LIST = 4;
    private static final int MSG_NETWORK_SEND_JOIN = 5;
    private static final int MSG_NETWORK_SEND_LEAVE = 6;
    private static final int MSG_NETWORK_SEND_MOVE = 7;
    private static final int MSG_NETWORK_SEND_DRAW = 8;
    private static final int MSG_NETWORK_SEND_RESIGN = 9;
    private static final int MSG_NETWORK_SEND_NEW = 10;
    private static final int MSG_NETWORK_SEND_RESET = 11;
    
    @SuppressLint("HandlerLeak") @Override
    public void run() {
        Log.d(TAG, "Running...");
        Looper.prepare();

        handler_ = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case MSG_NETWORK_CONNECT_TO_SERVER:
                            handleConnectToServer();
                            break;
                            
                        case MSG_NETWORK_DISCONNECT_FROM_SERVER:
                            handleDisconnectFromServer();
                            break;
    
                        case MSG_NETWORK_CHECK_FOR_WORK:
                            handleCheckForWork();
                            break;

                        case MSG_NETWORK_SEND_LIST:
                            send_LIST();
                            break;

                        case MSG_NETWORK_SEND_JOIN:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_JOIN(map);
                            break;
                        }

                        case MSG_NETWORK_SEND_LEAVE:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_LEAVE(map);
                            break;
                        }

                        case MSG_NETWORK_SEND_MOVE:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_MOVE(map);
                            break;
                        }
                        
                        case MSG_NETWORK_SEND_DRAW:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_DRAW(map);
                            break;
                        }
                        
                        case MSG_NETWORK_SEND_RESIGN:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_RESIGN(map);
                            break;
                        }
           
                        case MSG_NETWORK_SEND_NEW:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_NEW(map);
                            break;
                        }
                        
                        case MSG_NETWORK_SEND_RESET:
                        {
                            @SuppressWarnings("unchecked")
                            Map<String, String> map = (Map<String, String>) msg.obj;
                            send_RESET(map);
                            break;
                        }
                        
                        default:
                            break;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "An IOException exception while handling network messages.");
                    connectionState_ = ConnectionState.CONNECTION_STATE_NONE;
                    if (!disconnectionRequested_) {
                        disconnectionRequested_ = true;
                    }
                    HoxApp.getApp().postNetworkCode(NETWORK_CODE_IO_EXCEPTION);
                    return;
                }
            }
        };
        Looper.loop();
    }
    
    // -------------------------------------------------------------------------------------
    NetworkPlayer() {
       // empty
    }
    
    public void setLoginInfo(String pid, String password) {
        pid_ = pid;
        password_ = password;
    }
    
    public boolean isOnline() {
        return (connectionState_ != ConnectionState.CONNECTION_STATE_NONE);
    }
    
    // -------------------------------------------------------------------------------------
    public void connectToServer() {
        Log.d(TAG, "Connect to server...");
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_CONNECT_TO_SERVER));
    }

    public void disconnectFromServer() {
        Log.d(TAG, "Disconnect from server...");
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_DISCONNECT_FROM_SERVER));
    }
    
    public void sendRequest_LIST() {
        Log.d(TAG, "Send 'LIST' request to server...");
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_LIST));
    }

    public void sendRequest_JOIN(String tableId, String joinColor) {
        Log.d(TAG, "Send 'JOIN' to server... TableId: " + tableId + ", joinColor: " + joinColor);
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        map.put("color", joinColor);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_JOIN, map));
    }

    public void sendRequest_LEAVE(String tableId) {
        Log.d(TAG, "Send 'LEAVE' request to server...");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_LEAVE, map));
    }

    public void sendRequest_DRAW(String tableId) {
        Log.d(TAG, "Send 'DRAW' request to server...");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_DRAW, map));
    }

    public void sendRequest_RESIGN(String tableId) {
        Log.d(TAG, "Send 'RESIGN' request to server...");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_RESIGN, map));
    }

    public void sendRequest_RESET(String tableId) {
        Log.d(TAG, "Send 'RESET' request to server...");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_RESET, map));
    }
    
    public void sendRequest_MOVE(String tableId, String move) {
        Log.d(TAG, "Send 'MOVE' request to server...");
        Map<String, String> map = new HashMap<String, String>();
        map.put("tid", tableId);
        map.put("move", move);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_MOVE, map));
    }
    
    public void sendRequest_NEW(String itimes) {
        Log.d(TAG, "Send 'NEW (table)' request to server. itimes = " + itimes);
        Map<String, String> map = new HashMap<String, String>();
        map.put("itimes", itimes);
        handler_.sendMessage(handler_.obtainMessage(MSG_NETWORK_SEND_NEW, map));
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
        try {
            socketChannel_.connect(new InetSocketAddress("games.playxiangqi.com", 80));
        } catch (UnresolvedAddressException ex) {
            Log.e(TAG, "UnresolvedAddressException caught while connecting.");
            connectionState_ = ConnectionState.CONNECTION_STATE_NONE;
            HoxApp.getApp().postNetworkCode(NETWORK_CODE_UNRESOLVED_ADDRESS);
            return;
        }
        Log.d(TAG, "... Continue with connecting....");
        
        while( (! socketChannel_.finishConnect()) || disconnectionRequested_ ) {
            try { sleep(1000); } catch (InterruptedException e) { }
        }
        connectionState_ = ConnectionState.CONNECTION_STATE_CONNECTED;
        Log.d(TAG, "... Connection established!");
        HoxApp.getApp().postNetworkCode(NETWORK_CODE_CONNECTED);
        
        handler_.sendMessageDelayed(handler_.obtainMessage(MSG_NETWORK_CHECK_FOR_WORK), 1000);
        
        Log.i(TAG, "Handle 'Connect to server'... DONE *****");
    }
    
    private void handleCheckForWork() throws IOException {
        //Log.d(TAG, "Handle 'Check for work'...");
            
        int readyChannels = selector_.select(1000); // 1-second interval
        if (readyChannels == 0) {
            // Do nothing
        } else {
            Set<SelectionKey> selectedKeys = selector_.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isConnectable()) {
                    Log.d(TAG, "a connection was established with a remote server.");

                } else if (key.isReadable()) {
                    Log.d(TAG, "a channel is ready for reading");
                    try {
                        readIncomingData();
                    } finally {
                        processIncomingData(); // always process whatever data collected so far.
                    }

                } else if (key.isWritable()) {
                    //Log.d(TAG, "a channel is ready for writing");
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
            handler_.sendMessageDelayed(handler_.obtainMessage(MSG_NETWORK_CHECK_FOR_WORK), 2000);
        }
        
        //Log.d(TAG, "Handle 'Check for work'... DONE *****");
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

    private void send_LIST() throws IOException {
        String request = "op=LIST&pid=" + pid_;
        sendRequest(request);
    }

    private void send_JOIN(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        final String joinColor = map.get("color");
        String request = "op=JOIN&pid=" + pid_ + "&tid=" + tableId + "&color=" + joinColor;
        sendRequest(request);
    }

    private void send_LEAVE(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        String request = "op=LEAVE&pid=" + pid_ + "&tid=" + tableId;
        sendRequest(request);
    }

    private void send_MOVE(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        final String move = map.get("move");
        String request = "op=MOVE&pid=" + pid_ + "&tid=" + tableId  + "&move=" + move;
        sendRequest(request);
    }
    
    private void send_DRAW(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        String request = "op=DRAW&pid=" + pid_ + "&tid=" + tableId;
        sendRequest(request);
    }

    private void send_RESIGN(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        String request = "op=RESIGN&pid=" + pid_ + "&tid=" + tableId;
        sendRequest(request);
    }

    private void send_NEW(Map<String, String> map) throws IOException {
        final String itimes = map.get("itimes");
        String request = "op=NEW&pid=" + pid_ + "&itimes=" + itimes;
        sendRequest(request);
    }
    
    private void send_RESET(Map<String, String> map) throws IOException {
        final String tableId = map.get("tid");
        String request = "op=RESET&pid=" + pid_ + "&tid=" + tableId;
        sendRequest(request);
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
        Log.i(TAG, "READ (data): Enter. inData_ 's length = " + inData_.length());
        
        ByteBuffer buf = ByteBuffer.allocate(1*1024);
        
        while (true) {
            int bytesRead = socketChannel_.read(buf); // read into buffer.
            Log.d(TAG, "READ (data): ... bytesRead = " + bytesRead);
            if (bytesRead == -1) {
                //
                // NOTE: It appears that we at least should handle the (-1) return value:
                //  http://stackoverflow.com/questions/3484972/java-socketchannel-doesnt-detect-disconnection
                //  http://stackoverflow.com/questions/14010194/detecting-socket-disconnection
                //
                Log.w(TAG, "READ (data): ... bytesRead returns -1, which is an orderly close.");
                throw new IOException("READ returns -1 (an orderly close detected)");
                
            } else if (bytesRead <= 0) {
                break;
            }
            
            buf.flip();  // make buffer ready for read
            while (buf.hasRemaining()) {
                inData_.append((char) buf.get()); // read 1 byte at a time
            }
            buf.clear(); // make buffer ready for writing
        }
        
        Log.i(TAG, "READ (data): End. inData_ 's length = " + inData_.length());
    }
    
    private void processIncomingData() {
        Log.i(TAG, "Process (data): Enter. inData_ 's length = " + inData_.length());
        
        final int length = inData_.length();
        boolean bSawOne = false;  // just saw one '\n'?
        char currentChar; // The current character being examined.
        int startIndex = 0;
        for (int index = startIndex; index < length; index++) {
            currentChar = inData_.charAt(index);
            
            if (!bSawOne && currentChar == '\n') {
                bSawOne = true;
            } else if (bSawOne && currentChar == '\n') { // the end mark "\n\n" of an event?
                final String anEvent = inData_.substring(startIndex, index-1);
                //Log.i(TAG, "Process (data): ... got an event = [" + anEvent + "].");
                startIndex = index + 1;
                
                HoxApp.getApp().postNetworkEvent(anEvent);
                
            } else if (bSawOne) {
                bSawOne = false;
            }
        }
        
        inData_.delete(0, startIndex);
        Log.i(TAG, "Process (data): End. inData_ 's length = " + inData_.length());
    }
}
