package com.kr.gallery.pro.models;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.kk.taurus.playerbase.receiver.BaseCover;
import com.kr.gallery.pro.R;
import com.kr.gallery.pro.interfaces.DataInter;


public class CloseCover extends BaseCover {

    ImageView mCloseIcon;

    public CloseCover(Context context) {
        super(context);
    }

    @Override
    public View onCreateCoverView(Context context) {
        View view = View.inflate(context, R.layout.layout_close_cover, null);
        mCloseIcon = view.findViewById(R.id.iv_close);
        mCloseIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_CLOSE, null);
            }
        });
        return view;
    }

    @Override
    public void onPlayerEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public void onErrorEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public void onReceiverEvent(int eventCode, Bundle bundle) {

    }

    @Override
    public int getCoverLevel() {
        return levelMedium(10);
    }
}
