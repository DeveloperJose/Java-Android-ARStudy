package edu.utep.developerjose.arstudy.network.threading;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;

import edu.utep.developerjose.arstudy.network.NetManager;

public class ConnectionThread extends Thread {
    private static final String TAG = "ARStudy-ConnectionThread";
    private Socket clientSocket;

    public ConnectionThread(Socket clientSocket) {
        NetManager.broadcastConnect();
        NetManager.isRunning = true;
        this.clientSocket = clientSocket;
        new ReceivingThread(clientSocket).start();
        new SendingThread(clientSocket).start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (!NetManager.isRunning)
                    break;
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error while processing connection thread " + ex.getMessage());
        } finally {
            try {
                if (clientSocket != null)
                    clientSocket.close();
                NetManager.broadcastDisconnect();
                NetManager.isRunning = false;
                Log.d(TAG, "Closed connection thread");
            } catch (IOException ex) {
                Log.d(TAG, "Error while closing socket " + ex.getMessage());
            }
        }
    }
}
