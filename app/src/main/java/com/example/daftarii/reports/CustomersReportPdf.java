package com.example.daftarii.reports;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import com.example.daftarii.R;
import com.example.daftarii.auth.AuthRepository;
import com.example.daftarii.logic.DueAlerts;
import com.example.daftarii.logic.Money;
import com.example.daftarii.notifications.CustomerSource;
import com.example.daftarii.util.LanguageManager;
import com.example.daftarii.logic.AppLanguage;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * "تصدير PDF" بحسابي: كشف بكل الزباين (الاسم، الجوال، الرصيد، موعد السداد) والمجموع،
 * بعدين مشاركة (واتساب، إيميل، طباعة...). العربي محاذى يمين (StaticLayout باتجاه RTL).
 * الزباين بينقروا من CustomerSource، فإذا تغيّرت بنية البيانات هاد الملف ما بيتغيّر.
 */
public final class CustomersReportPdf {

    private static final int PAGE_W = 595;   // A4 بالنقاط
    private static final int PAGE_H = 842;
    private static final int MARGIN = 36;
    private static final int ROW_H = 26;

    private CustomersReportPdf() {}

    public static void export(Activity activity) {
        String uid = new AuthRepository().currentUid();
        if (uid == null) return;
        Toast.makeText(activity, R.string.report_pdf_preparing, Toast.LENGTH_SHORT).show();
        CustomerSource.query(uid).addOnCompleteListener(task -> {
            if (activity.isFinishing()) return;
            if (!task.isSuccessful()) {
                Toast.makeText(activity, R.string.load_failed, Toast.LENGTH_LONG).show();
                return;
            }
            List<DueAlerts.CustomerInfo> customers = CustomerSource.toCustomers(task.getResult());
            if (customers.isEmpty()) {
                Toast.makeText(activity, R.string.report_pdf_empty, Toast.LENGTH_LONG).show();
                return;
            }
            File file = write(activity, customers);
            if (file == null) {
                Toast.makeText(activity, R.string.statement_pdf_failed, Toast.LENGTH_LONG).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(share, activity.getString(R.string.report_pdf_share)));
        });
    }

    private static File write(Context c, List<DueAlerts.CustomerInfo> customers) {
        boolean rtl = LanguageManager.current() == AppLanguage.ARABIC;
        List<DueAlerts.CustomerInfo> rows = new ArrayList<>(customers);
        // الأعلى رصيد فوق
        Collections.sort(rows, (a, b) -> Long.compare(b.balanceAgorot, a.balanceAgorot));

        Typeface body = font(c, R.font.readex_pro_regular);
        Typeface bold = font(c, R.font.alexandria_semibold);
        TextPaint text = paint(10, ContextCompat.getColor(c, R.color.pdf_text), body);
        TextPaint head = paint(10, ContextCompat.getColor(c, R.color.pdf_text_secondary), bold);
        TextPaint title = paint(18, ContextCompat.getColor(c, R.color.pdf_text), bold);
        Paint line = new Paint();
        line.setColor(ContextCompat.getColor(c, R.color.pdf_line));
        line.setStrokeWidth(0.8f);

        // الأعمدة (بالعربي من اليمين): الاسم، الجوال، الرصيد، الموعد
        int content = PAGE_W - 2 * MARGIN;
        int[] widths = {(int) (content * 0.34), (int) (content * 0.24), (int) (content * 0.20), 0};
        widths[3] = content - widths[0] - widths[1] - widths[2];
        String[] headers = {c.getString(R.string.report_col_name), c.getString(R.string.report_col_phone),
                c.getString(R.string.report_col_balance), c.getString(R.string.report_col_due)};

        String shop = new AuthRepository().currentDisplayName();
        if (shop == null) shop = c.getString(R.string.home_store_name);
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(new Date());
        DateFormat dueFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

        PdfDocument pdf = new PdfDocument();
        int pageNo = 0;
        PdfDocument.Page page = null;
        Canvas canvas = null;
        int y = 0;
        long total = 0;
        try {
            for (int i = 0; i <= rows.size(); i++) {
                if (page == null || y + ROW_H > PAGE_H - MARGIN - ROW_H) {
                    if (page != null) pdf.finishPage(page);
                    pageNo++;
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create());
                    canvas = page.getCanvas();
                    y = MARGIN;
                    if (pageNo == 1) {
                        drawText(canvas, c.getString(R.string.report_pdf_title), title, MARGIN, y, content, rtl);
                        y += 28;
                        drawText(canvas, shop + " · " + date, head, MARGIN, y, content, rtl);
                        y += 26;
                    }
                    y = drawRow(canvas, headers, widths, head, y, rtl);
                    canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line);
                    y += 4;
                }
                if (i == rows.size()) break;
                DueAlerts.CustomerInfo cu = rows.get(i);
                total += cu.balanceAgorot;
                String[] cells = {
                        cu.name != null ? cu.name : "",
                        cu.phone != null ? cu.phone : "",
                        "₪ " + Money.format(cu.balanceAgorot),
                        cu.dueDateMillis > 0 ? dueFormat.format(new Date(cu.dueDateMillis)) : "—"
                };
                y = drawRow(canvas, cells, widths, text, y, rtl);
                canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line);
            }
            // المجموع
            y += 8;
            drawText(canvas, c.getString(R.string.report_pdf_total, rows.size(), Money.format(total)),
                    title, MARGIN, y, content, rtl);
            pdf.finishPage(page);

            File dir = c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) return null;
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
            File out = new File(dir, "Daftari_customers_" + stamp + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                pdf.writeTo(fos);
            }
            return out;
        } catch (Exception e) {
            return null;
        } finally {
            pdf.close();
        }
    }

    /** صف بأربع خلايا. بالعربي أول عمود على اليمين. بيرجع y تحت الصف. */
    private static int drawRow(Canvas canvas, String[] cells, int[] widths, TextPaint paint, int y, boolean rtl) {
        int x = rtl ? PAGE_W - MARGIN : MARGIN;
        for (int i = 0; i < cells.length; i++) {
            int w = widths[i];
            int left = rtl ? x - w : x;
            drawText(canvas, cells[i], paint, left + 4, y + 6, w - 8, rtl);
            x = rtl ? x - w : x + w;
        }
        return y + ROW_H;
    }

    /** نص بسطر واحد (بيتقص بـ ... إذا طويل)، محاذى حسب اللغة. */
    private static void drawText(Canvas canvas, String s, TextPaint paint, int left, int top, int width, boolean rtl) {
        CharSequence fitted = TextUtils.ellipsize(s, paint, width, TextUtils.TruncateAt.END);
        StaticLayout layout = StaticLayout.Builder.obtain(fitted, 0, fitted.length(), paint, Math.max(width, 1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(rtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR)
                .setMaxLines(1)
                .build();
        canvas.save();
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restore();
    }

    private static TextPaint paint(float size, int color, Typeface tf) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        if (tf != null) p.setTypeface(tf);
        return p;
    }

    private static Typeface font(Context c, int res) {
        try {
            return ResourcesCompat.getFont(c, res);
        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }
}
