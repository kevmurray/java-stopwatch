package name.kevmurray.stopwatch;

import static org.fest.assertions.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class StopwatchTest {

	/** Test default functionality with millisecond resolution */
	public static class Functionality {
		private Stopwatch sut;

		private static final long opLen = 100;
		private static final long slop = 50;

		private static boolean verbose = true;

		@Before
		public void beforeEachTest() {
			sut = new Stopwatch();
		}

		@Test
		public void defaultEvent() throws Exception {
			sut.start();
			Thread.sleep(opLen);
			sut.stop();

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent();
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen).isLessThan(opLen + slop);
		}

		@Test
		public void namedEvent() throws Exception {
			sut.start("unittest");
			Thread.sleep(opLen);
			sut.stop("unittest");

			if (verbose)
				sut.report(System.out);

			assertThat(sut.getEvent()).isNull();

			Stopwatch.Event event = sut.getEvent("unittest");
			if (verbose)
				System.out.println(event);
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen).isLessThan(opLen + slop);
		}

		@Test
		public void singleNestedEvent() throws Exception {
			sut.start("unittest");
			Thread.sleep(opLen);
			sut.start("nest");
			Thread.sleep(opLen);
			sut.stop("nest");
			Thread.sleep(opLen);
			sut.stop("unittest");

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent("unittest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 3).isLessThan(opLen * 3 + slop);

			event = sut.getEvent("unittest.nest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen).isLessThan(opLen + slop);

			assertThat(sut.report()).doesNotContain("(");
		}

		@Test
		public void doubleNestedEvent() throws Exception {
			sut.start("unittest");
			Thread.sleep(opLen);
			sut.start("nest");
			Thread.sleep(opLen);
			sut.start("deeper");
			Thread.sleep(opLen);
			sut.stop("deeper");
			Thread.sleep(opLen);
			sut.stop("nest");
			Thread.sleep(opLen);
			sut.stop("unittest");

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent("unittest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 5).isLessThan(opLen * 5 + slop);

			event = sut.getEvent("unittest.nest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 3).isLessThan(opLen * 3 + slop);

			event = sut.getEvent("unittest.nest.deeper");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen).isLessThan(opLen + slop);

			assertThat(sut.report()).doesNotContain("(");
		}

		@Test
		public void repeatedNestedEvent() throws Exception {
			sut.start("unittest");
			Thread.sleep(opLen);
			sut.start("nest");
			Thread.sleep(opLen);
			sut.stop("nest");
			Thread.sleep(opLen);
			sut.start("nest");
			Thread.sleep(opLen);
			sut.stop("nest");
			Thread.sleep(opLen);
			sut.stop("unittest");

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent("unittest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 5).isLessThan(opLen * 5 + slop);

			event = sut.getEvent("unittest.nest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 2).isLessThan(opLen * 2 + slop);
			assertThat(event.getCount()).isEqualTo(2);

			assertThat(sut.report()).contains("(2 @");
		}

		@Test
		public void unclosedEvent() throws Exception {
			sut.start("unittest");

			if (verbose)
				sut.report(System.out);

			assertThat(sut.getEvent("unittest")).isNull();
			assertThat(sut.report()).contains("1 event is still running");
		}

		@Test
		public void unclosedEvents() throws Exception {
			sut.start("unittest");
			sut.start("nest");

			if (verbose)
				sut.report(System.out);

			assertThat(sut.getEvent("unittest")).isNull();
			assertThat(sut.report()).contains("2 events are still running");
		}

		@Test
		public void unclosedNestedEvent() throws Exception {
			sut.start("unittest");
			Thread.sleep(opLen);
			sut.start("nest");
			Thread.sleep(opLen);
			// sut.stop("nest");
			Thread.sleep(opLen);
			sut.stop("unittest");

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent("unittest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 3).isLessThan(opLen * 3 + slop);

			// nest was never closed, so it should have been closed when unittest was closed
			event = sut.getEvent("unittest.nest");
			assertThat(event.getTime()).isGreaterThanOrEqualTo(opLen * 2).isLessThan(opLen * 2 + slop);
		}
	}

	/** Test default functionality with millisecond resolution */
	public static class Units {
		private Stopwatch sut;

		private static final long opLen = 100;
		private static final long slop = 50;

		private static boolean verbose = true;

		@Test
		public void millis() throws Exception {
			sut = new Stopwatch(TimeUnit.MILLISECONDS);
			sut.start();
			Thread.sleep(1);
			sut.stop();

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent();
			assertThat(event.getTime()).isGreaterThanOrEqualTo(1).isLessThan(10);
			assertThat(event.toString()).endsWith(event.getTime() + "ms");
		}

		@Test
		public void micros() throws Exception {
			sut = new Stopwatch(TimeUnit.MICROSECONDS);
			sut.start();
			Thread.sleep(1);
			sut.stop();

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent();
			assertThat(event.getTime()).isGreaterThanOrEqualTo(1000).isLessThan(10000);
			assertThat(event.toString()).endsWith(event.getTime() + "micros");
		}

		@Test
		public void seconds() throws Exception {
			sut = new Stopwatch(TimeUnit.SECONDS);
			sut.start();
			Thread.sleep(1);
			sut.stop();

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent();
			assertThat(event.getTime()).isEqualTo(0);
			assertThat(event.toString()).endsWith("0s");
		}

		@Test
		public void multipleSeconds() throws Exception {
			sut = new Stopwatch(TimeUnit.SECONDS);
			sut.start();
			Thread.sleep(2100);
			sut.stop();

			if (verbose)
				sut.report(System.out);

			Stopwatch.Event event = sut.getEvent();
			assertThat(event.getTime()).isEqualTo(2);
			assertThat(event.toString()).endsWith("2s");
		}
	}
}
