package com.musicalarm.mikael.musicalarm.fragments.RecycleUtils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.musicalarm.mikael.musicalarm.AlarmItem;
import com.musicalarm.mikael.musicalarm.R;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by mikael on 2017-04-12.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecycleViewHolder> {

    public interface AdapterListener {
        void onDeleteClick(AlarmItem item);
        void onItemClicked(AlarmItem item);
    }

    private AdapterListener listener;
    private List<AlarmItem> alarmItems;
    private Context context;

    public RecyclerViewAdapter(Context context, List<AlarmItem> alarmItems) {
        this.alarmItems = alarmItems;
        this.context = context;
    }

    @Override
    public RecycleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.alarm_card_text, null);
        RecycleViewHolder viewHolder = new RecycleViewHolder(layoutView);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RecycleViewHolder holder, int idx) {

        final AlarmItem alarmItem = alarmItems.get(idx);

        String hourPrefix = alarmItem.getHour() < 10 ? "0" : "";
        String minutePrefix = alarmItem.getMinute() < 10 ? "0" : "";
        holder.getCardTime().setText(hourPrefix + alarmItem.getHour()
                + ":" + minutePrefix + alarmItem.getMinute());

        holder.setId(alarmItem.getAlarmID());
        holder.getCardName().setText(alarmItem.getName());
        holder.getCardArtist().setText(alarmItem.getArtist());

        // setting thumbnail ..
        Picasso.with(context)
                .load(alarmItem.getImageUrl())
                .fit()
                .centerCrop()
                .into(holder.getCardImage());


        holder.getCardDelete().setOnClickListener(view -> {
            listener.onDeleteClick(alarmItem);
        });

        holder.getCardImage().setOnClickListener(view1 -> {
            listener.onItemClicked(alarmItem);
        });
    }

    @Override
    public int getItemCount() {
        return alarmItems.size();
    }

    public void setAdapterListener(AdapterListener listener) {
        this.listener = listener;
    }
}
