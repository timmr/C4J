* [Downloads](https://github.com/C4J-Team/C4J/downloads)
* [Installation](https://github.com/C4J-Team/C4J/wiki/Installation)
* [![Build Status](https://buildhive.cloudbees.com/job/C4J-Team/job/C4J/badge/icon)](https://buildhive.cloudbees.com/job/C4J-Team/job/C4J/)

The C4J Contracts Framework for Java is, since version 4.0, using inheritance to provide classes and interfaces with contracts.

So let's start with some easy code-examples:

Let's assume having a class `CashTerminal` that has to satisfy the contract `CashTerminalContract`:

```java
@ContractReference(CashTerminalContract.class)
public class CashTerminal {
  public void withdraw(int amount) {
    // withdraw money from customer's bank account
  }
}
```

Our contract `CashTerminalContract` could ensure that only a positive amount of money can be withdrawn from a `CashTerminal`:

```java
import static de.vksi.c4j.Condition.*;

public class CashTerminalContract extends CashTerminal {
  @Override
  public void withdraw(int amount) {
    if (preCondition()) {
      assert amount > 0;
    }
  }
}
```

Let's look at this code example in more detail, step by step.

## Declaring Contracts
A Contract is declared by using the `@ContractReference` annotation on the _Target_ class or interface, providing the class defining the contract:

```java
@ContractReference(CashTerminalContract.class)
public class CashTerminal {
  // target class body
}
```

```java
@ContractReference(InterfaceContract.class)
public interface Interface {
  // interface declarations
}
```

A contract must be satisfied not only by the type for which it is declared, but also by all classes that are extending or implementing this type.

![Inheritance rules for C4J Contracts](/c4j/doc/inheritance.png)

## Defining Contracts
A contract is defined in a _Contract Class_. This class can extend the _Target Class_ (or implement the _Target Interface_) for convenience and to allow easy refactoring in modern Java IDEs, but doesn't have to (for `final` classes, see chapter below).

```java
public class CashTerminalContract extends CashTerminal {
  // contract class body
}
```

```java
public class InterfaceContract implements Interface {
  // contract class body for an interface
}
```

Within the contract class, the methods of the target class can be "overridden", and _Pre-Conditions_ and _Post-Conditions_ can be defined for specific methods. Also, _Class-Invariants_ can be defined.

## Defining Pre- and Post-Conditions
Pre- and Post-Conditions for a method can be defined within the contract class as follows:

```java
@ContractReference(CashTerminalContract.class)
public class CashTerminal {
  public void withdraw(int amount) {
    // impl
  }
  public int getBalance() {
    // impl
  }
}
```

```java
import static de.vksi.c4j.Condition.*;

public class CashTerminalContract extends CashTerminal {
  @Target
  private CashTerminal target;

  @Override
  public void withdraw(int amount) {
    if (preCondition()) {
      assert amount > 0;
    }
    if (postCondition()) {
      assert target.getBalance() == old(target.getBalance()) - amount;
    }
  }
}
```

The Pre-Condition of `withdraw(int)` requires, that the parameter `amount` is greater than 0. The Post-Condition ensures, that the balance (after execution of method `withdraw(int)`) is equal to the old balance (before execution) substracted by the parameter `amount`.

Each instance of a target class being protected by a contract has its corresponding instance of the contract class. In order to access the target instance from within the contract, a field having the type of the target class can be annotated with the `@Target` annotation.

## old
`old`, as seen in the example above, can be used to access the value of an expression before the execution of the method in question. There can be no usage of local variables inside the parameter being passed to `old`.

```java
  @Override
  public void withdraw(int amount) {
    if (postCondition()) {
      assert target.getBalance() == old(target.getBalance()) - amount; // works
      assert target.getBalance() == old(target.getBalance() - amount); // works
      assert target.getBalance() == old(arbitraryObject.arbitraryMethod("123", 456, new BigDecimal("0.01")).getAmount() + SomeClass.staticMethodCall()); // works
      int localVar = 3;
      assert target.getBalance() == old(target.getBalance() - localVar); // doesn't work as a local variable is used within old()
    }
  }
```

## Class-Invariants
Sometimes, a set of conditions can be defined for a class, that always must be satisfied. These conditions can then be defined in a so-called _Class-Invariant_.

```java
@ContractReference(CashTerminalContract.class)
public class CashTerminal {
  public int getBalance() {
    // impl
  }
}
```

```java
public class CashTerminalContract extends CashTerminal {
  @Target
  private CashTerminal target;

  @ClassInvariant
  public void invariant() {
    assert target.getBalance() >= 0;
  }
}
```

The class-invariant is introduced by defining a non-overridden method in the contract class, which is annotated by `@ClassInvariant`. The class-invariant will be run after the execution of any non-private method or constructor, enforcing the conditions defined in the class-invariant.

## Final Classes and Methods
Defining contracts for `final` classes and methods can be tricky, as the contract class cannot extend a final target class or override a final method an thus, no refactoring-support is available.

```java
@ContractReference(FinalClassContract.class)
public final class FinalClass {
  public final void finalMethod() {
    // impl
  }
}
```

```java
public class FinalClassContract {
  public void finalMethod() {
    // contract impl
  }
}
```

In this example, renaming `finalMethod()` in `FinalClass` would not rename `finalMethod()` in `FinalClassContract` and a warning would be issued on class-loading.

There is one way to avoid the refactoring-trouble: By introducing an interface. Assuming that target and contract class are located within the same package, the interface can even be made package-private and thus can be located within the same source-file as the final class. This avoids confusion and hides the synthetic interface from clients. The contract declaration thus moves from the final class to the interface.

```java
@ContractReference(FinalClassInterfaceContract.class)
interface FinalClassInterface {
  void finalMethod();
}

public final class FinalClass implements FinalClassInterface {
  public final void finalMethod() {
    // impl
  }
}
```

```java
public class FinalClassInterfaceContract implements FinalClassInterface {
  @Override
  public void finalMethod() {
    // contract impl
  }
}
```

# The concept of Pure Methods.
The following will only work with a custom `Configuration` which includes `PureBehavior.VALIDATE_PURE` (details below). This functionality is experimental and has not yet been fully tested.

## Setting up a custom Configuration

* Implement a class implementing `Configuration` or extending `DefaultConfiguration`, declaring at least one package in `getRootPackages()` and adding `PureBehavior.VALIDATE_PURE` to `getPureBehaviors()`.
* Add the fully qualified class name of your `Configuration` as a javaagent-Parameter in the VM Arguments, e.g. `-ea -javaagent:/path/to/c4j-[version].jar=package.with.the.CustomConfiguration`.

## Method purity
There's a very good introduction to method purity for Microsoft's Code Contracts on [this blog](http://www.minddriven.de/index.php/technology/dot-net/code-contracts/code-contracts-method-purity). In C4J 4.0, all contract methods are implicitly declared pure. If you want to declare methods pure in target classes, you can either use the `@Pure` annotation on the method directly, or use the `@PureTarget` annotation on the corresponding contract method (especially handy when using external contracts). When referencing methods outside the root packages, a warning is issued, asking you to declare the method either pure or unpure using `Configuration.getPureRegistry()`.

## unchanged
This feature can only be used when `PureBehavior.VALIDATE_PURE` is turned on, as it's relying on the same implementation as pure methods.

As methods should often only modify a particular set of attributes of a class, many post-conditions ensure that the other attributes stay unchanged.

```java
@ContractReference(TimeOfDayContract.class)
public interface TimeOfDay {
  int getHour();
  int getMinute();
  int getSecond();
  void setHour(int hour);
  void setMinute(int minute);
  void setSecond(int second);
}
```

```java
import static de.vksi.c4j.Condition.*;

public class TimeOfDayContract implements TimeOfDay {
  @Target
  private TimeOfDay target;

  @Override
  public void setHour(final int hour) {
    if (postCondition()) {
      unchanged(target.getMinute(), target.getSecond());
    }
  }
  // etc.
}
```

For primitive types, unchanged checks if the value at the end of the method call is the same as in the beginning of the method call. For reference types, it ensures that the object's state is not being modified while the method is running.

In addition to fields and methods without parameters, method parameters can also be used with `unchanged`. As parameters act as local variables, a redefinition is not being checked and thus unchanged does not have any effect at all, if the method parameter is of primitive type.
[![githalytics.com alpha](https://cruel-carlota.gopagoda.com/c2ddfc8e7d65d09f59976e6e89c94fd4 "githalytics.com")](http://githalytics.com/C4J-Team/C4J)
