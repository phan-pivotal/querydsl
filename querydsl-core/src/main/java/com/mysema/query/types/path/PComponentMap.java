/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.types.path;

import java.util.Map;

import com.mysema.query.types.Visitor;
import com.mysema.query.types.expr.EBoolean;
import com.mysema.query.types.expr.EMapBase;
import com.mysema.query.types.expr.Expr;
import com.mysema.query.types.operation.OBoolean;
import com.mysema.query.types.operation.Ops;
import com.mysema.query.util.NotEmpty;

/**
 * PComponentMap represents component map paths
 * 
 * @author tiwe
 * 
 * @param <K> key type
 * @param <V> value type
 */
@SuppressWarnings("serial")
public class PComponentMap<K, V> extends EMapBase<K, V> implements PMap<K, V> {    
    
    private final Class<K> keyType;
    
    private final PathMetadata<?> metadata;
    
    private final Class<V> valueType;
    
    private final Path<?> root;
    
    private volatile EBoolean isnull, isnotnull;    

    @SuppressWarnings("unchecked")
    public PComponentMap(Class<? super K> keyType, Class<? super V> valueType, PathMetadata<?> metadata) {
        super((Class)Map.class);
        this.keyType = (Class<K>)keyType;
        this.valueType = (Class<V>)valueType;
        this.metadata = metadata;
        this.root = metadata.getRoot() != null ? metadata.getRoot() : this;
    }

    public PComponentMap(Class<? super K> keyType, Class<? super V> valueType, @NotEmpty String var) {
        this(keyType, valueType, PathMetadata.forVariable(var));
    }
    
    public PComponentMap(Class<? super K> keyType, Class<? super V> valueType, Path<?> parent, @NotEmpty String property) {
        this(keyType, valueType, PathMetadata.forProperty(parent, property));
    }

    
    @Override
    public void accept(Visitor v) {
        v.visit(this);        
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        return o instanceof Path ? ((Path<?>) o).getMetadata().equals(metadata) : false;
    }

    @Override
    public PSimple<V> get(Expr<K> key) {
        return new PSimple<V>(valueType, PathMetadata.forMapAccess(this, key));
    }

    @Override
    public PSimple<V> get(K key) {
        return new PSimple<V>(valueType, PathMetadata.forMapAccess(this, key));
    }

    @Override
    public Class<K> getKeyType() {
        return keyType;
    }

    @Override
    public PathMetadata<?> getMetadata() {
        return metadata;
    }

    @Override
    public Path<?> getRoot() {
        return root;
    }

    @Override
    public Class<V> getValueType() {
        return valueType;
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public EBoolean isNotNull() {
        if (isnotnull == null) {
            isnotnull = OBoolean.create(Ops.IS_NOT_NULL, this);
        }
        return isnotnull;
    }
    
    @Override
    public EBoolean isNull() {
        if (isnull == null) {
            isnull = OBoolean.create(Ops.IS_NULL, this);
        }
        return isnull;
    }
    
    @Override
    public EMapBase<K,V> asExpr() {
        return this;
    }
    
}