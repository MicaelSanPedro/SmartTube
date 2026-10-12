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
 *
 * IMPORTANTE (fix de crash): estende ControlButtonPresenter porque o
 * ControlBarPresenter faz cast do presenter para
 * ControlButtonPresenterSelector.ControlButtonPresenter ao registrar o
 * long-click (bindControlToAction). Sem essa heranca, TODO video
 * crashava com ClassCastException no primeiro bind do dock secundario.
 * Todos os metodos do pai que dependem do layout do leanback
 * (ActionViewHolder / R.id.button) sao sobrescritos abaixo.
 */
public class PillActionPresenter extends ControlButtonPresenterSelector.ControlButtonPresenter {
    public PillActionPresenter() {
        // Construtor package-private (mesmo pacote): passa o layout da pill,
        // embora onCreateViewHolder seja sobrescrito e nao use esse campo.
        super(R.layout.fork_pill_action);
    }

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
            vh.mIcon.setImageDrawable(null);
            vh.mIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        PillViewHolder vh = (PillViewHolder) viewHolder;
        vh.mIcon.setImageDrawable(null);
        vh.mLabel.setText(null);
        vh.mIcon.clearColorFilter();
    }

    @Override
    public void setOnClickListener(Presenter.ViewHolder viewHolder, View.OnClickListener listener) {
        // A pill inteira e clicavel/focavel; nao usa o padrao do pai, que
        // depende da view R.id.button do layout original do leanback.
        viewHolder.view.setOnClickListener(listener);
    }

    @Override
    public void setOnLongClickListener(Presenter.ViewHolder viewHolder, View.OnLongClickListener listener) {
        // Mesmo motivo do setOnClickListener: o pai faria cast para
        // ActionViewHolder e quebraria com o ViewHolder da pill.
        viewHolder.view.setOnLongClickListener(listener);
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
