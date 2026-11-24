package com.example.daftarii;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ArchiveActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private int transactionsCount = 0;
    private int customersCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ArchivePagerAdapter pagerAdapter = new ArchivePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("الحركات (" + transactionsCount + ")");
            } else {
                tab.setText("الزبائن (" + customersCount + ")");
            }
        }).attach();
    }

    public void updateTransactionsCount(int count) {
        this.transactionsCount = count;
        TabLayout.Tab tab = tabLayout.getTabAt(0);
        if (tab != null) {
            tab.setText("الحركات (" + transactionsCount + ")");
        }
    }

    public void updateCustomersCount(int count) {
        this.customersCount = count;
        TabLayout.Tab tab = tabLayout.getTabAt(1);
        if (tab != null) {
            tab.setText("الزبائن (" + customersCount + ")");
        }
    }

    private static class ArchivePagerAdapter extends FragmentStateAdapter {
        public ArchivePagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new ArchivedTransactionsFragment();
            } else {
                return new ArchivedCustomersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}