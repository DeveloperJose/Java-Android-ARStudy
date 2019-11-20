package edu.utep.developerjose.arstudy;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ARStudy-Main";
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final int REQUEST_IMAGE = 4;

    private Toolbar uxToolbar;
    private ArFragment uxArFragment;
    private Snackbar uxSnackbar;

    private ViewRenderable mImageRenderable;
    private Bitmap mBitmap;
    private View mUpdateView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        // Set-up regular components (m)
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_android_black_24dp);

        // Set-up user interface (UX) components
        setContentView(R.layout.activity_main);

        uxSnackbar = Snackbar.make(findViewById(R.id.uxTopSnackBar), "Example", Snackbar.LENGTH_INDEFINITE);
        uxArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.uxFragment);
        uxToolbar = findViewById(R.id.uxToolbar);
        setSupportActionBar(uxToolbar);

        // Set-up 3D renderable components
        ViewRenderable.builder()
                .setView(this, R.layout.object)
                .build()
                .thenAccept(renderable -> mImageRenderable = renderable);
        uxArFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (mImageRenderable == null || mBitmap == null)
                return;

            // Create the Anchor.
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(uxArFragment.getArSceneView().getScene());

            // Prepare object node and connect to anchor
            TransformableNode node = new TransformableNode(uxArFragment.getTransformationSystem());
            anchorNode.addChild(node);

            // Set-up object view and renderable
            ViewRenderable rend = mImageRenderable.makeCopy();
            setupObjectDetails(rend.getView(), node, anchorNode);
            node.setRenderable(rend);

            // Update activity
            mBitmap = null;
            updateSnackBar();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mAddImage)
            startImagePickingIntent();

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMAGE || resultCode != Activity.RESULT_OK)
            return;

        try {
            final Uri imageUri = data.getData();
            final InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
//            selectedImage = Bitmap.createScaledBitmap(selectedImage, 50, 50, true);
            // If we are not updating, then we are placing a new node
            if (mUpdateView == null) {
                mBitmap = selectedImage;
                updateSnackBar();
            } else {
                ImageView imageView = mUpdateView.findViewById(R.id.uxImage);
                imageView.setImageBitmap(selectedImage);
                mUpdateView = null;
            }

        } catch (FileNotFoundException ex) {
            Toast.makeText(this, "Could not load selected image.", Toast.LENGTH_LONG).show();
        }

    }

    private void updateSnackBar() {
        if (mBitmap != null) {
            uxSnackbar.setText("Placing image onto whiteboard plane");
            uxSnackbar.show();
        } else {
            uxSnackbar.dismiss();
        }
    }

    private void startImagePickingIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_IMAGE);
    }

    private void setupObjectDetails(View objectView, TransformableNode objectNode, AnchorNode anchorNode) {
        // Default to no transformations
        objectNode.getTranslationController().setEnabled(false);
        objectNode.getScaleController().setEnabled(false);
        objectNode.getRotationController().setEnabled(false);

        // Hide object details by default
        objectView.findViewById(R.id.uxObjectDetails).setVisibility(View.INVISIBLE);

        // Toggle visibility of details by long clicks
        objectView.setOnLongClickListener((View view) -> {
            LinearLayout lay = view.findViewById(R.id.uxObjectDetails);
            if (lay.getVisibility() == View.VISIBLE)
                lay.setVisibility(View.INVISIBLE);
            else
                lay.setVisibility(View.VISIBLE);
            return true;
        });

        // Default bitmap
        ImageView imageView = objectView.findViewById(R.id.uxImage);
        imageView.setImageBitmap(mBitmap);

        Button btnReplace = objectView.findViewById(R.id.uxBtnReplace);
        btnReplace.setOnClickListener((btnView) -> {
            mUpdateView = objectView;
            startImagePickingIntent();
        });

        Button btnDelete = objectView.findViewById(R.id.uxBtnDelete);
        btnDelete.setOnClickListener((btnView) -> {
            anchorNode.removeChild(objectNode);
        });

        Switch switchMove = objectView.findViewById(R.id.uxSwitchMove);
        switchMove.setOnCheckedChangeListener((btnView, isChecked) -> {
            objectNode.getTranslationController().setEnabled(isChecked);
        });

        Switch switchScale = objectView.findViewById(R.id.uxSwitchScale);
        switchScale.setOnCheckedChangeListener((btnView, isChecked) -> {
            objectNode.getScaleController().setEnabled(isChecked);
        });

        Switch switchRotation = objectView.findViewById(R.id.uxSwitchRotation);
        switchRotation.setOnCheckedChangeListener((btnView, isChecked) -> {
            objectNode.getRotationController().setEnabled(isChecked);
        });
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
}
