/*
 *
 * $Id: EPSDiagram.java 154 2004-12-17 23:51:02Z cgawron $
 *
 * © 2001 Christian Gawron. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 */

package de.cgawron.go.render.eps;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.Value;
import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This Class creates Postscript code for drawing a GobanModel.
 * @author Christian Gawron
 * @version $Revision: 154 $
 */
public class EPSDiagram
{
    private OutputStream outputStream;
    private PrintWriter out;
    private String title = "";
    private double size = 190;
    private static Logger logger = Logger.getLogger(EPSDiagram.class.getName());

    private Value.PointList view;
    private Goban goban;
    private int modulus = 0;

    /**
     * Create a EPSDiagram with the specified output file and title.
     * @param <code>file</code> the output file
     * @param <code>title</code> the diagram title
     * @throws IOException
     */
    public EPSDiagram(File file, String title) throws IOException
    {
        this(new FileOutputStream(file), title);
    }

    /**
     * Create a EPSDiagram with the specified output stream and title.
     * @param <code>os</code> the output stream
     * @param <code>title</code> the diagram title
     * @throws IOException
     */
    public EPSDiagram(OutputStream os, String title) throws IOException
    {
	if (os instanceof BufferedOutputStream)
	    outputStream = os;
	else
	    outputStream = new BufferedOutputStream(os, 4096);
        out = new PrintWriter(outputStream);
        this.title = title;
	modulus = Integer.getInteger("de.cgawron.go.goban.modulus", 0).intValue();
    }

    private String formatMoveNo(int moveNo)
    {
	if (modulus > 0)
	    while (moveNo > modulus)
		moveNo -= modulus;
	return Integer.toString(moveNo);
    }

    private String getBoundingBox(int boardSize)
    {
        if (view != null)
        {
            StringBuffer sb = new StringBuffer();
            sb.append(10 * view.getMinX()).append(' ').append(10 * (boardSize - view.getMaxY() - 1)).append(' ');
            sb.append(10 * (view.getMaxX() + 1)).append(' ').append(10 * (boardSize - view.getMinY()));
            return sb.toString();
        }
        else
            return "0 0 190 190";
    }

    private void printHeader(int boardSize) throws IOException
    {
        char buffer[] = new char[4096];
        out.println("%!PS-Adobe-3.0 EPSF-3.0");
        out.print("%%Title: "); out.println(title);
        out.print("%%BoundingBox: "); out.println(getBoundingBox(boardSize));
        URL url = getClass().getResource("gopro.eps");
        InputStream stream = url.openStream();
        Reader in = new InputStreamReader(stream);
        int n;
        while ((n = in.read(buffer)) > 0)
        {
            out.write(buffer, 0, n);
        }
    }

    private void printView(Value.PointList view) throws IOException
    {
        if (view != null)
        {
            out.println("/llbdx " + view.getMinX() + " def");
            out.println("/llbdy " + view.getMinY() + " def");
            out.println("/urbdx " + view.getMaxX() + " def");
            out.println("/urbdy " + view.getMaxY() + " def");
        }
        else
        {
            out.println("/llbdx  0 def");
            out.println("/llbdy  0 def");
            out.println("/urbdy 18 def");
            out.println("/urbdx 18 def");
        }
    }

    private void printStone(Point pt, BoardType p)
    {
        StringBuffer b = new StringBuffer(8);
        Point.append(b, pt);
        if (p == BoardType.WHITE)
            b.append(" ws");
        else if (p == BoardType.BLACK)
            b.append(" bs");
        out.println(b.toString());
    }

    private void printStone(Point pt, int moveNo, BoardType p)
    {
        StringBuffer b = new StringBuffer(8);
        if (p == BoardType.WHITE)
            b.append("0 ");
        else if (p == BoardType.BLACK)
            b.append("1 ");
        Point.append(b, pt).append(" (").append(formatMoveNo(moveNo)).append(") metamark");
        out.println(b.toString());
    }

    private void printStone(Point pt, MarkupModel.Markup m, BoardType p)
    {
        logger.fine("Markup: " + m);
        if (m instanceof MarkupModel.Text)
        {
            logger.fine("Label " + m);
            StringBuffer b = new StringBuffer(8);
            if (p == BoardType.WHITE)
            {
                b.append("0 ");
                Point.append(b, pt).append(" (").append(m.toString()).append(") metamark");
            }
            else if (p == BoardType.BLACK)
            {
                b.append("1 ");
                Point.append(b, pt).append(" (").append(m.toString()).append(") metamark");
            }
            else if (p == BoardType.EMPTY)
            {
                Point.append(b.append(" (").append(m.toString()).append(") "), pt).append(" fs");
            }
            out.println(b.toString());
        }
	else if (m instanceof MarkupModel.Triangle) {
            StringBuffer b = new StringBuffer(8);
	    if (p == BoardType.WHITE) {
		b.append("0 ");
		Point.append(b, pt).append(" triang_e");
            }
	    else if (p == BoardType.BLACK) {
		b.append("1 ");
		Point.append(b, pt).append(" triang_e");
            }
	    else {
		Point.append(b, pt).append(" triang_ee");
            }
            out.println(b.toString());
	}

    }


    public void setView(Value.PointList view)
    {
        this.view = view;
        logger.fine("View: " + view.getMinX() + ", " + view.getMaxX() + ", " + view.getMinY() + ", " + view.getMaxY());
    }

    /** @param goban the goban to create the diagram from */
    public void print(Goban goban) throws IOException
    {
        try
        {
            this.goban = goban;
            printHeader(goban.getBoardSize());
            out.println("Liberty begin");

            // print options
            out.println("/cache false def");
            out.println("/axis false def");

            // print size
            out.print(goban.getBoardSize());
            out.print(' ');
            out.print(size);
            out.println(" boardinit");

            printView(view);

            out.println("board");
            short x;
            short y;
            Point pt;
            BoardType p;
            MarkupModel.Markup m = null;
            for (x = 0; x < goban.getBoardSize(); x++)
                for (y = 0; y < goban.getBoardSize(); y++)
                {
                    pt = new Point(x, y);
                    p = goban.getStone(x, y);
                    if (goban instanceof MarkupModel)
                        m = ((MarkupModel)goban).getMarkup(x, y);
                    if (m instanceof MarkupModel.Move)
                    {
                        MarkupModel.Move move = (MarkupModel.Move) m;
                        printStone(pt, move.getMoveNo(), move.getColor());
                    }
                    else if (m instanceof MarkupModel.Stone)
                    {
                        MarkupModel.Stone stone = (MarkupModel.Stone) m;
                        printStone(pt, stone.getColor());
                    }
                    else if (m != null)
                        printStone(pt, m, p);
                    else if (p != BoardType.EMPTY)
                        printStone(pt, p);
                }

            out.println("end");
            out.println("%%Trailer");
            out.flush();
            out.close();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "exception in EPSDiagram.print", ex);
            throw new RuntimeException(ex);
        }
    }
}
