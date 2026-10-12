package com.liskovsoft.smartyoutubetv2.tv.ui.playback.other;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.PlaybackControlsRow;

import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.List;

/**
 * Fork: popup estilo YouTube TV com as funções extras do player
 * (aberto pelo botão de engrenagem). Renderiza cada ação como pill
 * em um grid de 2 colunas com scroll.
 */
public class ForkActionsPopup {
    private static final int PILL_PADDING_H_DP = 12;
    private static final int PILL_PADDING_V_DP = 8;
    private static final int ROW_SPACING_DP = 10;
    private static final int CONTAINER_PADDING_DP = 24;
    private static final int ICON_SIZE_DP = 18;
    private static final int DIALOG_BOTTOM_MARGIN_DP = 60;

    public interface OnActionPickedListener {
        void onActionPicked(int actionId, int actionIndex);
    }

    public static void show(android.content.Context context, List<Action> actions,
                            OnActionPickedListener listener) {
        if (context == null || actions == null || actions.isEmpty()) {
            return;
        }

        float density = context.getResources().getDisplayMetrics().density;
        int pillPaddingH = dp(PILL_PADDING_H_DP, density);
        int pillPaddingV = dp(PILL_PADDING_V_DP, density);
        int rowSpacing = dp(ROW_SPACING_DP, density);
        int containerPadding = dp(CONTAINER_PADDING_DP, density);
        int iconSize = dp(ICON_SIZE_DP, density);

        // Fork: grid de 2 colunas (o menu concentra as funções que saíram dos
        // docks — em configurações cheias podem ser mais de uma tela de pills)
        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);

        View firstView = null;

        for (int i = 0; i < actions.size(); i++) {
            View pill = createPill(context, actions.get(i), listener,
                    pillPaddingH, pillPaddingV, iconSize, density);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 2),
                    GridLayout.spec(i % 2, GridLayout.START));
            params.topMargin = i >= 2 ? rowSpacing : 0;
            params.leftMargin = (i % 2) == 1 ? rowSpacing : 0;
            grid.addView(pill, params);
            if (firstView == null) {
                firstView = pill;
            }
        }

        // Fork: cartão escuro arredondado com scroll, ancorado no canto inferior direito
        ScrollView scroller = new ScrollView(context);
        int cardPadding = dp(16, density);
        scroller.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
        scroller.setBackground(ContextCompat.getDrawable(context, R.drawable.fork_popup_bg));
        scroller.addView(grid);

        // Fork: raiz transparente que posiciona o cartão no canto inferior direito
        android.widget.FrameLayout root = new android.widget.FrameLayout(context);
        root.setPadding(containerPadding, containerPadding, containerPadding,
                dp(DIALOG_BOTTOM_MARGIN_DP, density));
        root.addView(scroller, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END));

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0.5f);
        }
        dialog.setCanceledOnTouchOutside(true);

        if (firstView != null) {
            firstView.requestFocus();
        }

        dismissCurrent();
        sCurrentDialog = dialog;
        dialog.show();
    }

    private static View createPill(android.content.Context context, Action action,
                                   OnActionPickedListener listener,
                                   int paddingH, int paddingV, int iconSize, float density) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackground(ContextCompat.getDrawable(context, R.drawable.fork_pill_bg));
        pill.setPadding(paddingH, paddingV, paddingH, paddingV);
        pill.setFocusable(true);
        pill.setClickable(true);

        Drawable icon = action.getIcon();
        if (icon != null) {
            ImageView iconView = new ImageView(context);
            iconView.setImageDrawable(icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.rightMargin = dp(8, density);
            iconView.setLayoutParams(iconParams);
            pill.addView(iconView);
            pill.setTag(iconView);
            pill.setOnFocusChangeListener((v, hasFocus) -> {
                ImageView iv = (ImageView) v.getTag();
                if (hasFocus) {
                    iv.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP));
                } else {
                    iv.clearColorFilter();
                }
            });
        }

        TextView labelView = new TextView(context);
        CharSequence label = action.getLabel1();
        labelView.setText(label != null ? label : "");
        labelView.setTextColor(
                ResourcesCompat.getColorStateList(context.getResources(), R.color.fork_pill_text, null));
        labelView.setTextSize(13);
        labelView.setMaxLines(1);
        pill.addView(labelView);

        final int actionId = (int) action.getId();
        final int actionIndex = action instanceof PlaybackControlsRow.MultiAction
                ? ((PlaybackControlsRow.MultiAction) action).getIndex() : 0;

        pill.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionPicked(actionId, actionIndex);
            }
            dismissCurrent();
        });

        return pill;
    }

    private static Dialog sCurrentDialog;

    private static void dismissCurrent() {
        if (sCurrentDialog != null) {
            sCurrentDialog.dismiss();
            sCurrentDialog = null;
        }
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
