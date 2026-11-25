package com.example.daftarii;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatementPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "extra_customer_id";
    public static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
    public static final String EXTRA_CUSTOMER_PHONE = "extra_customer_phone";
    public static final String EXTRA_TOTAL_DEBT = "extra_total_debt";

    private ImageView btnBack;
    private TextView tvPageCount;
    private LinearLayout sheetRoot, rowsContainer, btnShare, btnPrint;
    private TextView tvShopName, tvStatementDate, tvCustomerName, tvCustomerPhone, tvTotal;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String customerId;
    private String customerName;
    private String customerPhone;
    private double totalDebt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statement_preview);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        customerId = getIntent().getStringExtra(EXTRA_CUSTOMER_ID);
        customerName = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        customerPhone = getIntent().getStringExtra(EXTRA_CUSTOMER_PHONE);
        totalDebt = getIntent().getDoubleExtra(EXTRA_TOTAL_DEBT, 0.0);

        bindViews();
        bindHeaderInfo();
        setupListeners();
        loadShopName();
        loadStatementRows();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPageCount = findViewById(R.id.tvPageCount);

        View sheetView = findViewById(R.id.statementSheet);
        sheetRoot = sheetView.findViewById(R.id.sheetRoot);
        rowsContainer = sheetView.findViewById(R.id.rowsContainer);
        tvShopName = sheetView.findViewById(R.id.tvShopName);
        tvStatementDate = sheetView.findViewById(R.id.tvStatementDate);
        tvCustomerName = sheetView.findViewById(R.id.tvCustomerName);
        tvCustomerPhone = sheetView.findViewById(R.id.tvCustomerPhone);
        tvTotal = sheetView.findViewById(R.id.tvTotal);

        btnShare = findViewById(R.id.btnShare);
        btnPrint = findViewById(R.id.btnPrint);
    }

    private void bindHeaderInfo() {
        tvCustomerName.setText(customerName != null && !customerName.trim().isEmpty() ? customerName.trim() : "");
        tvCustomerPhone.setText(customerPhone != null && !customerPhone.trim().isEmpty() ? customerPhone.trim() : "");
        tvTotal.setText(String.format(Locale.US, "₪ %.2f", totalDebt));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
        tvStatementDate.setText("كشف حساب زبون · " + dateFormat.format(new Date()));
        tvPageCount.setText("صفحة 1 من 1");
    }

    private void loadShopName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            tvShopName.setText(user.getDisplayName().trim());
        }

        if (user == null) return;
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    String shop = doc.getString("shopName");
                    if (shop == null || shop.trim().isEmpty()) {
                        shop = doc.getString("storeName");
                    }
                    if (shop == null || shop.trim().isEmpty()) {
                        shop = doc.getString("displayName");
                    }
                    if (shop != null && !shop.trim().isEmpty()) {
                        tvShopName.setText(shop.trim());
                    }
                });
    }

    private void loadStatementRows() {
        rowsContainer.removeAllViews();
        if (customerId == null || customerId.isEmpty()) return;

        db.collection("customers").document(customerId)
                .collection("debts")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null) return;

                    List<DebtItem> items = new ArrayList<>();
                    double sum = 0.0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Boolean archived = doc.getBoolean("archived");
                        if (Boolean.TRUE.equals(archived)) continue;

                        DebtItem item = doc.toObject(DebtItem.class);
                        if (item == null) continue;
                        item.setId(doc.getId());
                        item.setCustomerId(customerId);
                        items.add(item);
                        sum += item.getAmount();
                    }

                    // ترتيب تصاعدي لحساب الرصيد التراكمي بشكل صحيح
                    items.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                    double runningBalance = 0.0;
                    List<RowDisplayData> displayRows = new ArrayList<>();
                    for (DebtItem item : items) {
                        runningBalance = Math.round((runningBalance + item.getAmount()) * 100.0) / 100.0;
                        displayRows.add(new RowDisplayData(item, runningBalance));
                    }

                    // عكس الترتيب لعرض الأحدث أولاً في الكشف
                    Collections.reverse(displayRows);

                    totalDebt = Math.round(sum * 100.0) / 100.0;
                    tvTotal.setText(String.format(Locale.US, "₪ %.2f", totalDebt));

                    LayoutInflater inflater = LayoutInflater.from(this);
                    SimpleDateFormat rowDateFormat = new SimpleDateFormat("dd MMM", new Locale("ar"));

                    for (RowDisplayData rowData : displayRows) {
                        View rowView = inflater.inflate(R.layout.item_statement_row, rowsContainer, false);
                        TextView tvDate = rowView.findViewById(R.id.tvDate);
                        TextView tvDesc = rowView.findViewById(R.id.tvDesc);
                        TextView tvAmount = rowView.findViewById(R.id.tvAmount);
                        TextView tvBalance = rowView.findViewById(R.id.tvBalance);

                        DebtItem item = rowData.item;
                        String dateText = item.getTimestamp() > 0
                                ? rowDateFormat.format(new Date(item.getTimestamp()))
                                : "";
                        String descText = item.getItemName() != null ? item.getItemName() : "";

                        double amt = item.getAmount();
                        boolean isPayment = amt < 0;
                        String amountText = (isPayment ? "-" : "+") + String.format(Locale.US, "%.2f", Math.abs(amt));

                        tvDate.setText(dateText);
                        tvDesc.setText(descText);
                        tvAmount.setText(amountText);
                        tvAmount.setTextColor(isPayment
                                ? Color.parseColor("#1E8449")
                                : Color.parseColor("#C0392B"));
                        tvBalance.setText(String.format(Locale.US, "%.2f", rowData.runningBalance));

                        rowsContainer.addView(rowView);
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareStatementPdf());
        btnPrint.setOnClickListener(v -> printStatement());
    }

    private File createStatementPdfFile() {
        int width = sheetRoot.getWidth() > 0 ? sheetRoot.getWidth() : 540;
        int height = sheetRoot.getHeight() > 0 ? sheetRoot.getHeight() : 760;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas viewCanvas = new Canvas(bitmap);
        sheetRoot.draw(viewCanvas);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas pdfCanvas = page.getCanvas();
        pdfCanvas.drawColor(Color.WHITE);

        float scale = Math.min((595f - 48f) / width, (842f - 48f) / height);
        float drawWidth = width * scale;
        float drawHeight = height * scale;
        float left = (595f - drawWidth) / 2f;
        float top = 24f;

        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, Math.round(drawWidth), Math.round(drawHeight), true);
        pdfCanvas.drawBitmap(scaled, left, top, new Paint());
        pdfDocument.finishPage(page);

        File docsDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docsDir != null && !docsDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            docsDir.mkdirs();
        }
        File outFile = new File(docsDir, "Statement_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            pdfDocument.writeTo(fos);
            return outFile;
        } catch (IOException e) {
            Toast.makeText(this, "تعذر إنشاء ملف PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        } finally {
            pdfDocument.close();
            bitmap.recycle();
            if (scaled != bitmap) {
                scaled.recycle();
            }
        }
    }

    private void shareStatementPdf() {
        File pdfFile = createStatementPdfFile();
        if (pdfFile == null || !pdfFile.exists()) return;

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب"));
    }

    private void printStatement() {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(this, "خدمة الطباعة غير متوفرة على هذا الجهاز", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintDocumentAdapter printAdapter = new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 android.os.CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                callback.onLayoutFinished(
                        new android.print.PrintDocumentInfo.Builder("customer_statement.pdf")
                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .build(),
                        true
                );
            }

            @Override
            public void onWrite(android.print.PageRange[] pages,
                                android.os.ParcelFileDescriptor destination,
                                android.os.CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {
                File pdfFile = createStatementPdfFile();
                if (pdfFile == null) {
                    callback.onWriteFailed("تعذر إنشاء ملف الطباعة");
                    return;
                }
                try (java.io.FileInputStream in = new java.io.FileInputStream(pdfFile);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(destination.getFileDescriptor())) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }
        };

        printManager.print("كشف حساب " + (customerName != null ? customerName : ""), printAdapter, null);
    }

    private static class RowDisplayData {
        final DebtItem item;
        final double runningBalance;

        RowDisplayData(DebtItem item, double runningBalance) {
            this.item = item;
            this.runningBalance = runningBalance;
        }
    }
}