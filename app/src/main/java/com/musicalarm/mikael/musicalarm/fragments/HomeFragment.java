package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
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

public class HomeFragment extends Fragment implements AddFragment.AddListener{

    private AddFragment addFragment;

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
        addText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAddFragment();
            }
        });

        gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridView = (RecyclerView) view.findViewById(R.id.grid_view);
        gridView.setHasFixedSize(true);
        gridView.setLayoutManager(gridLayoutManager);

        adapter = new RecyclerViewAdapter(getContext(), alarmItems);

        gridView.setAdapter(adapter);
    }

    public void startAddFragment() {
        addFragment = new AddFragment();
        addFragment.addListener(HomeFragment.this);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, addFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void addClicked(AlarmItem item) {

    }

    @Override
    public void deleteClicked(AlarmItem item) {

    }

    @Override
    public void editDoneClicked(AlarmItem item) {

    }
}
