package de.andrena.next.internal.transformer;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtNewConstructor;
import de.andrena.next.internal.ContractRegistry.ContractInfo;

public class ConstructorTransformer extends AbstractContractClassTransformer {
	public static final String CONSTRUCTOR_REPLACEMENT_NAME = "constructor$";

	@Override
	public void transform(ContractInfo contractInfo, CtClass contractClass) throws Exception {
		if (contractClass.equals(contractInfo.getContractClass())) {
			for (CtConstructor constructor : contractClass.getConstructors()) {
				contractClass.addMethod(constructor.toMethod(CONSTRUCTOR_REPLACEMENT_NAME, contractClass));
			}
			contractClass.getClassFile().setSuperclass(null);
			for (CtConstructor constructor : contractClass.getConstructors()) {
				contractClass.removeConstructor(constructor);
			}
			contractClass.addConstructor(CtNewConstructor.defaultConstructor(contractClass));
		}
	}

}