/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice.schedule;

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.time.ZonedDateTime;
import java.util.Calendar;

import javax.ejb.ScheduleExpression;

import org.jboss.as.ejb3.timerservice.schedule.temporal.ScheduleExpressionAdjuster;

/**
 * CalendarBasedTimeout
 *
 * @author Jaikiran Pai
 * @author "<a href=\"mailto:wfink@redhat.com\">Wolf-Dieter Fink</a>"
 * @author Eduardo Martins
 * @author Joerg Baesner
 * @version $Revision: $
 */
public class CalendarBasedTimeout {

    ScheduleExpressionAdjuster adjustmentByScheduleExpression;

    /**
     * Creates a {@link CalendarBasedTimeout} from the passed <code>schedule</code>.
     * <p>
     * This Constructor does some <code>null</code> checks on the <i>schedule</i> and creates an instance of the {@link ScheduleExpressionAdjuster}
     * </p>
     *
     * @param schedule
     */
    public CalendarBasedTimeout(ScheduleExpression schedule) {

        if (schedule == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpression(this.getClass().getName());
        }
        // make sure that the schedule doesn't have null values for its various
        // attributes
        this.nullCheckScheduleAttributes(schedule);

        // setup the TemporalAdjuster for the given ScheduleExpression
        adjustmentByScheduleExpression = new ScheduleExpressionAdjuster(schedule);
    }

    /**
     * Returns the original {@link javax.ejb.ScheduleExpression} from which this {@link CalendarBasedTimeout} was created.
     *
     * @return
     */
    public ScheduleExpression getScheduleExpression() {
        return this.adjustmentByScheduleExpression.getScheduleExpression();
    }

    public Calendar getFirstTimeout() {
        return CalendarBasedTimeoutUtil.fromZonedDateTime(this.adjustmentByScheduleExpression.getFirstTimeout());
    }

    public Calendar getNextTimeout() {
        return getNextTimeout(ZonedDateTime.now(adjustmentByScheduleExpression.getZoneId()));
    }

    public Calendar getNextTimeout(Calendar currentCal) {
        ZonedDateTime zdt = CalendarBasedTimeoutUtil.fromCalendar(currentCal);
        return getNextTimeout(zdt);
    }

    private Calendar getNextTimeout(ZonedDateTime currentZdt) {
        currentZdt = currentZdt.with(adjustmentByScheduleExpression);
        return CalendarBasedTimeoutUtil.fromZonedDateTime(currentZdt);
    }

    private void nullCheckScheduleAttributes(ScheduleExpression schedule) {
        if (schedule.getSecond() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionSecond(schedule);
        }
        if (schedule.getMinute() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionMinute(schedule);
        }
        if (schedule.getHour() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionHour(schedule);
        }
        if (schedule.getDayOfMonth() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionDayOfMonth(schedule);
        }
        if (schedule.getDayOfWeek() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionDayOfWeek(schedule);
        }
        if (schedule.getMonth() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionMonth(schedule);
        }
        if (schedule.getYear() == null) {
            throw EJB3_TIMER_LOGGER.invalidScheduleExpressionYear(schedule);
        }
    }
}
