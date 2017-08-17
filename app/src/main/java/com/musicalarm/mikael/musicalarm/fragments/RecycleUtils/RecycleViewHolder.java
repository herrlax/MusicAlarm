package com.musicalarm.mikael.musicalarm.fragments.RecycleUtils;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.musicalarm.mikael.musicalarm.R;

/**
 * Created by mikael on 2017-04-12.
 */

public class RecycleViewHolder extends RecyclerView.ViewHolder {

    private TextView cardTime;
    private ImageView cardImage;
    private TextView cardName;
    private TextView cardArtist;

    private int id = 0;

    public RecycleViewHolder(View view) {
        super(view);
        cardTime = (TextView) view.findViewById(R.id.card_time);
        cardImage = (ImageView) view.findViewById(R.id.card_image);
        cardName = (TextView) view.findViewById(R.id.card_name);
        cardArtist = (TextView) view.findViewById(R.id.card_artist);

        cardImage.setOnClickListener(view1 -> {

        });

    }

    public TextView getCardTime() {
        return cardTime;
    }

    public ImageView getCardImage() {
        return cardImage;
    }

    public TextView getCardName() {
        return cardName;
    }

    public TextView getCardArtist() {
        return cardArtist;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
