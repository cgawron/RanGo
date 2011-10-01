/*
 *
 * $Id: GobanComponentListener.java 15 2003-03-15 23:25:52Z cgawron $
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.logging.Logger;

public class GobanComponentListener implements ComponentListener
{
    private static Logger log = Logger.getLogger(GobanComponentListener.class.getName());

    /** SGFComponentListener constructor comment. */
    public GobanComponentListener()
    {
        super();
    }

    public void componentHidden(ComponentEvent e)
    {
        log.fine("Hidden " + e);
    }

    public void componentMoved(ComponentEvent e)
    {
        log.fine("Moved " + e);
    }

    public void componentResized(ComponentEvent e)
    {
        log.fine("Resized " + e);
        Component g = e.getComponent();
        Dimension os = g.getSize();
        if (os.width != os.height)
        {
            int s = os.width < os.height ? os.width : os.height;
            g.setSize(s, s);
        }
    }

    public void componentShown(ComponentEvent e)
    {
        log.fine("Shown " + e);
    }
}
