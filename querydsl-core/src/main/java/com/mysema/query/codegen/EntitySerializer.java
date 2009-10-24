/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.codegen;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.Immutable;

@Immutable
public class EntitySerializer implements Serializer{
    
    public void serialize(BeanModel model, Writer writer) throws IOException{
        // intro
        intro(model, writer);
        
        // fields
        for (PropertyModel field : model.getStringProperties()){
            stringField(field, writer);
        }
        for (PropertyModel field : model.getBooleanProperties()){
            booleanField(field, writer);
        }
        for (PropertyModel field : model.getSimpleProperties()){
            simpleField(field, writer);
        }
        for (PropertyModel field : model.getComparableProperties()){
            comparableField(field, writer);
        }
        for (PropertyModel field : model.getDateProperties()){
            dateField(field, writer);
        }
        for (PropertyModel field : model.getDateTimeProperties()){
            dateTimeField(field, writer);
        }
        for (PropertyModel field : model.getTimeProperties()){
            timeField(field, writer);
        }
        for (PropertyModel field : model.getNumericProperties()){
            numericField(field, writer);
        }
        for (PropertyModel field : model.getSimpleCollections()){
            collectionOfSimple(field, writer);
        }
        for (PropertyModel field : model.getEntityCollections()){
            collectionOfEntity(field, writer);
        }
        for (PropertyModel field : model.getSimpleMaps()){
            mapOfSimple(field, writer);
        }
        for (PropertyModel field : model.getEntityMaps()){
            mapOfEntity(field, writer);
        }
        for (PropertyModel field : model.getSimpleLists()){
            listSimple(field, writer);
        }
        for (PropertyModel field : model.getEntityLists()){
            listOfEntity(field, writer);
        }
        for (PropertyModel field : model.getEntityProperties()){
            entityField(field, writer);
        }
        
        // constructors
        constructors(model, writer);
        
        // accessors
        for (PropertyModel field : model.getSimpleLists()){
            listOfSimpleAccessor(field, writer);
        }
        for (PropertyModel field : model.getEntityLists()){
            listOfEntityAccessor(field, writer);
        }
        for (PropertyModel field : model.getSimpleMaps()){
            mapOfSimpleAccessor(field, writer);
        }
        for (PropertyModel field : model.getEntityMaps()){
            mapOfEntityAccessor(field, writer);
        }        
        
        // outro
        outro(model, writer);
    }
        
    protected void introFactoryMethods(StringBuilder builder, BeanModel model) throws IOException {
        final String localName = model.getLocalName();
        final String genericName = model.getGenericName();
        
        for (ConstructorModel c : model.getConstructors()){
            // begin
            if (!localName.equals(genericName)){
                builder.append("    @SuppressWarnings(\"unchecked\")\n");
            }            
            builder.append("    public static EConstructor<" + genericName + "> create(");
            boolean first = true;
            for (ParameterModel p : c.getParameters()){
                if (!first) builder.append(", ");
                builder.append("Expr<" + p.getTypeName() + "> " + p.getName());
                first = false;
            }
            builder.append("){\n");
            
            // body
            builder.append("        return new EConstructor<" + genericName + ">(");
            if (!localName.equals(genericName)){
                builder.append("(Class)");
            }
            builder.append(localName + ".class");
            builder.append(", new Class[]{");
            first = true;
            for (ParameterModel p : c.getParameters()){
                if (!first) builder.append(", ");
                builder.append(p.getRealTypeName() + ".class");
                first = false;
            }
            builder.append("}");
            
            for (ParameterModel p : c.getParameters()){
                builder.append(", " + p.getName());
            }
            
            // end
            builder.append(");\n");
            builder.append("    }\n\n");
        }        
    }

