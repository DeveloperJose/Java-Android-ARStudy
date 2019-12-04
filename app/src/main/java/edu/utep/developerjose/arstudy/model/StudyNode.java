package edu.utep.developerjose.arstudy.model;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import edu.utep.developerjose.arstudy.view.ArActivity;
import edu.utep.developerjose.arstudy.R;

public class StudyNode extends Node {
    private static final String TAG = "ARStudy-StudyNode";
    private static final float INFO_CARD_Y_POS_COEFF = 0.3f;
    private static final float SCALE = 0.25f;

    public TransformableNode mDisplayNode;
    private Node mInfoNode;
    private AnchorNode mParentAnchor;
    private Bitmap mBitmap;

    private ArActivity mActivity;
    private ArFragment mArFragment;

    private ViewRenderable mDisplayRenderable;
    private ViewRenderable mInfoRenderable;

    public StudyNode(ArActivity activity, Bitmap bitmap, AnchorNode parent) {
        this.mActivity = activity;
        this.mBitmap = bitmap;
        this.mArFragment = activity.uxArFragment;
        this.mParentAnchor = parent;
    }

    public void onReceiveBitmap(Bitmap selectedImage) {
        mBitmap = selectedImage;

        View objectView = mDisplayRenderable.getView();
        ImageView imageView = objectView.findViewById(R.id.uxImage);
        imageView.setImageBitmap(mBitmap);
    }

    @Override
    public void onActivate() {
        if (getScene() == null) return;

        if (mInfoNode == null) {
            mInfoNode = new Node();

            ViewRenderable.builder()
                    .setView(mActivity, R.layout.object_info)
                    .build()
                    .thenAccept(renderable -> {
                        mInfoRenderable = renderable;
                        mInfoNode.setRenderable(renderable);
                        setupInfoNode();
                    });
        }

        if (mDisplayNode == null) {
            mDisplayNode = new TransformableNode(mArFragment.getTransformationSystem());

            ViewRenderable.builder()
                    .setView(mActivity, R.layout.object_display)
                    .build()
                    .thenAccept(renderable -> {
                        mDisplayRenderable = renderable;
                        mDisplayNode.setRenderable(renderable);
                        setupDisplayNode();
                    });
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (mInfoNode == null) {
            return;
        }

        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (getScene() == null) {
            return;
        }
        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 cardPosition = mInfoNode.getWorldPosition();
        Vector3 direction = Vector3.subtract(cameraPosition, cardPosition);
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        mInfoNode.setWorldRotation(lookRotation);
    }

    public void onTapNode(HitTestResult hitTestResult, MotionEvent motionEvent) {
        Log.d(TAG, "OnTap, null=" + (mInfoNode == null));
        if (mInfoNode == null) {
            return;
        }

        mInfoNode.setEnabled(!mInfoNode.isEnabled());
    }

    private void setupDisplayNode() {
        // Node setup
        mDisplayNode.setParent(mParentAnchor);
//        mDisplayNode.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), 180));
        mDisplayNode.setLocalScale(new Vector3(SCALE, SCALE, SCALE));
        mDisplayNode.getTranslationController().setEnabled(false);
        mDisplayNode.getScaleController().setEnabled(false);
        mDisplayNode.getRotationController().setEnabled(false);
        mDisplayNode.setOnTapListener(this::onTapNode);

        // View setup
        View objectView = mDisplayRenderable.getView();

        // Default bitmap
        ImageView imageView = objectView.findViewById(R.id.uxImage);
        imageView.setImageBitmap(mBitmap);
    }

    private void setupInfoNode() {
        // Node setup
        mInfoNode.setParent(mParentAnchor);
        mInfoNode.setEnabled(false);
        mInfoNode.setLocalScale(new Vector3(SCALE, SCALE, SCALE));
        mInfoNode.setLocalPosition(new Vector3(0.0f, INFO_CARD_Y_POS_COEFF, 0.0f));
        mInfoNode.setOnTapListener(this::onTapNode);

        // View setup
        View objectView = mInfoRenderable.getView();

        Button btnReplace = objectView.findViewById(R.id.uxBtnReplace);
        btnReplace.setOnClickListener((btnView) -> {
            mActivity.lastSelectedNode = this;
            mActivity.startGalleryIntent();
        });

        Button btnDelete = objectView.findViewById(R.id.uxBtnDelete);
        btnDelete.setOnClickListener((btnView) -> {
            getParent().removeChild(this);
        });

        Button btnResetScaling = objectView.findViewById(R.id.uxBtnResetScale);
        btnResetScaling.setOnClickListener((btnView) -> {
            mDisplayNode.setLocalScale(new Vector3(SCALE, SCALE, SCALE));
        });

        Switch switchMove = objectView.findViewById(R.id.uxSwitchMove);
        switchMove.setOnCheckedChangeListener((btnView, isChecked) -> {
            mDisplayNode.getTranslationController().setEnabled(isChecked);
        });

        Switch switchScale = objectView.findViewById(R.id.uxSwitchScale);
        switchScale.setOnCheckedChangeListener((btnView, isChecked) -> {
            mDisplayNode.getScaleController().setEnabled(isChecked);
        });

        Switch switchRotation = objectView.findViewById(R.id.uxSwitchRotation);
        switchRotation.setOnCheckedChangeListener((btnView, isChecked) -> {
            mDisplayNode.getRotationController().setEnabled(isChecked);
        });
    }
}