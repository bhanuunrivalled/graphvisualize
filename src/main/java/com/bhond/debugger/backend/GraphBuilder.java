package com.bhond.debugger.backend;

import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GraphBuilder {

    private final StringBuilder out = new StringBuilder();
    //TODO later create separate strategy to extract data  from JDI value
    //private final ExtractionStrategy<Value> strategy = new JdiValueStrategy();
    private final IdentityHashMap<Object, String> objectsRefId = new IdentityHashMap<>();
    private Direction direction = Direction.TB;
    private final List<FieldAttributesProvider> fieldAttributesProviders = new ArrayList<>();
    private final List<ArrayElementAttributeProvider> arrayElementAttributeProviders = new ArrayList<>();
    private final List<ObjectAttributesProvider> objectAttributesProviders = new ArrayList<>();


    public String generateDOT(Value obj) {
        out.append("digraph Java {\n")
                .append("\trankdir=\"")
                .append(direction)
                .append("\";\n")
                .append("\tnode[shape=plaintext]\n");
        generateDotInternal(obj);
        return out
                .append("}\n")
                .toString();
    }


    private void generateDotInternal(Value value) {
        if (value == null)
            out.append("\t")
                    .append(dotNameRef(null))
                    .append("[label=\"null\"")
                    .append(", shape=plaintext];\n");
            // returns the ref ID
        else if (!objectsRefId.containsKey(value)) {
            if (value instanceof ArrayReference) {
                if (looksLikePrimitiveArray((ArrayReference) value)) {
                    processPrimitiveArray((ArrayReference) value);
                } else {
                    processObjectArray((ArrayReference) value);
                }
            }
            else if(value instanceof PrimitiveValue){
                System.out.println(" TODO ");
            }
            else if (value instanceof ObjectReference) {
                ObjectReference ref = (ObjectReference) value;
                final ReferenceType refType = ref.referenceType();
                // default ObjectReference processing
                List<Field> fs = refType.allFields();
                System.out.println(fs);
                if (hasPrimitiveFields(fs, ref)) {
                     labelObjectWithSomePrimitiveFields(ref, fs);
                } else {
                    labelObjectWithNoPrimitiveFields(ref);
                }
                processFields(ref, fs);
            }
        }
    }

    private void labelObjectWithSomePrimitiveFields(ObjectReference objRef, List<Field> fs) {
        out.append("\t")
                .append(dotNameRef(objRef))
                .append("[label=<\n")
                .append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n")
                .append("\t\t\t<tr>\n\t\t\t\t<td rowspan='")
                .append(getFieldSize(objRef, fs) + 1)
                .append("'>")
                .append(className(objRef, false))
                .append("</td>\n\t\t\t</tr>\n");
        //TODO whats this cabs?
        String cabs = getObjectAttributes(objRef);
        for (Field field : fs) {
            // we ignore static I dont know why :)
            if(field.isStatic()){
                continue;
            }
            Value ref = objRef.getValue(field);
            if (fieldExistsAndIsPrimitive(field) || IsPrimitive(objRef)) {
                out.append("\t\t\t<tr>\n\t\t\t\t<td>");
                    out.append(field.name()).append(": ").append(Quote.quote(String.valueOf(ref)));
                out.append("</td>\n\t\t\t</tr>\n");
            }
        }
        out.append("\t\t</table>\n\t>")
                .append(cabs.isEmpty() ? "" : "," + cabs)
                .append("];\n");
    }

    private int getFieldSize(ObjectReference objRef, List<Field> fs) {
        int size = 0;
        for (Field field : fs) {
            Value ref = objRef.getValue(field);
            if (fieldExistsAndIsPrimitive(field) || IsPrimitive(objRef))
                size++;
        }
        return size;
    }

    private void processPrimitiveArray(ArrayReference ao) {
        Objects.requireNonNull(ao, "array Reference is Null ");
        out.append("\t")
                .append(dotNameRef(ao))
                .append("[label=<\n")
                .append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n")
                .append("\t\t\t<tr>\n");
        for (int i = 0, len = ao.length(); i < len; i++) {
            out.append("\t\t\t\t<td")
                    .append(getArrayElementAttributes(ao, i))
                    .append(">")
                    .append(Quote.quote(String.valueOf(ao.getValue(i))))
                    .append("</td>\n");
        }
        out.append("\t\t\t</tr>\n\t\t</table>\n\t>];\n");
    }

    private void processObjectArray(ObjectReference obj) {
        out.append("\t")
                .append(dotNameRef(obj))
                .append("[label=<\n")
                .append("\t\t<table border='0' cellborder='1' cellspacing='0' cellpadding='9'>\n")
                .append("\t\t\t<tr>\n");
        ArrayReference ao = (ArrayReference) obj;
        Objects.requireNonNull(ao,"The array reference passed is null");
        int len = ao.length();
        for (int i = 0; i < len; i++) {
            out.append("\t\t\t\t<td port=\"f")
                    .append(i)
                    .append("\"")
                    .append(getArrayElementAttributes(obj, i))
                    .append("></td>\n");
        }
        out.append("\t\t\t</tr>\n\t\t</table>\n\t>];\n");
        for (int i = 0; i < len; i++) {
            Value ref = ao.getValue(i);
            if (ref == null)
                continue;
            generateDotInternal(ref);
            out.append("\t")
                    .append(dotNameRef(obj))
                    .append(":f")
                    .append(i)
                    .append(" -> ")
                    .append(MiniTracer.isPrimitive(ref)?Quote.quote(String.valueOf(ao.getValue(i))):dotNameRef((ObjectReference) ref)) // if its not primitive then some reference
                    .append("[label=\"")
                    .append(i)
                    .append("\",fontsize=12];\n");
        }
    }


    private String dotNameRef(ObjectReference obj) {
        return obj == null ? "NULL" : objectsRefId.computeIfAbsent(obj, s -> "n" + (objectsRefId.size() + 1));
    }

    /**
     * if all the references are primitive then return True
     *
     * @param obj
     * @return
     */
    public boolean looksLikePrimitiveArray(ObjectReference obj) {
        ArrayReference ao = (ArrayReference) obj;
        for (int i = 0; i < ao.length(); i++) {
            if (!MiniTracer.isPrimitive(ao.getValue(i))) {
                return false;
            }
        }
        return true;
    }


    private void labelObjectWithNoPrimitiveFields(ObjectReference obj) {
        String cabs = getObjectAttributes(obj);
        out.append("\t")
                .append(dotNameRef(obj))
                .append("[label=<\n")
                .append("\t\t<table border='0' cellborder='1' cellspacing='0'>\n")
                .append("\t\t\t<tr>\n\t\t\t\t<td>")
                .append(className(obj, false))
                .append("</td>\n\t\t\t</tr>\n\t\t</table>\n\t>")
                .append(cabs.isEmpty() ? "" : "," + cabs)
                .append("];\n");
    }

    public String getObjectAttributes(ObjectReference o) {
        return objectAttributesProviders.stream()
                .map(p -> p.getAttribute(o))
                .filter(s -> !(s == null || s.isEmpty()))
                .collect(Collectors.joining(","));
    }


    private void processFields(ObjectReference objRef, List<com.sun.jdi.Field> fs) {
        for (Field field : fs) {
            if(!field.isStatic()){
                try {
                    Value ref = objRef.getValue(field);
                    if (field.type() instanceof PrimitiveType)
                        //- The field might be declared, say, Object, but the actual
                        //- object may be, say, a String.
                        continue;
                    String name = field.name();
                    String fabs = "";  //ljv.getFieldAttributes(field, ref); what is this fix later
                    generateDotInternal((ObjectReference) ref);
                    out.append("\t")
                            .append(dotNameRef(objRef))
                            .append(" -> ")
                            .append(dotNameRef((ObjectReference) ref))
                            .append("[label=\"")
                            .append(name)
                            .append("\",fontsize=12")
                            .append(fabs.isEmpty() ? "" : "," + fabs)
                            .append("];\n");
                } catch (ClassNotLoadedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * why do we need this? and how do we use this??
     *
     * @param array
     * @param index
     * @return
     */
    public String getArrayElementAttributes(ObjectReference array, int index) {
        String result = arrayElementAttributeProviders.stream()
                .map(p -> p.getAttribute(array, index))
                .filter(s -> !(s == null || s.isEmpty()))
                .collect(Collectors.joining(" "));
        if (!result.isEmpty()) {
            return " " + result;
        } else {
            return "";
        }
    }

    public boolean hasPrimitiveFields(List<Field> fs, ObjectReference obj) {
        for (Field f : fs)
            if (fieldExistsAndIsPrimitive(f))
                return true;
        return false;
    }

    private boolean fieldExistsAndIsPrimitive(Field field) {
        try {
            //- The order of these statements matters.  If field is not
            //- accessible, we want an IllegalAccessException to be raised
            //- (and caught).  It is not correct to return true if
            //- field.getType( ).isPrimitive( )
            Type type = field.type();
            if (type instanceof PrimitiveType)
                //- Just calling ljv.canTreatAsPrimitive is not adequate --
                //- val will be wrapped as a Boolean or Character, etc. if we
                //- are dealing with a truly primitive type.
                return true;
        } catch (ClassNotLoadedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean IsPrimitive(ObjectReference obj) {
        Type type = obj.type();
        return type instanceof PrimitiveType;
    }


    /**
     * @param objectReference
     * @param something       FIXME bad name
     * @return
     */
    public String className(ObjectReference objectReference, boolean something) {
        if (objectReference == null)
            return "";
        return objectReference.type()
                .name();
    }

}
