package com.wonderpush.sdk.segmentation;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISO8601Duration {

    public final boolean positive;
    public final double years;
    public final double months;
    public final double weeks;
    public final double days;
    public final double hours;
    public final double minutes;
    public final double seconds;

    // "\\d+(?:(?:[.,])\\d*)?" matches a double number
    public static final Pattern PATTERN = Pattern.compile("^([+-])?P(\\d+(?:(?:[.,])\\d*)?Y)?(\\d+(?:(?:[.,])\\d*)?M)?(\\d+(?:(?:[.,])\\d*)?W)?(\\d+(?:(?:[.,])\\d*)?D)?(?:T(\\d+(?:(?:[.,])\\d*)?H)?(\\d+(?:(?:[.,])\\d*)?M)?(\\d+(?:(?:[.,])\\d*)?S)?)?$");

    public ISO8601Duration(boolean positive, double years, double months, double weeks, double days, double hours, double minutes, double seconds) {
        this.positive = positive;
        this.years = years;
        this.months = months;
        this.weeks = weeks;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(positive ? '+' : '-');
        sb.append('P');
        sb.append(years);
        sb.append('Y');
        sb.append(months);
        sb.append('M');
        sb.append(weeks);
        sb.append('W');
        sb.append(days);
        sb.append('D');
        sb.append('T');
        sb.append(hours);
        sb.append('H');
        sb.append(minutes);
        sb.append('M');
        sb.append(seconds);
        sb.append('S');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ISO8601Duration duration = (ISO8601Duration) o;

        if (positive != duration.positive) return false;
        if (Double.compare(duration.years, years) != 0) return false;
        if (Double.compare(duration.months, months) != 0) return false;
        if (Double.compare(duration.weeks, weeks) != 0) return false;
        if (Double.compare(duration.days, days) != 0) return false;
        if (Double.compare(duration.hours, hours) != 0) return false;
        if (Double.compare(duration.minutes, minutes) != 0) return false;
        return Double.compare(duration.seconds, seconds) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (positive ? 1 : 0);
        temp = Double.doubleToLongBits(years);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(months);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(weeks);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(days);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(hours);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minutes);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(seconds);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public static ISO8601Duration parse(String input) throws BadInputError {
        if (input == null) {
            throw new BadInputError("\"PT\" ISO 8601 duration expects a string");
        }
        Matcher matcher = PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new BadInputError("invalid \"PT\" ISO 8601 duration given");
        }
        boolean positive = !"-".equals(matcher.group(1));
        double years = getPart(matcher, 2);
        double months = getPart(matcher, 3);
        double weeks = getPart(matcher, 4);
        double days = getPart(matcher, 5);
        double hours = getPart(matcher, 6);
        double minutes = getPart(matcher, 7);
        double seconds = getPart(matcher, 8);
        return new ISO8601Duration(positive, years, months, weeks, days, hours, minutes, seconds);
    }

    private static double getPart(Matcher matcher, int group) {
        String text = matcher.group(group);
        if (text == null) {
            return 0;
        }
        // Remove unit
        text = text.substring(0, text.length() - 1);
        // Parse number
        try {
            return Double.parseDouble(text.replaceAll(",", "."));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public long applyTo(long date) {
        Calendar rtn = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        rtn.setTimeInMillis(date);
        double remainder = 0;
        int sign = this.positive ? 1 : -1;
        int yearsInt = (int) (this.years + remainder);
        rtn.add(Calendar.YEAR, sign * yearsInt);
        remainder = this.years + remainder - yearsInt;
        remainder *= 12;
        int monthsInt = (int) (this.months + remainder);
        rtn.add(Calendar.MONTH, sign * monthsInt);
        remainder = this.months + remainder - monthsInt;
        remainder *= rtn.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysInt = (int) (this.days + this.weeks * 7 + remainder);
        rtn.add(Calendar.DAY_OF_MONTH, sign * daysInt);
        remainder = this.days + this.weeks * 7 + remainder - daysInt;
        remainder *= 24;
        int hoursInt = (int) (this.hours + remainder);
        rtn.add(Calendar.HOUR_OF_DAY, sign * hoursInt);
        remainder = this.hours + remainder - hoursInt;
        remainder *= 60;
        int minutesInt = (int) (this.minutes + remainder);
        rtn.add(Calendar.MINUTE, sign * minutesInt);
        remainder = this.minutes + remainder - minutesInt;
        remainder *= 60;
        int secondsInt = (int) (this.seconds + remainder);
        rtn.add(Calendar.SECOND, sign * secondsInt);
        remainder = this.seconds + remainder - secondsInt;
        remainder *= 1000;
        int milliSecondsInt = (int) Math.round(0 + remainder);
        rtn.add(Calendar.MILLISECOND, sign * milliSecondsInt);
        return rtn.getTimeInMillis();
    }

}
