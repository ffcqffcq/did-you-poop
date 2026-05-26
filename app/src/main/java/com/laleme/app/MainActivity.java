package com.laleme.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 41;
    private static final int SECTION_RECORD = 0;
    private static final int SECTION_ANALYSIS = 1;
    private static final int SECTION_CALENDAR = 2;
    private static final int SECTION_REMINDER = 3;
    private static final String PREFS_UI = "laleme_ui";
    private static final String KEY_INTRO_VERSION_CODE = "intro_version_code";
    private static final String[] STOOL_TYPES = {
            "未选择",
            "偏硬",
            "正常",
            "偏软",
            "腹泻"
    };
    private static final int COLOR_BACKGROUND = Color.rgb(246, 241, 233);
    private static final int COLOR_CARD = Color.WHITE;
    private static final int COLOR_TEXT = Color.rgb(34, 36, 33);
    private static final int COLOR_MUTED = Color.rgb(104, 109, 101);
    private static final int COLOR_GREEN = Color.rgb(47, 125, 92);
    private static final int COLOR_CORAL = Color.rgb(190, 82, 64);
    private static final int COLOR_BLUE = Color.rgb(58, 93, 145);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
    private final SimpleDateFormat fullTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);

    private PoopStore store;
    private LinearLayout content;
    private LinearLayout bottomNav;
    private Button recordNavButton;
    private Button analysisNavButton;
    private Button calendarNavButton;
    private Button reminderNavButton;
    private LinearLayout todayCard;
    private LinearLayout recordCard;
    private LinearLayout timerCard;
    private LinearLayout reminderCard;
    private LinearLayout analysisCard;
    private LinearLayout dateCard;
    private LinearLayout historyCard;
    private TextView todayCountText;
    private TextView todayDurationText;
    private TextView todayAverageText;
    private TextView analysisTitleText;
    private TextView analysisBodyText;
    private LinearLayout trendList;
    private TextView selectedDateTitleText;
    private LinearLayout selectedDateList;
    private LinearLayout historyList;
    private EditText durationInput;
    private EditText noteInput;
    private TextView recordTimeText;
    private Spinner stoolTypeSpinner;
    private Switch discomfortSwitch;
    private TextView timerText;
    private Button timerButton;
    private Switch reminderSwitch;
    private TimePicker reminderPicker;
    private boolean showingMonth;
    private int currentSection = SECTION_RECORD;
    private long timerStartedAt;
    private long selectedRecordTimeMillis;
    private long selectedHistoryDayMillis;

    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            if (timerStartedAt > 0) {
                long elapsed = System.currentTimeMillis() - timerStartedAt;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
                timerText.setText(String.format(Locale.CHINA, "%02d:%02d", minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new PoopStore(this);
        selectedRecordTimeMillis = System.currentTimeMillis();
        selectedHistoryDayMillis = startOfDayMillis(System.currentTimeMillis());
        requestNotificationPermissionIfNeeded();
        buildScreen();
        showSection(SECTION_RECORD);
        refreshScreen();
        maybeShowIntroDialog();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(timerTick);
        super.onDestroy();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(28));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        buildHeader();
        buildTodayCard();
        buildRecordCard();
        buildTimerCard();
        buildReminderCard();
        buildAnalysisCard();
        buildDateCard();
        buildHistoryCard();
        buildBottomNav(root);

        setContentView(root);
        applySafeAreaPadding();
    }

    private void buildHeader() {
        ImageView mascot = new ImageView(this);
        mascot.setImageResource(R.drawable.ic_poop_cute);
        mascot.setAdjustViewBounds(true);
        LinearLayout.LayoutParams mascotParams = new LinearLayout.LayoutParams(dp(86), dp(86));
        mascotParams.gravity = Gravity.CENTER_HORIZONTAL;
        mascotParams.bottomMargin = dp(4);
        content.addView(mascot, mascotParams);

        TextView title = text("拉了么", 32, COLOR_TEXT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0);
        content.addView(title, matchWrap());

        TextView subtitle = text("记录每天的次数、时长和身体节奏", 15, COLOR_MUTED, Typeface.NORMAL);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        content.addView(subtitle, matchWrap());
    }

    private void buildTodayCard() {
        LinearLayout card = card();
        todayCard = card;
        card.addView(sectionTitle("今日概览"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(10), 0, 0);

        todayCountText = metric(row, "次数", COLOR_GREEN);
        todayDurationText = metric(row, "总时长", COLOR_CORAL);
        todayAverageText = metric(row, "平均", COLOR_BLUE);

        card.addView(row);
        content.addView(card);
    }

    private void buildRecordCard() {
        LinearLayout card = card();
        recordCard = card;
        card.addView(sectionTitle("手动记录"));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        timeRow.setPadding(0, dp(8), 0, dp(4));

        recordTimeText = text("", 15, COLOR_TEXT, Typeface.NORMAL);
        timeRow.addView(recordTimeText, weightWrap(1));

        Button nowButton = secondaryButton("现在");
        nowButton.setOnClickListener(v -> {
            selectedRecordTimeMillis = System.currentTimeMillis();
            updateRecordTimeText();
        });
        timeRow.addView(nowButton, wrapWrap());

        Button chooseButton = secondaryButton("补录");
        chooseButton.setOnClickListener(v -> pickDateTime(selectedRecordTimeMillis, timeMillis -> {
            selectedRecordTimeMillis = timeMillis;
            updateRecordTimeText();
        }));
        timeRow.addView(chooseButton, wrapWrap());
        card.addView(timeRow, matchWrap());
        updateRecordTimeText();

        durationInput = new EditText(this);
        durationInput.setHint("本次时长（分钟）");
        durationInput.setText("5");
        durationInput.setSingleLine(true);
        durationInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        durationInput.setTextColor(COLOR_TEXT);
        durationInput.setHintTextColor(COLOR_MUTED);
        card.addView(durationInput, matchWrap());

        stoolTypeSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, STOOL_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stoolTypeSpinner.setAdapter(adapter);
        stoolTypeSpinner.setSelection(typeIndex("正常"));
        card.addView(stoolTypeSpinner, matchWrapWithTop(6));

        discomfortSwitch = new Switch(this);
        discomfortSwitch.setText("本次有腹痛、腹泻或明显不适");
        discomfortSwitch.setTextColor(COLOR_TEXT);
        discomfortSwitch.setTextSize(15);
        card.addView(discomfortSwitch, matchWrapWithTop(4));

        noteInput = new EditText(this);
        noteInput.setHint("备注（可选，例如饮食、状态）");
        noteInput.setSingleLine(false);
        noteInput.setMinLines(2);
        noteInput.setTextColor(COLOR_TEXT);
        noteInput.setHintTextColor(COLOR_MUTED);
        card.addView(noteInput, matchWrap());

        Button addButton = primaryButton("记录一次", COLOR_GREEN);
        addButton.setOnClickListener(v -> addManualEntry());
        card.addView(addButton, matchWrapWithTop(12));

        content.addView(card);
    }

    private void buildTimerCard() {
        LinearLayout card = card();
        timerCard = card;
        card.addView(sectionTitle("计时记录"));

        timerText = text("00:00", 42, COLOR_TEXT, Typeface.BOLD);
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(0, dp(8), 0, dp(8));
        card.addView(timerText, matchWrap());

        timerButton = primaryButton("开始计时", COLOR_CORAL);
        timerButton.setOnClickListener(v -> toggleTimer());
        card.addView(timerButton, matchWrap());

        content.addView(card);
    }

    private void buildReminderCard() {
        LinearLayout card = card();
        reminderCard = card;
        card.addView(sectionTitle("定时提醒"));

        reminderSwitch = new Switch(this);
        reminderSwitch.setText("开启每日提醒");
        reminderSwitch.setTextColor(COLOR_TEXT);
        reminderSwitch.setTextSize(16);
        reminderSwitch.setChecked(store.isReminderEnabled());
        card.addView(reminderSwitch, matchWrap());

        reminderPicker = new TimePicker(this);
        reminderPicker.setIs24HourView(true);
        reminderPicker.setHour(store.getReminderHour());
        reminderPicker.setMinute(store.getReminderMinute());
        card.addView(reminderPicker, matchWrap());

        Button saveButton = primaryButton("保存提醒", COLOR_BLUE);
        saveButton.setOnClickListener(v -> saveReminder());
        card.addView(saveButton, matchWrapWithTop(8));

        reminderSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                requestNotificationPermissionIfNeeded();
            }
        });

        content.addView(card);
    }

    private void buildAnalysisCard() {
        LinearLayout card = card();
        analysisCard = card;
        card.addView(sectionTitle("数据分析"));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);

        Button weekButton = secondaryButton("近 7 天");
        Button monthButton = secondaryButton("近 30 天");
        weekButton.setOnClickListener(v -> {
            showingMonth = false;
            refreshScreen();
        });
        monthButton.setOnClickListener(v -> {
            showingMonth = true;
            refreshScreen();
        });
        tabs.addView(weekButton, weightWrap(1));
        tabs.addView(monthButton, weightWrap(1));
        card.addView(tabs, matchWrapWithTop(4));

        analysisTitleText = text("", 18, COLOR_TEXT, Typeface.BOLD);
        analysisTitleText.setPadding(0, dp(14), 0, dp(4));
        card.addView(analysisTitleText, matchWrap());

        analysisBodyText = text("", 15, COLOR_MUTED, Typeface.NORMAL);
        analysisBodyText.setLineSpacing(dp(2), 1.0f);
        card.addView(analysisBodyText, matchWrap());

        TextView trendTitle = text("每日趋势", 16, COLOR_TEXT, Typeface.BOLD);
        trendTitle.setPadding(0, dp(14), 0, dp(4));
        card.addView(trendTitle, matchWrap());

        trendList = new LinearLayout(this);
        trendList.setOrientation(LinearLayout.VERTICAL);
        card.addView(trendList, matchWrap());

        TextView note = text("仅作为生活记录参考；若出现持续腹痛、腹泻、便血或明显不适，请及时就医。", 13, COLOR_MUTED, Typeface.NORMAL);
        note.setPadding(0, dp(10), 0, 0);
        card.addView(note, matchWrap());

        content.addView(card);
    }

    private void buildHistoryCard() {
        LinearLayout card = card();
        historyCard = card;
        card.addView(sectionTitle("最近记录"));
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        card.addView(historyList, matchWrapWithTop(4));
        content.addView(card);
    }

    private void buildDateCard() {
        LinearLayout card = card();
        dateCard = card;
        card.addView(sectionTitle("按日期查看"));

        CalendarView calendarView = new CalendarView(this);
        calendarView.setFirstDayOfWeek(Calendar.MONDAY);
        calendarView.setDate(selectedHistoryDayMillis, false, true);
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            resetToStartOfDay(selected);
            selectedHistoryDayMillis = selected.getTimeInMillis();
            refreshScreen();
        });
        card.addView(calendarView, matchWrapWithTop(4));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        Button todayButton = secondaryButton("今天");
        todayButton.setOnClickListener(v -> {
            selectedHistoryDayMillis = startOfDayMillis(System.currentTimeMillis());
            calendarView.setDate(selectedHistoryDayMillis, true, true);
            refreshScreen();
        });
        actions.addView(todayButton, weightWrap(1));

        Button addForDayButton = primaryButton("补录这天", COLOR_GREEN);
        addForDayButton.setOnClickListener(v -> {
            selectedRecordTimeMillis = selectedDayWithCurrentTime();
            updateRecordTimeText();
            Toast.makeText(this, "已把手动记录时间设为所选日期", Toast.LENGTH_SHORT).show();
        });
        actions.addView(addForDayButton, weightWrap(1));
        card.addView(actions, matchWrapWithTop(8));

        selectedDateTitleText = text("", 16, COLOR_TEXT, Typeface.BOLD);
        selectedDateTitleText.setPadding(0, dp(14), 0, dp(4));
        card.addView(selectedDateTitleText, matchWrap());

        selectedDateList = new LinearLayout(this);
        selectedDateList.setOrientation(LinearLayout.VERTICAL);
        card.addView(selectedDateList, matchWrap());

        content.addView(card);
    }

    private void buildBottomNav(LinearLayout root) {
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(10), dp(8), dp(10), dp(10));
        bottomNav.setBackgroundColor(Color.WHITE);

        recordNavButton = navButton("记录", SECTION_RECORD);
        analysisNavButton = navButton("分析", SECTION_ANALYSIS);
        calendarNavButton = navButton("日历", SECTION_CALENDAR);
        reminderNavButton = navButton("提醒", SECTION_REMINDER);

        bottomNav.addView(recordNavButton, weightWrap(1));
        bottomNav.addView(analysisNavButton, weightWrap(1));
        bottomNav.addView(calendarNavButton, weightWrap(1));
        bottomNav.addView(reminderNavButton, weightWrap(1));
        root.addView(bottomNav, matchWrap());
    }

    private Button navButton(String label, int section) {
        Button button = secondaryButton(label);
        button.setMinHeight(dp(46));
        button.setOnClickListener(v -> showSection(section));
        return button;
    }

    private void showSection(int section) {
        currentSection = section;
        setCardVisible(todayCard, section == SECTION_RECORD);
        setCardVisible(recordCard, section == SECTION_RECORD);
        setCardVisible(timerCard, section == SECTION_RECORD);
        setCardVisible(analysisCard, section == SECTION_ANALYSIS);
        setCardVisible(dateCard, section == SECTION_CALENDAR);
        setCardVisible(historyCard, section == SECTION_CALENDAR);
        setCardVisible(reminderCard, section == SECTION_REMINDER);

        updateNavButton(recordNavButton, section == SECTION_RECORD);
        updateNavButton(analysisNavButton, section == SECTION_ANALYSIS);
        updateNavButton(calendarNavButton, section == SECTION_CALENDAR);
        updateNavButton(reminderNavButton, section == SECTION_REMINDER);
    }

    private void setCardVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateNavButton(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTextColor(selected ? Color.WHITE : COLOR_TEXT);
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? COLOR_GREEN : Color.TRANSPARENT);
        background.setStroke(dp(1), selected ? COLOR_GREEN : Color.rgb(219, 211, 199));
        background.setCornerRadius(dp(8));
        button.setBackground(background);
    }

    private void applySafeAreaPadding() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            content.setPadding(dp(18), dp(36), dp(18), dp(28));
            return;
        }
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = Math.max(dp(36), insets.getSystemWindowInsetTop() + dp(12));
            int bottom = dp(18);
            view.setPadding(dp(18), top, dp(18), bottom);
            if (bottomNav != null) {
                int navBottom = Math.max(dp(10), insets.getSystemWindowInsetBottom() + dp(8));
                bottomNav.setPadding(dp(10), dp(8), dp(10), navBottom);
            }
            return insets;
        });
        content.requestApplyInsets();
    }

    private void maybeShowIntroDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_UI, MODE_PRIVATE);
        int currentVersion = currentVersionCode();
        if (prefs.getInt(KEY_INTRO_VERSION_CODE, 0) >= currentVersion) {
            return;
        }
        prefs.edit().putInt(KEY_INTRO_VERSION_CODE, currentVersion).apply();
        new AlertDialog.Builder(this)
                .setTitle("欢迎使用拉了么")
                .setMessage("拉了么用于记录每天的如厕次数、时长、形态和身体状态，并提供提醒、趋势和日历查看功能。\n\n所有数据默认保存在本机，仅作为生活记录参考，不能替代专业医疗建议。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private int currentVersionCode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
            }
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 1;
        }
    }

    private void refreshScreen() {
        List<PoopEntry> entries = store.loadEntries();
        PoopStats today = PoopStats.today(entries);
        todayCountText.setText(getString(R.string.today_count_format, today.count));
        todayDurationText.setText(getString(R.string.today_duration_format, today.totalMinutes));
        todayAverageText.setText(getString(R.string.today_average_format, today.averageDurationText()));

        PoopStats stats = PoopStats.recentDays(entries, showingMonth ? 30 : 7);
        analysisTitleText.setText(showingMonth ? "近 30 天概况" : "近 7 天概况");
        analysisBodyText.setText(getString(
                R.string.analysis_body_format,
                stats.count,
                stats.totalMinutes,
                stats.averageCountText(),
                stats.averageDurationText(),
                stats.insight
        ));
        refreshTrend(entries, showingMonth ? 30 : 7);
        refreshSelectedDate(entries);

        refreshHistory(entries);
    }

    private void refreshTrend(List<PoopEntry> entries, int days) {
        trendList.removeAllViews();
        int visibleDays = Math.min(days, showingMonth ? 10 : 7);
        Calendar day = Calendar.getInstance();
        resetToStartOfDay(day);

        for (int i = 0; i < visibleDays; i++) {
            long start = day.getTimeInMillis();
            long end = start + TimeUnit.DAYS.toMillis(1);
            int count = 0;
            int minutes = 0;
            for (PoopEntry entry : entries) {
                if (entry.timeMillis >= start && entry.timeMillis < end) {
                    count++;
                    minutes += Math.max(entry.durationMinutes, 0);
                }
            }

            TextView row = text(
                    new SimpleDateFormat("MM-dd", Locale.CHINA).format(day.getTime())
                            + "："
                            + count
                            + " 次 / "
                            + minutes
                            + " 分钟",
                    14,
                    count == 0 ? COLOR_MUTED : COLOR_TEXT,
                    Typeface.NORMAL
            );
            row.setPadding(0, dp(3), 0, dp(3));
            trendList.addView(row, matchWrap());
            day.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    private void refreshSelectedDate(List<PoopEntry> entries) {
        selectedDateList.removeAllViews();
        long start = selectedHistoryDayMillis;
        long end = start + TimeUnit.DAYS.toMillis(1);
        int count = 0;
        int minutes = 0;

        for (PoopEntry entry : entries) {
            if (entry.timeMillis >= start && entry.timeMillis < end) {
                count++;
                minutes += Math.max(entry.durationMinutes, 0);
            }
        }

        selectedDateTitleText.setText(dateOnlyFormat.format(new Date(start))
                + "："
                + count
                + " 次 / "
                + minutes
                + " 分钟");

        if (count == 0) {
            TextView empty = text("这一天还没有记录。", 15, COLOR_MUTED, Typeface.NORMAL);
            empty.setPadding(0, dp(8), 0, 0);
            selectedDateList.addView(empty);
            return;
        }

        for (PoopEntry entry : entries) {
            if (entry.timeMillis >= start && entry.timeMillis < end) {
                addRecordRow(selectedDateList, entry);
            }
        }
    }

    private void refreshHistory(List<PoopEntry> entries) {
        historyList.removeAllViews();
        if (entries.isEmpty()) {
            TextView empty = text("还没有记录，今天可以从第一条开始。", 15, COLOR_MUTED, Typeface.NORMAL);
            empty.setPadding(0, dp(8), 0, 0);
            historyList.addView(empty);
            return;
        }

        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            addRecordRow(historyList, entries.get(i));
        }
    }

    private void addRecordRow(LinearLayout parent, PoopEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView detail = text(
                formatEntry(entry),
                15,
                COLOR_TEXT,
                Typeface.NORMAL
        );
        row.addView(detail, weightWrap(1));

        Button edit = secondaryButton("编辑");
        edit.setOnClickListener(v -> showEditDialog(entry));
        row.addView(edit, wrapWrap());

        Button delete = secondaryButton("删除");
        delete.setTextColor(COLOR_CORAL);
        delete.setOnClickListener(v -> {
            store.deleteEntry(entry.id);
            refreshScreen();
        });
        row.addView(delete, wrapWrap());
        parent.addView(row);
    }

    private void addManualEntry() {
        int minutes = parseDuration();
        if (minutes <= 0) {
            Toast.makeText(this, "请输入有效时长", Toast.LENGTH_SHORT).show();
            return;
        }
        store.addEntry(new PoopEntry(
                selectedRecordTimeMillis,
                minutes,
                noteInput.getText().toString(),
                selectedStoolType(),
                discomfortSwitch.isChecked()
        ));
        selectedRecordTimeMillis = System.currentTimeMillis();
        updateRecordTimeText();
        noteInput.setText("");
        discomfortSwitch.setChecked(false);
        stoolTypeSpinner.setSelection(typeIndex("正常"));
        Toast.makeText(this, "已记录", Toast.LENGTH_SHORT).show();
        refreshScreen();
    }

    private void toggleTimer() {
        if (timerStartedAt == 0) {
            timerStartedAt = System.currentTimeMillis();
            timerButton.setText("结束并记录");
            handler.post(timerTick);
            return;
        }

        long elapsed = System.currentTimeMillis() - timerStartedAt;
        int minutes = (int) Math.max(1, Math.ceil(elapsed / 60000.0));
        store.addEntry(new PoopEntry(System.currentTimeMillis(), minutes, "计时记录"));
        timerStartedAt = 0;
        handler.removeCallbacks(timerTick);
        timerText.setText(R.string.zero_timer);
        timerButton.setText("开始计时");
        Toast.makeText(this, "已记录 " + minutes + " 分钟", Toast.LENGTH_SHORT).show();
        refreshScreen();
    }

    private void showEditDialog(PoopEntry entry) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(12);
        form.setPadding(padding, padding, padding, 0);

        long[] editTimeMillis = {entry.timeMillis};
        TextView editTimeText = text(fullTimeFormat.format(new Date(editTimeMillis[0])), 15, COLOR_TEXT, Typeface.NORMAL);
        form.addView(editTimeText, matchWrap());

        Button chooseTime = secondaryButton("选择日期和时间");
        chooseTime.setOnClickListener(v -> pickDateTime(editTimeMillis[0], timeMillis -> {
            editTimeMillis[0] = timeMillis;
            editTimeText.setText(fullTimeFormat.format(new Date(timeMillis)));
        }));
        form.addView(chooseTime, matchWrapWithTop(8));

        EditText duration = new EditText(this);
        duration.setHint("本次时长（分钟）");
        duration.setText(String.valueOf(entry.durationMinutes));
        duration.setSingleLine(true);
        duration.setInputType(InputType.TYPE_CLASS_NUMBER);
        duration.setTextColor(COLOR_TEXT);
        duration.setHintTextColor(COLOR_MUTED);
        form.addView(duration, matchWrapWithTop(8));

        Spinner stoolType = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, STOOL_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stoolType.setAdapter(adapter);
        stoolType.setSelection(typeIndex(entry.stoolType));
        form.addView(stoolType, matchWrapWithTop(8));

        Switch discomfort = new Switch(this);
        discomfort.setText("本次有腹痛、腹泻或明显不适");
        discomfort.setTextColor(COLOR_TEXT);
        discomfort.setTextSize(15);
        discomfort.setChecked(entry.discomfort);
        form.addView(discomfort, matchWrapWithTop(4));

        EditText note = new EditText(this);
        note.setHint("备注");
        note.setSingleLine(false);
        note.setMinLines(2);
        note.setText(entry.note);
        note.setTextColor(COLOR_TEXT);
        note.setHintTextColor(COLOR_MUTED);
        form.addView(note, matchWrapWithTop(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑记录")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int minutes = parseDuration(duration);
            if (minutes <= 0) {
                Toast.makeText(this, "请输入有效时长", Toast.LENGTH_SHORT).show();
                return;
            }
            store.updateEntry(new PoopEntry(
                    entry.id,
                    editTimeMillis[0],
                    minutes,
                    note.getText().toString(),
                    spinnerValue(stoolType),
                    discomfort.isChecked()
            ));
            dialog.dismiss();
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
            refreshScreen();
        }));
        dialog.show();
    }

    private void saveReminder() {
        int hour = reminderPicker.getHour();
        int minute = reminderPicker.getMinute();
        boolean enabled = reminderSwitch.isChecked();
        store.setReminderTime(hour, minute);
        store.setReminderEnabled(enabled);

        if (enabled) {
            ReminderScheduler.schedule(this, hour, minute);
            Toast.makeText(this, "提醒已开启", Toast.LENGTH_SHORT).show();
        } else {
            ReminderScheduler.cancel(this);
            Toast.makeText(this, "提醒已关闭", Toast.LENGTH_SHORT).show();
        }
    }

    private int parseDuration() {
        return parseDuration(durationInput);
    }

    private int parseDuration(EditText input) {
        try {
            int minutes = Integer.parseInt(input.getText().toString().trim());
            return Math.min(minutes, 240);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void updateRecordTimeText() {
        if (recordTimeText != null) {
            recordTimeText.setText("记录时间：" + fullTimeFormat.format(new Date(selectedRecordTimeMillis)));
        }
    }

    private void pickDateTime(long initialTimeMillis, TimeSelectionCallback callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(initialTimeMillis);
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(initialTimeMillis);
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timeDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                callback.onSelected(selected.getTimeInMillis());
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            true
                    );
                    timeDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private void resetToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private long startOfDayMillis(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        resetToStartOfDay(calendar);
        return calendar.getTimeInMillis();
    }

    private long selectedDayWithCurrentTime() {
        Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(selectedHistoryDayMillis);
        Calendar now = Calendar.getInstance();
        selected.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        selected.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);
        return selected.getTimeInMillis();
    }

    private String formatEntry(PoopEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(timeFormat.format(new Date(entry.timeMillis)))
                .append("  ")
                .append(entry.durationMinutes)
                .append(" 分钟");
        if (!entry.stoolType.isEmpty()) {
            builder.append("  ").append(entry.stoolType);
        }
        if (entry.discomfort) {
            builder.append("  有不适");
        }
        if (!entry.note.isEmpty()) {
            builder.append("\n").append(entry.note);
        }
        return builder.toString();
    }

    private String selectedStoolType() {
        return spinnerValue(stoolTypeSpinner);
    }

    private String spinnerValue(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        if (selected == null || "未选择".contentEquals(selected.toString())) {
            return "";
        }
        return selected.toString();
    }

    private int typeIndex(String value) {
        for (int i = 0; i < STOOL_TYPES.length; i++) {
            if (STOOL_TYPES[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(COLOR_CARD);
        background.setCornerRadius(dp(8));
        layout.setBackground(background);
        layout.setLayoutParams(matchWrapWithTop(12));
        return layout;
    }

    private TextView sectionTitle(String value) {
        return text(value, 18, COLOR_TEXT, Typeface.BOLD);
    }

    private TextView metric(LinearLayout row, String label, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);

        TextView value = text("0", 20, color, Typeface.BOLD);
        value.setGravity(Gravity.CENTER);
        TextView caption = text(label, 12, COLOR_MUTED, Typeface.NORMAL);
        caption.setGravity(Gravity.CENTER);

        box.addView(value);
        box.addView(caption);
        row.addView(box, weightWrap(1));
        return value;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        textView.setIncludeFontPadding(true);
        return textView;
    }

    private Button primaryButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setAllCaps(false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(8));
        button.setBackground(background);
        button.setMinHeight(dp(48));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(COLOR_TEXT);
        button.setTextSize(14);
        button.setAllCaps(false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.TRANSPARENT);
        background.setStroke(dp(1), Color.rgb(219, 211, 199));
        background.setCornerRadius(dp(8));
        button.setBackground(background);
        button.setMinHeight(dp(42));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams matchWrapWithTop(int topDp) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(topDp);
        return params;
    }

    private LinearLayout.LayoutParams weightWrap(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface TimeSelectionCallback {
        void onSelected(long timeMillis);
    }
}
