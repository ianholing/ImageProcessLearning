package com.metodica.imageprocess.ui;

import android.annotation.TargetApi;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockActivity;
import com.metodica.imageprocess.R;
import com.metodica.imageprocess.async.AsyncProcessImage;
import com.metodica.imageprocess.imageprocess.ImageProcessing;
import com.metodica.imageprocess.imageprocess.ImageProcessingRGB;
import com.metodica.imageprocess.imageprocess.ImageProcessingYUV;

import java.io.IOException;

public class MainActivity extends SherlockActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {
    static final String classTAG = "SecurityAlarmActivity";

    public Camera kamera;
    public SurfaceHolder surface;
    public SurfaceView kameraview;

    int pictureCounter = 0;
    int pictureCounterMax = 20;
    private int cameraFacing = 0;
    private boolean flash = false;

    private TextView results;
    private ImageView wrapperView;
    private ImageView resultView;
    private ImageView resultFullView;
    private ToggleButton rgbyuvSwitch;
    private ToggleButton greySwitch;
    private ToggleButton sobelSwitch;
    private ToggleButton substractionSwitch;
    private ToggleButton digitalSwitch;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private SeekBar sobelSeekBar;
    private EditText sobelThresholdText;
    private SeekBar substractionSeekBar;
    private EditText substractionThresholdText;
    private SeekBar keyframeSeekBar;
    private EditText keyframeThresholdText;
    private CheckBox paintMotionCheck;

    private ImageProcessing imageProcessing;
//    private ImageProcessingYUV imageProcessing;
    private AsyncProcessImage asyncImageProcess = null;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        kameraview =(SurfaceView) findViewById(R.id.surfaceView);
        surface = kameraview.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.addCallback(this);

        results = (TextView)findViewById(R.id.results);
        resultView = (ImageView)findViewById(R.id.resultView);
        rgbyuvSwitch = (ToggleButton)findViewById(R.id.rgbyuvSwitch);
        greySwitch = (ToggleButton)findViewById(R.id.greyscaleSwitch);
        sobelSwitch = (ToggleButton)findViewById(R.id.sobelSwitch);
        substractionSwitch = (ToggleButton)findViewById(R.id.substractionSwitch);
        digitalSwitch = (ToggleButton)findViewById(R.id.digitalCountSwitch);
        sobelSeekBar = (SeekBar)findViewById(R.id.sobelSeekBar);
        sobelThresholdText = (EditText)findViewById(R.id.sobelThresholdText);
        substractionSeekBar = (SeekBar)findViewById(R.id.substractionSeekBar);
        substractionThresholdText = (EditText)findViewById(R.id.substractionThresholdText);
        keyframeSeekBar = (SeekBar)findViewById(R.id.keyframeSeekBar);
        keyframeThresholdText = (EditText)findViewById(R.id.keyframeThresholdText);
        paintMotionCheck = (CheckBox)findViewById(R.id.paintmotion);

        imageProcessing = new ImageProcessingRGB();
//        imageProcessing = new ImageProcessingYUV();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        //TODO: When user cliks on the little image it should become FullScreen
        //and same the round way
        resultFullView = (ImageView)findViewById(R.id.resultFullView);
        wrapperView = resultView;
    }

    private void prepareActionBar() {
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setNavigationDrawer() {
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
//                ctx.invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
//                ctx.invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareActionBar();
        setNavigationDrawer();
        init_values();

        rgbyuvSwitch.setOnCheckedChangeListener(toggleListener);
        greySwitch.setOnCheckedChangeListener(toggleListener);
        sobelSwitch.setOnCheckedChangeListener(toggleListener);
        substractionSwitch.setOnCheckedChangeListener(toggleListener);
        digitalSwitch.setOnCheckedChangeListener(toggleListener);

        resultView.setOnClickListener(new View.OnClickListener() {
            //Start new list activity
            public void onClick(View v) {
                wrapperView = resultFullView;
                resultFullView.setVisibility(View.VISIBLE);
            }
        });

        resultFullView.setOnClickListener(new View.OnClickListener() {
            //Start new list activity
            public void onClick(View v) {
                wrapperView = resultView;
                resultFullView.setVisibility(View.GONE);
            }
        });

        sobelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
             public void onStartTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                Integer progress = sobelSeekBar.getProgress();
                sobelThresholdText.setText(progress.toString());
                imageProcessing.setSobelThreshold(progress);
                processAndShowImage();
            }
        });

        substractionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                Integer progress = substractionSeekBar.getProgress();
                substractionThresholdText.setText(progress.toString());
                imageProcessing.setSubstractionThreshold(progress);
                processAndShowImage();
            }
        });

        keyframeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {

            }
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                Integer progress = keyframeSeekBar.getProgress();
                keyframeThresholdText.setText(progress.toString());
                pictureCounterMax = progress;
            }
        });

        sobelThresholdText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    Integer progress = Integer.parseInt(s.toString());
                    sobelSeekBar.setProgress(progress);
                    imageProcessing.setSobelThreshold(progress);
                    processAndShowImage();
                } catch (Exception e) {

                }
            }
        });

        substractionThresholdText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    Integer progress = Integer.parseInt(s.toString());
                    substractionSeekBar.setProgress(progress);
                    imageProcessing.setSubstractionThreshold(progress);
                    processAndShowImage();
                } catch (Exception e) {

                }
            }
        });

        keyframeThresholdText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    Integer progress = Integer.parseInt(s.toString());
                    keyframeSeekBar.setProgress(progress);
                    pictureCounterMax = progress;
                } catch (Exception e) {

                }
            }
        });

        paintMotionCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               imageProcessing.setPaintMotion(isChecked);
            }
        });
    }

    private void init_values () {
        imageProcessing.setGreyscale(greySwitch.isChecked());
//        greySwitch.setChecked(imageProcessing.getGreyscale());
        imageProcessing.setSobelize(sobelSwitch.isChecked());
//        sobelSwitch.setChecked(imageProcessing.getSobelize());
        imageProcessing.setSubstraction(substractionSwitch.isChecked());
//        substractionSwitch.setChecked(imageProcessing.getSubstraction());
        imageProcessing.setDigitalCount(digitalSwitch.isChecked());
//        digitalSwitch.setChecked(imageProcessing.getDigitalCount());

        Integer sobel = sobelSeekBar.getProgress();
//        Integer sobel = imageProcessing.getSobelThreshold();
        Integer substraction = substractionSeekBar.getProgress();
//        Integer substraction = imageProcessing.getSubstractionThreshold();
        imageProcessing.setSobelThreshold(sobel);
        imageProcessing.setSubstractionThreshold(substraction);
        sobelThresholdText.setText(sobel.toString());
        substractionThresholdText.setText(substraction.toString());
        keyframeThresholdText.setText(Integer.toString(pictureCounterMax));

//        substractionSeekBar.setProgress(substraction);
//        sobelSeekBar.setProgress(sobel);
//        keyframeSeekBar.setProgress(pictureCounterMax);
    }

    public void link_camera() {
        surface = kameraview.getHolder();
        surface.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.addCallback(this);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void getFacing() {
        PackageManager pm = getPackageManager();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
            else if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        flash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int h, int w) {
        Log.d(classTAG, "SURFACE_CHANGED");
        kamera.startPreview();
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) kamera.autoFocus(this);
        Log.d(classTAG, "SURFACE_CHANGED_FINISH");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(classTAG, "SURFACE_CREATED");

        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) kamera = openCameraGingerbread();
            else kamera = Camera.open();
            imageProcessing.setSettings(kamera.getParameters());
