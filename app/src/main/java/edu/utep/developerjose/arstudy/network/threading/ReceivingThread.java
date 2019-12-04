package edu.utep.developerjose.arstudy.network.threading;

import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import edu.utep.developerjose.arstudy.network.NetManager;
import edu.utep.developerjose.arstudy.network.data.Packet;

public class ReceivingThread extends Thread {
    private static final String TAG = "ARStudy-ReceivingThread";
    private Socket clientSocket;
    private ObjectInputStream inputStream;

    public ReceivingThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            inputStream = new ObjectInputStream(new DataInputStream(clientSocket.getInputStream()));
            while (true) {
                if (!NetManager.isRunning)
                    break;

                Packet input = Packet.read(inputStream);
                NetManager.broadcastData(input);
                Log.d(TAG, "Received packet ID: " + input.ID);
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error while processing receiving thread " + ex.getMessage());
        } finally {
            try {
                if (clientSocket != null)
                    clientSocket.close();
                if (inputStream != null)
                    inputStream.close();
                NetManager.broadcastDisconnect();
                NetManager.isRunning = false;
            } catch (IOException ex) {
                Log.d(TAG, "Error while closing receiving socket " + ex.getMessage());
            }
        }
    }
}
