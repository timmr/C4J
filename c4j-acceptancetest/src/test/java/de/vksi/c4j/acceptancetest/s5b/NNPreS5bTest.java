package de.vksi.c4j.acceptancetest.s5b;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.vksi.c4j.systemtest.TransformerAwareRule;
import de.vksi.c4j.acceptancetest.subclasses.Bottom;

public class NNPreS5bTest {

	@Rule
	public TransformerAwareRule transformerAware = new TransformerAwareRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void returnsValueWhenPreConditionIsSatisfied() {
		assertThat(new Bottom(42).pre(""), is(42));
	}
	
	@Test
	public void failsWhenPreConditionIsNotMet() {
		thrown.expect(AssertionError.class);
		thrown.expectMessage("must not be null");
		new Bottom(42).pre(null);
	}
}
