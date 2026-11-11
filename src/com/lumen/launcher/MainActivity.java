package com.lumen.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lumen: minimal Android TV launcher. No recommendations, no network, no background services. */
public class MainActivity extends Activity {

    static final int BG = 0xFF151B24, BG2 = 0xFF22304A, TILE = 0xFF1E2633, FG = 0xFFE9EDF3,
            MUTED = 0xFF8B97A8, LINE = 0xFF2A3443, ACCENT = 0xFFF2B84B, OK = 0xFF5FD39A, BAD = 0xFFE5645A;
    static final List<String> FAV = Arrays.asList(
            "com.stremio.one", "org.videolan.vlc", "com.spotify.tv.android", "com.disney.disneyplus", "com.wbd.stream");
    static final String TV_APP = "com.mediatek.wwtv.tvcenter";
    static final ComponentName GOOGLE_HOME =
            new ComponentName("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.home.HomeActivity");

    final Handler h = new Handler(Looper.getMainLooper());
    SharedPreferences prefs;
    PackageManager pm;
    float dp;
    LinearLayout appRow, inputRow, sysRow;
    android.widget.ScrollView pageScroll;
    static final int COLS = 6;
    final List<View> appTiles = new ArrayList<>();
    TextView ram;
    String appSig = "";
    /** package -> label, in display order (hidden apps included). */
    final LinkedHashMap<String, String> allApps = new LinkedHashMap<>();
    final Map<String, ResolveInfo> infos = new LinkedHashMap<>();

