package com.sambosley.javatraits.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaFileWriter {

    private static final String INDENT = "    ";
    
    private Writer out;
    private Map<String, List<FullyQualifiedName>> knownNames;
    
    public JavaFileWriter(Writer out) {
        if (out == null)
            throw new IllegalArgumentException("Writer must be non-null");
        this.out = out;
        this.knownNames = new HashMap<String, List<FullyQualifiedName>>();
    }
    
    public void close() throws IOException {
        out.close();
    }
    
    public void writePackage(String packageName) throws IOException {
        out.append("package ").append(packageName).append(";\n\n");
    }
    
    public void writeImports(Collection<FullyQualifiedName> imports) throws IOException {
        for (FullyQualifiedName item : imports) {
            String simpleName = item.getSimpleName();
            List<FullyQualifiedName> allNames = knownNames.get(simpleName);
            if (allNames == null) {
                allNames = new ArrayList<FullyQualifiedName>();
                knownNames.put(simpleName, allNames);
            }
            
            if (!allNames.contains(item)) {
                out.append("import ").append(item.toString()).append(";\n");
                allNames.add(item);
            }
        }
    }
}
