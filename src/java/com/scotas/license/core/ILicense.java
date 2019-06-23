/*
 * Copyright (c) 2012 David Stites, Patrik Dufresne and others.
 *
 * You may distribute under the terms of either the MIT License, the Apache
 * License 2.0 or the Simplified BSD License, as specified in the README file.
 *
 * Contributors:
 *     David Stites - initial API and implementation
 *     Patrik Dufresne - refactoring
 */
package com.scotas.license.core;

import java.io.Serializable;

import java.util.Hashtable;

/**
 * This interface is used to represent a license data.
 *
 * @author ikus060
 *
 */
public interface ILicense extends Serializable {

    public boolean validate(Hashtable<?, ?> properties);

}
