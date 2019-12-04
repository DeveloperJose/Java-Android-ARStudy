package edu.utep.developerjose.arstudy.network;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import edu.utep.developerjose.arstudy.network.data.Packet;

public class NetManager {
    public static final int PORT = 9000;
    private static final String TAG = "ARStudy-NetManager";

    private static final List<NetInterface> listeners = new ArrayList<>();
    public static final BlockingQueue<Packet> packetList = new ArrayBlockingQueue<Packet>(20);
    public static boolean isRunning = false;

    public static synchronized void sendData(Packet p) {
        try {
            packetList.put(p);
        } catch (InterruptedException e) {
            Log.d(TAG, "addPacket interrupted");
        }
    }

    public static void addListener(NetInterface netInterface) {
        if (!listeners.contains(netInterface))
            listeners.add(netInterface);
    }

    public static void removeListener(NetInterface netInterface) {
        listeners.remove(netInterface);
    }

    public static void clearListeners() {
        listeners.clear();
    }

    public static void broadcastConnect() {
        for (final NetInterface listener : listeners)
            listener.onConnect();
    }

    public static void broadcastDisconnect() {
        for (final NetInterface listener : listeners)
            listener.onDisconnect();
    }

    public static void broadcastData(final Packet p) {
        for (final NetInterface listener : listeners)
            listener.onReceiveData(p);
    }
}