java-traits
===========

The aim of this project is to bring the language feature of [Traits](http://en.wikipedia.org/wiki/Trait_%28computer_programming%29) to Java using compile-time annotation processing.

For those who want the short explanation, traits are like a combination of a Java interface and an abstract class. They're a useful mechanism for reusing shared logic somewhat akin to multiple inheritance.

## Defining traits
For the purposes of this project, traits are defined as abstract classes with an @Trait annotation:

```java
@Trait
public abstract class Resizeable {
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }
    public abstract void setWidth(int width);
    public abstract void setHeight(int height);
}
```
A trait implicitly defines an interface of all its public declared methods:

```java
public interface ResizeableInterface {
    public void setSize(int width, int height);
    public void setWidth(int width);
    public void setHeight(int height);
}
```

The code generator will take care of generating this interface for you. A class that has the `Resizeable` trait will implement `ResizeableInterface` while guaranteeing that the implementation of `setSize` will be identical to the implementation declared in `Resizeable`.

## Using traits

### Declaring that a class has traits
To declare that one of your classes uses a trait you use the `@HasTraits` annotation:

```java
@HasTraits(traits={Resizeable.class})
public class Rectangle extends RectangleGen {
    ...
}
```

Take note that if your class uses the `@HasTraits` annotation, it MUST extend from a generated class with the suffix "Gen", e.g. `RectangleGen`. If you don't extend from the generated class with the correct name, none of the traits will be applied. "But what if I need to subclass some other thing?" you say? Simply specify your desired superclass in the `@HasTraits` annotation:

```java
@HasTraits(traits={Resizeable.class}, desiredSuperclass=Shape.class)
public class Rectangle extends RectangleGen {
    ...
}
```

The code generator will ensure that `RectangleGen` extends `Shape`. It will also generate the interface definitions implicitly defined by each trait as in the above example and ensure that the generated superclass explicitly implements all the required interfaces. For example, `RectangleGen` will look something like this:

```java
public abstract class RectangleGen extends Shape implements ResizeableInterface {
    ....
}
```

### Implement details
There are two things that you are still responsible for. One is to implement all the abstract methods declared in all the traits your class is using -- the code generator does NOT implement those for you! The other is that you must call `init()` (a method implemented by all generated superclasses) in your constructor--if you don't, exceptions will be thrown when you try to call trait methods.

A complete class definition using traits would look something like this:

```java
@HasTraits(traits={Resizeable.class}, desiredSuperclass=Shape.class)
public class Rectangle extends RectangleGen {
    private int width;
    private int height;
    public Rectangle() {
        init();
        width = 0;
        height = 0;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public void setHeight(int height) {
        this.height = height;
    }
}
```

The `Rectangle` class is guaranteed to implement the rest of `ResizeableInterface` for you as defined in the trait, and is guaranteed to still be a subclass of `Shape` -- the code generation takes care of all those details for you.

Remember, you can declare that a class has multiple traits! Just declare them in a comma-separated list, e.g. `@HasTraits(traits={Resizeable.class, Moveable.class})`

## Coming soon
Tutorial for setting up annotation processing in your IDE
