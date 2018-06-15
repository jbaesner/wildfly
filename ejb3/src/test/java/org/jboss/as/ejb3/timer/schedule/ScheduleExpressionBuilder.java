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
package org.jboss.as.ejb3.timer.schedule;

import java.util.Date;
import java.time.ZonedDateTime;

import javax.ejb.ScheduleExpression;

public class ScheduleExpressionBuilder {

    ScheduleExpression schedule;

    public static ScheduleExpressionBuilder create() {

        ScheduleExpressionBuilder builder = new ScheduleExpressionBuilder();
        builder.schedule = new ScheduleExpression();
        return builder;
    }

    public ScheduleExpressionBuilder withStart(Date start) {
        this.schedule.start(start);
        return this;
    }

    public ScheduleExpressionBuilder withStart(ZonedDateTime start) {
        this.schedule.start(Date.from(start.toInstant()));
        return this;
    }

    public ScheduleExpressionBuilder withEnd(Date end) {
        this.schedule.end(end);
        return this;
    }

    public ScheduleExpressionBuilder withEnd(ZonedDateTime end) {
        this.schedule.end(Date.from(end.toInstant()));
        return this;
    }

    public ScheduleExpressionBuilder withTimezone(String timezone) {
        this.schedule.timezone(timezone);
        return this;
    }

    public ScheduleExpressionBuilder withYear(String year) {
        this.schedule.year(year);
        return this;
    }

    public ScheduleExpressionBuilder withMonth(String month) {
        this.schedule.month(month);
        return this;
    }

    public ScheduleExpressionBuilder withDayOfMonth(String dayOfMonth) {
        this.schedule.dayOfMonth(dayOfMonth);
        return this;
    }

    public ScheduleExpressionBuilder withDayOfWeek(String dayOfWeek) {
        this.schedule.dayOfWeek(dayOfWeek);
        return this;
    }

    public ScheduleExpressionBuilder withHour(String hour) {
        this.schedule.hour(hour);
        return this;
    }

    public ScheduleExpressionBuilder withMinute(String minute) {
        this.schedule.minute(minute);
        return this;
    }

    public ScheduleExpressionBuilder withSecond(String second) {
        this.schedule.second(second);
        return this;
    }

    public ScheduleExpression build() {
        return schedule;
    }

}
