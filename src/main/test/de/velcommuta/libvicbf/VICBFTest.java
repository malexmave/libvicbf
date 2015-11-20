package de.velcommuta.libvicbf;

import static org.junit.Assert.*;

import org.junit.Test;

public class VICBFTest {

	@Test
	public void test_slot_calculation() {
		VICBF test = new VICBF(10000, 3);
		short v = test.calculateSlot("123", 0);
		assertEquals(v, (short) 9653);
		v = test.calculateSlot("testvicbf", 0);
		assertEquals(v, (short) 8525);
		v = test.calculateSlot("decafbad", 0);
		assertEquals(v, (short) 7634);
	}
	
	@Test
	public void test_increment_calculation() {
		VICBF test = new VICBF(10000, 3);
		byte t = test.calculateIncrement("123", 0);
		assertEquals(t, (byte) 5);
		t = test.calculateIncrement("testvicbf", 0);
		assertEquals(t, (byte) 7);
		t = test.calculateIncrement("decafbad", 0);
		assertEquals(t, (byte) 5);
	}
	
	@Test
	public void test_insert_query() {
		VICBF test = new VICBF(10000, 3);
		test.insert("deadbeef");
		assertTrue(test.query("deadbeef"));
		assertFalse(test.query("deafbeet"));
	}

}
