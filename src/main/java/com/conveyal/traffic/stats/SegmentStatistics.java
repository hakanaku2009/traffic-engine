package com.conveyal.traffic.stats;

import java.io.Serializable;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;


public class SegmentStatistics implements Serializable {

	private static final long serialVersionUID = -5885621202679850261l;

	public static int HOURS_IN_WEEK = 7 * 24;
	public static long WEEK_OFFSET = 24 * 60 * 60 * 1000 * 4; // Jan 1, 1970 is a Thursday. Need to offset to Monday

	public long sampleCount = 0;
	public double sampleSum = 0.0;

	public long hourSampleCount[] = new long[HOURS_IN_WEEK];
	public double hourSampleSum[] = new double[HOURS_IN_WEEK];

	public void addSample(SpeedSample ss) {
		synchronized (this) {
			int hour = getHourOfWeek(ss.time);
			sampleCount++;
			sampleSum += ss.getSpeed();

			hourSampleCount[hour]++;
			hourSampleSum[hour] += ss.getSpeed();
		}
	}

	public void addStats(SegmentStatistics stats) {
		synchronized (this) {

			if(stats == null)
				return;

			this.sampleCount += stats.sampleCount;
			this.sampleSum += stats.sampleSum;

			for (int i = 0; i < HOURS_IN_WEEK; i++) {
				hourSampleCount[i] += stats.hourSampleCount[i];
				hourSampleSum[i] += stats.hourSampleSum[i];
			}
		}
	}

	public void avgStats(SegmentStatistics stats) {
		this.sampleCount++;
		this.sampleSum += stats.sampleSum / stats.sampleCount;

		for(int i = 0; i < HOURS_IN_WEEK; i++) {
			if(stats.hourSampleCount[i] > 5) {
				hourSampleCount[i]++;
				hourSampleSum[i] += stats.hourSampleSum[i] / stats.hourSampleCount[i];
			}
		}
	}

	public void avgPercentChangeStats(SegmentStatistics stats1, SegmentStatistics stats2) {
		this.sampleCount++;
		this.sampleSum += ((stats2.sampleSum / stats2.sampleCount) - (stats1.sampleSum / stats1.sampleCount)) / (stats1.sampleSum / stats1.sampleCount);

		for(int i = 0; i < HOURS_IN_WEEK; i++) {
			if(stats1.hourSampleCount[i] > 5 && stats2.hourSampleCount[i] > 5) {
				hourSampleCount[i]++;
				hourSampleSum[i] += ((stats2.hourSampleSum[i] / stats2.hourSampleCount[i]) - (stats1.hourSampleSum[i] / stats1.hourSampleCount[i])) / (stats1.hourSampleSum[i] / stats1.hourSampleCount[i]);
			}
		}
	}

	public SummaryStatistics collectSummaryStatisics(Set<Integer> hours) {
		synchronized (this) {
			SummaryStatistics summary = new SummaryStatistics();

			summary.averageCount = 0l;
			summary.averageSpeedSum = 0.0;

			for(int i = 0; i < HOURS_IN_WEEK; i++) {
				if(hours != null && hours.size() > 0 && !hours.contains(i))
					continue;

				summary.averageCount += hourSampleCount[i];
				summary.averageSpeedSum += hourSampleSum[i];

				if(hourSampleCount[i] > 0)
					summary.speedByHourOfWeek[i] = hourSampleSum[i] / hourSampleCount[i];
				else
					summary.speedByHourOfWeek[i] = Double.NaN;
			}

			return summary;
		}
	}

	public static int getHourOfWeek(long time) {

		// check and convert to millisecond
		if(time < 15000000000l)
			time = time * 1000;


		Instant currentTime = Instant.ofEpochMilli(time);
		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(currentTime, ZoneId.of("UTC"));
		int dayOfWeek = zonedDateTime.get(ChronoField.DAY_OF_WEEK) - 1;
		int hourOfDay = zonedDateTime.get(ChronoField.HOUR_OF_DAY);

		return (dayOfWeek * 24) + hourOfDay;
	}

	// converts timestamp (ms) to the week bin beg
	public static long getWeekSinceEpoch(long time) {

		// check and convert to millisecond
		if(time < 15000000000l)
			time = time * 1000;

		Instant epoch = Instant.ofEpochMilli(WEEK_OFFSET);
		Instant currentTime = Instant.ofEpochMilli(time);

		LocalDateTime startDate = LocalDateTime.ofInstant(epoch, ZoneId.of("UTC"));
		LocalDateTime endDate = LocalDateTime.ofInstant(currentTime, ZoneId.of("UTC"));

		return ChronoUnit.WEEKS.between(startDate, endDate);
	}

	public static long getTimeForWeek(long week) {

		Instant epoch = Instant.ofEpochMilli(WEEK_OFFSET);

		LocalDateTime startDate = LocalDateTime.ofInstant(epoch, ZoneId.of("UTC"));
		LocalDateTime offsetTime = startDate.plusWeeks(week);

		return offsetTime.toEpochSecond(ZoneOffset.UTC) * 1000;
	}

	// converts timestamp (ms) to the week bin beg
	public static long getHourSinceEpoch(long time) {

		// check and convert to millisecond
		if(time < 15000000000l)
			time = time * 1000;


		Instant epoch = Instant.ofEpochMilli(WEEK_OFFSET);
		Instant currentTime = Instant.ofEpochMilli(time);

		LocalDateTime startDate = LocalDateTime.ofInstant(epoch, ZoneId.of("UTC"));
		LocalDateTime endDate = LocalDateTime.ofInstant(currentTime, ZoneId.of("UTC"));

		return ChronoUnit.HOURS.between(startDate, endDate);
	}
}
