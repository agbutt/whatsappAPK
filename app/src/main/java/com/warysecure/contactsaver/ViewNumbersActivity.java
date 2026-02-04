package com.warysecure.contactsaver;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ViewNumbersActivity extends Activity {

    private Button btnTabAll;
    private Button btnTabSaved;
    private Button btnTabUnsaved;
    private Button btnCloseView;
    
    private TextView statsTotal;
    private TextView statsSaved;
    private TextView statsUnsaved;
    
    private LinearLayout numberListContainer;
    
    private String currentTab = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_numbers);

        initViews();
        setupClickListeners();
        updateStats();
        displayNumbers("all");
    }

    private void initViews() {
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabSaved = findViewById(R.id.btnTabSaved);
        btnTabUnsaved = findViewById(R.id.btnTabUnsaved);
        btnCloseView = findViewById(R.id.btnCloseView);
        
        statsTotal = findViewById(R.id.statsTotal);
        statsSaved = findViewById(R.id.statsSaved);
        statsUnsaved = findViewById(R.id.statsUnsaved);
        
        numberListContainer = findViewById(R.id.numberListContainer);
    }

    private void setupClickListeners() {
        btnTabAll.setOnClickListener(v -> {
            currentTab = "all";
            updateTabStyle();
            displayNumbers("all");
        });

        btnTabSaved.setOnClickListener(v -> {
            currentTab = "saved";
            updateTabStyle();
            displayNumbers("saved");
        });

        btnTabUnsaved.setOnClickListener(v -> {
            currentTab = "unsaved";
            updateTabStyle();
            displayNumbers("unsaved");
        });

        btnCloseView.setOnClickListener(v -> finish());
    }

    private void updateTabStyle() {
        // Reset all tabs to default style
        btnTabAll.setTextColor(Color.parseColor("#666666"));
        btnTabAll.setBackgroundColor(Color.WHITE);
        
        btnTabSaved.setTextColor(Color.parseColor("#666666"));
        btnTabSaved.setBackgroundColor(Color.WHITE);
        
        btnTabUnsaved.setTextColor(Color.parseColor("#666666"));
        btnTabUnsaved.setBackgroundColor(Color.WHITE);
        
        // Highlight active tab
        if (currentTab.equals("all")) {
            btnTabAll.setTextColor(Color.WHITE);
            btnTabAll.setBackgroundColor(Color.parseColor("#075E54"));
        } else if (currentTab.equals("saved")) {
            btnTabSaved.setTextColor(Color.WHITE);
            btnTabSaved.setBackgroundColor(Color.parseColor("#25D366"));
        } else if (currentTab.equals("unsaved")) {
            btnTabUnsaved.setTextColor(Color.WHITE);
            btnTabUnsaved.setBackgroundColor(Color.parseColor("#FF5252"));
        }
    }

    private void updateStats() {
        int total = WhatsAppScannerService.detectedNumbers.size();
        int saved = WhatsAppScannerService.savedCount;
        int unsaved = WhatsAppScannerService.unsavedNumbers.size();
        
        statsTotal.setText(String.valueOf(total));
        statsSaved.setText(String.valueOf(saved));
        statsUnsaved.setText(String.valueOf(unsaved));
    }

    private void displayNumbers(String filter) {
        numberListContainer.removeAllViews();
        
        List<String> numbersToDisplay = new ArrayList<>();
        
        if (filter.equals("all")) {
            numbersToDisplay.addAll(WhatsAppScannerService.detectedNumbers);
        } else if (filter.equals("saved")) {
            // Show only saved numbers (those not in unsaved list)
            for (String number : WhatsAppScannerService.detectedNumbers) {
                if (!WhatsAppScannerService.unsavedNumbers.contains(number)) {
                    numbersToDisplay.add(number);
                }
            }
        } else if (filter.equals("unsaved")) {
            numbersToDisplay.addAll(WhatsAppScannerService.unsavedNumbers);
        }
        
        if (numbersToDisplay.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No numbers to display");
            emptyView.setTextSize(16);
            emptyView.setTextColor(Color.parseColor("#888888"));
            emptyView.setPadding(20, 40, 20, 40);
            emptyView.setGravity(android.view.Gravity.CENTER);
            numberListContainer.addView(emptyView);
            return;
        }
        
        int index = 0;
        for (String number : numbersToDisplay) {
            index++;
            View numberCard = createNumberCard(number, index, !WhatsAppScannerService.unsavedNumbers.contains(number));
            numberListContainer.addView(numberCard);
        }
    }

    private View createNumberCard(String phoneNumber, int index, boolean isSaved) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(15, 12, 15, 12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        card.setLayoutParams(params);
        card.setElevation(2);
        
        // Index
        TextView indexView = new TextView(this);
        indexView.setText(String.valueOf(index));
        indexView.setTextSize(14);
        indexView.setTextColor(Color.parseColor("#888888"));
        indexView.setWidth(60);
        card.addView(indexView);
        
        // Number
        TextView numberView = new TextView(this);
        numberView.setText(phoneNumber);
        numberView.setTextSize(16);
        numberView.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1
        );
        numberView.setLayoutParams(numberParams);
        card.addView(numberView);
        
        // Status badge
        TextView statusView = new TextView(this);
        if (isSaved) {
            statusView.setText("✓ SAVED");
            statusView.setTextColor(Color.WHITE);
            statusView.setBackgroundColor(Color.parseColor("#25D366"));
        } else {
            statusView.setText("UNSAVED");
            statusView.setTextColor(Color.WHITE);
            statusView.setBackgroundColor(Color.parseColor("#FF5252"));
        }
        statusView.setTextSize(11);
        statusView.setPadding(12, 6, 12, 6);
        card.addView(statusView);
        
        return card;
    }
}
