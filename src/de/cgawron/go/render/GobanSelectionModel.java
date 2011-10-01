/*
 *
 * $Id: GobanSelectionModel.java 15 2003-03-15 23:25:52Z cgawron $
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

import de.cgawron.go.Point;
import de.cgawron.go.render.GobanSelectionListener;
import java.util.Vector;

/**
 * Represents the set of selected points on a Goban.
 * @author Christian Gawron
 * @version $Id: GobanSelectionModel.java 15 2003-03-15 23:25:52Z cgawron $
 * @see Goban#setSelectionModel
 * @see Goban#getSelectionModel
 */
public interface GobanSelectionModel
{
    /**
     * Add a listener.
     * @param l goban.GobanSelectionListener
     */
    void addGobanSelectionListener(GobanSelectionListener l);

    /**
     * Add a point to the selection.
     * @param p goban.Point
     */
    void addSelection(Point p);

    /**
     * Get the selected points.
     * @return java.util.Vector
     */
    Vector getSelectedPoints();

    /**
     * Check if a point is selected.
     * @return boolean
     * @param p goban.Point
     */
    boolean isSelected(Point p);

    /**
     * Remove a listener.
     * @param l goban.GobanSelectionListener
     */
    void removeGobanSelectionListener(GobanSelectionListener l);

    /**
     * Set the selection to a single Point.
     * @param p goban.Point
     */
    void setSelection(Point p);
}
