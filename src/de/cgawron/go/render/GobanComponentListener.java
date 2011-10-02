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
