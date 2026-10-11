package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;
import android.view.KeyEvent;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class BrowseActivity extends LeanbackActivity {
    private static final String TAG = BrowseActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.fragment_main);
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }

    /**
     * Fork: tecla BACK estilo YouTube.<br/><br/>
     * 1) Fora da aba Inicio (ou barra lateral aberta) -> vai para o Inicio;<br/>
     * 2) Ja no Inicio -> fluxo de saida do app: "Pressione novamente para sair"
     * (LeanbackActivity.finish() + EXIT_DOUBLE_BACK) e sai no segundo aperto;<br/>
     * 3) Erro em tela ou fragmento ausente -> comportamento padrao.<br/>
     * Player, busca e dialogs tem janela propria e nao sao afetados.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0
                && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && !event.isCanceled()) {
            BrowseFragment browse = (BrowseFragment) getSupportFragmentManager().findFragmentById(R.id.main_frame);

            if (browse != null && !browse.forkIsErrorShowing()) {
                if (browse.forkHandleBackKey()) {
                    return true;
                }

                // Ja no Inicio: dispara o fluxo de saida (toast + sair no segundo aperto)
                finish();
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }
}
