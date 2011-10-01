/*
 *
 * $Id: GobanSelectionEvent.java 21 2003-04-12 19:59:07Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go.render;

import java.util.EventObject;
import java.util.Vector;

/**
 * Insert the type's description here. Creation date: (03/27/00 07:01:57)
 * @author Administrator
 */
public class GobanSelectionEvent extends EventObject
{
    protected Vector deselectedPoints;
    protected Vector selectedPoints;

    /** GobanSelectionEvent constructor comment. */
    public GobanSelectionEvent(GobanSelectionModel source)
    {
        super(source);
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 07:07:15)
     * @return java.util.Vector
     */
    public Vector getDeselectedPoints()
    {
        return deselectedPoints;
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 07:10:18)
     * @return java.util.Vector
     */
    public Vector getSelectedPoints()
    {
        return selectedPoints;
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 07:07:15)
     * @param newDeselectedPoints java.util.Vector
     */
    protected void setDeselectedPoints(Vector newDeselectedPoints)
    {
        deselectedPoints = newDeselectedPoints;
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 07:10:18)
     * @param newSelectedPoints java.util.Vector
     */
    protected void setSelectedPoints(Vector newSelectedPoints)
    {
        selectedPoints = newSelectedPoints;
    }

    /** valueChanged method comment. */
    public void valueChanged() { }
}
