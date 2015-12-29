package com.rea.learn;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.Camera;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.List;

import boofcv.android.gui.CameraPreview;
import boofcv.android.gui.VideoDisplayActivity;


public class MainActivity extends VideoDisplayActivity {


    boolean change = false;
    ColouredTrackingProcessing colouredTrackingProcessing;
    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setShowFPS(true);
    }
    Camera.Size s;

    @Override
    protected Camera openConfigureCamera(Camera.CameraInfo cameraInfo) {
        Camera mCamera = selectAndOpenCamera(cameraInfo);
        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> sizes = param.getSupportedPreviewSizes();
        Camera.Size s = sizes.get(closest(sizes,320,240));
        this.s = s;
        param.setPreviewSize(s.width,s.height);
        Log.e("ERBL", s.width + " , " + s.height);
        mCamera.setParameters(param);

        ////// This is the line where we set how the app will process the frames. We have defined
        ////// three methods of processing in the app, one for Gray, one for Color, and the other for
        ////// object detection.


        //// Please uncomment and use the processing method you would want to use, and make sure
        //// others are commented.

        ///// setProcessing(new GrayProcessing());
       // setProcessing(new ColorProcessing());
  //      setProcessing(new HomographyProcessing2(this,s.width,s.height));
     //   setProcessing(new CSVProcessing(this,s.width,s.height));
    //    setProcessing(new WekaProcessing(this,s.width,s.height));


       // colouredTrackingProcessing = new ColouredTrackingProcessing(this,s.width,s.height);

        Intent intent = getIntent();
        if(intent.hasExtra(Settings.IP_ADDRESS) && intent.hasExtra(Settings.SKIP_RATE)){
            setProcessing(new BitmapVariableHistoGramProcessing(this,s.width,s.height,intent.getStringExtra(Settings.IP_ADDRESS).trim(),intent.getIntExtra(Settings.SKIP_RATE,7)));
        }
        else {
            setProcessing(new BitmapVariableHistoGramProcessing(this,s.width,s.height));
        }

        return mCamera;
    }

    private Camera selectAndOpenCamera(Camera.CameraInfo cameraInfo) {
        int numberOfCameras = Camera.getNumberOfCameras();
        int selected = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);

            if( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
                selected = i;
                break;
            } else {
                // default to a front facing camera if a back facing one can't be found
                selected = i;
            }
        }

        if( selected == -1 ) {
            dialogNoCamera();
            return null; // won't ever be called
        } else {
            return Camera.open(selected);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).setTitle(Html.fromHtml("<font color='white'>One Object</font>"));
        menu.getItem(1).setTitle(Html.fromHtml("<font color='white'>Two Object</font>"));
        menu.getItem(2).setTitle(Html.fromHtml("<font color='white'>Object CSV</font>"));
        return true;
    }


    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.action_one_object:
                setProcessing(new MatchProcessing(this, s.width, s.height));
                break;
            case R.id.action_two_object:
                setProcessing(new TwoObjectProcessing(this, s.width, s.height));
                break;
            case R.id.action_csv:
                setProcessing(new CSVProcessing(this, s.width, s.height));
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int test = item.getItemId();
        String title = item.getTitle().toString();
        Log.e("TEST ", test + title);
        switch (item.getItemId())
        {
            case R.id.action_one_object:
                Toast.makeText(this,"TEST",Toast.LENGTH_LONG).show();
                break;
            case R.id.action_two_object:
                Toast.makeText(this,"TEST2",Toast.LENGTH_LONG).show();
                break;
            case R.id.action_csv:
                Toast.makeText(this,"TEST3",Toast.LENGTH_LONG).show();
                break;
        }
        return true;
    }

    private void dialogNoCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your device has no cameras!")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        System.exit(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Goes through the size list and selects the one which is the closest specified size
     */
    public static int closest( List<Camera.Size> sizes , int width , int height ) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for( int i = 0; i < sizes.size(); i++ ) {
            Camera.Size s = sizes.get(i);

            int dx = s.width-width;
            int dy = s.height-height;

            int score = dx*dx + dy*dy;
            if( score < bestScore ) {
                best = i;
                bestScore = score;
            }
        }

        return best;
    }


    @Override
    public void onBackPressed() {

      /*  if(change)
        {
            setProcessing(new RoomTagProcessing2(this,s.width,s.height));
        }
        else
        {
            colouredTrackingProcessing = new ColouredTrackingProcessing(this,s.width,s.height);
            setProcessing(colouredTrackingProcessing);
        }

        change = !change;*/
        super.onBackPressed();
    }
/*
    @Override
    public boolean onTouchEvent(MotionEvent event) {
      //


            View v = getViewPreview().getChildAt(0);
            Log.e("BEZY", "Child:  " +  0 + " ; " + v.getHeight() + " , " + v.getWidth() + " , " + v.getX() + " , "  +v.getY());
            CameraPreview cameraPreview = (CameraPreview) v;
            for(int i = 0 ; i < cameraPreview.getChildCount() ; i++)
            {
                 v = cameraPreview.getChildAt(i);
                Log.e("BEZYS", "Child:  " +  i + " ; " + v.getHeight() + " , " + v.getWidth() + " , " + v.getX() + " , "  +v.getY());
            }
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getHeight();
        Log.e("ERBL", display.getHeight() + " , " + display.getWidth());
      //  int width = size.x;
       // int height = size.y;
        for(Object p: colouredTrackingProcessing.points)
        {   Point point = (Point) p;
            float diff_X = Math.abs(point.x - event.getX());
            float diff_Y = Math.abs(point.y - event.getY());
            Log.e("ERBL", point.y + " , " + event.getY());
            if(diff_X <= 20 && diff_Y <= 20)
            {
                Toast.makeText(this,"Test",Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }*/
}
