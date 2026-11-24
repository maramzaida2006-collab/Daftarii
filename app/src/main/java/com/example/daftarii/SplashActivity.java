package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.daftarii.auth.SplashViewModel;
import com.example.daftarii.logic.StartRouter;

/**
 * أول شاشة بتفتح بالتطبيق. بتعرض الشعار شوي، وبعدين بتوجّه المستخدم
 * حسب StartRouter: الترحيب، الدخول، تفعيل البريد، القفل، أو الرئيسية للزائر.
 */
public class SplashActivity extends AppCompatActivity {

    /** قديش بتضل الشاشة ظاهرة قبل ما ننتقل. */
    private static final long SPLASH_DELAY_MS = 1200;
    /** قديش كل خطوة بحركة نقاط التحميل. */
    private static final long DOT_STEP_MS = 300;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable routeRunnable = this::route;
    private final Runnable dotsRunnable = new Runnable() {
        @Override
        public void run() {
            activeDot = (activeDot + 1) % dots.getChildCount();
            updateDots();
            handler.postDelayed(this, DOT_STEP_MS);
        }
    };

    private SplashViewModel viewModel;
    private ViewGroup dots;
    private int activeDot = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        viewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        dots = findViewById(R.id.splashDots);

        // ظهور ناعم للشعار والاسم
        View content = findViewById(R.id.splashContent);
        content.setAlpha(0f);
        content.setTranslationY(24f);
        content.animate().alpha(1f).translationY(0f).setDuration(450).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(dotsRunnable, DOT_STEP_MS);
        handler.postDelayed(routeRunnable, SPLASH_DELAY_MS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // إذا طلع من التطبيق قبل ما نخلص، ما بننتقل وهو برّا
        handler.removeCallbacks(dotsRunnable);
        handler.removeCallbacks(routeRunnable);
    }

    private void route() {
        if (isFinishing()) return;
        StartRouter.Destination destination = viewModel.decideStart();
        Class<?> target;
        switch (destination) {
            case ONBOARDING:
                target = OnboardingActivity.class;
                break;
            case VERIFY_EMAIL:
                target = VerifyEmailActivity.class;
                break;
            case LOCK:
                target = LockActivity.class;
                break;
            case HOME_GUEST:
                // الزائر ما إله PIN ولا بصمة: على الرئيسية مباشرة
                target = MainActivity.class;
                break;
            case GUEST_EXPIRED:
                // شاشة تحويل الزائر لحساب جاية بالدفعة 4. هلأ بنشرحله وبنوديه على الدخول.
                Toast.makeText(this, R.string.guest_trial_expired, Toast.LENGTH_LONG).show();
                target = LoginActivity.class;
                break;
            case LOGIN:
            default:
                target = LoginActivity.class;
                break;
        }
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** النقطة الحالية طويلة (22dp) وكاملة اللون، والباقي صغار (6dp) وشفافين. */
    private void updateDots() {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < dots.getChildCount(); i++) {
            View dot = dots.getChildAt(i);
            boolean active = i == activeDot;
            ViewGroup.LayoutParams lp = dot.getLayoutParams();
            lp.width = Math.round((active ? 22 : 6) * density);
            dot.setLayoutParams(lp);
            dot.setAlpha(active ? 1f : 0.35f);
        }
    }
}
