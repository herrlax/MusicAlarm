package com.musicalarm.mikael.musicalarm.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.MainActivity;
import com.musicalarm.mikael.musicalarm.R;
import com.musicalarm.mikael.musicalarm.fragments.RecycleUtils.RecyclerViewAdapter;

/**
 * Created by mikael on 2017-06-05.
 */

public class HomeFragment extends Fragment {

    public interface HomeFragmentListener {
        void addButtonClicked();
    }

    private HomeFragmentListener listener;

    private TextView addText;

    private RecyclerView gridView;
    private GridLayoutManager gridLayoutManager;
    private RecyclerViewAdapter adapter;

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
                listener.addButtonClicked();
            }
        });

        gridLayoutManager = new GridLayoutManager(getContext(), 2);
        gridView = (RecyclerView) view.findViewById(R.id.grid_view);
        gridView.setHasFixedSize(true);
        gridView.setLayoutManager(gridLayoutManager);

        adapter = new RecyclerViewAdapter(getContext(), ((MainActivity) getContext()).getAlarms());

        gridView.setAdapter(adapter);
    }

    // updates items in list and notifies adapter
    public void refreshList() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (HomeFragmentListener) context;
    }
}
