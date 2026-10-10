package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.Context;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Fix: soft keyboard sometimes doesn't show up or doesn't accept any input
 * after the user clicks/focuses the search field.
 * <br/><br/>
 * Root causes:
 * <ul>
 * <li> {@link InputMethodManager#showSoftInput(View, int)} silently fails when called
 *      before the window gains focus (screen transition still in progress) or right
 *      after the IME has been dismissed (race condition). A single call isn't reliable
 *      enough on TV devices. </li>
 * <li> The OK button on most remotes sends {@link KeyEvent#KEYCODE_DPAD_CENTER}, which
 *      isn't handled by the EditText key fix, so the IME never pops up on click. </li>
 * <li> The IME may stay on screen but lose its {@link android.view.inputmethod.InputConnection}
 *      (e.g. focus moved during results update). Keystrokes then go nowhere. </li>
 * </ul>
 */
public class KeyboardFix {
    private static final String TAG = KeyboardFix.class.getSimpleName();

    /** Delays between show attempts (ms). Gives the IME and the window time to settle. */
    private static final long[] SHOW_RETRY_DELAYS_MS = {0, 250, 600, 1200};
    /** Interval between window focus checks (ms). */
    private static final long WINDOW_FOCUS_CHECK_INTERVAL_MS = 50;
    /** Max time to wait for the window focus before forcing the show attempt (ms). */
    private static final long WINDOW_FOCUS_TIMEOUT_MS = 1000;

    /** Pending show tasks per view, so a new request cancels the previous one. */
    private static final WeakHashMap<View, WeakReference<ImeShowTask>> sPendingTasks = new WeakHashMap<>();

    private KeyboardFix() {}

    /**
     * Robustly shows the soft keyboard.
     * Unlike {@link InputMethodManager#showSoftInput(View, int)} this method:
     * <ol>
     * <li> waits until the view's window gains focus (the IME rejects requests before that) </li>
     * <li> retries with escalating strategies:
     *      showSoftInput -> synthetic tap -> showSoftInput -> toggleSoftInput </li>
     * <li> repairs a dead InputConnection when the IME is up but isn't bound to the field </li>
     * </ol>
     */
    public static void showKeyboard(Context context, View view) {
        if (context == null || view == null) {
            return;
        }

        // Cancel previous show attempts on this view to avoid overlapping loops
        WeakReference<ImeShowTask> pending = sPendingTasks.remove(view);
        if (pending != null) {
            ImeShowTask task = pending.get();
            if (task != null) {
                view.removeCallbacks(task);
            }
        }

        ImeShowTask task = new ImeShowTask(context.getApplicationContext(), view);
        sPendingTasks.put(view, new WeakReference<>(task));
        // View.post works even when the view isn't attached yet: runs right after attach
        view.post(task);
    }

    /**
     * Makes an EditText reliable on TV:
     * <ul>
     * <li> OK (DPAD_CENTER) / Enter on the focused field opens the IME when it's closed </li>
     * <li> repairs the InputConnection when the IME is up but typing does nothing </li>
     * <li> when the IME is open and working, keys pass through untouched (e.g. commit) </li>
     * </ul>
     */
    public static void installSoftInputFix(EditText editField) {
        if (editField == null) {
            return;
        }

        editField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN || !v.hasFocus()) {
                return false;
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                    Context context = v.getContext();
                    InputMethodManager imm = getInputMethodManager(context);
                    if (imm == null) {
                        return false;
                    }

                    // IME is up but not bound to this field: keys do nothing. Repair the connection.
                    if (Helpers.isKeyboardShown(context) && !imm.isActive(v)) {
                        Log.d(TAG, "IME is up but input connection is dead. Repairing...");
                        imm.restartInput(v);
                        return true;
                    }

                    // IME closed: open it. This is the "clicked on the search field" case.
                    if (!Helpers.isKeyboardShown(context)) {
                        Log.d(TAG, "Confirm key on the edit field with IME closed. Showing keyboard...");
                        showKeyboard(context, v);
                        return true;
                    }

                    // IME open and bound: let the field/IME handle the key (e.g. commit query).
                    return false;
                default:
                    return false;
            }
        });
    }

    private static InputMethodManager getInputMethodManager(Context context) {
        if (context == null) {
            return null;
        }

        return (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private static class ImeShowTask implements Runnable {
        private final Context mAppContext;
        private final View mView;
        private final InputMethodManager mImm;
        private int mAttempt;
        private long mFocusWaitedMs;

        ImeShowTask(Context appContext, View view) {
            mAppContext = appContext;
            mView = view;
            mImm = getInputMethodManager(appContext);
        }

        @Override
        public void run() {
            if (mImm == null || !mView.isAttachedToWindow()) {
                return; // no IME service or the view got detached: abort
            }

            // The IME rejects show requests until the window gains focus
            // (e.g. a screen transition is still in progress). Wait a bit.
            if (mAttempt == 0 && !mView.hasWindowFocus() && mFocusWaitedMs < WINDOW_FOCUS_TIMEOUT_MS) {
                mFocusWaitedMs += WINDOW_FOCUS_CHECK_INTERVAL_MS;
                mView.postDelayed(this, WINDOW_FOCUS_CHECK_INTERVAL_MS);
                return;
            }

            // IME is up but not bound to the focused field: keys do nothing. Repair and bail.
            if (mView.hasFocus() && Helpers.isKeyboardShown(mAppContext) && !mImm.isActive(mView)) {
                Log.d(TAG, "IME is up but input connection is dead. Repairing...");
                mImm.restartInput(mView);
                return;
            }

            // Someone (or a previous attempt) already won the race: done.
            if (mAttempt > 0 && Helpers.isKeyboardShown(mAppContext)) {
                return;
            }

            switch (mAttempt) {
                case 0: // regular request, implicit flag works best on most TV IMEs
                    mView.requestFocus();
                    mImm.showSoftInput(mView, InputMethodManager.SHOW_IMPLICIT);
                    break;
                case 1: // AOSP leanback hack: synthetic tap reliably pops the IME on TV
                    syntheticTap(mView);
                    break;
                case 2: // some IMEs need a second nudge after the tap
                    mImm.showSoftInput(mView, 0);
                    break;
                default: // last resort hammer, only when there is no active IME session
                    if (!Helpers.isKeyboardShown(mAppContext) && !mImm.isActive(mView)) {
                        Log.d(TAG, "Keyboard still not shown. Toggling...");
                        mImm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                    return; // no more attempts
            }

            mAttempt++;
            mView.postDelayed(this, SHOW_RETRY_DELAYS_MS[mAttempt]);
        }
    }

    /**
     * Same trick as AOSP leanback SearchBar.showNativeKeyboard():
     * a synthetic tap on the view makes the framework show the IME.
     */
    private static void syntheticTap(View view) {
        view.requestFocusFromTouch();
        view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN, view.getWidth(), view.getHeight(), 0));
        view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP, view.getWidth(), view.getHeight(), 0));
    }
}
