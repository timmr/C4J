package de.vksi.c4j;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares a contract for a class (and its subclasses) or interface (and its implementing classes). The class (or
 * interface) declaring this annotation is called the contract-class of the contract, the type this contract is written
 * for is called the target-type. If the contract-class containing this annotation is not inheriting from the
 * target-type (e.g. when the target-class is final), it must be declared in the attribute forTarget, otherwise this is
 * optional.
 * <p>
 * Note that defining a contract relation on the contract-class itself (and not on the target-type using @
 * {@link ContractReference}) only works when the contract's package is contained within a
 * <code>&lt;contract-scan-package&gt;</code> in the local configuration.
 */
@Documented
@Target(ElementType.TYPE)
public @interface Contract {
	Class<?> forTarget() default InheritedType.class;

	public static interface InheritedType {
	}
}
