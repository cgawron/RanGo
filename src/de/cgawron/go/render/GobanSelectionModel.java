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
