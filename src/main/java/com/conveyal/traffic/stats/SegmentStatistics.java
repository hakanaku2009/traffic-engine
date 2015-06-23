package com.conveyal.traffic.stats;

import com.conveyal.traffic.geom.StreetSegment;

import javax.swing.event.DocumentEvent;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

public class SegmentStatistics implements Serializable {

	static long serialVersionUID = 100;

	public static int HOURS_IN_WEEK = 7 * 24;
	public static long WEEK_OFFSET = 24 * 60 * 60 * 1000 * 4; // Jan 1, 1970 is a Thursday. Need to offset to Monday

	public long sampleCount = 0;
	public double sampleSum = 0.0;

	public long hourSampleCount[] = new long[HOURS_IN_WEEK];
	public double hourSampleSum[] = new double[HOURS_IN_WEEK];

	public void addSample(SpeedSample ss) {
		int hour = getHourOfWeek(ss.time);
		sampleCount++;
		sampleSum += ss.getSpeed();

		hourSampleCount[hour]++;
		hourSampleSum[hour] += ss.getSpeed();
	}

	public void addStats(SegmentStatistics stats) {
		this.sampleCount += stats.sampleCount;
		this.sampleSum += stats.sampleSum;

		for(int i = 0; i < HOURS_IN_WEEK; i++) {
			hourSampleCount[i] += stats.hourSampleCount[i];
			hourSampleSum[i] += stats.hourSampleSum[i];
		}
	}

	public SummaryStatistics collectSummaryStatisics() {

		synchronized (this) {

			long countByHourOfWeek[] = new long[HOURS_IN_WEEK];
			double speedSumByHourOfWeek[] = new double[HOURS_IN_WEEK];

			SummaryStatistics summary = new SummaryStatistics();

			summary.averageCount = this.sampleCount;
			summary.averageSpeedSum = this.sampleSum;

			for(int i = 0; i < HOURS_IN_WEEK; i++) {
				if(hourSampleCount[i] > 0)
					summary.speedByHourOfWeek[i] = hourSampleSum[i] / hourSampleCount[i];
				else
					summary.speedByHourOfWeek[i] = Double.NaN;
			}

			return summary;
		}
	}

	public static int getHourOfWeek(long time) {

		Instant currentTime = Instant.ofEpochMilli(time);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(currentTime, ZoneId.of("UTC"));
		int dayOfWeek = zonedDateTime.get(ChronoField.DAY_OF_WEEK) - 1;
		int hourOfDay = zonedDateTime.get(ChronoField.HOUR_OF_DAY);

		return (dayOfWeek * 24) + hourOfDay;
	}

	// converts timestamp (ms) to the week bin beg
	public static long getWeekSinceEpoch(long time) {

		// check and convert to milliseconds
		if(time < 15000000000l)
			time = time * 1000;

		Instant epoch = Instant.ofEpochMilli(WEEK_OFFSET);
		Instant currentTime = Instant.ofEpochMilli(time);

		LocalDateTime startDate = LocalDateTime.ofInstant(epoch, ZoneId.systemDefault());
		LocalDateTime endDate = LocalDateTime.ofInstant(currentTime, ZoneId.systemDefault());

		return ChronoUnit.WEEKS.between(startDate, endDate);
	}

	// converts timestamp (ms) to the week bin beg
	public static long getHourSinceEpoch(long time) {

		Instant epoch = Instant.ofEpochMilli(WEEK_OFFSET);
		Instant currentTime = Instant.ofEpochMilli(time);

		LocalDateTime startDate = LocalDateTime.ofInstant(epoch, ZoneId.systemDefault());
		LocalDateTime endDate = LocalDateTime.ofInstant(currentTime, ZoneId.systemDefault());

		return ChronoUnit.HOURS.between(startDate, endDate);
	}
}
