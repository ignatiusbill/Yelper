package com.scriptr.yelper;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.InputStream;

/**
 * Created by Billy on 8/20/2016.
 */
public class QueryFragment extends Fragment {

    private static final String CONSUMER_KEY = "quAhTjyeMqq9PQEMtXUE3g";
    private static final String CONSUMER_SECRET = "l74Pi7HP-_ov19f9CZH3X0HA2o0";
    private static final String TOKEN = "1FcAvGR4zvp5llxYRdBUHlDNhrTHmXIi";
    private static final String TOKEN_SECRET = "1hkCoA_6qTBJlDPYbOrqwZ-7mT8";

    private EditText foodEditText, locationEditText;
//    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private Button foodSearchButton, businessMobileURLBtn;

    private RelativeLayout yelpRL;
    private ImageView businessImg, businessRatingImg, yelpLogo;
    private TextView businessNameTV, businessReviewTV, businessAddressTV, businessCategoriesTV,
    yelpDisplayRequirementTV;

    private String userFoodChoice, userLocationChoice, curBusinessId = "", lastBusinessId = "";
    private boolean firstSearch=true;

    public JSONObject response;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.query_fragment, container, false);

        foodEditText = (EditText) view.findViewById(R.id.foodEditText);
        locationEditText = (EditText) view.findViewById(R.id.locationEditText);
//        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressDialog = new ProgressDialog(getActivity());
        foodSearchButton = (Button) view.findViewById(R.id.searchButton);

        businessImg = (ImageView) view.findViewById(R.id.businessImage);
        businessRatingImg = (ImageView) view.findViewById(R.id.businessRatingImage);
        businessNameTV = (TextView) view.findViewById(R.id.businessName);
        businessReviewTV = (TextView) view.findViewById(R.id.businessReviewCount);
        businessAddressTV = (TextView) view.findViewById(R.id.businessAddress);
        businessCategoriesTV = (TextView) view.findViewById(R.id.businessCategories);
        yelpRL = (RelativeLayout) view.findViewById(R.id.yelpRL);
        yelpRL.setVisibility(View.GONE);
        yelpDisplayRequirementTV = (TextView) view.findViewById(R.id.yelpDisplayRequirement);
        yelpLogo = (ImageView) view.findViewById(R.id.yelpLogo);
        yelpLogo.setImageResource(R.drawable.yelp_logo);
        businessMobileURLBtn = (Button) view.findViewById(R.id.businessMobileURL);

        locationEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_GO){
                    Log.d("DEBUG TIME", "inside onEditorAction()");
                    queryYelpAPI();
                    return true;
                }
                return false;
            }
        });


        foodSearchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                queryYelpAPI();
            }
        });

        return view;
    }

    private void queryYelpAPI(){
        userFoodChoice = foodEditText.getText().toString();
        userLocationChoice = locationEditText.getText().toString();

        if(userFoodChoice.length() != 0 && userLocationChoice.length() != 0){
            InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(locationEditText.getWindowToken(), 0);

            QueryYelpAPI queryYelpAPI = new QueryYelpAPI();
            queryYelpAPI.execute();
        } else{
            if (userFoodChoice.length() == 0)
                foodEditText.setError("Please enter food");
            if (userLocationChoice.length() == 0)
                locationEditText.setError("Please enter location");
        }
    }

    private class QueryYelpAPI extends AsyncTask<Void, Void, JSONObject>{
        protected void onPreExecute() {
//            progressBar.setVisibility(View.VISIBLE);
            progressDialog = ProgressDialog.show(getActivity(),
                    "Please wait ...", "Searching for restaurant ...", true, true);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try{
                YelpAPI yelpApi = new YelpAPI(CONSUMER_KEY, CONSUMER_SECRET, TOKEN, TOKEN_SECRET);
                Log.d("DEBUG TIME", "inside doInBackground()");

                response = yelpApi.queryAPI(yelpApi, userFoodChoice, userLocationChoice);
                curBusinessId = response.get("id").toString();
                while(curBusinessId == lastBusinessId){
                    response = yelpApi.queryAPI(yelpApi, userFoodChoice, userLocationChoice);
                    lastBusinessId = curBusinessId;
                    curBusinessId = response.get("id").toString();
                }

                return response;
            }
            catch(Exception e){
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(JSONObject response) {
            if(response == null) {
                Log.e("ERROR", "THERE WAS AN ERROR");
                Toast.makeText(getActivity(), "Unable to find restaurant. Please try again.",
                        Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
                return;
            }

//            progressBar.setVisibility(View.GONE);
            progressDialog.dismiss();
            businessMobileURLBtn.setVisibility(View.VISIBLE);

            JSONObject location = (JSONObject) response.get("location");
            JSONArray address = (JSONArray) location.get("address");
            String businessAddress = "";

            for (int i = 0; i<address.size(); i++)
                businessAddress += address.get(i) + ", ";

            JSONArray categories = (JSONArray) response.get("categories");
            String businessCategories = ((JSONArray) categories.get(0)).get(0).toString();

            for (int i = 1; i<categories.size(); i++)
                businessCategories += ", " + ((JSONArray) categories.get(i)).get(0).toString();

            final String businessMobileURL = response.get("url").toString();

            new DownloadImageTask(businessImg).execute(response.get("image_url").toString());
            businessNameTV.setText(response.get("name").toString());
            new DownloadImageTask(businessRatingImg).execute(response.get("rating_img_url").toString());
            businessReviewTV.setText(response.get("review_count").toString() + " reviews");
            businessAddressTV.setText(businessAddress + location.get("city").toString());
            businessCategoriesTV.setText(businessCategories);
            if(firstSearch) {
                yelpRL.setVisibility(View.VISIBLE);
                firstSearch=false;
            }
            businessMobileURLBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    Uri uri = Uri.parse(businessMobileURL);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }
            });
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
}