package edu.utep.developerjose.arstudy;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.utep.developerjose.arstudy.network.NetInterface;
import edu.utep.developerjose.arstudy.network.NetManager;
import edu.utep.developerjose.arstudy.network.data.Packet;
import edu.utep.developerjose.arstudy.network.threading.ConnectionThread;


public class MainActivity extends AppCompatActivity implements NetInterface {
    private static final String TAG = "ARStudy-MainActivity";
    private static final double MIN_OPENGL_VERSION = 3.0;

    private Handler uxHandler;

    private RadioGroup uxConnectionType;
    private TextView uxTextStatus;
    private EditText uxEditIPAddress;
    private TextView uxDisplayIPAddress;
    private Button uxBtnConnect;
    private ProgressBar uxProgressBar;

    private RadioButton uxBtnHost;
    private RadioButton uxBtnClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        // Set-up user interface (UX) components
        setContentView(R.layout.activity_main);

        uxHandler = new Handler();

        uxConnectionType = findViewById(R.id.uxConnectionType);
        uxTextStatus = findViewById(R.id.uxConnectionStatus);
        uxEditIPAddress = findViewById(R.id.uxInputIPAddress);
        uxDisplayIPAddress = findViewById(R.id.uxDisplayIPAddress);
        uxBtnConnect = findViewById(R.id.uxConnect);
        uxBtnHost = findViewById(R.id.uxHost);
        uxBtnClient = findViewById(R.id.uxClient);
        uxProgressBar = findViewById(R.id.uxProgressBar);

        uxConnectionType.setOnCheckedChangeListener(this::onConnectionTypeCheckedChanged);

        // Get IP address and display it
        try {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            String ip = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
            uxDisplayIPAddress.setText(ip);
        } catch (UnknownHostException ex) {
            uxDisplayIPAddress.setText("Couldn't your IP address automatically (are you connected to a network?)");
        }

        NetManager.addListener(this);
    }

    public void onConnectionTypeCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.uxHost) {
            uxBtnConnect.setText("Host");
            uxEditIPAddress.setVisibility(android.view.View.INVISIBLE);

        } else {
            uxBtnConnect.setText("Connect");
            uxEditIPAddress.setVisibility(android.view.View.VISIBLE);
        }
    }

    public void onClickBtnConnect(View view) {
        uxBtnConnect.setEnabled(false);
        uxProgressBar.setVisibility(View.VISIBLE);

        if (uxBtnHost.isChecked()) {
            uxTextStatus.setText("Waiting for someone to connect to your IP...");

            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(NetManager.PORT);
                    Socket clientSocket = serverSocket.accept();
                    new ConnectionThread(clientSocket).start();
                } catch (IOException ex) {
                    uxHandler.post(() -> {
                        uxTextStatus.setText("Error while trying to host: " + ex.getMessage());
                        uxBtnConnect.setEnabled(true);
                        uxProgressBar.setVisibility(View.INVISIBLE);
                        ex.printStackTrace();
                    });
                }
            }).start();
        } else {
            String friendHostIP = uxEditIPAddress.getText().toString();
            uxTextStatus.setText("Attempting to connect to " + friendHostIP);

            new Thread(() -> {
                try {
                    Socket clientSocket = new Socket(friendHostIP, NetManager.PORT);
                    new ConnectionThread(clientSocket).start();
                } catch (IOException ex) {
                    uxHandler.post(() -> {
                        uxTextStatus.setText("Error while trying to connect: " + ex.getMessage());
                        uxBtnConnect.setEnabled(true);
                        uxProgressBar.setVisibility(View.INVISIBLE);
                    });
                }
            }).start();
        }
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onConnect() {
        uxHandler.post(() -> {
            NetManager.clearListeners();
            uxBtnConnect.setEnabled(true);
            uxProgressBar.setVisibility(View.INVISIBLE);

            Intent intent = new Intent(this, ArActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onReceiveData(Packet p) {

    }
}
