package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class SplashActivity extends MotherActivity implements SplashView {
    private static final String TAG = SplashActivity.class.getSimpleName();
    private static final long FORK_SPLASH_MIN_MS = 2400;
    private Intent mNewIntent;
    private SplashPresenter mPresenter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator mGlowPulse;
    private boolean mForkSplashShown;
    private boolean mForkFinishScheduled;
    private long mForkSplashStartMs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNewIntent = getIntent();

        // Fork intro: shown on cold start from the launcher only.
        // App init continues behind it, so the actual startup isn't delayed.
        if (shouldShowForkSplash(savedInstanceState)) {
            showForkSplash();
        }

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();

        //finish();
    }

    private boolean shouldShowForkSplash(Bundle savedInstanceState) {
        return savedInstanceState == null
                && mNewIntent != null
                && Intent.ACTION_MAIN.equals(mNewIntent.getAction());
    }

    private void showForkSplash() {
        setContentView(R.layout.activity_fork_splash);

        initForkSplashTexts();
        startForkSplashAnimations();

        mForkSplashShown = true;
        mForkSplashStartMs = SystemClock.uptimeMillis();
    }

    private void initForkSplashTexts() {
        TextView subtitle = findViewById(R.id.fork_splash_subtitle);
        TextView version = findViewById(R.id.fork_splash_version);

        if (subtitle != null) {
            SpannableStringBuilder sb = new SpannableStringBuilder("Fork por ");
            int nameStart = sb.length();
            sb.append("MicaelSanPedro");
            sb.setSpan(new StyleSpan(Typeface.BOLD), nameStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(0xFFFFFFFF), nameStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            subtitle.setText(sb);
        }

        if (version != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                version.setText(String.format("v%s", versionName));
            } catch (Exception e) {
                version.setVisibility(View.GONE);
            }
        }
    }

    private void startForkSplashAnimations() {
        View glow = findViewById(R.id.fork_splash_glow);
        View icon = findViewById(R.id.fork_splash_icon);
        View title = findViewById(R.id.fork_splash_title);
        View divider = findViewById(R.id.fork_splash_divider);
        View subtitle = findViewById(R.id.fork_splash_subtitle);
        View credits = findViewById(R.id.fork_splash_credits);

        if (glow != null) {
            glow.setAlpha(0f);
            glow.animate().alpha(1f).setStartDelay(150).setDuration(900)
                    .setInterpolator(new DecelerateInterpolator()).start();
            startGlowPulse(glow);
        }

        if (icon != null) {
            icon.setAlpha(0f);
            icon.setScaleX(0.85f);
            icon.setScaleY(0.85f);
            icon.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(80).setDuration(650)
                    .setInterpolator(new DecelerateInterpolator(1.6f)).start();
        }

        if (title != null) {
            title.setAlpha(0f);
            title.setTranslationY(16f);
            title.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(500)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        if (divider != null) {
            divider.setAlpha(0f);
            divider.setScaleX(0.3f);
            divider.animate().alpha(1f).scaleX(1f).setStartDelay(520).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        if (subtitle != null) {
            subtitle.setAlpha(0f);
            subtitle.animate().alpha(1f).setStartDelay(700).setDuration(450)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        if (credits != null) {
            credits.setAlpha(0f);
            credits.animate().alpha(1f).setStartDelay(950).setDuration(600)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void startGlowPulse(View glow) {
        mGlowPulse = ValueAnimator.ofFloat(1f, 1.1f, 1f);
        mGlowPulse.setStartDelay(1200);
        mGlowPulse.setDuration(2400);
        mGlowPulse.setRepeatCount(ValueAnimator.INFINITE);
        mGlowPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        mGlowPulse.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            glow.setScaleX(scale);
            glow.setScaleY(scale);
        });
        mGlowPulse.start();
    }

    private long getForkSplashRemainingMs() {
        if (!mForkSplashShown) {
            return 0;
        }

        long elapsedMs = SystemClock.uptimeMillis() - mForkSplashStartMs;

        return Math.max(0, FORK_SPLASH_MIN_MS - elapsedMs);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;

        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        if (mGlowPulse != null) {
            mGlowPulse.cancel();
            mGlowPulse = null;
        }

        mHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        long delayMs = getForkSplashRemainingMs();

        // Keep the fork intro on screen for the minimum duration.
        // Everything else (app init, next activity) already happened behind it.
        if (delayMs > 50) {
            if (!mForkFinishScheduled) {
                mForkFinishScheduled = true;
                mHandler.postDelayed(this::reallyFinishView, delayMs);
            }
            return;
        }

        reallyFinishView();
    }

    private void reallyFinishView() {
        mForkFinishScheduled = false;

        try {
            finish();
        } catch (NullPointerException e) {
            // NullPointerException: Attempt to invoke virtual method 'void com.android.server.wm.DisplayContent.moveStack(com.android.server.wm.TaskStack, boolean)'
            e.printStackTrace();
        }
    }
}
