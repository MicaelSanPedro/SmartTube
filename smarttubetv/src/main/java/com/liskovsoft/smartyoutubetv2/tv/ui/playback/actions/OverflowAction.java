package com.liskovsoft.smartyoutubetv2.tv.ui.playback.actions;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Fork: botao de engrenagem que abre o menu com as demais funcoes do player
 * (estilo YouTube TV).
 */
public class OverflowAction extends Action {
    public OverflowAction(Context context) {
        super(R.id.action_overflow);
        setIcon(ContextCompat.getDrawable(context, R.drawable.icon_settings));
        setLabel1(context.getString(R.string.player_buttons));
    }
}
