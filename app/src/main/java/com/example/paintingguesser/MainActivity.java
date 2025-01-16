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
import android.text.Html;
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
    TextView nameText;

    int index;

    class PortraitsDownloader extends Thread {

        StringBuilder result = new StringBuilder();
        String[] urls;
        HttpURLConnection urlConnection = null;

        PortraitsDownloader(String[] urls) {
            this.urls = urls;
        }

        @Override
        public void run() {

            try {

                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String data;

                while ((data = reader.readLine()) != null) {

                    result.append(data);

                }

            } catch (Exception e) {
                Log.i("Result with error", result.toString());
                e.printStackTrace();
            }

        }

        public StringBuilder getResult() {
            return result;
        }
    }

    class ImageDownloader extends Thread {

        Bitmap result;
        String[] urls;
        HttpURLConnection urlConnection = null;

        ImageDownloader(String[] urls) {
            this.urls = urls;
        }

        @Override
        public void run() {

            try {

                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                result = BitmapFactory.decodeStream(inputStream);


            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        public Bitmap getResult() {
            return result;
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
        nameText = (TextView) findViewById(R.id.nameText);

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
        String[] urls = {portraitUrl};
        PortraitsDownloader task = new PortraitsDownloader(urls);
        String result = null;

        try {
            task.start();
            task.join();
            result = String.valueOf(task.getResult());
        }
        catch (Exception e) {
            Log.i("Error", "Results found nothing");
            e.printStackTrace();
        }

        getImageData(result);

        setPortrait();
    }

    public void increaseIndex() {
        index++;
    }

    public void randomIndex() {
        int i = (int) (Math.random() * imgUrls.size());

        index = i;
    }

    public void setPortrait() {
        String[] urls = {imgUrls.get(index)};
        ImageDownloader imgTask = new ImageDownloader(urls);

        Bitmap image = null;
        try {
            imgTask.start();
            imgTask.join();
            Log.i("URL", imgUrls.get(index));

            image = imgTask.getResult();
            portrait.setImageBitmap(image);

            String name = getName();
            nameText.setText(name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getImageData(String res) {

        String[] splitRes = res.split("<div id=\"bsf_rt_marker\">");
        Log.i("Res", splitRes[0] + " " + String.valueOf(splitRes.length));

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
        String name = getName();
        String artist = artists.get(index);
        Toast.makeText(getApplicationContext(), name + " by " + artist, Toast.LENGTH_LONG).show();
    }

    public String getName() {
        String name = names.get(index);
        name = name.split("The famous painting")[0];
        name = name.split("the famous painting")[0];
        name = name.split("famous painting")[0];

        return name;
    }

}