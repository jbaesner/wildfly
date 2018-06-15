/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.ejb3.timerservice.schedule.temporal;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.util.Arrays;
import java.util.Objects;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.timerservice.schedule.CalendarBasedTimeout;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth;
import org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfWeek;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Hour;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Minute;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Month;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Second;
import org.jboss.as.ejb3.timerservice.schedule.attribute.Year;
import org.jboss.logging.Logger;

/**
 * An implementation to adjust a {@link ZonedDateTime} with a {@link ScheduleExpression}.
 * <p/>
 * The rules used in the implementation within this class have been taken from the previous version of the
 * {@link CalendarBasedTimeout} class and been adapted to used the classes from the java.time API.
 *
 * @author Joerg Baesner
 */
public class ScheduleExpressionAdjuster implements TemporalAdjuster {

    /**
     * logger used to log EJB timer messages
     */
    private final EjbLogger EJB3_TIMER_LOGGER = Logger.getMessageLogger(EjbLogger.class, "org.jboss.as.ejb3.timer");

    private final ScheduleExpression schedule;

    /**
     * The {@link Second} created out of the {@link javax.ejb.ScheduleExpression#getSecond()} value
     */
    private Second second;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Minute} created out of the
     * {@link javax.ejb.ScheduleExpression#getMinute()} value
     */
    private Minute minute;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Hour} created out of the
     * {@link javax.ejb.ScheduleExpression#getHour()} value
     */
    private Hour hour;

    /**
     * The {@link DayOfWeek} created out of the {@link javax.ejb.ScheduleExpression#getDayOfWeek()} value
     */
    private DayOfWeek dayOfWeek;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.DayOfMonth} created out of the
     * {@link javax.ejb.ScheduleExpression#getDayOfMonth()} value
     */
    private DayOfMonth dayOfMonth;

    /**
     * The {@link Month} created out of the {@link javax.ejb.ScheduleExpression#getMonth()} value
     */
    private Month month;

    /**
     * The {@link org.jboss.as.ejb3.timerservice.schedule.attribute.Year} created out of the
     * {@link javax.ejb.ScheduleExpression#getYear()} value
     */
    private Year year;

    private ZoneId zoneId;

    private ZonedDateTime start = null;

    private ZonedDateTime end = null;

    private ZonedDateTime firstTimeout;

    /**
     * Immutable first time as {@link LocalTime} create from the {@link ScheduleExpression} value in the constructor
     */
    private final LocalTime firstTime;

    public ScheduleExpressionAdjuster(ScheduleExpression schedule) {
        this.schedule = schedule;

        this.zoneId = fromTimezoneId(schedule.getTimezone());

        this.start = (schedule.getStart() != null) ? ZonedDateTime.ofInstant(schedule.getStart().toInstant(), zoneId) : null;
        this.end = (schedule.getEnd() != null) ? ZonedDateTime.ofInstant(schedule.getEnd().toInstant(), zoneId) : null;

        // parse the ScheduleExpression
        this.second = new Second(schedule.getSecond());
        this.minute = new Minute(schedule.getMinute());
        this.hour = new Hour(schedule.getHour());
        this.dayOfWeek = new DayOfWeek(schedule.getDayOfWeek());
        this.dayOfMonth = new DayOfMonth(schedule.getDayOfMonth());
        this.month = new Month(schedule.getMonth());
        this.year = new Year(schedule.getYear());

        // set the first time
        this.firstTime = LocalTime.of(hour.getFirst(), minute.getFirst(), second.getFirst()).withNano(0);

        // determine the first Timeout
        this.firstTimeout = determineFirstTimeout(start);
    }

    /**
     * Return the {@link ScheduleExpression} used to create this instance.
     *
     * @return
     */
    public ScheduleExpression getScheduleExpression() {
        return this.schedule;
    }

    /**
     * Return the {@link ZoneId} of this instance. This is the <code>ZoneId</code> used in the
     * {@link ScheduleExpressionAdjuster} during the creation of this inctance.
     *
     * @return
     */
    public ZoneId getZoneId() {
        return this.zoneId;
    }

    /**
     * Return the FirstTimeout that was determined directly after the creation of this instance.
     *
     * @return
     */
    public ZonedDateTime getFirstTimeout() {
        return this.firstTimeout;
    }

    /**
     * Return a {@link Temporal} of the type {@link ZonedDateTime} adjusted by the {@link ScheduleExpression} used when creating
     * this {@link TemporalAdjuster}.
     * <p/>
     * This method will return <code>null</code> if there's not further timeout according to the specification of the
     * <code>ScheduleExpression</code> based on the given <i>temporal</i>.
     *
     * @param temporal the temporal object to adjust, not null. This temporal object must be of the type
     *        <code>ZonedDateTime</code>
     * @return an object of the same observable type with the adjustment made, could be null
     * @throws IllegalArgumentException if temporal is not a <code>ZonedDateTime</code> instance
     */
    @Override
    public Temporal adjustInto(Temporal temporal) {
        Objects.requireNonNull(temporal, "temporal");

        // ensure the Temporal is a ZonedDateTime
        if (!(temporal instanceof ZonedDateTime)) {
            throw EJB3_TIMER_LOGGER.invalidTemporalType(temporal.getClass().getName());
        }

        // we need to adjust the Zone if the incoming ZonedDateTime is different from what is set on the ScheduleExpression
        ZonedDateTime nextTrigger = ((ZonedDateTime) temporal).withZoneSameInstant(zoneId);

        return determineNextTrigger(nextTrigger, true);
    }

    private ZonedDateTime determineFirstTimeout(ZonedDateTime start) {

        final ZonedDateTime firstTimeout;

        if (start != null) {
            firstTimeout = start;
        } else {
            /*
             * no start date available in the ScheduleExpression: use 'now' and reset to the first time afterwards substract a
             * second which is added in 'adjustInto' automatically afterwards
             */
            firstTimeout = ZonedDateTime.now(zoneId).with(localTimeOf(firstTime)).minusSeconds(1);
        }

        // now determine the firstTimeout
        return determineNextTrigger(firstTimeout, false);
    }

    private ZonedDateTime determineNextTrigger(ZonedDateTime nextTrigger, boolean increment) {

        // no more timeouts, return <null>
        if (this.noMoreTimeouts(nextTrigger)) {
            return null;
        }

        // adjust the nextTrigger - either to the start ZonedDateTime
        // or by adding a second if increment is true
        if (start != null && nextTrigger.isBefore(start)) {
            nextTrigger = start;
        } else {
            if (increment) {
                nextTrigger = nextTrigger.plusSeconds(1);
            }
            nextTrigger = nextTrigger.withNano(0);
        }

        nextTrigger = this.computeNextTime(nextTrigger);
        if (nextTrigger == null) {
            return null;
        }

        nextTrigger = this.computeNextMonth(nextTrigger);
        if (nextTrigger == null) {
            return null;
        }

        nextTrigger = this.computeNextDate(nextTrigger);
        if (nextTrigger == null) {
            return null;
        }

        nextTrigger = this.computeNextYear(nextTrigger);
        if (nextTrigger == null) {
            return null;
        }

        return nextTrigger;
    }

    private ZoneId fromTimezoneId(String timezoneId) {

        final TimeZone timezone;

        if (timezoneId != null && !(timezoneId = timezoneId.trim()).isEmpty()) {
            // If the timezone ID wasn't valid, then Timezone.getTimeZone() returns GMT, which
            // may not always be desirable. So we first check to see if the timezone id specified
            // is available in timezone ids in the system. If it's not available then we log a
            // WARN message and fallback on the server's default timezone.
            String[] availableTimeZoneIDs = TimeZone.getAvailableIDs();
            if (availableTimeZoneIDs != null && Arrays.asList(availableTimeZoneIDs).contains(timezoneId)) {
                timezone = TimeZone.getTimeZone(timezoneId);
            } else {
                // use server's timezone
                timezone = TimeZone.getDefault();
                EJB3_TIMER_LOGGER.unknownTimezoneId(timezoneId, timezone.getID());
            }
        } else {
            timezone = TimeZone.getDefault();
        }

        // convert from the old java.util.TimeZone to the new java.time.ZoneId
        return timezone.toZoneId();
    }

    private boolean isDayOfWeekWildcard() {
        return this.schedule.getDayOfWeek().equals("*");
    }

    private boolean isDayOfMonthWildcard() {
        return this.schedule.getDayOfMonth().equals("*");
    }

    private boolean noMoreTimeouts(ZonedDateTime zdt) {
        if (zdt.getYear() > Year.MAX_YEAR || end != null && zdt.isAfter(end)) {
            return true;
        }
        return false;
    }

    private ZonedDateTime computeNextTime(ZonedDateTime currentZonedDateTime) {

        int currentSecond = currentZonedDateTime.getSecond();
        int currentMinute = currentZonedDateTime.getMinute();
        int currentHour = currentZonedDateTime.getHour();
        final int currentTimeInSeconds = currentHour * 3600 + currentMinute * 60 + currentSecond;

        // compute next second
        Integer nextSecond = this.second.getNextMatch(currentSecond);
        if (nextSecond == null) {
            return null;
        }
        // compute next minute
        if (nextSecond < currentSecond) {
            currentMinute++;
        }
        Integer nextMinute = this.minute.getNextMatch(currentMinute < 60 ? currentMinute : 0);
        if (nextMinute == null) {
            return null;
        }
        // reset second if minute was changed (Fix WFLY-5955)
        if (nextMinute != currentMinute) {
            nextSecond = this.second.getNextMatch(0);
        }
        // compute next hour
        if (nextMinute < currentMinute) {
            currentHour++;
        }
        Integer nextHour = this.hour.getNextMatch(currentHour < 24 ? currentHour : 0);
        if (nextHour == null) {
            return null;
        }
        if (nextHour != currentHour) {
            // reset second/minute if hour changed (Fix WFLY-5955)
            nextSecond = this.second.getNextMatch(0);
            nextMinute = this.minute.getNextMatch(0);
        }

        final int nextTimeInSeconds = nextHour * 3600 + nextMinute * 60 + nextSecond;
        if (nextTimeInSeconds == currentTimeInSeconds) {
            // no change in time
            return currentZonedDateTime;
        }

        currentZonedDateTime = currentZonedDateTime //
                .withSecond(nextSecond) //
                .with(nextOrSameMinuteOfHour(nextMinute)) //
                .with(nextOrSameHourOfDay(nextHour));

        return currentZonedDateTime;
    }

    private ZonedDateTime computeNextMonth(ZonedDateTime currentZonedDateTime) {

        int currentMonth = currentZonedDateTime.getMonthValue();

        Integer nextMonth = this.month.getNextMatch(currentMonth);
        if (nextMonth == null) {
            return null;
        }

        // if the current month is a match, then nothing else to do. Just return back the calendar
        if (currentMonth == nextMonth) {
            return currentZonedDateTime;
        }

        // got to the next month and reset the day to the first of the month
        currentZonedDateTime = currentZonedDateTime.with(firstDayOfMonth(nextMonth));

        // since we are moving to a different month (as compared to the current month), we should
        // reset the second, minute and hour appropriately, to their first possible values
        currentZonedDateTime = currentZonedDateTime.with(localTimeOf(firstTime));

        return currentZonedDateTime;
    }

    private ZonedDateTime computeNextDate(ZonedDateTime currentZonedDateTime) {
        if (this.isDayOfMonthWildcard()) {
            return this.computeNextDayOfWeek(currentZonedDateTime);
        }

        if (this.isDayOfWeekWildcard()) {
            return this.computeNextDayOfMonth(currentZonedDateTime);
        }

        // both day-of-month and day-of-week are *non-wildcards*
        ZonedDateTime nextDayOfMonthZdt = this.computeNextDayOfMonth(currentZonedDateTime);
        ZonedDateTime nextDayOfWeekZdt = this.computeNextDayOfWeek(currentZonedDateTime);

        if (nextDayOfMonthZdt == null) {
            return nextDayOfWeekZdt;
        }
        if (nextDayOfWeekZdt == null) {
            return nextDayOfMonthZdt;
        }

        return nextDayOfWeekZdt.isBefore(nextDayOfMonthZdt) ? nextDayOfWeekZdt : nextDayOfMonthZdt;
    }

    private ZonedDateTime computeNextDayOfWeek(ZonedDateTime currentZonedDateTime) {

        java.time.DayOfWeek nextDayOfWeek = this.dayOfWeek.getNextMatch(currentZonedDateTime);

        if (nextDayOfWeek == null) {
            return null;
        }

        java.time.DayOfWeek currentDayOfWeek = currentZonedDateTime.getDayOfWeek();

        // if the current day-of-week is a match, then nothing else to do. Just return back the ZonedDateTime
        if (currentDayOfWeek == nextDayOfWeek) {
            return currentZonedDateTime;
        }

        // go forward to the next DayOfWeek
        currentZonedDateTime = currentZonedDateTime.with(java.time.temporal.TemporalAdjusters.next(nextDayOfWeek));

        // since we are moving to a different day (as compared to the current day), we should
        // reset the second, minute and hour appropriately, to their first possible values
        currentZonedDateTime = currentZonedDateTime.with(localTimeOf(firstTime));

        return currentZonedDateTime;
    }

    private ZonedDateTime computeNextDayOfMonth(ZonedDateTime currentZonedDateTime) {

        Integer nextDayOfMonth = this.dayOfMonth.getNextMatch(currentZonedDateTime);

        if (nextDayOfMonth == null) {
            return null;
        }

        int currentDayOfMonth = currentZonedDateTime.getDayOfMonth();

        // if the current day-of-month is a match, then nothing else to do. Just return back the ZonedDateTime
        if (currentDayOfMonth == nextDayOfMonth) {
            return currentZonedDateTime;
        }

        if (nextDayOfMonth > currentDayOfMonth) {
            if (this.monthHasDate(currentZonedDateTime, nextDayOfMonth)) {

                // set the chosen day-of-month
                currentZonedDateTime = currentZonedDateTime.withDayOfMonth(nextDayOfMonth);

                // since we are moving to a different day-of-month (as compared to the current day-of-month),
                // we should reset the second, minute and hour appropriately, to their first possible values
                currentZonedDateTime = currentZonedDateTime.with(localTimeOf(firstTime));

            } else {
                currentZonedDateTime = this.advanceTillMonthHasDate(currentZonedDateTime, nextDayOfMonth);
            }
        } else {
            // since the next day is before the current day we need to shift to the next month
            currentZonedDateTime = currentZonedDateTime.plusMonths(1);

            // also we need to reset the time
            currentZonedDateTime = currentZonedDateTime.with(localTimeOf(firstTime));

            currentZonedDateTime = this.computeNextMonth(currentZonedDateTime);

            if (currentZonedDateTime == null) {
                return null;
            }

            nextDayOfMonth = this.dayOfMonth.getFirstMatch(currentZonedDateTime);

            if (nextDayOfMonth == null) {
                return null;
            }

            // make sure the month can handle the date
            currentZonedDateTime = this.advanceTillMonthHasDate(currentZonedDateTime, nextDayOfMonth);
        }
        return currentZonedDateTime;
    }

    private ZonedDateTime computeNextYear(ZonedDateTime currentZonedDateTime) {

        Integer nextYear = this.year.getNextMatch(currentZonedDateTime.getYear());

        if (nextYear == null || nextYear > Year.MAX_YEAR) {
            return null;
        }
        int currentYear = currentZonedDateTime.getYear();

        // if the current year is a match, then nothing else to do. Just return back the ZonedDateTime
        if (currentYear == nextYear) {
            return currentZonedDateTime;
        }

        // if the next year is lesser than the current year, then we have no more timeouts for the
        // calendar expression
        if (nextYear < currentYear) {
            return null;
        }

        // at this point we have chosen a year which is greater than the current, so add a year
        currentZonedDateTime = currentZonedDateTime.plusYears(1);

        // since we are moving to a different year (as compared to the current year), we should reset
        // all other calendar attribute expressions appropriately, to their first possible values
        currentZonedDateTime = currentZonedDateTime.with(firstDayOfMonth(this.month.getFirstMatch()));
        currentZonedDateTime = currentZonedDateTime.with(localTimeOf(firstTime));

        // recompute date
        currentZonedDateTime = this.computeNextDate(currentZonedDateTime);

        return currentZonedDateTime;
    }

    private boolean monthHasDate(ZonedDateTime zonedDateTime, int date) {
        int currentMonthLength = zonedDateTime.getMonth().length(zonedDateTime.toLocalDate().isLeapYear());
        return date <= currentMonthLength;
    }

    private ZonedDateTime advanceTillMonthHasDate(ZonedDateTime zonedDateTime, Integer date) {

        zonedDateTime = zonedDateTime.with(localTimeOf(firstTime));

        // make sure the month can handle the date
        while (monthHasDate(zonedDateTime, date) == false) {
            if (zonedDateTime.getYear() > Year.MAX_YEAR) {
                return null;
            }
            // this month can't handle the date, so advance month to next month and get the next suitable matching month
            zonedDateTime = zonedDateTime.plusMonths(1);
            zonedDateTime = this.computeNextMonth(zonedDateTime);
            if (zonedDateTime == null) {
                return null;
            }
            date = this.dayOfMonth.getFirstMatch(zonedDateTime);
            if (date == null) {
                return null;
            }
        }
        zonedDateTime = zonedDateTime.withDayOfMonth(date);
        return zonedDateTime;
    }

    /* package */ static TemporalAdjuster nextOrSameMinuteOfHour(int minuteOfHour) {
        return (temporal) -> {

            int currentMinute = temporal.get(ChronoField.MINUTE_OF_HOUR);

            // same minute, return same
            if (currentMinute == minuteOfHour) {
                return temporal;
            }

            temporal = ((ZonedDateTime) temporal).withMinute(minuteOfHour);

            int resolvingMinute = temporal.get(ChronoField.MINUTE_OF_HOUR);

            while (resolvingMinute != minuteOfHour) {
                Duration diff = Duration.ofMinutes(resolvingMinute - minuteOfHour);
                temporal = temporal.plus(diff);
                resolvingMinute = temporal.get(ChronoField.MINUTE_OF_HOUR);
            }

            return temporal;
        };
    }

    /* package */ static TemporalAdjuster nextOrSameHourOfDay(int hourOfDay) {
        return (temporal) -> {
            int currentHour = temporal.get(ChronoField.HOUR_OF_DAY);

            // same hour, return same
            if (currentHour == hourOfDay) {
                return temporal;
            }

            long diff = hourOfDay - currentHour;
            return temporal.plus(diff >= 0 ? diff : (24 + diff), ChronoUnit.HOURS);
        };
    }

    /* package */ static TemporalAdjuster firstDayOfMonth(Integer month) {
        Objects.requireNonNull(month, "month");
        int newMonth = month.intValue();
        return (temporal) -> {
            int currentMonth = temporal.get(ChronoField.MONTH_OF_YEAR);
            long diff = newMonth - currentMonth;
            return temporal.with(DAY_OF_MONTH, 1).plus(diff >= 0 ? diff : (12 + diff), MONTHS);
        };
    }

    /* package */ static TemporalAdjuster localTimeOf(LocalTime localTime) {
        Objects.requireNonNull(localTime, "localTime");
        return (temporal) -> {
            return temporal //
                    .with(ChronoField.HOUR_OF_DAY, localTime.getHour()) //
                    .with(ChronoField.MINUTE_OF_HOUR, localTime.getMinute()) //
                    .with(ChronoField.SECOND_OF_MINUTE, localTime.getSecond()) //
            ;
        };
    }
}
