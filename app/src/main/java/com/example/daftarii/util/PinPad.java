package com.example.daftarii.util;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.core.view.ViewCompat;

import com.example.daftarii.R;

/** كود مشترك للوحة أرقام الـ PIN ونقاطها (إنشاء PIN وشاشة القفل). */
public final class PinPad {

    public interface Listener {
        void onDigit(char digit);
        void onDelete();
    }

    private static final int[] KEY_IDS = {
            R.id.key0, R.id.key1, R.id.key2, R.id.key3, R.id.key4,
            R.id.key5, R.id.key6, R.id.key7, R.id.key8, R.id.key9
    };

    private PinPad() {}

    /** بيربط الأزرار 0–9 وزر الحذف بالـ listener. */
    public static void bind(View keypad, Listener listener) {
        for (int i = 0; i < KEY_IDS.length; i++) {
            final char digit = (char) ('0' + i);
            keypad.findViewById(KEY_IDS[i]).setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                listener.onDigit(digit);
            });
        }
        keypad.findViewById(R.id.keyDelete).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            listener.onDelete();
        });
    }

    /** وقت الوقف بعد محاولات كتير: الأزرار ما بتشتغل وبتبيّن باهتة. */
    public static void setEnabled(View keypad, boolean enabled) {
        for (int id : KEY_IDS) keypad.findViewById(id).setEnabled(enabled);
        keypad.findViewById(R.id.keyDelete).setEnabled(enabled);
        keypad.setAlpha(enabled ? 1f : 0.4f);
    }

    /** أول {@code filled} نقاط بلون {@code on}، والباقي بلون {@code off}. */
    public static void showDots(ViewGroup dots, int filled, @ColorInt int on, @ColorInt int off) {
        for (int i = 0; i < dots.getChildCount(); i++) {
            ViewCompat.setBackgroundTintList(dots.getChildAt(i),
                    ColorStateList.valueOf(i < filled ? on : off));
        }
    }

    /** هزّة صغيرة للنقاط لما يكون الرمز غلط. */
    public static void shake(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        float d = view.getResources().getDisplayMetrics().density;
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0, 14 * d, -14 * d, 10 * d, -10 * d, 5 * d, -5 * d, 0)
                .setDuration(380)
                .start();
    }
}
