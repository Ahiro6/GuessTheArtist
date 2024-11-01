package com.example.paintingguesser;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Array;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    String portraitUrl = "https://bookmypainting.com/blog/famous-paintings/";

    ArrayList<String> names;
    ArrayList<String> imgUrls;
    ArrayList<String> artists;

    ImageView portrait;
    Button submit;
    Button next;
    ImageButton hint;
    EditText inputField;
    TextView artistText;

    int index;

    class PortraitsDownloader extends AsyncTask<String, Void, String> {

        StringBuilder result = new StringBuilder();
        URL url;
        HttpURLConnection urlConnection = null;

        @Override
        protected String doInBackground(String... urls) {

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String data;

                while ((data = reader.readLine()) != null) {

                    result.append(data);

                }

                return result.toString();

            } catch (Exception e) {
                Log.i("Result with error", result.toString());
                e.printStackTrace();
            }


            return "Done";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    class ImageDownloader extends AsyncTask<String, Void, Bitmap> {

        Bitmap result;
        URL url;
        HttpURLConnection urlConnection = null;

        @Override
        protected Bitmap doInBackground(String... urls) {

            try {

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                result = BitmapFactory.decodeStream(inputStream);

                return result;

            } catch (Exception e) {

                e.printStackTrace();
            }


            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hint = (ImageButton) findViewById(R.id.hint);
        submit = (Button) findViewById(R.id.submit);
        next = (Button) findViewById(R.id.next);
        inputField = (EditText) findViewById(R.id.inputField);
        portrait = (ImageView) findViewById(R.id.portrait);
        artistText = (TextView) findViewById(R.id.artistText);

        index = 0;

        hint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hint();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                compareAnswer();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                randomIndex();
                setPortrait();
            }
        });

        initiateApp();
    }

    public void initiateApp() {
        PortraitsDownloader task = new PortraitsDownloader();
        String result = null;

        try {

            result = task.execute(portraitUrl).get();
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);


        }
        catch (Exception e) {
            Log.i("Error", "Results found nothing");
            e.printStackTrace();
        }

        getImageData(result);

        setPortrait();
        //findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }

    public void increaseIndex() {
        index++;
    }

    public void randomIndex() {
        int i = (int) (Math.random() * imgUrls.size());

        index = i;
    }

    public void setPortrait() {
        ImageDownloader imgTask = new ImageDownloader();

        Bitmap image = null;
        try {
            image = imgTask.execute(imgUrls.get(index)).get();
            Log.i("URL", imgUrls.get(index));
            portrait.setImageBitmap(image);

            String name = names.get(index);
            name = name.split("The famous painting")[0];
            name = name.split("the famous painting")[0];
            name = name.split("famous painting")[0];
            artistText.setText(name);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void getImageData(String res) {

        String[] splitRes = res.split("<div id=\"bsf_rt_marker\">");

        ArrayList<String> alts = new ArrayList<String>();
        ArrayList<String> srcs = new ArrayList<String>();
        ArrayList<String> creators = new ArrayList<String>();

        Pattern pAlt = Pattern.compile("alt=\"(.*?)\"");
        Pattern pSrc = Pattern.compile("data-orig-file=\"(.*?)\"");
        Pattern pCreator = Pattern.compile("<strong>Artist:(.*?)</strong>");

        Matcher mAlt = pAlt.matcher(splitRes[1]);
        Matcher mSrc = pSrc.matcher(splitRes[1]);
        Matcher mCreator = pCreator.matcher(splitRes[1]);


        while(mAlt.find()) {
            alts.add(mAlt.group(1).trim());
        }

        while(mSrc.find()) {
            srcs.add(mSrc.group(1));
        }

        while(mCreator.find()) {
            creators.add(mCreator.group(1).trim());
        }

        Log.i("Alts", alts.toString());
        Log.i("Srcs", srcs.toString());
        Log.i("Artist", creators.toString());
        Log.i("Lenghts", "" + alts.size() + " " + srcs.size() + " " + creators.size());
        Log.i("res", splitRes[1]);

        names = alts;
        imgUrls = srcs;
        artists = creators;
    }

    public void compareAnswer() {
        String ans = inputField.getText().toString();

        String correct = artists.get(index);

        if(correct.trim().equals(ans.trim())) {
            Toast.makeText(getApplicationContext(), "Correct", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "Incorrect", Toast.LENGTH_LONG).show();
        }

    }

    public void hint() {
        String name = names.get(index);
        name = name.split("The famous painting")[0];
        name = name.split("the famous painting")[0];
        name = name.split("famous painting")[0];
        String artist = artists.get(index);
        Toast.makeText(getApplicationContext(), name + " by " + artist, Toast.LENGTH_LONG).show();
    }



}