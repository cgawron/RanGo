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
