package de.andrena.next.internal.transformer;

import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import de.andrena.next.ClassInvariant;
import de.andrena.next.internal.compiler.ArrayExp;
import de.andrena.next.internal.compiler.NestedExp;
import de.andrena.next.internal.compiler.StaticCallExp;
import de.andrena.next.internal.compiler.ValueExp;
import de.andrena.next.internal.evaluator.Evaluator;
import de.andrena.next.internal.util.ContractRegistry.ContractInfo;
import de.andrena.next.internal.util.ObjectConverter;

public class BeforeAndAfterTriggerTransformer extends AffectedClassTransformerForSingleContract {

	@Override
	public void transform(ContractInfo contractInfo, CtClass affectedClass) throws Exception {
		for (CtBehavior contractBehavior : contractInfo.getContractClass().getDeclaredBehaviors()) {
			transform(contractInfo, affectedClass, contractBehavior);
		}
	}

	public void transform(ContractInfo contractInfo, CtClass affectedClass, CtBehavior contractBehavior)
			throws Exception {
		CtBehavior affectedBehavior = getAffectedBehavior(contractInfo, affectedClass, contractBehavior);
		if (affectedBehavior == null) {
			return;
		}
		String contractBehaviorName = getContractBehaviorName(contractBehavior);
		logger.info("transforming method " + affectedBehavior.getLongName() + ", triggered by "
				+ contractBehavior.getLongName());
		ArrayExp paramTypesArray = ArrayExp.forParamTypes(contractBehavior);
		ArrayExp argsArray = getArgsArray(affectedClass, contractBehavior);
		StaticCallExp callBefore = new StaticCallExp(Evaluator.before, NestedExp.THIS, new ValueExp(
				contractInfo.getContractClass()), new ValueExp(contractBehaviorName), paramTypesArray, argsArray);
		StaticCallExp callAfter = new StaticCallExp(Evaluator.after, NestedExp.THIS, new ValueExp(
				contractInfo.getContractClass()), new ValueExp(contractBehaviorName), paramTypesArray, argsArray,
				getReturnValueExp(affectedBehavior));
		logger.info("before: " + callBefore.toStandalone().getCode());
		logger.info("after: " + callAfter.toStandalone().getCode());
		callBefore.toStandalone().insertBefore(affectedBehavior);
		callAfter.toStandalone().insertAfter(affectedBehavior);
	}

	private NestedExp getReturnValueExp(CtBehavior affectedBehavior) throws NotFoundException {
		if (!(affectedBehavior instanceof CtMethod)
				|| ((CtMethod) affectedBehavior).getReturnType().equals(CtClass.voidType)) {
			return NestedExp.NULL;
		}
		return new StaticCallExp(ObjectConverter.toObject, NestedExp.RETURN_VALUE);
	}

	String getContractBehaviorName(CtBehavior contractBehavior) {
		String contractBehaviorName;
		if (isConstructor(contractBehavior)) {
			contractBehaviorName = ConstructorTransformer.CONSTRUCTOR_REPLACEMENT_NAME;
		} else {
			contractBehaviorName = contractBehavior.getName();
		}
		return contractBehaviorName;
	}

	CtBehavior getAffectedBehavior(ContractInfo contractInfo, CtClass affectedClass, CtBehavior contractBehavior)
			throws NotFoundException, CannotCompileException {
		CtBehavior affectedBehavior = null;
		if (contractBehavior.hasAnnotation(ClassInvariant.class)) {
			return null;
		}
		if (isConstructor(contractBehavior)) {
			affectedBehavior = getAffectedConstructor(contractInfo, affectedClass, contractBehavior);
		} else if (contractBehavior instanceof CtMethod) {
			affectedBehavior = getAffectedMethod(contractInfo, affectedClass, contractBehavior);
		} else {
			throw new TransformationException("contractBehavior " + contractBehavior.getLongName()
					+ " is neither constructor nor method");
		}
		return affectedBehavior;
	}

	CtMethod getAffectedMethod(ContractInfo contractInfo, CtClass affectedClass, CtBehavior contractBehavior)
			throws NotFoundException, CannotCompileException {
		CtClass currentClass = affectedClass;
		CtMethod affectedMethod = null;
		while (affectedMethod == null && currentClass != null) {
			try {
				affectedMethod = currentClass.getDeclaredMethod(contractBehavior.getName(),
						contractBehavior.getParameterTypes());
			} catch (NotFoundException e) {
			}
			currentClass = currentClass.getSuperclass();
		}
		if (affectedMethod == null) {
			logger.warn("could not find a matching method in affected class " + affectedClass.getName()
					+ " for method '" + contractBehavior.getName() + "' in contract class "
					+ contractInfo.getContractClass().getName());
			return null;
		}
		if (!affectedMethod.getDeclaringClass().equals(affectedClass)) {
			logger.warn("could not find method " + contractBehavior.getName() + " in affected class "
					+ affectedClass.getName() + " for contract class " + contractInfo.getContractClass().getName()
					+ " - inserting an empty method");
			affectedMethod = CtNewMethod.delegator(affectedMethod, affectedClass);
			affectedClass.addMethod(affectedMethod);
		}
		return affectedMethod;
	}

	CtConstructor getAffectedConstructor(ContractInfo contractInfo, CtClass affectedClass, CtBehavior contractBehavior) {
		if (contractInfo.getTargetClass().isInterface()) {
			return null;
		}
		CtConstructor affectedConstructor;
		try {
			affectedConstructor = affectedClass.getDeclaredConstructor(getConstructorParameterTypes(affectedClass,
					contractBehavior));
		} catch (NotFoundException e) {
			logger.warn("could not find a matching constructor in affected class " + affectedClass.getName()
					+ " for the constructor defined in contract class " + contractInfo.getContractClass().getName());
			return null;
		}
		if (contractBehavior instanceof CtMethod) {
			return affectedConstructor;
		}
		try {
			contractInfo.getContractClass().getDeclaredMethod(ConstructorTransformer.CONSTRUCTOR_REPLACEMENT_NAME,
					contractBehavior.getParameterTypes());
			return null;
		} catch (NotFoundException e) {
			return affectedConstructor;
		}
	}

	private CtClass[] getConstructorParameterTypes(CtClass affectedClass, CtBehavior contractBehavior)
			throws NotFoundException {
		CtClass[] parameterTypes = contractBehavior.getParameterTypes();
		if (constructorHasAdditionalParameter(affectedClass)) {
			CtClass[] initialParameterTypes = parameterTypes;
			parameterTypes = new CtClass[parameterTypes.length + 1];
			parameterTypes[0] = affectedClass.getDeclaringClass();
			for (int i = 0; i < initialParameterTypes.length; i++) {
				parameterTypes[i + 1] = initialParameterTypes[i];
			}
		}
		return parameterTypes;
	}

	private boolean constructorHasAdditionalParameter(CtClass affectedClass) throws NotFoundException {
		return affectedClass.getDeclaringClass() != null && !Modifier.isStatic(affectedClass.getModifiers());
	}

	private ArrayExp getArgsArray(CtClass affectedClass, CtBehavior contractBehavior) throws NotFoundException {
		if (isConstructor(contractBehavior) && constructorHasAdditionalParameter(affectedClass)) {
			return ArrayExp.forArgs(contractBehavior, 2);
		}
		return ArrayExp.forArgs(contractBehavior);
	}

	boolean isConstructor(CtBehavior contractBehavior) {
		return contractBehavior instanceof CtConstructor
				|| contractBehavior.getName().equals(ConstructorTransformer.CONSTRUCTOR_REPLACEMENT_NAME);
	}
}
