package de.vksi.c4j.systemtest.pure;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.vksi.c4j.ContractReference;
import de.vksi.c4j.PureTarget;
import de.vksi.c4j.systemtest.TransformerAwareRule;

public class PureTargetSystemTest {
	@Rule
	public TransformerAwareRule transformerAware = new TransformerAwareRule();

	private TargetClass target;

	@Before
	public void before() {
		target = new TargetClass();
	}

	@Test
	public void testPureMethod() {
		target.pureMethod();
	}

	@Test
	public void testPureMethodReadingField() {
		target.pureMethodReadingField();
	}

	@Test(expected = AssertionError.class)
	public void testPureMethodWritingField() {
		target.unpureMethodWritingField();
	}

	@Test
	public void testPureMethodCallingOtherPureMethod() {
		target.pureMethodCallingOtherPureMethod();
	}

	@Test(expected = AssertionError.class)
	public void testUnpureMethodCallingOtherUnpureMethod() {
		target.unpureMethodCallingOtherUnpureMethod();
	}

	@Test
	public void testPureMethodCallingPureMethodInOtherClass() {
		target.pureMethodCallingPureMethodInOtherClass(new OtherClass());
	}

	@Test(expected = AssertionError.class)
	public void testUnpureMethodCallingUnpureMethodInOtherClass() {
		target.unpureMethodCallingUnpureMethodInOtherClass(new OtherClass());
	}

	@ContractReference(ContractClass.class)
	private static class TargetClass {
		private String field = "sample";

		public void pureMethod() {
		}

		public String pureMethodReadingField() {
			return field;
		}

		public void unpureMethodWritingField() {
			field = "illegal";
		}

		public void pureMethodCallingOtherPureMethod() {
			otherPureMethod();
		}

		protected void otherPureMethod() {
		}

		public void unpureMethodCallingOtherUnpureMethod() {
			otherUnpureMethod();
		}

		private void otherUnpureMethod() {
		}

		public void pureMethodCallingPureMethodInOtherClass(OtherClass other) {
			other.pureMethodInOtherClass();
		}

		public void unpureMethodCallingUnpureMethodInOtherClass(OtherClass other) {
			other.unpureMethodInOtherClass();
		}
	}

	private static class ContractClass extends TargetClass {
		@Override
		@PureTarget
		public void pureMethod() {
		}

		@Override
		@PureTarget
		public String pureMethodReadingField() {
			return null;
		}

		@Override
		@PureTarget
		public void unpureMethodWritingField() {
		}

		@Override
		@PureTarget
		public void pureMethodCallingOtherPureMethod() {
		}

		@Override
		@PureTarget
		protected void otherPureMethod() {
		}

		@Override
		@PureTarget
		public void unpureMethodCallingOtherUnpureMethod() {
		}

		@Override
		@PureTarget
		public void pureMethodCallingPureMethodInOtherClass(OtherClass other) {
		}

		@Override
		@PureTarget
		public void unpureMethodCallingUnpureMethodInOtherClass(OtherClass other) {
		}
	}

	@ContractReference(OtherClassContract.class)
	private static class OtherClass {
		public void pureMethodInOtherClass() {
		}

		public void unpureMethodInOtherClass() {
		}
	}

	private static class OtherClassContract extends OtherClass {
		@Override
		@PureTarget
		public void pureMethodInOtherClass() {
		}
	}
}
