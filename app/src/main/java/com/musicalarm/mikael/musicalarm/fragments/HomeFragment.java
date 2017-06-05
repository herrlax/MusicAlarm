package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.AlarmItem;
import com.musicalarm.mikael.musicalarm.R;
import com.musicalarm.mikael.musicalarm.fragments.RecycleUtils.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mikael on 2017-06-05.
 */

public class HomeFragment extends Fragment {

    private TextView addText;

    private RecyclerView gridView;
    private GridLayoutManager gridLayoutManager;
    private RecyclerViewAdapter adapter;
    private List<AlarmItem> alarmItems = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initUI(view);

        return view;
    }

    private void initUI(View view) {

        addText = (TextView) view.findViewById(R.id.addnew_button);

        gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridView = (RecyclerView) view.findViewById(R.id.grid_view);
        gridView.setHasFixedSize(true);
        gridView.setLayoutManager(gridLayoutManager);

        adapter = new RecyclerViewAdapter(getContext(), alarmItems);

        gridView.setAdapter(adapter);
    }
}
