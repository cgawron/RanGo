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

package de.cgawron.go.render.svg;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.Iterator;

import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.StyleHandler;
import org.w3c.dom.Element;

import de.cgawron.go.Goban;
import de.cgawron.go.render.SimpleGobanRenderer;
import de.cgawron.go.sgf.MarkupModel;

/**
 * The UI delegate for a Goban component.
 * @author Christian Gawron
 */
public class SVGGobanRenderer extends SimpleGobanRenderer
{
    public SVGGobanRenderer()
    {
	super();
	scale = 10;
    }

    protected void drawBoard(Graphics2D g, Goban model)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "board");
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
	super.drawBoard(g, model);
	ctx.setStyleHandler(oldHandler);
    }

    protected void drawBackground(Graphics2D g, Goban model)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "background");
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
	super.drawBackground(g, model);
	ctx.setStyleHandler(oldHandler);
    }

    protected void drawEmpty(Graphics2D g, int x, int y)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "board");
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
	super.drawEmpty(g2, x, y);
	ctx.setStyleHandler(oldHandler);
    }

    public void drawBlackStone(Graphics2D g, short x, short y)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "black-stone");
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
        Shape c = new Ellipse2D.Double(scale*(x+drb), scale*(y+drb), scale*(1-2*drb), scale*(1-2*drb));
        g2.draw(c);
	ctx.setStyleHandler(oldHandler);
    }

    public void drawWhiteStone(Graphics2D g, short x, short y)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "white-stone");
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
        Shape c = new Ellipse2D.Double(scale*(x+drw), scale*(y+drw), scale*(1-2*drw), scale*(1-2*drw));
        g2.draw(c);
	ctx.setStyleHandler(oldHandler);
    }

    protected void drawMarkup(Graphics2D g, short x, short y, MarkupModel model)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g.create();
	SVGGeneratorContext ctx = g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "markup");

		    Iterator iter = styleMap.keySet().iterator();
		    while (iter.hasNext()) {
			String key = (String)iter.next();
			String value = (String)styleMap.get(key);
			element.setAttribute(key, value);
		    }
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
	super.drawMarkup(g2, x, y, model);
	ctx.setStyleHandler(oldHandler);
    }
    
    protected void drawString(Graphics2D g, short x, short y, String s)
    {
	SVGGraphics2D g2 = (SVGGraphics2D) g;
	SVGGeneratorContext ctx =  g2.getGeneratorContext();
	
	StyleHandler styleHandler = new StyleHandler()
	    {
		public void setStyle(Element element,
				     java.util.Map styleMap,
				     SVGGeneratorContext generatorContext)
		{
		    element.setAttribute("class", "text");
		    
		    Iterator iter = styleMap.keySet().iterator();
		    while (iter.hasNext()) {
			String key = (String)iter.next();
			String value = (String)styleMap.get(key);
			element.setAttribute(key, value);
		    }
		}		
	    };

	StyleHandler oldHandler = ctx.getStyleHandler();
	ctx.setStyleHandler(styleHandler);
	super.drawString(g2, x, y, s);
	ctx.setStyleHandler(oldHandler);
    }
    
}
