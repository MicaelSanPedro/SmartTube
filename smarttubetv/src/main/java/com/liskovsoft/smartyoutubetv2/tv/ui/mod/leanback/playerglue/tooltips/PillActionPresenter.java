package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tooltips;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.Presenter;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Fork: presenter que renderiza uma Action como "pill" translucida
 * (icone + texto sempre visivel), estilo YouTube TV.
 */
public class PillActionPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fork_pill_action, parent, false);
        return new PillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        if (!(item instanceof Action)) {
            return;
        }

        Action action = (Action) item;
        PillViewHolder vh = (PillViewHolder) viewHolder;

        // MultiAction.getLabel1() ja reflete o estado atual (index)
        CharSequence label = action.getLabel1();
        vh.mLabel.setText(label != null ? label : "");
        vh.mLabel.setVisibility(vh.mLabel.getText().length() > 0 ? View.VISIBLE : View.GONE);

        if (action.getIcon() != null) {
            vh.mIcon.setImageDrawable(action.getIcon());
            vh.mIcon.setVisibility(View.VISIBLE);
        } else {
            vh.mIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // nothing to release
    }

    @Override
    public void setOnClickListener(Presenter.ViewHolder viewHolder, View.OnClickListener listener) {
        viewHolder.view.setOnClickListener(listener);
    }

    static class PillViewHolder extends Presenter.ViewHolder {
        final ImageView mIcon;
        final TextView mLabel;

        PillViewHolder(View view) {
            super(view);
            mIcon = (ImageView) view.findViewById(R.id.icon);
            mLabel = (TextView) view.findViewById(R.id.label);

            // Fork: quando focado, o fundo fica branco -> icone vira preto
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    mIcon.setColorFilter(new PorterDuffColorFilter(
                            ContextCompat.getColor(v.getContext(), android.R.color.black),
                            PorterDuff.Mode.SRC_ATOP));
                } else {
                    mIcon.clearColorFilter();
                }
            });
        }
    }
}
