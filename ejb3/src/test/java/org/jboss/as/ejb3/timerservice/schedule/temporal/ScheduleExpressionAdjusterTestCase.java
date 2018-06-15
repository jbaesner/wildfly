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

import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameMinuteOfHour;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.ejb.ScheduleExpression;

import org.hamcrest.MatcherAssert;
import org.jboss.as.ejb3.timer.schedule.ScheduleExpressionBuilder;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ScheduleExpressionAdjusterTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private static ZoneId UTC = ZoneId.of("Z");

    /**
     * Logger
     */
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(ScheduleExpressionAdjusterTestCase.class);

    public ScheduleExpressionAdjusterTestCase() {
    }

    @Test
    public void testAdjustIntoWithWrongType() {

        // expect an IllegalArgumentException with a Message containing Message id 508
        expectedException.expect(isA(IllegalArgumentException.class));
        expectedException.expectMessage(containsString("WFLYEJB0508"));

        ScheduleExpression schedule = ScheduleExpressionBuilder.create().build();

        ScheduleExpressionAdjuster adjuster = new ScheduleExpressionAdjuster(schedule);

        LocalDateTime now = LocalDateTime.now(UTC);

        // now call the adjust with an invalid type
        now.with(adjuster);
    }

    @Test
    public void testNextOrSameMinuteOfHour_WithInvalidMinute60() {

        // prepare
        final int minute = 60;
        ZonedDateTime zdt = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, UTC);

        // expect Exception
        // expect an IllegalArgumentException with a Message containing the String "valid values 0 - 59"
        expectedException.expect(isA(DateTimeException.class));
        expectedException.expectMessage(containsString("valid values 0 - 59"));

        // now call the adjustment
        zdt.with(ScheduleExpressionAdjuster.nextOrSameMinuteOfHour(minute));

    }

    @Test
    public void testNextOrSameMinuteOfHour_SameMinute() {

        // prepare
        final int minute = 20;
        ZonedDateTime zdt = ZonedDateTime.of(2018, 1, 1, 0, minute, 0, 0, UTC);

        // now call the adjustment
        ZonedDateTime newZdt = zdt.with(ScheduleExpressionAdjuster.nextOrSameMinuteOfHour(minute));
        MatcherAssert.assertThat(newZdt, sameMinuteOfHour(zdt));
    }

    @Test
    public void testNextOrSameMinuteOfHour_KeepSameHour() {

        // prepare
        final int hour = 1;
        final int minute = 30;
        ZonedDateTime zdt = ZonedDateTime.of(2018, 1, 1, hour, 0, 0, 0, UTC);

        // now call the adjustment
        ZonedDateTime newZdt = zdt.with(ScheduleExpressionAdjuster.nextOrSameMinuteOfHour(minute));
        Assert.assertThat(newZdt.getMinute(), is(minute));
        Assert.assertThat(newZdt.getHour(), is(zdt.getHour()));
    }

    /**
     * This test is specific in a way that Lord Howe in Australia has a DST offset of 30 min
     */
    @Test
    public void testNextOrSameMinuteOfHour_DSTsApartFromOneHourOffset_Australia_Lord_Howe() {

        // https://www.timeanddate.com/time/change/australia/lord-howe-island
        ZoneId zoneId = ZoneId.of("Australia/Lord_Howe");

        // zdt below is 2018-10-07T02:30+11:00[Australia/Lord_Howe]
        ZonedDateTime zdt = ZonedDateTime.of(2018, 10, 7, 2, 0, 0, 0, zoneId);

        ZonedDateTime newZdt = zdt.with(ScheduleExpressionAdjuster.nextOrSameMinuteOfHour(0));

        // expected newZdt is 2018-10-07T03:00+11:00[Australia/Lord_Howe]
        Assert.assertThat(newZdt.getMinute(), is(0));
        Assert.assertThat(newZdt.getHour(), is(3));
    }
}
