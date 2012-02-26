package de.andrena.next.internal.util;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import org.apache.log4j.Logger;

import de.andrena.next.ClassInvariant;
import de.andrena.next.internal.RootTransformer;
import de.andrena.next.internal.transformer.ContractBehaviorTransformer;
import de.andrena.next.internal.transformer.TransformationException;
import de.andrena.next.internal.util.ContractRegistry.ContractInfo;

public class AffectedBehaviorLocator {
	private ReflectionHelper reflectionHelper = new ReflectionHelper();
	private InvolvedTypeInspector involvedTypeInspector = new InvolvedTypeInspector();
	private Logger logger = Logger.getLogger(getClass());

	public CtBehavior getAffectedBehavior(ContractInfo contractInfo, CtClass affectedClass, CtBehavior contractBehavior)
			throws NotFoundException, CannotCompileException {
		CtBehavior affectedBehavior = null;
		if (contractBehavior.hasAnnotation(ClassInvariant.class)) {
			return null;
		}
		if (reflectionHelper.isContractConstructor(contractBehavior)) {
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
		if (affectedMethod.getDeclaringClass().equals(affectedClass)) {
			return affectedMethod;
		}
		if (!hasContract(affectedMethod.getDeclaringClass(), contractInfo)) {
			logger.warn("could not find method " + contractBehavior.getName() + " in affected class "
					+ affectedClass.getName() + " for contract class " + contractInfo.getContractClass().getName()
					+ " - inserting an empty method");
			affectedMethod = CtNewMethod.delegator(affectedMethod, affectedClass);
			affectedMethod.setModifiers(Modifier.clear(affectedMethod.getModifiers(), Modifier.NATIVE));
			affectedMethod.setModifiers(Modifier.clear(affectedMethod.getModifiers(), Modifier.ABSTRACT));
			affectedClass.addMethod(affectedMethod);
			return affectedMethod;
		}
		return null;
	}

	private boolean hasContract(CtClass clazz, ContractInfo contractInfo) throws NotFoundException {
		ListOrderedSet<CtClass> involvedTypes = involvedTypeInspector.inspect(clazz);
		ListOrderedSet<ContractInfo> contracts = RootTransformer.INSTANCE.getContractsForTypes(involvedTypes);
		return contracts.contains(contractInfo);
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
			contractInfo.getContractClass().getDeclaredMethod(ContractBehaviorTransformer.CONSTRUCTOR_REPLACEMENT_NAME,
					contractBehavior.getParameterTypes());
			return null;
		} catch (NotFoundException e) {
			return affectedConstructor;
		}
	}

	private CtClass[] getConstructorParameterTypes(CtClass affectedClass, CtBehavior contractBehavior)
			throws NotFoundException {
		CtClass[] parameterTypes = contractBehavior.getParameterTypes();
		if (reflectionHelper.constructorHasAdditionalParameter(affectedClass)) {
			CtClass[] initialParameterTypes = parameterTypes;
			parameterTypes = new CtClass[parameterTypes.length + 1];
			parameterTypes[0] = affectedClass.getDeclaringClass();
			for (int i = 0; i < initialParameterTypes.length; i++) {
				parameterTypes[i + 1] = initialParameterTypes[i];
			}
		}
		return parameterTypes;
	}
}
