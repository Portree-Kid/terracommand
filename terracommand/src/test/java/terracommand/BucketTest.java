package terracommand;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.keithpaterson.terracommand.entities.OneDegreeBucket;
import de.keithpaterson.terracommand.entities.TenDegreeBucket;

public class BucketTest {

	@Test
	public void testZero() {
		String bucket = OneDegreeBucket.calcName(0d, 0d);
		assertEquals("e000n00", bucket);
	}

	@Test
	public void testNegative() {
		String bucket = OneDegreeBucket.calcName(-0.1d, -0.1d);
		assertEquals("w001s01", bucket);
	}

	@Test
	public void testNegative2() {
		String bucket = OneDegreeBucket.calcName(-1.1d, -1.1d);
		assertEquals("w002s02", bucket);
	}
	@Test
	public void testTenZero() {
		String bucket = TenDegreeBucket.calcName(0d, 0d);
		assertEquals("e000n00", bucket);
	}

	@Test
	public void testTenNegative() {
		String bucket = TenDegreeBucket.calcName(-0.1d, -0.1d);
		assertEquals("w010s10", bucket);
	}

	@Test
	public void testTenNegative2() {
		String bucket = TenDegreeBucket.calcName(-1.1d, -1.1d);
		assertEquals("w010s10", bucket);
	}
}
