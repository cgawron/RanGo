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
