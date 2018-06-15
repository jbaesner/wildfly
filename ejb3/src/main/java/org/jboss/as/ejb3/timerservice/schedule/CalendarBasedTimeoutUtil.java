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
package org.jboss.as.ejb3.timerservice.schedule;

import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Utilities for the CalendarBasedTimeout calculation
 * <p/>
 *
 * @author Joerg Baesner
 *
 */
public class CalendarBasedTimeoutUtil {

    /**
     * Create a {@link GregorianCalendar} instance from the given {@link ZonedDateTime}. The <code>ZonedDateTime</code> can be
     * <code>null</code>.
     *
     * @param zonedDateTime
     * @return the <code>GegorianCalendar</code> instance from the <code>ZonedDateTime</code> or <code>null</code> if
     *         <code>ZonedDateTime</code> is <code>null</code>
     */
    public static Calendar fromZonedDateTime(ZonedDateTime zonedDateTime) {

        if (zonedDateTime == null) {
            return null;
        }

        return GregorianCalendar.from(zonedDateTime);
    }

    /**
     * Create a {@link ZonedDateTime} instance from the given {@link Calendar} instance. The <code>Calendar</code> instance can
     * be <code>null</code>. If it is not <code>null</code>, it must be an instance of {@link GregorianCalendar}.
     *
     * @param calendar
     * @return the <code>ZonedDateTime</code> instance from the <code>Calendar</code> or <code>null</code> if
     *         <code>Calendar</code> is <code>null</code>
     * @throws <code>IllegalArgumentException</code> if the given <code>Calendar</code> instance is not an instance of
     *         <code>GregorianCalendar</code>
     */
    public static ZonedDateTime fromCalendar(Calendar calendar) {

        if (calendar == null) {
            return null;
        }

        if (calendar instanceof GregorianCalendar) {
            return ((GregorianCalendar) calendar).toZonedDateTime();
        } else {
            throw EJB3_TIMER_LOGGER.invalidCalendarType(calendar.getClass().getName());
        }
    }
}
