/*
 *
 * $Id: UndoableAction.java 15 2003-03-15 23:25:52Z cgawron $
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

import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

public abstract class UndoableAction extends AbstractAction
{
    private static String cvsId = "$Id: UndoableAction.java 15 2003-03-15 23:25:52Z cgawron $";
    private static Logger logger = Logger.getLogger(UndoableAction.class.getName());

    UndoableEditSupport undoableEditSupport = null;

    public UndoableAction()
    {
	super();
	undoableEditSupport = new UndoableEditSupport(this);
    }

    public UndoableAction(String name)
    {
	super(name);
	undoableEditSupport = new UndoableEditSupport(this);
    }

    public void addUndoableEditListener(UndoableEditListener l)
    {
	undoableEditSupport.addUndoableEditListener(l);
    }
    
    protected void postEdit(UndoableEdit e)
    {
	undoableEditSupport.postEdit(e);
    }
    
    protected void beginUpdate()
    {
	undoableEditSupport.beginUpdate();
    }
    
    protected void endUpdate()
    {
	undoableEditSupport.endUpdate();
    }
}
