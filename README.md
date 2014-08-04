java-traits
===========

The aim of this project is to bring the language feature of [Traits](http://en.wikipedia.org/wiki/Trait_%28computer_programming%29) to Java using compile-time annotation processing.

For those who want the short explanation, traits are like a combination of a Java interface and an abstract class. They're a useful mechanism for reusing shared logic somewhat akin to multiple inheritance.

## Defining traits
For the purposes of this project, traits are defined as abstract classes with an `@Trait` annotation:

```java
@Trait
public abstract class Rectangular {

    public abstract int getWidth();
    public abstract int getHeight();

    public int getPerimeter() {
        return 2 * (getWitdth() + getHeight());
    }

    public int getArea() {
        return getWidth() * getHeight();
    }

    public boolean isSquare() {
        return getWidth() == getHeight();
    }
}
```
A trait implicitly defines an interface of all its public declared methods:

```java
public interface IRectangular {
    public int getWidth();
    public int getHeight();
    public int getPerimeter();
    public int getArea();
    public boolean isSquare();
}
```

The code generator will take care of generating this interface for you. A class that has the `Rectangular` trait will implement `IRectangular` while guaranteeing that the implementation of the concrete methods `getArea()` etc. will be identical to the implementations declared in `Rectangular`.

## Using traits

### Declaring that a class has traits
To declare that one of your classes uses a trait you use the `@HasTraits` annotation:

```java
@HasTraits(traits={Rectangular.class})
public class FootballField extends FootballFieldWithTraits {
    ...
}
```

Take note that if your class uses the `@HasTraits` annotation, it MUST extend from a generated class with the suffix "WithTraits", e.g. `FootballFieldWithTraits`. If you don't extend from the generated class with the correct name, none of the traits will be applied. "But what if I need to subclass some other thing?" you say? Simply specify your desired superclass in the `@HasTraits` annotation:

```java
@HasTraits(traits={Rectangular.class}, desiredSuperclass=SportsField.class)
public class FootballField extends FootballFieldWithTraits {
    ...
}
```

The code generator will ensure that `FootballFieldWithTraits` extends `SportsField`. It will also generate the interface definitions implicitly defined by each trait as in the above example and ensure that the generated superclass explicitly implements all the required interfaces. For example, `FootballFieldWithTraits` will look something like this:

```java
public abstract class FootballFieldWithTraits extends SportsField implements IRectangular {
    ....
}
```

### Implement details
There only thing you are still responsible for is to implement all the abstract methods declared in all the traits your class is using -- the code generator does NOT implement those for you!

A complete class definition using traits would look something like this:

```java
@HasTraits(traits={Rectangular.class}, desiredSuperclass=SportsField.class)
public class FootballField extends FootballFieldWithTraits {

    public int getWidth() {
        return 160;
    }
    public int getHeight() {
        return 320;
    }
}
```

The `FootballField` class is guaranteed to implement the rest of `IRectangular` for you as defined in the trait, and is guaranteed to still be a subclass of `SportsField` -- the code generation takes care of all those details for you.

Remember, you can declare that a class has multiple traits! Just declare them in a comma-separated list, e.g. `@HasTraits(traits={Rectangular.class, Resizeable.class})`
