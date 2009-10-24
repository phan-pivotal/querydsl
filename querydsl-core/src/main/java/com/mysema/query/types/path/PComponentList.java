/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.types.path;

import com.mysema.query.types.expr.Expr;

/**
 * PComponentList represents component list paths
 * 
 * @author tiwe
 * 
 * @param <D> component type
 */
@SuppressWarnings("serial")
public class PComponentList<D> extends PComponentCollection<D> implements PList<D> {
    
    public PComponentList(Class<D> type, PathMetadata<?> metadata) {
        super(type, metadata);
    }

    @Override
    public PSimple<D> get(Expr<Integer> index) {
        return new PSimple<D>(type, PathMetadata.forListAccess(this, index));
    }

    @Override
    public PSimple<D> get(int index) {
        return new PSimple<D>(type, PathMetadata.forListAccess(this, index));
    }
}