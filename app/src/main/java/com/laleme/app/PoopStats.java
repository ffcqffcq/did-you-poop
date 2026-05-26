package com.laleme.app;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

final class PoopStats {
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");

    final int count;
    final int totalMinutes;
    final int days;
    final double averageCountPerDay;
    final double averageMinutesPerVisit;
    final String insight;

    private PoopStats(int count, int totalMinutes, int days, String insight) {
        this.count = count;
        this.totalMinutes = totalMinutes;
        this.days = days;
        this.averageCountPerDay = days <= 0 ? 0 : count / (double) days;
        this.averageMinutesPerVisit = count <= 0 ? 0 : totalMinutes / (double) count;
        this.insight = insight;
    }

    static PoopStats today(List<PoopEntry> entries) {
        long start = startOfToday();
        long end = start + 24L * 60L * 60L * 1000L;
        return build(entries, start, end, 1);
    }

    static PoopStats recentDays(List<PoopEntry> entries, int days) {
        long todayStart = startOfToday();
        long start = todayStart - (long) (days - 1) * 24L * 60L * 60L * 1000L;
        long end = todayStart + 24L * 60L * 60L * 1000L;
        return build(entries, start, end, days);
    }

    String averageCountText() {
        return ONE_DECIMAL.format(averageCountPerDay);
    }

    String averageDurationText() {
        return ONE_DECIMAL.format(averageMinutesPerVisit);
    }

    private static PoopStats build(List<PoopEntry> entries, long startInclusive, long endExclusive, int days) {
        int count = 0;
        int totalMinutes = 0;
        for (PoopEntry entry : entries) {
            if (entry.timeMillis >= startInclusive && entry.timeMillis < endExclusive) {
                count++;
                totalMinutes += Math.max(entry.durationMinutes, 0);
            }
        }
        return new PoopStats(count, totalMinutes, days, createInsight(count, totalMinutes, days));
    }

    private static String createInsight(int count, int totalMinutes, int days) {
        double avgCount = days <= 0 ? 0 : count / (double) days;
        double avgDuration = count <= 0 ? 0 : totalMinutes / (double) count;

        if (count == 0) {
            return "近期没有记录。若不是忘记记录，可以留意饮水、膳食纤维和运动量。";
        }
        if (avgCount < 0.5) {
            return "频率偏低，建议观察是否有便秘倾向，并增加饮水和蔬果摄入。";
        }
        if (avgCount > 3.0) {
            return "频率偏高，若伴随腹痛、腹泻或不适，建议及时咨询医生。";
        }
        if (avgDuration > 15.0) {
            return "单次时长偏长，尽量减少久坐用力，保持规律作息和饮食。";
        }
        return "近期记录看起来比较平稳，继续保持规律饮食和适量运动。";
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
