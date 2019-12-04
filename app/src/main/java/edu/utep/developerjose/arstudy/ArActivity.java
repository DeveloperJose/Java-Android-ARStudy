package edu.utep.developerjose.arstudy;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.utep.developerjose.arstudy.model.StudyNode;
import edu.utep.developerjose.arstudy.network.NetInterface;
import edu.utep.developerjose.arstudy.network.NetManager;
import edu.utep.developerjose.arstudy.network.data.Packet;
import edu.utep.developerjose.arstudy.network.data.PacketClear;
import edu.utep.developerjose.arstudy.network.data.PacketImage;
import edu.utep.developerjose.arstudy.network.data.PacketMessage;


public class ArActivity extends AppCompatActivity implements NetInterface {
    enum SnackbarState {HIDING, PLACING, SETTING, DISCONNECTED}

    private static final String TAG = "ARStudy-ArActivity";
    private static final int REQUEST_GALLERY = 4;

    public StudyNode lastSelectedNode;
    public ArFragment uxArFragment;

    private Handler uxHandler;
    private Toolbar uxToolbar;
    private Snackbar uxSnackbar;
    private Bitmap mBitmap;

    private AnchorNode mAnchor;
    private Renderable mAnchorRenderable;

    private String mChatLog;
    private List<StudyNode> mStudyNodes;
    private View mDialogView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set-up regular components (m)
        mStudyNodes = new ArrayList<>();
        mChatLog = "";
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_android_black_24dp);

        // Set-up user interface (UX) components
        setContentView(R.layout.activity_ar);

        uxHandler = new Handler();
        uxSnackbar = Snackbar.make(findViewById(R.id.uxTopSnackBar), "Example", Snackbar.LENGTH_INDEFINITE);
        uxArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.uxFragment);
        uxToolbar = findViewById(R.id.uxToolbar);
        updateSnackBar(SnackbarState.SETTING);

        // Set-up augmented reality components
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> {
                    mAnchorRenderable = ShapeFactory.makeCube(
                            new Vector3(0.01f, 0.01f, 0.01f),
                            new Vector3(0.0f, 0.15f, 0.0f),
                            material);
                });

        uxArFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            // Part 1: Setting center anchor
            if (mAnchorRenderable == null)
                return;

            if (mAnchor == null) {
                Log.d(TAG, "NoAnchor");
                mAnchor = new AnchorNode(hitResult.createAnchor());
                mAnchor.setRenderable(mAnchorRenderable);
                mAnchor.setParent(uxArFragment.getArSceneView().getScene());

                setSupportActionBar(uxToolbar);
                updateSnackBar(SnackbarState.HIDING);

                // Begin listening for network events
                NetManager.addListener(this);
                return;
            }

            // Part 2: Object node adding
            if (mBitmap == null)
                return;

            StudyNode example = new StudyNode(this, mBitmap, mAnchor);
            mAnchor.addChild(example);
            mStudyNodes.add(example);

            // Update network
            NetManager.packetList.add(new PacketImage(example.getWorldPosition(), mBitmap));

            // Update activity
            mBitmap = null;
            updateSnackBar(SnackbarState.HIDING);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mAddImageGallery)
            startGalleryIntent();
        else if (item.getItemId() == R.id.mClear)
            clearObjects(true);
        else if (item.getItemId() == R.id.mChat)
            openChat();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK)
            return;

        Bitmap receivedBitmap = null;
        if (requestCode == REQUEST_GALLERY) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                receivedBitmap = BitmapFactory.decodeStream(imageStream);
            } catch (FileNotFoundException ex) {
                Toast.makeText(this, "Could not load selected image.", Toast.LENGTH_LONG).show();
            }
        }

        // If we are not updating, then we are placing a new node
        if (lastSelectedNode == null) {
            mBitmap = receivedBitmap;
            updateSnackBar(SnackbarState.PLACING);
        } else {
            lastSelectedNode.onReceiveBitmap(receivedBitmap);
            lastSelectedNode = null;
        }
    }

    private void updateSnackBar(SnackbarState snackbarState) {
        if (snackbarState == SnackbarState.HIDING) {
            uxSnackbar.dismiss();
        } else if (snackbarState == SnackbarState.PLACING) {
            uxSnackbar.setText("Placing image onto whiteboard plane");
            uxSnackbar.show();
        } else if (snackbarState == SnackbarState.SETTING) {
            uxSnackbar.setText("Tap the middle of your whiteboard to begin");
            uxSnackbar.show();
        } else if (snackbarState == SnackbarState.DISCONNECTED) {
            uxSnackbar.setText("Your friend has disconnected from the study session");
            uxSnackbar.show();
        }
    }

    public void startGalleryIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_GALLERY);
    }

    public void clearObjects(boolean sendPacket) {
        for (StudyNode studyNode : mStudyNodes) {
            mAnchor.removeChild(studyNode);
        }
        mStudyNodes.clear();

        if (sendPacket) {
            NetManager.packetList.add(new PacketClear());
        }
    }

    public void openChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        mDialogView = getLayoutInflater().inflate(R.layout.dialog_chatlog, null);

        EditText uxMessage = mDialogView.findViewById(R.id.uxMessage);
        Button uxSendMessage = mDialogView.findViewById(R.id.uxSendMessage);
        TextView uxChatLog = mDialogView.findViewById(R.id.uxChatLog);

        uxChatLog.setText(mChatLog);
        uxSendMessage.setOnClickListener((view) -> {
            String message = uxMessage.getText().toString();
            String format = "<font color=\"red\">Friend</font>" + ":" + message + "\n";
            mChatLog += format;

            uxChatLog.setText(Html.fromHtml(mChatLog, Html.FROM_HTML_MODE_LEGACY));
            uxMessage.setText("");

            NetManager.packetList.add(new PacketMessage(message));
        });

        builder.setView(mDialogView)
                .setPositiveButton("Close", (dialogInterface, i) -> {
                    mDialogView = null;
                })
                .create().show();
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {
        uxHandler.post(() -> {
            updateSnackBar(SnackbarState.DISCONNECTED);
            finish();
        });
    }

    @Override
    public void onReceiveData(Packet p) {
        uxHandler.post(() -> {
            if (p instanceof PacketImage)
                processPacketImage((PacketImage) p);
            else if (p instanceof PacketMessage)
                processPacketMessage((PacketMessage) p);
            else if (p instanceof PacketClear)
                processPacketClear((PacketClear) p);
        });
    }

    private void processPacketImage(PacketImage p) {
        Log.d(TAG, "Received image packet: BM=" + p.mBitmap + ", P=" + p.mPosition);

        Toast.makeText(this, "Received notes from your friend", Toast.LENGTH_LONG).show();

        StudyNode studyNode = new StudyNode(this, p.mBitmap, mAnchor);
        studyNode.setWorldPosition(p.mPosition);
        mAnchor.addChild(studyNode);
        mStudyNodes.add(studyNode);
    }

    private void processPacketClear(PacketClear p) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_text, null);

        TextView uxText = dialogView.findViewById(R.id.uxText);
        uxText.setText("Your friend cleared all their objects. Clear all objects from the session?");

        builder.setView(dialogView)
                .setNegativeButton("Yes, clear them.",
                        (dialogInterface, i) -> {
                            clearObjects(false);
                        }
                )
                .setPositiveButton("No, keep them.", null)
                .create().show();
    }

    private void processPacketMessage(PacketMessage p) {
        String message = p.mStrMessage;
        String format = "<font color=\"red\">Friend</font>" + ":" + message + "\n";
        mChatLog += format;

        Toast.makeText(this, "Received message from your friend", Toast.LENGTH_LONG).show();

        if (mDialogView != null) {
            TextView uxChatLog = mDialogView.findViewById(R.id.uxChatLog);
            uxChatLog.setText(Html.fromHtml(mChatLog, Html.FROM_HTML_MODE_LEGACY));
        }
    }
}
