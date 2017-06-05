package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.musicalarm.mikael.musicalarm.AlarmItem;
import com.musicalarm.mikael.musicalarm.MainActivity;
import com.musicalarm.mikael.musicalarm.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mikael on 2017-06-05.
 */

public class AddFragment extends Fragment implements Response.Listener<String>, Response.ErrorListener {

    private TextView titleText;
    private RelativeLayout background;
    private RelativeLayout addButton;
    private ImageView backButton;
    private AutoCompleteTextView trackField;
    private static TextView timeText; // static to be able to be accessed from inner class
    private ImageView preview;
    private ImageView albumImage;
    private TextView cancelButton;

    private boolean itemClicked = false;

    private static AlarmItem alarmItem = new AlarmItem("", "", "", "", 6, 0, true, (int) System.currentTimeMillis());

    private ArrayAdapter<String> searchAdapter;
    private List<AlarmItem> searchResultsItems = new ArrayList<>();
    private List<String> stringResults = new ArrayList<>();

    interface AddListener {
        void addClicked(AlarmItem item);
        void deleteClicked(AlarmItem item);
        void editDoneClicked(AlarmItem item);
    }

    private List<AddListener> listeners;

    public void addListener(AddListener listener) {

        if(listeners == null)
            listeners = new ArrayList<>();

        listeners.add(listener);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        initUI(view);

        return view;
    }

    private void initUI(View view) {

        titleText = (TextView) view.findViewById(R.id.title_text);
        albumImage = (ImageView) view.findViewById(R.id.album_image);
        addButton = (RelativeLayout) view.findViewById(R.id.add_layout);
        background = (RelativeLayout) view.findViewById(R.id.add_background);

        cancelButton = (TextView) view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFragment();
            }
        });

        backButton = (ImageView) view.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFragment();
            }
        });

        timeText = (TextView) view.findViewById(R.id.time_text);
        timeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Todo open time picker
            }
        });

        view.findViewById(R.id.clock_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Todo open time picker
            }
        });


        searchAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line,
                stringResults);

        trackField = (AutoCompleteTextView) view.findViewById(R.id.track_field);
        trackField.setAdapter(searchAdapter);

        // when users enters text, suggest new songs..
        trackField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {

                // if the change is due to the user clicking a suggested song, don't suggest more
                if(itemClicked) {
                    itemClicked = false;
                    return;
                }

                // when users enters another character..
                updateSongs(trackField.getText().toString());

            }
        });

        // when a users selects a track from clicking enter on keyboard
        trackField.setOnKeyListener(new View.OnKeyListener() { // listen to enter clicked

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

                itemClicked = true;

                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    // Perform search on enter press
                    searchTrack(trackField.getText().toString().replaceAll(" ", "&20"));
                    hideKeyboard();

                    return true;
                }

                return false;
            }
        });

        preview = (ImageView) view.findViewById(R.id.preview);

    }

    // searches for track with http GET
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void searchTrack(String trackName) {

        // if trackname is too short, do nothing
        if(trackName.length() < 3)
            return;

        String url = "https://api.spotify.com/v1/search?q=" + trackName + "&type=track&limit=1";

        RequestQueue queue = Volley.newRequestQueue(getContext());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("MainActivity", "success");
                        setTrackFromTitle(response);
                    }
                },
                this){@Override public Map<String, String> getHeaders() {

            Map<String, String> params = new HashMap<>();
            params.put("Authorization", "Bearer " + MainActivity.token);
            return params;
        }};

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public List<String> updateSongs(String input) {

        final List<String> songs = new ArrayList<>();

        // if input is too short, return empty array
        if(input.length() < 3)
            return songs;

        String url = "https://api.spotify.com/v1/search?q=" + input.replaceAll(" ", "&20") + "&type=track&limit=3";
        //String url = "https://api.spotify.com/v1/search?q=tania%20bowra&type=artist";

        RequestQueue queue = Volley.newRequestQueue(getContext());

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, this, this
        ){@Override public Map<String, String> getHeaders() {
            Map<String, String> params = new HashMap<>();
            params.put("Authorization", "Bearer " + MainActivity.token);
            return params;
        }};


        try {
            Log.d("MainActivity", stringRequest.getHeaders().toString());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
        }

        Log.d("MainActivity", stringRequest.toString());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        return songs;
    }

    @Override
    public void onResponse(String response) {

        try {
            JSONObject reader = new JSONObject(response);
            JSONObject tracks  = reader.getJSONObject("tracks");
            JSONArray items  = tracks.getJSONArray("items");

            searchResultsItems.clear();
            stringResults.clear();

            for(int i = 0; i < items.length(); i++) {
                JSONObject result = items.getJSONObject(i);
                String uri = result.getString("uri");
                String name = result.getString("name");
                String artist = result.getJSONArray("artists").getJSONObject(0).getString("name");
                String imageUrl = result.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");

                // creating a minimum AlarmItem for storing uri, image, name and artist..
                // the rest of the attributes will be set separately
                AlarmItem item = new AlarmItem(uri, imageUrl, name, artist,
                        0,
                        0,
                        false,
                        0);

                stringResults.add(name);
                searchResultsItems.add(item);
            }

            try {
                searchAdapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        stringResults);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            trackField.setAdapter(searchAdapter);

            searchAdapter.notifyDataSetChanged();


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    // resets alarm and returns to homefragment
    public void exitFragment() {
        alarmItem = new AlarmItem("", "", "", "", 6, 0, true, (int) System.currentTimeMillis());
        getFragmentManager().popBackStack();
    }

    public void setTrackFromTitle(String title) {

        try {
            JSONObject reader = new JSONObject(title);
            JSONObject tracks  = reader.getJSONObject("tracks");
            JSONArray items  = tracks.getJSONArray("items");
            JSONObject result = items.getJSONObject(0);
            String uri = result.getString("uri");
            String name = result.getString("name");
            String artist = items.getJSONObject(0).getJSONArray("artists").getJSONObject(0).getString("name");
            String imageUrl = result.getJSONObject("album").getJSONArray("images").getJSONObject(0).getString("url");

            // updates UI
            trackField.setText(artist + " - " + name);

            // todo update album art

            preview.setVisibility(View.VISIBLE);

            // updates alarmItem
            alarmItem.setArtist(artist);
            alarmItem.setName(name);
            alarmItem.setTrackUri(uri);
            alarmItem.setImageUrl(imageUrl);

            try {
                alarmItem.jsonify(); // updates json in alarmItem
            } catch (JSONException e) {}


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("MainActivity", error.toString());
        Toast.makeText(getContext(), "probelmós de los internetós", Toast.LENGTH_SHORT).show();

    }
}
