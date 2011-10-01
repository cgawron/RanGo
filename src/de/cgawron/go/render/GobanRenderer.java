/*
 *
 * $Id: GobanRenderer.java 15 2003-03-15 23:25:52Z cgawron $
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

import java.awt.Graphics2D;

import de.cgawron.go.Goban;

public interface GobanRenderer
{
    /**
     * @param g the <code>Graphics2D</code> object to draw on
     * @param goban the <code>GobanModel</code> to draw
     */
    void paint(Graphics2D g, Goban goban);
}
