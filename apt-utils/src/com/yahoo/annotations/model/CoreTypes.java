package com.yahoo.annotations.model;

/**
 * Defines lots of constant {@link com.yahoo.annotations.model.DeclaredTypeName}s for core java types
 * (e.g. primitives, primitive boxes, String, etc.)
 *
 * If you want a type representing an array of one of these or to modify them in some other way, be
 * sure to call clone() first.
 */
public class CoreTypes {

    public static final String JAVA_LANG = "java.lang";

    public static final DeclaredTypeName JAVA_OBJECT = new DeclaredTypeName(JAVA_LANG, "Object");
    public static final DeclaredTypeName JAVA_STRING = new DeclaredTypeName(JAVA_LANG, "String");
    public static final DeclaredTypeName JAVA_INTEGER = new DeclaredTypeName(JAVA_LANG, "Integer");
    public static final DeclaredTypeName JAVA_LONG = new DeclaredTypeName(JAVA_LANG, "Long");
    public static final DeclaredTypeName JAVA_DOUBLE = new DeclaredTypeName(JAVA_LANG, "Double");
    public static final DeclaredTypeName JAVA_FLOAT = new DeclaredTypeName(JAVA_LANG, "Float");
    public static final DeclaredTypeName JAVA_SHORT = new DeclaredTypeName(JAVA_LANG, "Short");
    public static final DeclaredTypeName JAVA_BYTE = new DeclaredTypeName(JAVA_LANG, "Byte");
    public static final DeclaredTypeName JAVA_BOOLEAN = new DeclaredTypeName(JAVA_LANG, "Boolean");
    public static final DeclaredTypeName JAVA_CHARACTER = new DeclaredTypeName(JAVA_LANG, "Character");
    
    public static final DeclaredTypeName PRIMITIVE_INT = new DeclaredTypeName("", "int");
    public static final DeclaredTypeName PRIMITIVE_LONG = new DeclaredTypeName("", "long");
    public static final DeclaredTypeName PRIMITIVE_DOUBLE = new DeclaredTypeName("", "double");
    public static final DeclaredTypeName PRIMITIVE_FLOAT = new DeclaredTypeName("", "float");
    public static final DeclaredTypeName PRIMITIVE_SHORT = new DeclaredTypeName("", "short");
    public static final DeclaredTypeName PRIMITIVE_BYTE = new DeclaredTypeName("", "byte");
    public static final DeclaredTypeName PRIMITIVE_BOOLEAN = new DeclaredTypeName("", "boolean");
    public static final DeclaredTypeName PRIMITIVE_CHAR = new DeclaredTypeName("", "char");
    
    public static final DeclaredTypeName OVERRIDE = new DeclaredTypeName(JAVA_LANG, "Override");
    public static final DeclaredTypeName DEPRECATED = new DeclaredTypeName(JAVA_LANG, "Deprecated");
}