    int px(float v) { return Math.round(v * dp); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        prefs = getSharedPreferences("lumen", MODE_PRIVATE);
        pm = getPackageManager();

        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TR_BL, new int[]{BG2, BG, BG}));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(48), px(32), px(48), px(20));
        col.setClipChildren(false);
        col.setClipToPadding(false);
        root.setClipChildren(false);
        // Optional background: Android/data/com.lumen.launcher/files/fondo.jpg, right-aligned with a gradient so text stays readable
        bgImage = new ImageView(this);
        bgImage.setScaleType(ImageView.ScaleType.FIT_END);
        root.addView(bgImage, new FrameLayout.LayoutParams(-1, -1));
        bgScrim = new View(this);
        bgScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{BG, 0xCC151B24, 0x55151B24, 0x22151B24, 0x22151B24}));
        root.addView(bgScrim, new FrameLayout.LayoutParams(-1, -1, Gravity.END));
        bgImage.setVisibility(View.GONE);
        bgScrim.setVisibility(View.GONE);
        pageScroll = new android.widget.ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setClipChildren(false);
        pageScroll.setVerticalScrollBarEnabled(false);
        pageScroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        root.addView(pageScroll, new FrameLayout.LayoutParams(-1, -1));

        // Header: time, date and free RAM
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.VERTICAL);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextSize(46);
        clock.setTextColor(FG);
        clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        when.addView(clock);
        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d 'de' MMMM");
        date.setFormat12Hour("EEEE, d 'de' MMMM");
        date.setTextSize(14);
        date.setTextColor(MUTED);
        date.setPadding(0, px(4), 0, 0);
        when.addView(date);
        top.addView(when, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        ram = new TextView(this);
        ram.setTextSize(13);
        ram.setTextColor(MUTED);
        ram.setTypeface(Typeface.MONOSPACE);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xB3151B24);
        pill.setCornerRadius(px(14));
        ram.setBackground(pill);
        ram.setPadding(px(12), px(5), px(12), px(5));
        right.addView(ram);
        top.addView(right);
        col.addView(top);

        TextView at = new TextView(this);
        at.setText("APPS");
        at.setTextSize(11);
        at.setLetterSpacing(0.12f);
        at.setTextColor(MUTED);
        at.setTypeface(Typeface.DEFAULT_BOLD);
        at.setPadding(0, px(18), 0, px(2));
        col.addView(at);
        appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.VERTICAL);
        appRow.setClipChildren(false);
        appRow.setClipToPadding(false);
        appRow.setPadding(0, px(12), 0, 0);
        col.addView(appRow);
        inputRow = row();
        int before = col.getChildCount();
        section(col, "Entradas", inputRow, 12);
        inputViews = new View[]{col.getChildAt(before), col.getChildAt(before + 1)};
        setInputsVisible(prefs.getBoolean("showInputs", false));
        sysRow = row();
        section(col, "Sistema", sysRow, 12);

        setContentView(root);
        buildInputs();
        buildSystem();
    }

    LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setClipChildren(false);
        r.setClipToPadding(false);
        return r;
    }

    HorizontalScrollView section(LinearLayout col, String title, LinearLayout r, int topDp) {
        TextView t = new TextView(this);
        t.setText(title.toUpperCase());
        t.setTextSize(11);
        t.setLetterSpacing(0.12f);
        t.setTextColor(MUTED);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, px(topDp), 0, px(2));
        col.addView(t);
        HorizontalScrollView s = new HorizontalScrollView(this);
        s.setHorizontalScrollBarEnabled(false);
        s.setClipChildren(false);
        s.setClipToPadding(false);
        s.setPadding(px(16), px(12), px(16), px(12));
        s.addView(r);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.leftMargin = -px(16);
        col.addView(s, lp);
        return s;
    }

    FrameLayout tile(int wDp, int hDp, int color) {
        FrameLayout f = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(wDp), px(hDp));
        lp.rightMargin = px(14);
        f.setLayoutParams(lp);
        f.setFocusable(true);
        f.setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(10));
        bg.setColor(color);
        f.setBackground(bg);
        f.setClipToOutline(true);
        final GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(px(10));
        ring.setStroke(px(3), ACCENT);
        ring.setColor(Color.TRANSPARENT);
        f.setOnFocusChangeListener((v, has) -> {
            v.animate().scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f)
                    .translationZ(has ? px(10) : 0).setDuration(110).start();
            v.setForeground(has ? ring : null);
        });
        return f;
    }

    FrameLayout textTile(String title, String sub, boolean dim) {
        FrameLayout f = tile(130, 73, TILE);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.BOTTOM);
        l.setPadding(px(12), px(8), px(12), px(10));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTextColor(dim ? MUTED : FG);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setSingleLine(true);
        l.addView(t);
        if (sub != null) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextSize(11);
            s.setTextColor(MUTED);
            s.setSingleLine(true);
            l.addView(s);
        }
        f.addView(l, new FrameLayout.LayoutParams(-1, -1));
        return f;
    }

    // ---------- Apps ----------

    void loadApps() {
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
        infos.clear();
        final Map<String, String> labels = new LinkedHashMap<>();
        for (ResolveInfo ri : list) {
            String p = ri.activityInfo.packageName;
            if (p.equals(getPackageName()) || infos.containsKey(p)) continue;
            infos.put(p, ri);
            labels.put(p, String.valueOf(ri.loadLabel(pm)));
        }
        final List<String> saved = new ArrayList<>(Arrays.asList(prefs.getString("order", "").split(",")));
        List<String> pk = new ArrayList<>(labels.keySet());
        Collections.sort(pk, (a, b) -> {
            int ia = rank(saved, a), ib = rank(saved, b);
            if (ia != ib) return ia - ib;
            return labels.get(a).compareToIgnoreCase(labels.get(b));
        });
        allApps.clear();
        for (String p : pk) allApps.put(p, labels.get(p));

        importHidden();
        Set<String> hidden = hidden();
        StringBuilder sig = new StringBuilder();
        for (String p : pk) if (!hidden.contains(p)) sig.append(p).append(',');
        if (sig.toString().equals(appSig)) return;
        appSig = sig.toString();

        appRow.removeAllViews();
        appTiles.clear();
        LinearLayout cur = null;
        for (String p : pk) {
            if (hidden.contains(p)) continue;
            if (appTiles.size() % COLS == 0) {
                cur = row();
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-2, -2);
                rl.bottomMargin = px(14);
                appRow.addView(cur, rl);
            }
            View t = appTile(p, labels.get(p));
            cur.addView(t);
            appTiles.add(t);
        }
        if (!appTiles.isEmpty() && getCurrentFocus() == null) appTiles.get(0).requestFocus();
    }

    int rank(List<String> saved, String p) {
        int i = saved.indexOf(p);
        if (i >= 0) return i;
        int f = FAV.indexOf(p);
        return f >= 0 ? 1000 + f : 2000;
    }

    View appTile(final String pkg, final String label) {
        ResolveInfo ri = infos.get(pkg);
        final ActivityInfo ai = ri.activityInfo;
        FrameLayout f = tile(130, 73, colorFor(pkg));
        f.setTag(pkg);
        Drawable banner = ai.loadBanner(pm);
        if (banner != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(banner);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(ri.loadIcon(pm));
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(px(40), px(40), Gravity.CENTER_HORIZONTAL);
            il.topMargin = px(12);
            f.addView(iv, il);
            TextView t = new TextView(this);
            t.setText(label);
            t.setTextSize(12);
            t.setTextColor(FG);
            t.setSingleLine(true);
            t.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            tl.bottomMargin = px(8);
            f.addView(t, tl);
        }
        f.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setClassName(pkg, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            start(i, "No se pudo abrir " + label);
        });
        f.setOnLongClickListener(v -> { appMenu(pkg, label); return true; });
        return f;
    }

    int colorFor(String pkg) {
        float[] hsv = {Math.abs(pkg.hashCode() % 360), 0.35f, 0.38f};
        return Color.HSVToColor(hsv);
    }

    void appMenu(final String pkg, String label) {
        String[] opts = {"Mover antes", "Mover después", "Mover al inicio", "Ocultar", "Información de la app"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(label)
                .setItems(opts, (d, w) -> {
                    if (w <= 2) move(pkg, w == 0 ? -1 : w == 1 ? 1 : Integer.MIN_VALUE);
                    else if (w == 3) {
                        Set<String> hs = hidden();
                        hs.add(pkg);
                        prefs.edit().putStringSet("hidden", hs).apply();
                        reloadApps(null);
                    } else {
                        start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "No se pudo abrir la información");
                    }
                }).show();
    }

    void move(String pkg, int delta) {
        List<String> visible = new ArrayList<>();
        Set<String> hs = hidden();
        for (String p : allApps.keySet()) if (!hs.contains(p)) visible.add(p);
        int i = visible.indexOf(pkg);
        int j = delta == Integer.MIN_VALUE ? 0 : Math.max(0, Math.min(visible.size() - 1, i + delta));
        visible.remove(i);
        visible.add(j, pkg);
        for (String p : allApps.keySet()) if (hs.contains(p)) visible.add(p);
        prefs.edit().putString("order", String.join(",", visible)).apply();
        reloadApps(pkg);
    }

    void reloadApps(String focusPkg) {
        appSig = "";
        loadApps();
        if (focusPkg == null) { if (!appTiles.isEmpty()) appTiles.get(0).requestFocus(); return; }
        for (int k = 0; k < appTiles.size(); k++) {
            View c = appTiles.get(k);
            if (focusPkg.equals(c.getTag())) { c.requestFocus(); break; }
        }
    }

    /** Lets you hide apps from a PC: one package per line in Android/data/com.lumen.launcher/files/ocultar.txt */
    void importHidden() {
        File dir = getExternalFilesDir(null);
        if (dir == null) return;
        File f = new File(dir, "ocultar.txt");
        if (!f.exists()) return;
        Set<String> hs = hidden();
        try {
            for (String line : new String(java.nio.file.Files.readAllBytes(f.toPath())).split("\\s+"))
                if (!line.isEmpty()) hs.add(line.trim());
        } catch (java.io.IOException e) { return; }
        prefs.edit().putStringSet("hidden", hs).apply();
        f.delete();
    }

    Set<String> hidden() { return new HashSet<>(prefs.getStringSet("hidden", new HashSet<>())); }

    void showHidden() {
        final Set<String> hs = hidden();
        final List<String> list = new ArrayList<>();
        for (String p : allApps.keySet()) if (hs.contains(p)) list.add(p);
        if (list.isEmpty()) { toast("No hay apps ocultas"); return; }
        String[] names = new String[list.size()];
        for (int i = 0; i < names.length; i++) names[i] = "Mostrar " + allApps.get(list.get(i));
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Apps ocultas")
                .setItems(names, (d, w) -> {
                    hs.remove(list.get(w));
                    prefs.edit().putStringSet("hidden", hs).apply();
                    reloadApps(list.get(w));
                }).show();
    }

    // ---------- Inputs ----------

    void buildInputs() {
        inputRow.removeAllViews();
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        boolean tv = pm.getLaunchIntentForPackage(TV_APP) != null || pm.getLeanbackLaunchIntentForPackage(TV_APP) != null;
        if (tv) {
            FrameLayout t = textTile("TV", "Canales", false);
            t.setOnClickListener(v -> openTv());
            inputRow.addView(t);
        }
        if (tim != null) {
            List<TvInputInfo> ins = new ArrayList<>();
            for (TvInputInfo info : tim.getTvInputList())
                if (info.isPassthroughInput() && info.getParentId() == null) ins.add(info);
            Collections.sort(ins, (a, b) -> String.valueOf(a.loadLabel(this)).compareToIgnoreCase(String.valueOf(b.loadLabel(this))));
            for (final TvInputInfo info : ins) {
                int st = tim.getInputState(info.getId());
                boolean off = st == TvInputManager.INPUT_STATE_DISCONNECTED;
                final String label = String.valueOf(info.loadLabel(this));
                FrameLayout t = textTile(label, off ? "Sin cable" : "Conectado", off);
                t.setOnClickListener(v -> openInput(info.getId(), label));
                inputRow.addView(t);
            }
        }
        FrameLayout all = textTile("Todas", "Elegir entrada", false);
        all.setOnClickListener(v -> start(new Intent("com.android.tv.action.VIEW_INPUTS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "No se pudo abrir el selector de entradas"));
        inputRow.addView(all);
    }

    View[] inputViews;
    FrameLayout inputsTile;

    void setInputsVisible(boolean show) {
        for (View v : inputViews) v.setVisibility(show ? View.VISIBLE : View.GONE);
        updateInputsTile();
    }

    void updateInputsTile() {
        if (inputsTile == null) return;
        LinearLayout l = (LinearLayout) inputsTile.getChildAt(0);
        ((TextView) l.getChildAt(1)).setText(inputViews[0].getVisibility() == View.VISIBLE ? "Ocultar HDMI y TV" : "Mostrar HDMI y TV");
    }

    void openTv() {
        Intent i = pm.getLeanbackLaunchIntentForPackage(TV_APP);
        if (i == null) i = pm.getLaunchIntentForPackage(TV_APP);
        if (i == null) { toast("No se encontró la app de TV"); return; }
        start(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "No se pudo abrir la TV");
    }

    void openInput(String id, String label) {
        Uri u = TvContract.buildChannelUriForPassthroughInput(id);
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(u, "vnd.android.cursor.item/channel")
                .setPackage("com.google.android.tv.inputplayer").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return; } catch (ActivityNotFoundException e) { /* retry without a package */ }
        i.setPackage(null);
        start(i, "No se pudo cambiar a " + label);
    }

    // ---------- System ----------

    void buildSystem() {
        inputsTile = textTile("Entradas", "", false);
        inputsTile.setOnClickListener(v -> {
            boolean show = inputViews[0].getVisibility() != View.VISIBLE;
            setInputsVisible(show);
            prefs.edit().putBoolean("showInputs", show).apply();
            if (show && inputRow.getChildCount() > 0) inputRow.getChildAt(0).requestFocus();
        });
        sysRow.addView(inputsTile);
        updateInputsTile();
        FrameLayout s = textTile("Ajustes", "De la TV", false);
        s.setOnClickListener(v -> start(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "No se pudieron abrir los ajustes"));
        sysRow.addView(s);
        FrameLayout r = textTile("Liberar RAM", "Cerrar apps", false);
        r.setOnClickListener(v -> freeRam());
        sysRow.addView(r);
        FrameLayout hdn = textTile("Apps ocultas", "Volver a mostrar", false);
        hdn.setOnClickListener(v -> showHidden());
        sysRow.addView(hdn);
        FrameLayout fondo = textTile("Fondo", "Elegir o quitar", false);
        fondo.setOnClickListener(v -> backgroundMenu());
        sysRow.addView(fondo);
    }

    void freeRam() {
        final long before = availMb();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if (ai.packageName.equals(getPackageName())) continue;
            if ((ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0) continue;
            am.killBackgroundProcesses(ai.packageName);
        }
        h.postDelayed(() -> {
            long after = availMb();
            toast("Liberados " + Math.max(0, after - before) + " MB · libre " + after + " MB");
            updateRam();
        }, 1500);
    }

    /** The Google launcher stays alive in the background: kill it so it does not hold RAM. */
    final Runnable killGoogle = () -> {
        if (HomeService.googleMode) return;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses(GOOGLE_HOME.getPackageName());
    };

    ImageView bgImage;
    View bgScrim;
    long bgStamp = -1;

    void loadBackground() {
        File dir = getExternalFilesDir(null);
        File f = dir == null ? null : new File(dir, "fondo.jpg");
        long stamp = f != null && f.exists() ? f.lastModified() : 0;
        if (stamp == bgStamp) return;
        bgStamp = stamp;
        Bitmap bm = null;
        if (stamp != 0) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            bm = BitmapFactory.decodeFile(f.getPath(), o);
        }
        bgImage.setImageBitmap(bm);
        if (bm != null) {
            int sh = getResources().getDisplayMetrics().heightPixels;
            bgScrim.getLayoutParams().width = Math.round(bm.getWidth() * (sh / (float) bm.getHeight())) + 2;
            bgScrim.requestLayout();
        }
        int vis = bm == null ? View.GONE : View.VISIBLE;
        bgImage.setVisibility(vis);
        bgScrim.setVisibility(vis);
    }

    static final int PICK_BG = 7;

    File bgFile() {
        File dir = getExternalFilesDir(null);
        return dir == null ? null : new File(dir, "fondo.jpg");
    }

    void backgroundMenu() {
        File f = bgFile();
        boolean has = f != null && f.exists();
        String[] opts = has ? new String[]{"Elegir imagen", "Quitar fondo"} : new String[]{"Elegir imagen"};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Fondo")
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        try {
                            startActivityForResult(new Intent(android.provider.MediaStore.ACTION_PICK_IMAGES), PICK_BG);
                        } catch (Exception e) {
                            try {
                                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                                        .addCategory(Intent.CATEGORY_OPENABLE), PICK_BG);
                            } catch (Exception e2) { toast("Esta TV no tiene selector de imágenes"); }
                        }
                    } else {
                        f.delete();
                        loadBackground();
                        toast("Fondo quitado");
                    }
                }).show();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_BG || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri u = data.getData();
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (java.io.InputStream in = getContentResolver().openInputStream(u)) { BitmapFactory.decodeStream(in, null, o); }
            int target = getResources().getDisplayMetrics().heightPixels;
            int sample = 1;
            while (o.outHeight / (sample * 2) >= target && o.outWidth / (sample * 2) >= 320) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            Bitmap bm;
            try (java.io.InputStream in = getContentResolver().openInputStream(u)) { bm = BitmapFactory.decodeStream(in, null, o2); }
            if (bm == null) { toast("No se pudo leer la imagen"); return; }
            if (bm.getHeight() > target) {
                Bitmap sc = Bitmap.createScaledBitmap(bm, Math.round(bm.getWidth() * (target / (float) bm.getHeight())), target, true);
                bm.recycle();
                bm = sc;
            }
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(bgFile())) {
                bm.compress(Bitmap.CompressFormat.JPEG, 85, out);
            }
            bm.recycle();
            loadBackground();
            toast("Fondo actualizado");
        } catch (Exception e) {
            toast("No se pudo usar esa imagen");
        }
    }

    // ---------- RAM ----------

    long availMb() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
        return mi.availMem / (1024 * 1024);
    }

    void updateRam() {
        long mb = availMb();
        ram.setText("RAM libre  " + mb + " MB" + (mb < 150 ? "  · conviene liberar" : ""));
        ram.setTextColor(mb < 150 ? BAD : mb < 300 ? ACCENT : OK);
    }

    final Runnable ramTick = new Runnable() {
        @Override public void run() { updateRam(); h.postDelayed(this, 5000); }
    };

    // ---------- Lifecycle ----------

    @Override protected void onResume() {
        super.onResume();
        HomeService.googleMode = false;
        loadBackground();
        h.removeCallbacks(killGoogle);
        h.postDelayed(killGoogle, 4000);
        loadApps();
        buildInputs();
        h.removeCallbacks(ramTick);
        h.post(ramTick);
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(ramTick);
        h.removeCallbacks(killGoogle);
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        pageScroll.smoothScrollTo(0, 0);
        if (!appTiles.isEmpty()) appTiles.get(0).requestFocus();
    }

    @Override public void onBackPressed() { /* this is the home screen: there is nowhere to go back to */ }

    void start(Intent i, String err) {
        try { startActivity(i); } catch (Exception e) { toast(err); }
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
