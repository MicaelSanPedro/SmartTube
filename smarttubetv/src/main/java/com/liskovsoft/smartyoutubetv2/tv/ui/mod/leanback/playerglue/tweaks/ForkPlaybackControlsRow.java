package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.playerglue.tweaks;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;

/**
 * Fork: PlaybackControlsRow com um terceiro adapter (dock da direita,
 * estilo YouTube TV: like/dislike/CC/qualidade/engrenagem).
 */
public class ForkPlaybackControlsRow extends PlaybackControlsRow {
    private ArrayObjectAdapter mTertiaryActionsAdapter;

    public ForkPlaybackControlsRow(Object obj) {
        super(obj);
    }

    /**
     * Returns the tertiary actions adapter (right icon dock).
     */
    public ArrayObjectAdapter getTertiaryActionsAdapter() {
        return mTertiaryActionsAdapter;
    }

    /**
     * Sets the tertiary actions adapter (right icon dock).
     */
    public void setTertiaryActionsAdapter(ArrayObjectAdapter adapter) {
        mTertiaryActionsAdapter = adapter;
    }
}
