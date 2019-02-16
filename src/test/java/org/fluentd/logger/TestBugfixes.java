package org.fluentd.logger;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

public class TestBugfixes {
	/** Prior to the issue fix, this test fails with an NPE on the last line.
	 */
	@Test
	public void validLoggerReturned_whenOpenThenCloseThenOpenWithSameParameters() {
		// use test sender so we don't need to have an actual fluentd running...
		System.setProperty(Config.FLUENT_SENDER_CLASS, "org.fluentd.logger.sender.NullSender");		
		FluentLogger logger = FluentLogger.getLogger("test");

		// this works
		logger.log("tag", Collections.<String, Object>emptyMap());
		
		// now close it; sender is closed and set to null	
		logger.close();
		assertEquals(null, logger.sender);
		
		// get another logger with the exact same parameters; we'd expect this to work, yes?
		FluentLogger logger2 = FluentLogger.getLogger("test");

		// let's see if it does
		logger2.log("tag", Collections.<String, Object>emptyMap());
	}
}
