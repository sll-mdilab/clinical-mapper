package net.sllmdilab.clinicalmapper.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import net.sllmdilab.clinicalmapper.util.TimeShift;
import net.sllmdilab.clinicalmapper.util.TimeShift.Interval;

@RunWith(MockitoJUnitRunner.class)
public class TimeShiftTest {
	
	@Test
	public void transformToMaster() {
		Interval master = Interval.of(5, 10);
		
		assertEquals(6,TimeShift.transformTimestampToMaster(master, 11));
		assertEquals(7,TimeShift.transformTimestampToMaster(master, 22));
		assertEquals(8,TimeShift.transformTimestampToMaster(master, 3));
		assertEquals(5,TimeShift.transformTimestampToMaster(master, 0));
		assertEquals(7,TimeShift.transformTimestampToMaster(master, 7));
	}
	
	@Test
	public void transformFromMaster() {
		Interval master = Interval.of(5, 11);
		
		assertEquals(7, TimeShift.transformTimestampFromMaster(master, 6, 7, 0));
		assertEquals(15, TimeShift.transformTimestampFromMaster(master, 12, 9, 0));
		assertEquals(21, TimeShift.transformTimestampFromMaster(master, 12, 9, 1));
	}
	
	@Test
	public void getNumIterations() {
		Interval master = Interval.of(5, 11);
		
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(10, 18)));
		assertEquals(0, TimeShift.getNumIterations(master, Interval.of(9, 10)));
		assertEquals(1, TimeShift.getNumIterations(master, Interval.of(9, 16)));
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(9, 17)));
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(9, 19)));
	}
	
	
	@Test
	public void getNumIterationsBeforeMaster() {
		Interval master = Interval.of(29, 35);
		
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(10, 18)));
		assertEquals(0, TimeShift.getNumIterations(master, Interval.of(9, 10)));
		assertEquals(1, TimeShift.getNumIterations(master, Interval.of(9, 16)));
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(9, 17)));
		assertEquals(2, TimeShift.getNumIterations(master, Interval.of(9, 19)));
	}
	@Test
	public void shortIntervalWithinMaster() {
		Interval master = Interval.of(5, 10);
		Interval query = Interval.of(21, 24);
		List<Interval> intervals = TimeShift.getIntervalsToFetch(master, query);
		
		assertEquals(1, intervals.size());
		assertEquals(Interval.of(6, 9), intervals.get(0));
	}
	
	@Test
	public void shortIntervalWrapsAround() {
		Interval master = Interval.of(5, 10);
		Interval query = Interval.of(22, 25);
		List<Interval> intervals = TimeShift.getIntervalsToFetch(master, query);
		
		assertEquals(2, intervals.size());
	}
	
	@Test
	public void longIntervalWrapsAround() {
		Interval master = Interval.of(5, 10);
		Interval query = Interval.of(24, 31);
		List<Interval> intervals = TimeShift.getIntervalsToFetch(master, query);
		
		assertEquals(1, intervals.size());
		assertEquals(master, intervals.get(0));
	}
	
	@Test
	public void longIntervalDoesntWrapAround() {
		Interval master = Interval.of(5, 10);
		Interval query = Interval.of(21, 34);
		List<Interval> intervals = TimeShift.getIntervalsToFetch(master, query);
		
		assertEquals(1, intervals.size());
		assertEquals(master, intervals.get(0));
	}
}
