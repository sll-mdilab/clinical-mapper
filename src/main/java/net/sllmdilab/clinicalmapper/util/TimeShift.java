package net.sllmdilab.clinicalmapper.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeShift {

	public static class Interval {
		public final long start;
		public final long end;
		
		private Interval(long start, long end) {
			this.start = start;
			this.end = end;
		}
		
		public long length() {
			return end - start;
		}
		
		@Override
		public boolean equals(Object i) {
			if(i instanceof Interval) {
				return ((Interval)i).start == start && ((Interval)i).end == end;
			} else {
				return false;
			}
		}
		
		public static Interval of(long start, long end) {
			return new Interval(start, end);
		}
		
		public static Interval of(Date start, Date end) {
			return new Interval(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli());
		}
		
		@Override
		public String toString() {
			return "(" + new Date(start) + ", " + new Date(end) + ")";
		}
	}
	
	public static long transformTimestampToMaster(Interval master, long timestamp) {
		long mod = (timestamp - master.start) % master.length();
		if(mod < 0) {
			mod += master.length();
		}
		return master.start + mod;
	}
	
	public static long transformTimestampFromMaster(Interval master, long startTimestamp, long timestamp, int iteration) {
		long nearestIntervalStart = master.length() * ((startTimestamp - master.start)/master.length());
		return nearestIntervalStart + timestamp + master.length()*iteration;
	}
	
	/**
	 * Calculate how many master intervals this interval would span.
	 * @param master
	 * @param interval
	 * @return
	 */
	public static int getNumIterations(Interval master, Interval interval) {
		long distanceFromIntervalStart = (interval.start - master.start) % master.length();
		if(distanceFromIntervalStart < 0) {
			distanceFromIntervalStart += master.length();
		}
		
		return (int) ((interval.length() + distanceFromIntervalStart) / master.length());
	}
	
	public static List<Interval> getIntervalsToFetch(Interval master, Interval query) {
		List<Interval> result = new ArrayList<>();
		
		// If the requested period is at least as long as the master interval, we need to fetch all of it
		if(query.length() >= master.length()) {
			result.add(master);
		} else {
			// If the requested interval is within the master interval
			if(transformTimestampToMaster(master, query.start) <= transformTimestampToMaster(master, query.end)) {
				result.add(Interval.of(transformTimestampToMaster(master, query.start), transformTimestampToMaster(master, query.end)));
			// If the requested interval wraps around the edge
			} else {
				result.add(Interval.of(transformTimestampToMaster(master, query.start), master.end));
				result.add(Interval.of(master.start, transformTimestampToMaster(master, query.end)));
			}
		}
		
		return result;
	}
}
