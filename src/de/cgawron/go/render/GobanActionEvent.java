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
