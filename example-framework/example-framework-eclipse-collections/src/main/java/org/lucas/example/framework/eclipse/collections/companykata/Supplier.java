/*
 * Copyright (c) 2018 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.lucas.example.framework.eclipse.collections.companykata;

import org.eclipse.collections.api.list.ImmutableList;

/**
 * Suppliers have a name and an array of itemNames.
 */
public class Supplier {
    private final String name;
    // Refactor to an ImmutableList<String>
    private final ImmutableList<String> itemNames;

    public Supplier(String name, ImmutableList<String> itemNames) {
        this.name = name;
        this.itemNames = itemNames;
    }

    public String getName() {
        return this.name;
    }

    public ImmutableList<String> getItemNames() {
        return this.itemNames;
    }
}