//            p.setPreviewFormat(ImageFormat.RGB_565);
//            kamera.setParameters(p);
            kamera.setPreviewDisplay(holder);
        }

        catch(IOException e)
        {
            kamera.release();
            finish();
        }

        Log.d(classTAG, "SURFACE_CREATED_FINISH");
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private Camera openCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        // NEEDS TO PROBE THAT IN NEXUS 7
        if (cameraCount == 1) return Camera.open( 0 );

        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if (cameraInfo.facing == cameraFacing) {
                cam = Camera.open( camIdx );
                break;
            }
        }

        return cam;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (kamera != null) {
            kamera.setPreviewCallback(null);
            kamera.stopPreview();
            kamera.release();
        }
    }

    public void onAutoFocus(boolean success, Camera camera) {
        kamera.setPreviewCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }








    ////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    ////            MOTION DETECTOR                 \\\\

    ////////////////////////\\\\\\\\\\\\\\\\\\\\\\\\\\\\

    @Override
    public void onPreviewFrame(byte[] data, Camera kamera) {
        if(data.length == 0) return;

//        // I REJECTED THE ASYNC SOLUTION CAUSE IT IS NOT RELIABLE WITH TIMECOUNT
//        if (asyncImageProcess == null) executeImageBackgroundProcess(data);
//        if (asyncImageProcess.getStatus() == AsyncTask.Status.FINISHED) {
//            resultView.setImageBitmap(
//                    Bitmap.createBitmap(
//                            imageProcessing.getLastImage(),
//                            imageProcessing.getWidth(),
//                            imageProcessing.getHeight(),
//                            Bitmap.Config.ARGB_8888)
//            );
//
//            imageProcessing.refreshKeyFrameValues();
//            results.setText(imageProcessing.getLastInfoText());
//        }

        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) return;

        long initTime = System.currentTimeMillis();
        imageProcessing.setNewFrame(data);
        processAndShowImage();
        

        if (pictureCounter > pictureCounterMax) {
            pictureCounter = 0;
            imageProcessing.refreshKeyFrameValues();
            Log.d(classTAG, "REFRESH KEY FRAME");
        } else
            imageProcessing.addLastInfoText(
                "Frame To Refresh Key Frame: " + (pictureCounterMax - pictureCounter) + "\n");
        pictureCounter++;

        imageProcessing.addLastInfoText("Frame Process Time: " + ((System.currentTimeMillis() - initTime)) + "ms");
        results.setText(imageProcessing.getLastInfoText());
    }

    public void processAndShowImage() {
        imageProcessing.process();
        refreshProcessedImage();
    }

    private void refreshProcessedImage() {
        wrapperView.setImageBitmap(imageProcessing.getLastImage());
    }

    CompoundButton.OnCheckedChangeListener toggleListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switch(buttonView.getId()) {
                        case R.id.rgbyuvSwitch:
                            if (isChecked) {
                                imageProcessing = new ImageProcessingRGB(
                                        imageProcessing.getPlainImage(),
                                        kamera.getParameters());
                            } else {
                                imageProcessing = new ImageProcessingYUV(
                                        imageProcessing.getPlainImage(),
                                        kamera.getParameters());
                            }
                            init_values ();
                            processAndShowImage();
                            break;
                        case R.id.greyscaleSwitch:
                            imageProcessing.setGreyscale(isChecked);
                            processAndShowImage();
                            break;

                        case R.id.sobelSwitch:
                            imageProcessing.setSobelize(isChecked);
                            processAndShowImage();
                            break;

                        case R.id.substractionSwitch:
                            imageProcessing.setSubstraction(isChecked);
                            processAndShowImage();

                            break;

                        case R.id.digitalCountSwitch:
                            imageProcessing.setDigitalCount(isChecked);
                            processAndShowImage();
                            break;
                        default:
                            return;
                    }
                    // Save the state here using key
                }
            };
}
