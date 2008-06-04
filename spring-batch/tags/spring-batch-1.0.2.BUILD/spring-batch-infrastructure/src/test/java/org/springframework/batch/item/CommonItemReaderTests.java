package org.springframework.batch.item;

import junit.framework.TestCase;

import org.springframework.batch.item.sample.Foo;

/**
 * Common tests for {@link ItemReader} implementations. Expected input is five
 * {@link Foo} objects with values 1 to 5.
 */
public abstract class CommonItemReaderTests extends TestCase {

	protected ItemReader tested;

	/**
	 * @return configured ItemReader ready for use.
	 */
	protected abstract ItemReader getItemReader() throws Exception;

	protected void setUp() throws Exception {
		tested = getItemReader();
	}

	/**
	 * Regular scenario - read the input and eventually return null.
	 */
	public void testRead() throws Exception {

		Foo foo1 = (Foo) tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) tested.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = (Foo) tested.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = (Foo) tested.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = (Foo) tested.read();
		assertEquals(5, foo5.getValue());

		assertNull(tested.read());
	}

	/**
	 * Rollback scenario - reader resets to last marked point.
	 */
	public void testReset() throws Exception {
		Foo foo1 = (Foo) tested.read();
		assertEquals(1, foo1.getValue());
		
		tested.mark();

		Foo foo2 = (Foo) tested.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = (Foo) tested.read();
		assertEquals(3, foo3.getValue());

		tested.reset();

		assertEquals(foo2, tested.read());
		
		assertEquals(foo3, tested.read());
		

		Foo foo4 = (Foo) tested.read();
		assertEquals(4, foo4.getValue());
		
		tested.mark();
		
		Foo foo5 = (Foo) tested.read();
		assertEquals(5, foo5.getValue());
		
		assertNull(tested.read());
		
	}

}
