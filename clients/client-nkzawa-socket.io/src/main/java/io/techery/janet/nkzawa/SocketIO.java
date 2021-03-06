package io.techery.janet.nkzawa;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import io.techery.janet.AsyncClient;

public class SocketIO extends AsyncClient {

    private Socket socket;

    private Set<String> events = new HashSet<String>();

    @Override protected boolean isConnected() {
        return socket != null && socket.connected();
    }

    @Override protected void connect(final String url, final boolean reconnectIfConnected) throws Throwable {
        if (isConnected()) {
            if (reconnectIfConnected) {
                socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override public void call(Object... args) {
                        try {
                            connect(url, reconnectIfConnected);
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
                });
                socket.disconnect();
                socket = null;
            } else {
                callback.onConnect();
            }
            return;
        }
        socket = IO.socket(url);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override public void call(Object... args) {
                callback.onConnect();
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override public void call(Object... args) {
                String reason = null;
                if (args.length > 0) {
                    reason = String.valueOf(args[0]);
                }
                callback.onDisconnect(reason);
            }
        }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override public void call(Object... args) {
                Throwable throwable = null;
                if (args.length > 0) {
                    throwable = (Throwable) args[0];
                }
                callback.onError(throwable);
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override public void call(Object... args) {
                Throwable throwable = null;
                if (args.length > 0) {
                    throwable = (Throwable) args[0];
                }
                callback.onConnectionError(throwable);
            }
        });
        for (final String event : events) {
            socket.on(event, new Emitter.Listener() {
                @Override public void call(Object... args) {
                    if (args.length > 0) {
                        Object value = args[0];
                        callback.onMessage(event, String.valueOf(value));
                    } else {
                        callback.onMessage(event, (String) null);
                    }
                }
            });
        }
        socket.connect();
    }

    @Override protected void disconnect() throws Throwable {
        if (isConnected()) {
            socket.disconnect();
            for (String event : events) {
                socket.off(event);
            }
        } else {
            callback.onDisconnect("not connected");
        }
    }

    @Override protected void send(String event, String payload) throws Throwable {
        if (!isConnected()) return;
        //
        Object obj;
        try {
            obj = new JSONObject(payload);
        } catch (JSONException e) {
            obj = payload;
        }
        socket.emit(event, obj);
    }

    @Override protected void send(String event, byte[] payload) throws Throwable {
        if (isConnected()) {
            socket.emit(event, new Object[]{payload});
        }
    }

    @Override protected void subscribe(final String event) {
        events.add(event);
    }
}
