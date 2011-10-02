/*
 * Copyright (C) 2011 Christian Gawron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
