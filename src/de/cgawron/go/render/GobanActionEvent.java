/*
 *
 * $Id: GobanActionEvent.java 21 2003-04-12 19:59:07Z cgawron $
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

import de.cgawron.go.Goban;
import de.cgawron.go.Point;
import java.awt.event.ActionEvent;

/**
 * Insert the type's description here. Creation date: (04/07/00 17:47:57)
 * @author Administrator
 */
public class GobanActionEvent extends ActionEvent
{
    protected Point point;
    protected Goban model;

    /**
     * GobanActionEvent constructor comment.
     * @param source java.lang.Object
     * @param id int
     * @param command java.lang.String
     * @param modifiers int
     */
    public GobanActionEvent(Object source, int id, String command, Point p, int modifiers)
    {
        super(source, id, command, modifiers);
        point = p;
    }

    /**
     * Insert the method's description here. Creation date: (04/12/00 21:21:04)
     * @return gawron.go.goban.GobanModel
     */
    public Goban getModel()
    {
        return model;
    }

    /**
     * Insert the method's description here. Creation date: (04/07/00 17:49:18)
     * @return gawron.go.Point
     */
    public Point getPoint()
    {
        return point;
    }

    /**
     * Insert the method's description here. Creation date: (04/12/00 21:21:04)
     * @param newModel gawron.go.goban.GobanModel
     */
    protected void setModel(Goban newModel)
    {
        model = newModel;
    }

    /**
     * Insert the method's description here. Creation date: (04/07/00 17:49:18)
     * @param newPoint gawron.go.Point
     */
    public void setPoint(Point newPoint)
    {
        point = newPoint;
    }
}
