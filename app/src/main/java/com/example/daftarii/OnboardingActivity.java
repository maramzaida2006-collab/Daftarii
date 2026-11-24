package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.daftarii.auth.OnboardingViewModel;

/**
 * شاشتين الترحيب (بيطلعوا أول مرة بس). صفحتين بتتقلّب بالسحب أو بزر "التالي".
 * "تخطي" و "ابدأ الآن" بيحفظوا إنه شافها، وبيودّوا على تسجيل الدخول.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final int[] PAGES = {
            R.layout.item_onboarding_page1,
            R.layout.item_onboarding_page2
    };

    private OnboardingViewModel viewModel;
    private ViewPager2 pager;
    private View btnNext, btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        pager = findViewById(R.id.onboardingPager);
        btnNext = findViewById(R.id.btnNext);
        btnStart = findViewById(R.id.btnStart);

        pager.setAdapter(new PagesAdapter(PAGES));
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtons(position);
            }
        });
        updateButtons(pager.getCurrentItem());

        btnNext.setOnClickListener(v -> pager.setCurrentItem(pager.getCurrentItem() + 1));
        btnStart.setOnClickListener(v -> finishOnboarding());
        findViewById(R.id.btnSkip).setOnClickListener(v -> finishOnboarding());

        // زر الرجوع: من الصفحة التانية بيرجع للأولى، ومن الأولى بيطلع من التطبيق
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pager.getCurrentItem() > 0) {
                    pager.setCurrentItem(pager.getCurrentItem() - 1);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /** آخر صفحة: "ابدأ الآن" بعرض الشاشة. غير هيك: زر "التالي" الدائري. */
    private void updateButtons(int position) {
        boolean last = position == PAGES.length - 1;
        btnNext.setVisibility(last ? View.GONE : View.VISIBLE);
        btnStart.setVisibility(last ? View.VISIBLE : View.GONE);
    }

    private void finishOnboarding() {
        viewModel.complete();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** كل صفحة إلها layout ثابت، فالـ adapter بس بيعمل inflate. */
    private static class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.Holder> {

        private final int[] layouts;

        PagesAdapter(int[] layouts) {
            this.layouts = layouts;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            @LayoutRes int layout = layouts[viewType];
            View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            // ما في بيانات تتغيّر: كل إشي بالـ layout
        }

        @Override
        public int getItemCount() {
            return layouts.length;
        }

        static class Holder extends RecyclerView.ViewHolder {
            Holder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