    protected void booleanField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PBoolean", writer, "createBoolean");
    }

    protected void collectionOfEntity(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PEntityCollection<" + field.getGenericTypeName()+">", writer, "createEntityCollection", field.getTypeName()+".class");        
    }
    
    protected void collectionOfSimple(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PComponentCollection<" + field.getGenericTypeName()+">", writer, "createSimpleCollection", field.getTypeName()+".class");        
    }
        
    protected void comparableField(PropertyModel field, Writer writer) throws IOException {        
        serialize(field, "PComparable<" + field.getGenericTypeName() + ">", writer, "createComparable", field.getTypeName() + ".class");
    }
    
    protected void constructors(BeanModel model, Writer writer) throws IOException {
        final String simpleName = model.getSimpleName();
        final String queryType = model.getPrefix() + simpleName;
        final String localName = model.getLocalName();
        final String genericName = model.getGenericName();
        
        StringBuilder builder = new StringBuilder();
        
        boolean hasEntityFields = !model.getEntityProperties().isEmpty();
        String thisOrSuper = hasEntityFields ? "this" : "super";
        
        // 1
        constructorsForVariables(builder, model);    

        // 2
        builder.append("    public " + queryType + "(PEntity<? extends "+genericName+"> entity) {\n");
        builder.append("        "+thisOrSuper+"(entity.getType(), entity.getEntityName(), entity.getMetadata()");
        if (hasEntityFields){
            builder.append(", entity.getMetadata().isRoot() ? __inits : PathInits.DEFAULT");
        }
        builder.append(");\n");
        builder.append("    }\n\n");
        
        // 3        
        builder.append("    public " + queryType + "(PathMetadata<?> metadata) {\n");
        if (hasEntityFields){
            builder.append("        this(metadata, metadata.isRoot() ? __inits : PathInits.DEFAULT);\n");    
        }else{
            builder.append("        this(metadata, PathInits.DEFAULT);\n");
        }        
        builder.append("    }\n\n");
        
        // 4
        if (!localName.equals(genericName)){
            builder.append("    @SuppressWarnings(\"unchecked\")\n");
        }        
        builder.append("    public " + queryType + "(PathMetadata<?> metadata, PathInits inits) {\n");
        builder.append("        "+thisOrSuper+"(");
        if (!localName.equals(genericName)){
            builder.append("(Class)");
        }
        builder.append(localName + ".class, \"" + simpleName + "\", metadata");
        if (hasEntityFields){
            builder.append(", inits");
        }
        builder.append(");\n");
        builder.append("    }\n\n");
        
        if (hasEntityFields){
            // 5 (with entity field initialization)
            builder.append("    public "+queryType+"(Class<? extends "+genericName+"> type, @NotEmpty String entityName, PathMetadata<?> metadata, PathInits inits) {\n");
            builder.append("        super(type, entityName, metadata);\n");
            initEntityFields(builder, model);
            builder.append("    }\n\n"); 
        }
        
        writer.append(builder.toString());
        
    }

    protected void initEntityFields(StringBuilder builder, BeanModel model) {
        BeanModel superModel = model.getSuperModel();
        if (superModel != null && !superModel.getEntityProperties().isEmpty()){
            String superQueryType = superModel.getPrefix() + superModel.getSimpleName();
            if (!superModel.getPackageName().equals(model.getPackageName())){
                superQueryType = superModel.getPackageName() + "." + superQueryType;
            }   
            builder.append("        this._super = new " + superQueryType + "(type, entityName, metadata, inits);\n");            
        }
        
        for (PropertyModel field : model.getEntityProperties()){
            builder.append("        this." + field.getEscapedName() + " = ");
            if (!field.isInherited()){
                builder.append("inits.isInitialized(\""+field.getName()+"\") ? ");
                builder.append("new " + field.getQueryTypeName() + "(PathMetadata.forProperty(this,\"" + field.getName() + "\"), inits.getInits(\""+field.getName()+"\")) : null;\n");    
            }else{
                builder.append("_super." + field.getEscapedName() +";\n");
            }
        }
    }
    
    protected void constructorsForVariables(StringBuilder builder, BeanModel model) {
        final String simpleName = model.getSimpleName();
        final String queryType = model.getPrefix() + simpleName;
        final String localName = model.getLocalName();
        final String genericName = model.getGenericName();
        
        boolean hasEntityFields = !model.getEntityProperties().isEmpty();
        String thisOrSuper = hasEntityFields ? "this" : "super";
        
        if (!localName.equals(genericName)){
            builder.append("    @SuppressWarnings(\"unchecked\")\n");
        }        
        builder.append("    public " + queryType + "(@NotEmpty String variable) {\n");
        builder.append("        "+thisOrSuper+"(");
        if (!localName.equals(genericName)){
            builder.append("(Class)");   
        }
        builder.append(localName + ".class, \""+simpleName+"\", PathMetadata.forVariable(variable)");
        if (hasEntityFields){
            builder.append(", __inits");
        }
        builder.append(");\n");
        builder.append("    }\n\n");        
    }  

    protected void dateField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PDate<" + field.getGenericTypeName() + ">", writer, "createDate", field.getTypeName()+".class");
    }

    protected void dateTimeField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PDateTime<" + field.getGenericTypeName() + ">", writer, "createDateTime", field.getTypeName()+".class");        
    }

    protected void entityField(PropertyModel field, Writer writer) throws IOException {
        final String type = field.getQueryTypeName();
        
        StringBuilder builder = new StringBuilder();
        if (field.isInherited()){
            builder.append("    // inherited\n");
        }       
        builder.append("    public final " + type + " " + field.getEscapedName() + ";\n\n");
        writer.append(builder.toString());
    }

    protected void intro(BeanModel model, Writer writer) throws IOException {        
        StringBuilder builder = new StringBuilder();        
        introPackage(builder, model);        
        introImports(builder, model);        
        introJavadoc(builder, model);        
        introClassHeader(builder, model);        
        introFactoryMethods(builder, model);   
        introInits(builder, model);
        introDefaultInstance(builder, model);   
        if (model.getSuperModel() != null){
            introSuper(builder, model);    
        }        
        writer.append(builder.toString());
    }

    protected void introInits(StringBuilder builder, BeanModel model) {
        if (!model.getEntityProperties().isEmpty()){
            List<String> inits = new ArrayList<String>();
            for (PropertyModel property : model.getEntityProperties()){
                for (String init : property.getInits()){
                    inits.add(property.getEscapedName() + "." + init);    
                }
            }                
            if (!inits.isEmpty()){
                builder.append("    private static final PathInits __inits = new PathInits(\"*\"");
                for (String init : inits){
                    builder.append(", \"" + init + "\"");    
                }    
                builder.append(");\n\n");    
            }else{
                builder.append("    private static final PathInits __inits = PathInits.DIRECT;\n\n");
            }
                
        }               
    }

    protected void introSuper(StringBuilder builder, BeanModel model) {
        BeanModel superModel = model.getSuperModel();
        String superQueryType = superModel.getPrefix() + superModel.getSimpleName();
        if (!model.getPackageName().equals(superModel.getPackageName())){
            superQueryType = superModel.getPackageName() + "." + superQueryType;
        }
        if (superModel.getEntityProperties().isEmpty()){
            builder.append("    public final "+superQueryType+" _super = new " + superQueryType + "(this);\n\n");    
        }else{
            builder.append("    public final "+superQueryType+" _super;\n\n");    
        }                  
    }

    protected void introClassHeader(StringBuilder builder, BeanModel model) {
        final String queryType = model.getPrefix() + model.getSimpleName();
        final String localName = model.getGenericName();
        
        builder.append("@SuppressWarnings(\"serial\")\n");
        builder.append("public class " + queryType + " extends PEntity<" + localName + "> {\n\n");
    }

    protected void introDefaultInstance(StringBuilder builder, BeanModel model) {
        final String simpleName = model.getSimpleName();
        final String unscapSimpleName = model.getUncapSimpleName();
        final String queryType = model.getPrefix() + simpleName;
        
        builder.append("    public static final " + queryType + " " + unscapSimpleName + " = new " + queryType + "(\"" + unscapSimpleName + "\");\n\n");
    }

    protected void introImports(StringBuilder builder, BeanModel model) {
        builder.append("import com.mysema.query.util.*;\n");
        builder.append("import com.mysema.query.types.path.*;\n");
        if (!model.getConstructors().isEmpty()){
            builder.append("import com.mysema.query.types.expr.*;\n");
        }
    }

    protected void introJavadoc(StringBuilder builder, BeanModel model) {
        final String simpleName = model.getSimpleName();
        final String queryType = model.getPrefix() + simpleName;
        
        builder.append("/**\n");
        builder.append(" * " + queryType + " is a Querydsl query type for " + simpleName + "\n");
        builder.append(" * \n");
        builder.append(" */ \n");
    }

    protected void introPackage(StringBuilder builder, BeanModel model) {
        builder.append("package " + model.getPackageName() + ";\n\n");
    }

    protected void listOfEntity(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PEntityList<" + field.getGenericTypeName()+ "," + field.getQueryTypeName() +  ">", writer, "createEntityList", 
                field.getTypeName()+".class",
                field.getQueryTypeName() +".class");        
    }

    protected void listOfEntityAccessor(PropertyModel field, Writer writer) throws IOException {
        final String escapedName = field.getEscapedName();
        final String queryType = field.getQueryTypeName();               
        
        StringBuilder builder = new StringBuilder();        
        builder.append("    public " + queryType + " " + escapedName + "(int index) {\n");
        builder.append("        return " + escapedName + ".get(index);\n");
        builder.append("    }\n\n");
        builder.append("    public " + queryType + " " + escapedName + "(com.mysema.query.types.expr.Expr<Integer> index) {\n");
        builder.append("        return " + escapedName + ".get(index);\n");
        builder.append("    }\n\n");
        writer.append(builder.toString());
    }

    protected void listOfSimpleAccessor(PropertyModel field, Writer writer) throws IOException { 
        final String escapedName = field.getEscapedName();
        final String valueType = field.getParameterName(0);
        
        StringBuilder builder = new StringBuilder();        
        builder.append("    public PSimple<" + valueType + "> " + escapedName + "(int index) {\n");
        builder.append("        return " + escapedName + ".get(index);\n");
        builder.append("    }\n\n");
        builder.append("    public PSimple<" + valueType + "> " + escapedName + "(com.mysema.query.types.expr.Expr<Integer> index) {\n");
        builder.append("        return " + escapedName + ".get(index);\n");
        builder.append("    }\n\n");
        writer.append(builder.toString());
        
    }

    protected void listSimple(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PComponentList<" + field.getTypeName()+">", writer, "createSimpleList", field.getTypeName()+".class");        
    }

    protected void mapOfEntity(PropertyModel field, Writer writer) throws IOException{
        final String keyType = field.getParameterName(0);
        final String valueType = field.getParameterName(1);
//        final String simpleName = field.getSimpleTypeName();
        final String genericKey = field.getGenericParameterName(0);
        final String genericValue = field.getGenericParameterName(1);
        
        serialize(field, "PEntityMap<"+genericKey+","+genericValue+","+field.getQueryTypeName()+">",
                writer, "createEntityMap", 
                keyType+".class", 
                valueType+".class", 
                field.getQueryTypeName()+".class");
        
    }

    protected void mapOfEntityAccessor(PropertyModel field, Writer writer) throws IOException {
        final String escapedName = field.getEscapedName();
        final String queryType = field.getQueryTypeName();
        final String keyType = field.getGenericParameterName(0);
        final String genericKey = field.getGenericParameterName(0);
        
        StringBuilder builder = new StringBuilder();        
        builder.append("    public " + queryType + " " + escapedName + "(" + keyType+ " key) {\n");
        builder.append("        return " + escapedName + ".get(key);\n");
        builder.append("    }\n\n");        
        builder.append("    public " + queryType + " " + escapedName + "(com.mysema.query.types.expr.Expr<"+genericKey+"> key) {\n");
        builder.append("        return " + escapedName + ".get(key);\n");
        builder.append("    }\n\n");
        writer.append(builder.toString());
        
    }

    protected void mapOfSimple(PropertyModel field, Writer writer) throws IOException {               
        final String keyType = field.getParameterName(0);
        final String valueType = field.getParameterName(1);
        final String genericKey = field.getGenericParameterName(0);
        final String genericValue = field.getGenericParameterName(1);
        
        serialize(field, "PComponentMap<"+genericKey+","+genericValue+">", 
            writer, "createSimpleMap", keyType+".class", valueType+".class");
        
    }
    
    protected void mapOfSimpleAccessor(PropertyModel field, Writer writer) throws IOException {
        final String escapedName = field.getEscapedName();
//        final String keyType = field.getParameterName(0);
//        final String valueType = field.getParameterName(1);
        final String genericKey = field.getGenericParameterName(0);
        final String genericValue = field.getGenericParameterName(1);
        
        StringBuilder builder = new StringBuilder();
        
        builder.append("    public PSimple<" + genericValue + "> " + escapedName + "(" + genericKey + " key) {\n");
        builder.append("        return " + escapedName + ".get(key);\n");
        builder.append("    }\n\n");
        builder.append("    public PSimple<" + genericValue + "> " + escapedName + "(com.mysema.query.types.expr.Expr<"+genericKey+"> key) {\n");
        builder.append("        return " + escapedName + ".get(key);\n");
        builder.append("    }\n\n");
        writer.append(builder.toString());
        
    }

    protected void numericField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PNumber<" + field.getGenericTypeName() + ">", writer, "createNumber", field.getTypeName() +".class");        
    }

    protected void outro(BeanModel model, Writer writer) throws IOException {
        writer.write("}\n");        
    }

    protected void serialize(PropertyModel field, String type, Writer writer, String factoryMethod, String... args) throws IOException{
        BeanModel superModel = field.getBeanModel().getSuperModel();
        // construct value
        StringBuilder value = new StringBuilder();
        if (field.isInherited() && superModel != null && superModel.getEntityProperties().isEmpty()){
            // copy from super
            value.append("_super." + field.getEscapedName());
        }else{
            value.append(factoryMethod + "(\"" + field.getName() + "\"");
            for (String arg : args){
                value.append(", " + arg);
            }        
            value.append(")");    
        }                 
        
        // serialize it
        StringBuilder builder = new StringBuilder();
        if (field.isInherited()){
            builder.append("    // inherited\n");
        }        
        builder.append("    public final " + type + " " + field.getEscapedName() + " = " + value + ";\n\n");
        writer.append(builder.toString());
    }

    protected void simpleField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PSimple<" + field.getGenericTypeName()+">", writer, "createSimple", field.getTypeName()+".class");        
    }

    protected void stringField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PString", writer, "createString");        
    }

    protected void timeField(PropertyModel field, Writer writer) throws IOException {
        serialize(field, "PTime<" + field.getGenericTypeName() + ">", writer, "createTime", field.getTypeName()+".class");        
    }

}
