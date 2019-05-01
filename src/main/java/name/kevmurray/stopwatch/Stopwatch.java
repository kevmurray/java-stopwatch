package name.kevmurray.stopwatch;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class Stopwatch {

	private final Deque<Event> stack = new LinkedList<>();
	private final Map<String, Event> events = new HashMap<>();
	private final TimeUnit resolution;
	private final TimeSource timeSource;

	Stopwatch() {
		this(TimeUnit.MILLISECONDS);
	}

	Stopwatch(TimeUnit resolution) {
		this.resolution = resolution;
		switch (resolution) {
		case NANOSECONDS:
		case MICROSECONDS:
			timeSource = new NanoTimeSource();
			break;
		case MILLISECONDS:
		case SECONDS:
		case MINUTES:
		case HOURS:
		case DAYS:
		default:
			timeSource = new MilliTimeSource();
			break;
		}
	}

	public void start() {
		start("stopwatch");
	}

	public Event stop() {
		return stop("stopwatch");
	}

	public void start(String name) {
		stack.push(new Event(name, stack.peek(), timeSource.getTime()));
	}

	public Event stop(String name) {
		if (!stackContainsEvent(name))
			return null;

		while (!stack.isEmpty()) {
			Event event = stack.pop();
			event.stop(timeSource.getTime());
			record(event);
			if (name.equals(event.getName()))
				return event;
		}

		return null;
	}

	private boolean stackContainsEvent(String name) {
		if (name != null)
			for (Event event : stack)
				if (name.equals(event.getName()))
					return true;

		return false;
	}

	private void record(Event event) {
		String fullPath = event.getPath();
		Event other = events.get(fullPath);
		if (other == null)
			events.put(fullPath, event);
		else
			other.merge(event);
	}

	public Event getEvent() {
		return getEvent("stopwatch");
	}

	public Event getEvent(String path) {
		return events.get(path);
	}

	public String report() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			report(ps);
			ps.close();
			return baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// inconceivable!
			return null;
		}
	}

	public void report(PrintStream outStream) {
		events.keySet().stream() //
				.sorted() //
				.map(n -> events.get(n)) //
				.forEach(e -> outStream.println(e.toString()));

		switch (stack.size()) {
		case 0:
			break;
		case 1:
			outStream.println("(1 event is still running: " + stack.peek().getName() + ")");
			break;
		default:
			outStream.println("(" + stack.size() + " events are still running)");
			break;
		}
	}

	public class Event {
		final String name;
		final String dir;
		long time;
		int count = 0;

		Event(String name, Event enclosing, long time) {
			this.name = name;
			dir = enclosing == null ? null : enclosing.getPath();
			this.time = time;
		}

		long stop(long time) {
			this.time = time - this.time;
			count++;
			return time;
		}

		public String getName() {
			return name;
		}

		public String getPath() {
			return dir == null ? name : dir + '.' + name;
		}

		/**
		 * Get duration of the event. Units are whatever Stopwatch was configured with
		 * (millis by default)
		 */
		long getTime() {
			return getTime(time, resolution);
		}

		long getTime(long time, TimeUnit unit) {
			switch (unit) {
			case NANOSECONDS:
				return timeSource.getUnit().toNanos(time);
			case MICROSECONDS:
				return timeSource.getUnit().toMicros(time);
			case MILLISECONDS:
				return timeSource.getUnit().toMillis(time);
			case SECONDS:
				return timeSource.getUnit().toSeconds(time);
			case MINUTES:
				return timeSource.getUnit().toMinutes(time);
			case HOURS:
				return timeSource.getUnit().toHours(time);
			case DAYS:
				return timeSource.getUnit().toDays(time);
			default:
				return 0;
			}
		}

		String getTimeWithUnits(TimeUnit unit) {
			return getTimeWithUnits(getTime(), unit);
		}

		String getTimeWithUnits(long time, TimeUnit unit) {
			switch (unit) {
			case NANOSECONDS:
				return time + "ns";
			case MICROSECONDS:
				return time + "micros";
			case MILLISECONDS:
				return time + "ms";
			case SECONDS:
				return time + "s";
			case MINUTES:
				return time + "m";
			case HOURS:
				return time + "h";
			case DAYS:
				return time + "d";
			default:
				return time + "";
			}
		}

		public int getCount() {
			return count;
		}

		void merge(Event other) {
			time += other.getTime();
			count += other.getCount();
		}

		@Override
		public String toString() {
			if (count < 2)
				return String.format("%-24s %s", getPath(), getTimeWithUnits(resolution));

			return String.format("%-24s %s (%d @ %s)", getPath(), getTimeWithUnits(resolution), getCount(),
					getTimeWithUnits(getTime() / getCount(), resolution));
		}

	}

	private static abstract class TimeSource {
		abstract TimeUnit getUnit();

		abstract long getTime();
	}

	private static class MilliTimeSource extends TimeSource {
		@Override
		TimeUnit getUnit() {
			return TimeUnit.MILLISECONDS;
		}

		@Override
		long getTime() {
			return System.currentTimeMillis();
		}

	}

	private static class NanoTimeSource extends TimeSource {
		@Override
		TimeUnit getUnit() {
			return TimeUnit.NANOSECONDS;
		}

		@Override
		long getTime() {
			return System.nanoTime();
		}
	}
}
