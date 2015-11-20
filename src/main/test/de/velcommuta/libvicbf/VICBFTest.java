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
		assertFalse(test.query("deadbeef"));
		test.insert("deadbeef");
		assertTrue(test.query("deadbeef"));
		assertFalse(test.query("deafbeet"));
	}
	
	@Test
	public void test_insert_delete() {
		VICBF test = new VICBF(10000, 3);
		test.insert("decafbad");
		assertTrue(test.query("decafbad"));
		try {
			test.remove("decafbad");
		} catch (Exception e) {
			assertTrue(false);
		}
		assertFalse(test.query("decafbad"));
	}
	
	@Test
	public void test_delete_not_inserted() {
		VICBF test = new VICBF(10000, 3);
		try {
			test.remove("decafbad");
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
	}
	
	@Test
	public void test_multi_insert_one_delete() {
		VICBF test = new VICBF(10000, 3);
		test.insert("decafbad");
		test.insert("deadbeef");
		test.insert("carebearstare");
		try {
			test.remove("decafbad");
		} catch (Exception e) {
			assertTrue(false);
		}
		assertFalse(test.query("decafbad"));
		assertTrue(test.query("deadbeef"));
		assertTrue(test.query("carebearstare"));
	}
	
	@Test
	public void test_delete_regression_1() {
		// This test aims to check a specific bordercase in the deletion routine
		// that could result in an inconsistent VICBF when triggered.
		VICBF test = new VICBF(10000, 3);
		test.insert("106");
		test.insert("771");
		try {
			test.remove("132");
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
		try {
			test.remove("106");
		} catch (Exception e) {
			assertFalse(true);
		}
		assertTrue(test.query("771"));
	}
}
