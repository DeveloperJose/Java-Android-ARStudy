package edu.utep.developerjose.arstudy.network.threading;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import edu.utep.developerjose.arstudy.network.NetManager;
import edu.utep.developerjose.arstudy.network.data.Packet;

public class SendingThread extends Thread {
    private static final String TAG = "ARStudy-SendingThread";
    private Socket clientSocket;
    private ObjectOutputStream outputStream;

    public SendingThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        NetManager.broadcastConnect();
        NetManager.isRunning = true;

        try {
            outputStream = new ObjectOutputStream(new DataOutputStream(clientSocket.getOutputStream()));

            while (true) {
                if (!NetManager.isRunning)
                    break;

                Packet output = NetManager.packetList.take();
                output.send(outputStream);
                outputStream.flush();
                Log.d(TAG, "Sent packet");
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error while processing receiving thread " + ex.getMessage());
        } finally {
            try {
                if (clientSocket != null)
                    clientSocket.close();
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException ex) {
                Log.d(TAG, "Error while closing receiving socket " + ex.getMessage());
            }
        }
    }
}
