/*
 *
 * $Id: SimpleGobanSelectionModel.java 289 2005-08-15 09:44:31Z cgawron $
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

import java.util.Vector;

import javax.swing.event.EventListenerList;

import de.cgawron.go.Point;

/**
 * Insert the type's description here. Creation date: (03/27/00 08:47:53)
 * @author Administrator
 */
public class SimpleGobanSelectionModel implements GobanSelectionModel
{
    protected Vector<Point> selectedPoints;
    protected EventListenerList listenerList;

    /** SimpleGobanSelectionModel constructor comment. */
    public SimpleGobanSelectionModel()
    {
        super();
        listenerList = new EventListenerList();
        selectedPoints = new Vector<Point>();
    }

    /** addGobanSelectionListener method comment. */
    public void addGobanSelectionListener(GobanSelectionListener l)
    {
        listenerList.add(GobanSelectionListener.class, l);
    }

    /** addSelection method comment. */
    public void addSelection(Point p) { }

    /**
     * Insert the method's description here. Creation date: (03/27/00 09:00:55)
     * @param e goban.GobanSelectionEvent
     */
    public void fireValueChanged(GobanSelectionEvent e)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == GobanSelectionListener.class)
            {
                ((GobanSelectionListener)listeners[i + 1]).valueChanged(e);
            }
        }
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 22:37:45)
     * @return java.util.Vector
     */
    public Vector getSelectedPoints()
    {
        return selectedPoints;
    }

    /** isSelected method comment. */
    public boolean isSelected(Point p)
    {
        return selectedPoints.contains(p);
    }

    /** removeGobanSelectionListener method comment. */
    public void removeGobanSelectionListener(GobanSelectionListener l) { }

    /**
     * Insert the method's description here. Creation date: (03/27/00 22:37:45)
     * @param newSelectedPoints java.util.Vector
     */
    void setSelectedPoints(Vector<Point> newSelectedPoints)
    {
        selectedPoints = newSelectedPoints;
    }

    /** setSelection method comment. */
    public void setSelection(Point p)
    {
        GobanSelectionEvent e = new GobanSelectionEvent(this);
        e.setDeselectedPoints(selectedPoints);
        selectedPoints = new Vector<Point>();
        selectedPoints.addElement(p);
        e.setSelectedPoints(selectedPoints);
        fireValueChanged(e);
    }
}
