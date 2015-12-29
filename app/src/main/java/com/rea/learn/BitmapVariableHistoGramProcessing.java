package com.rea.learn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import boofcv.alg.misc.ImageStatistics;
import boofcv.android.ConvertBitmap;
import boofcv.android.gui.VideoRenderProcessing;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;

/**
 * Created by ericbhatti on 11/17/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 17 November, 2015
 */
public class BitmapVariableHistoGramProcessing extends VideoRenderProcessing<MultiSpectral<ImageFloat32>> {

    // output image which is displayed by the GUI
    private Bitmap outputGUI;
    // storage used during image convert
    private byte[] storage;
    // output image which is modified by processing thread
    private Bitmap output;
    private int width, height;

    private Bitmap grayBitmap;

    int frameCount = 0;

    int skipRate;

    long lastServerTime;

    String ipAddress;

    String location = "";

    Paint paint;

    private ImageFloat32 lastServerImage;

    MainActivity mainActivity;

    View augmentView;
    Button markerButton;
    ListView listViewClassSchedules;

    public BitmapVariableHistoGramProcessing(MainActivity mainActivity, int width, int height) {
        super(ImageType.ms(3, ImageFloat32.class));
        this.width = width;
        this.height = height;
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.skipRate = 7;
        this.ipAddress = "http://192.168.11.252:8080/StrutsMavenProject/image.json";
        initPaint(Color.RED, 17);
        this.mainActivity = mainActivity;
        initAugmentView(mainActivity);
    }

    private void initAugmentView(MainActivity mainActivity) {
        LayoutInflater layoutInflater = LayoutInflater.from(mainActivity);
        augmentView = layoutInflater.inflate(R.layout.augment, null);
        markerButton = (Button) augmentView.findViewById(R.id.buttonMarker);
        listViewClassSchedules = (ListView) augmentView.findViewById(R.id.listViewClassSchedules);
        listViewClassSchedules.setAdapter(new ArrayAdapter<String>(mainActivity,android.R.layout.simple_list_item_1,new String[]{"Test","Hello","World"}));
        mainActivity.addContentView(augmentView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        augmentView.bringToFront();
        markerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listViewClassSchedules.getVisibility() == View.GONE) {
                    listViewClassSchedules.setVisibility(View.VISIBLE);
                } else {
                    listViewClassSchedules.setVisibility(View.GONE);
                }
            }
        });
    }

    public BitmapVariableHistoGramProcessing(MainActivity mainActivity, int width, int height, String ipAddress, int skipRate) {
        super(ImageType.ms(3, ImageFloat32.class));
        this.width = width;
        this.height = height;
        output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        outputGUI = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        this.ipAddress = ipAddress;
        //this.skipRate = skipRate;
        this.skipRate = skipRate * 1000;
        initPaint(Color.RED, 17);
        this.mainActivity = mainActivity;
        initAugmentView(mainActivity);
    }

    private void initPaint(int color, float textSize) {
        paint = new Paint();
        paint.setColor(color);
        paint.setTextSize(textSize);
    }


    @Override
    protected void render(Canvas canvas, double imageToOutput) {
        synchronized (new Object()) {
            canvas.drawBitmap(outputGUI, 0, 0, null);
            canvas.drawText(location, 75, 75, paint);
        }
    }


    int count = 0;
    long time = 0;


    @Override
    protected void process(MultiSpectral<ImageFloat32> imageFloat32MultiSpectral) {
        frameCount++;
        synchronized (new Object()) {
            //ConvertBitmap.declareStorage(gray ,storage);
            ConvertBitmap.multiToBitmap(imageFloat32MultiSpectral, output, storage);
            //  ConvertBitmap.grayToBitmap(gray, output, storage);
            outputGUI = output;
        }
        ImageFloat32 currentFrame = new ImageFloat32(imageFloat32MultiSpectral.width, imageFloat32MultiSpectral.height);
        ConvertImage.average(imageFloat32MultiSpectral, currentFrame);
        //if(frameCount % skipRate == 0) {
        if (lastServerImage == null) {
            postImageToServer(currentFrame, grayBitmap);
        } else {
            double error = ImageStatistics.meanDiffAbs(lastServerImage, currentFrame);
            long lastPingDiff = System.currentTimeMillis() - lastServerTime;
            if (error > 35 || lastPingDiff >= skipRate) {
                Log.e("SERVER_ERROR", "Error: " + String.valueOf(error) + "----- Server Time: " + lastPingDiff);
                postImageToServer(currentFrame, grayBitmap);
            }
        }
    }


    private void postImageToServer(ImageFloat32 currentFrame, Bitmap grayBitmap) {
        ConvertBitmap.grayToBitmap(currentFrame, grayBitmap, storage);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        grayBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();
        String img = Base64.encodeToString(data, Base64.DEFAULT);
        GetLocationAsync getLocationAsync = new GetLocationAsync();
        getLocationAsync.execute(img);
        lastServerImage = currentFrame;
        lastServerTime = System.currentTimeMillis();
    }


    class GetLocationAsync extends AsyncTask<String, Void, String> {
        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            return result.toString();
        }


        @Override
        protected String doInBackground(String... params) {

            count++;
            long start = System.currentTimeMillis();

            URL url;
            String response = "";
            try {
                url = new URL(ipAddress);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                HashMap<String, String> postDataParams = new HashMap<>();
                postDataParams.put("data", params[0]);
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            long stop = System.currentTimeMillis();
            long diff = stop - start;
            time = time + diff;
            Log.e("SERVER_REST", String.valueOf(diff));
            Log.e("SERVER_AVG", String.valueOf(time / count));
            try {
                JSONObject jsonObject = new JSONObject(response);
                location = jsonObject.getString("location");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    augmentView.setVisibility(View.VISIBLE);
                }
            });

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    augmentView.setVisibility(View.GONE);
                }
            });

        }
    }
}


