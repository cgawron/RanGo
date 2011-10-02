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
import de.cgawron.go.GobanEvent;
import de.cgawron.go.Point;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.Region;
import de.cgawron.go.sgf.Value.PointList;

import java.awt.*;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.*;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ComponentUI;

/**
 * The UI delegate for a Goban component.
 * @author Christian Gawron
 */
public class GobanUI extends ComponentUI implements PropertyChangeListener, DragGestureListener
{
    protected Image[] whiteStoneImages;
    protected Image wsi;
    protected Image gobanBackground;
    protected Image bsi;
    protected ActionListener actionListener = null;

    private static Logger logger = Logger.getLogger(JGoban.class.getName());
    private AffineTransform transformToDevice = new AffineTransform();

    protected JGoban goban = null;
    protected Goban model = null;
    protected GobanSelectionModel selectionModel = null;
    protected Region region = null;
    protected GobanRenderer renderer = null;

    private static class SimpleRegion extends de.cgawron.go.sgf.SimpleRegion
    {

		public SimpleRegion(PointList pointList) {
			super(pointList);
		}

		public SimpleRegion(java.awt.geom.Rectangle2D.Double frame) {
			super((short) frame.x, (short) frame.y, (short) (frame.x + frame.width), (short) (frame.y + frame.height));
		}

		public static Shape getShape(Region region) {
			// TODO Auto-generated method stub
			return null;
		}
    	
    }
    
    private RegionSelecter regionSelecter;

    private class ToolTipHandler extends MouseMotionAdapter
    {
        public void mouseMoved(MouseEvent e)
        {
            Point2D q = new Point2D.Float();
            toBoard(e.getPoint(), q);
            goban.setToolTipText((char)('A' + (q.getX() > 9 ? q.getX() + 1 : q.getX())) + "" + ((int)(q.getY() + 1)));
        }
    }


    private class RegionSelecter extends MouseInputAdapter
    {
        private Graphics2D graphics;
        private Cursor savedCursor;
        private Point2D.Float p1, p2;
        private Rectangle2D.Double frame;
        private boolean ready = false;
        private boolean armed = false;

        class Queue extends EventQueue
        {
            void waitUntilReady()
            {
                while (!ready)
                {
                    try
                    {
                        AWTEvent event = getNextEvent();
                        logger.info("queue: " + event);
                        dispatchEvent(event);
                    }
                    catch (InterruptedException ex)
                    {
                    }
                }
                pop();
            }
        }


        private Region getRegion()
        {
            goban.addMouseMotionListener(this);
            goban.addMouseListener(this);

            savedCursor = goban.getCursor();
            goban.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

            logger.info("Creating queue");
            Queue queue = new Queue();
            goban.getToolkit().getSystemEventQueue().push(queue);
            queue.waitUntilReady();

            goban.removeMouseMotionListener(this);
            goban.removeMouseListener(this);
            return new SimpleRegion(frame);
        }

        private synchronized void dragFrame()
        {
            if (frame != null)
                graphics.draw(frame);
            else
                frame = new Rectangle2D.Double();

            double minX = Math.min(p1.getX(), p2.getX());
            double maxX = Math.max(p1.getX(), p2.getX());
            double minY = Math.min(p1.getY(), p2.getY());
            double maxY = Math.max(p1.getY(), p2.getY());
            logger.info("drag: " + minX + ", " + minY + ", " + maxX + ", " + maxY);
            frame.setRect(minX, minY, maxX - minX, maxY - minY);
            graphics.draw(frame);
        }

        public synchronized void mousePressed(MouseEvent e)
        {
            if (!armed)
            {
                armed = true;
                goban.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
                graphics = scaleGraphics(goban.getGraphics(), goban);
                graphics.setXORMode(Color.white);

                p1 = new Point2D.Float();
                p2 = new Point2D.Float();
                toBoard(e.getPoint(), p1);
                toBoard(e.getPoint(), p2);
                normalizePoint(p1);
                normalizePoint(p2);
                p1.setLocation(Math.rint(p1.getX()), Math.rint(p1.getY()));
                p2.setLocation(Math.rint(p2.getX()), Math.rint(p2.getY()));
                dragFrame();
            }
            else
            {
                ready = true;
                goban.setCursor(savedCursor);
                goban.getToolkit().getSystemEventQueue().postEvent(new ActionEvent(this, AWTEvent.RESERVED_ID_MAX, ""));
            }
        }

        public synchronized void mouseReleased(MouseEvent e)
        {
            if (armed)
            {
                ready = true;
                goban.setCursor(savedCursor);
                goban.getToolkit().getSystemEventQueue().postEvent(new ActionEvent(this, AWTEvent.RESERVED_ID_MAX, ""));
            }
        }

        public synchronized void mouseDragged(MouseEvent e)
        {
            if (armed)
            {
                toBoard(e.getPoint(), p2);
                normalizePoint(p2);
                p2.setLocation(Math.rint(p2.getX()), Math.rint(p2.getY()));
                logger.fine("getRegion: " + p2);
                dragFrame();
            }
        }
    }


    public Region selectRegion()
    {
        regionSelecter = new RegionSelecter();
        Region r = regionSelecter.getRegion();
	regionSelecter = null;
	return r;
    }

    private class RegionResizer extends MouseInputAdapter
    {
        final static int EAST = 1;
        final static int WEST = 2;
        final static int NORTH = 4;
        final static int SOUTH = 8;

        private boolean inside = false;
        private boolean armed = false;
        private boolean active = false;

        private double minX, maxX, minY, maxY;
        private Rectangle2D.Double frame;
        private int dragMode;
        private Graphics2D graphics;
        private Cursor savedCursor;

        RegionResizer()
        {
        }

        void setRegion(Region region)
        {
            if (region != null)
            {
                RectangularShape bounds = SimpleRegion.getShape(region).getBounds();
                minX = bounds.getMinX();
                maxX = bounds.getMaxX();
                minY = bounds.getMinY();
                maxY = bounds.getMaxY();
                goban.addMouseMotionListener(regionResizer);
                goban.addMouseListener(regionResizer);
            }
            else
            {
                goban.removeMouseListener(regionResizer);
                goban.removeMouseMotionListener(regionResizer);
            }
        }

        private void dragFrame(Point2D p)
        {
            normalizePoint(p);
            if (frame != null)
                graphics.draw(frame);
            else
                frame = new Rectangle2D.Double();
            if ((dragMode & EAST) != 0)
                maxX = Math.round(p.getX());
            if ((dragMode & WEST) != 0)
                minX = Math.round(p.getX());
            if ((dragMode & NORTH) != 0)
                minY = Math.round(p.getY());
            if ((dragMode & SOUTH) != 0)
                maxY = Math.round(p.getY());
            frame.setRect(minX, minY, maxX - minX, maxY - minY);
            graphics.draw(frame);
        }

        private int getDragMode(Point2D p)
        {
            int mode = 0;
            logger.fine("getDragMode: " + p + ", " + minX + ", " + maxX + ", " + minY + ", " + maxY);
            if (Math.abs(p.getX() - minX) < 0.1)
                mode |= WEST;
            if (Math.abs(p.getX() - maxX) < 0.1)
                mode |= EAST;
            if (Math.abs(p.getY() - minY) < 0.1)
                mode |= NORTH;
            if (Math.abs(p.getY() - maxY) < 0.1)
                mode |= SOUTH;
            return mode;
        }

        private boolean checkRegionHit(Point2D p)
        {
            assert getRegion() != null;
            boolean contained = SimpleRegion.getShape(getRegion()).contains(p);
            if (contained != inside)
            {
                inside = !inside;
                logger.info("Region hit");
                return true;
            }
            return false;
        }

        private Cursor getResizeCursor()
        {
            Cursor cursor = null;
            switch (dragMode)
            {
                case NORTH:
                    cursor = new Cursor(Cursor.N_RESIZE_CURSOR);
                    break;
                case NORTH | EAST:
                    cursor = new Cursor(Cursor.NE_RESIZE_CURSOR);
                    break;
                case EAST:
                    cursor = new Cursor(Cursor.E_RESIZE_CURSOR);
                    break;
                case SOUTH | EAST:
                    cursor = new Cursor(Cursor.SE_RESIZE_CURSOR);
                    break;
                case SOUTH:
                    cursor = new Cursor(Cursor.S_RESIZE_CURSOR);
                    break;
                case SOUTH | WEST:
                    cursor = new Cursor(Cursor.SW_RESIZE_CURSOR);
                    break;
                case WEST:
                    cursor = new Cursor(Cursor.W_RESIZE_CURSOR);
                    break;
                case NORTH | WEST:
                    cursor = new Cursor(Cursor.NW_RESIZE_CURSOR);
                    break;

                default:
                    logger.warning("Illegal dragMode: " + dragMode);
                    assert false;
            }
            return cursor;
        }

        public void mouseMoved(MouseEvent e)
        {
            Point2D q = new Point2D.Float();
            toBoard(e.getPoint(), q);
            if (!active)
            {
                //if (checkRegionHit(q)) {
                if ((dragMode = getDragMode(q)) != 0)
                {
                    if (!armed)
                    {
                        savedCursor = goban.getCursor();
                    }
                    armed = true;
                    goban.setCursor(getResizeCursor());
                    logger.info("RegionResizer: armed");
                }
                else
                {
                    armed = false;
                    goban.setCursor(savedCursor);
                }
            }
        }

        public void mouseDragged(MouseEvent e)
        {
            if (active)
            {
                Point2D q = new Point2D.Float();
                toBoard(e.getPoint(), q);
                logger.fine("Resizing: " + q);
                dragFrame(q);
            }
        }

        public void mousePressed(MouseEvent e)
        {
            if (armed)
            {
                Point2D q = new Point2D.Float();
                toBoard(e.getPoint(), q);
                active = true;
                armed = false;
                graphics = scaleGraphics(goban.getGraphics(), goban);
                graphics.setXORMode(Color.white);
                dragFrame(q);
                logger.fine("RegionResizer: activated");
            }
        }

        public void mouseReleased(MouseEvent e)
        {
            if (active)
            {
                logger.info("RegionResizer: resized");
                if (frame != null)
                    graphics.draw(frame);
                frame = null;
                graphics.dispose();
                region.set((short)minX, (short)minY, (short)maxX, (short)maxY);
                goban.setCursor(savedCursor);
                goban.repaint();
            }
            active = false;
            armed = false;
            logger.info("RegionResizer: deactivated");
        }

    }


    private RegionResizer regionResizer = new RegionResizer();

    private GobanUI()
    {
        renderer = new SimpleGobanRenderer();
    }

    /**
     * Creates a new instance of <code>GobanUI</code>
     * @return the created instance
     */
    public static GobanUI createUI()
    {
        return new GobanUI();
    }

    /** Install this UI on a Goban. */
    public void installUI(JComponent c)
    {
        this.goban = (JGoban) c;
        //addComponentListener(new GobanComponentListener());
        goban.addPropertyChangeListener(this);
        goban.setLayout(null);
        goban.setBackground(new Color(209, 181, 135));
        goban.setPreferredSize(new Dimension(160, 160));
        goban.setCursor(new Cursor(Cursor.HAND_CURSOR));
	//goban.addMouseMotionListener(new ToolTipHandler());
	ToolTipManager.sharedInstance().registerComponent(goban);
	setModel(goban.getModel());
        initImages(goban);
    }

    public void setRenderer(GobanRenderer renderer)
    {
        this.renderer = renderer;
    }

    public void dragGestureRecognized(DragGestureEvent dge)
    {
	if (regionSelecter == null)
	    dge.startDrag(DragSource.DefaultCopyDrop, ((JGoban) dge.getComponent()).getTransferable());
    }

    /**
     * Convert a point on the component to the logical position on a Goban.
     * @param q A <code>Point2D</code> value to be converted
     * @param p A <code>Point2D</code> value where the result is stored
     */
    public void toBoard(Point2D q, Point2D p)
    {
        try
        {
            transformToDevice.inverseTransform(q, p);
        }
        catch (NoninvertibleTransformException ex)
        {
            logger.warning("toBoard: singular matrix: " + ex);
            assert false;
        }
        logger.fine("toBoard: " + q + "->" + p);
    }

    /**
     * Convert a point on the component to the logical position on a Goban
     * @return de.cgawron.go.Point
     * @param p java.awt.geom.Point2D
     */
    public final Point toBoard(Point2D q)
    {
        Point2D p = new Point2D.Float();
        try
        {
            transformToDevice.inverseTransform(q, p);
        }
        catch (NoninvertibleTransformException ex)
        {
            logger.warning("toBoard: singular matrix: " + ex);
            assert false;
        }
        logger.fine("toBoard: " + q + "->" + p);
        return new Point((int) p.getX(), (int) p.getY());
    }

    private void normalizePoint(Point2D p)
    {
        if (p.getX() < 0)
            p.setLocation(0, p.getY());
        else if (p.getX() > model.getBoardSize())
            p.setLocation(model.getBoardSize(), p.getY());
        if (p.getY() < 0)
            p.setLocation(p.getX(), 0);
        else if (p.getY() > model.getBoardSize())
            p.setLocation(p.getX(), model.getBoardSize());
    }

    private Graphics2D scaleGraphics(Graphics graphics, JGoban goban)
    {
        Graphics2D g = (Graphics2D)graphics;
        if (g == null)
            return null;

        assert goban != null;
        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.addRenderingHints(hints);

        Dimension size = goban.getSize();
        double min = Math.min(size.getWidth(), size.getHeight());

        assert transformToDevice != null;
        transformToDevice.setToScale(min / goban.getBoardSize(), min / goban.getBoardSize());
        g.scale(min / goban.getBoardSize(), min / goban.getBoardSize());

        logger.info("scaleGraphics: " + min + ", " + goban.getBoardSize());
        g.setStroke(new BasicStroke(0.02f));

        return g;
    }

    public void addActionListener(ActionListener l)
    {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    /** Insert the method's description here. Creation date: (03/27/00 23:44:27) */
    protected void doHighlights(Graphics g)
    {
        Point p = null;
        Enumeration enu = goban.getSelectionModel().getSelectedPoints().elements();
        while (enu.hasMoreElements())
        {
            p = (Point)enu.nextElement();
            highlight(g, p);
            logger.fine("Selected: " + p);
        }
    }

    /**
     * Insert the method's description here. Creation date: (03/30/00 15:05:45)
     * @param g java.awt.Graphics
     */
    protected void drawBackground(Graphics2D g, JGoban goban)
    {
        short i;
        short j;
        Point p, q;
        int boardSize = goban.getBoardSize();
        int max = boardSize - 1;

        g.setColor(goban.getBackground());
        g.fillRect(0, 0, max + 1, max + 1);
        g.setColor(goban.getForeground());

        Line2D.Double line1 = new Line2D.Double();
        Line2D.Double line2 = new Line2D.Double();
        for (i = 0; i < boardSize; i++)
        {
            line1.setLine(i + 0.5, 0.5, i + 0.5, max + 0.5);
            line2.setLine(0.5, i + 0.5, max + 0.5, i + 0.5);
            g.draw(line1);
            g.draw(line2);
        }

        BasicStroke stroke = (BasicStroke)g.getStroke();
        Shape boundary = new Rectangle2D.Double(0.5, 0.5, max, max);
        g.setStroke(new BasicStroke(2 * stroke.getLineWidth()));
        g.draw(boundary);
        g.setStroke(stroke);

        if (boardSize > 9)
        {
            //short r = (short) (1+gridWidth/8);
            double r = 0.1;
            short m = (short)(boardSize / 2);
            for (i = 3; i < boardSize; i += m - 3)
                for (j = 3; j < boardSize; j += m - 3)
                {
                    Point h = new Point(i, j);
                    Shape c = new Ellipse2D.Double(i + 0.5 - r, j + 0.5 - r, 2 * r, 2 * r);
                    g.draw(c);
                    g.fill(c);
                }
        }
    }

    /*
    protected void drawString(Graphics2D graphics, short x, short y, String s)
    {
        Graphics2D g = (Graphics2D)graphics.create();
	Font font;
	try {
	    logger.info("Loading font GillSans");
	    font = Font.createFont(Font.TRUETYPE_FONT, this.getClass().getResourceAsStream("gillsans.ttf")).deriveFont(0.5f);
	}
	catch (Exception ex)
	{
	    logger.error("Could not load font: " + ex);
	    logger.info("Using font SanSerif");
	    font = new Font("SansSerif", Font.PLAIN, 1).deriveFont(0.5f);
	}
        g.setFont(font);
        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(s, frc);
        LineMetrics metrics = font.getLineMetrics(s, frc);
        float width = (float)bounds.getWidth();
        float height = (float)bounds.getHeight();
        float lineHeight = metrics.getHeight();
        float ascent = metrics.getAscent();
        logger.fine("width: " + width + ", height: " + height + ", ascent: " + ascent);
        float x0 = (float)(x + 0.5 - width / 2);
        //float y0 = (float) (y + 0.5  + (height/2) - ascent/2);
        float y0 = (float)(y + 0.5 + 0.2);
        g.drawString(s, x0, y0);
    }
    */

    /*
    protected void drawMarkup(Graphics2D g, short x, short y, MarkupModel model)
    {
        MarkupModel.Markup m = model.getMarkup(x, y);
        if (m instanceof MarkupModel.Move)
        {
            MarkupModel.Move move = (MarkupModel.Move) m;
            logger.fine("Move at " + x + ", " + y);
            if (move.getColor() == BoardType.Black)
            {
                drawBlackStone(g, x, y);
                g.setColor(Color.white);
            }
            else
            {
                drawWhiteStone(g, x, y);
                g.setColor(Color.black);
            }

            String text = Integer.toString(move.getMoveNo());
            drawString(g, x, y, text);
        }
        if (m instanceof MarkupModel.Text)
        {
            MarkupModel.Text text = (MarkupModel.Text) m;
            BoardType color = model.getStone(x, y);
            Color bg;
            Color fg;
            logger.fine("Move at " + x + ", " + y);
            drawStone(g, x, y, color);
            if (color == BoardType.Black)
            {
                bg = Color.black;
                fg = Color.white;
            }
            else if (color == BoardType.White)
            {
                bg = Color.white;
                fg = Color.black;
            }
            else
            {
                fg = Color.black;
                bg = goban.getBackground();
            }
            g.setColor(bg);
            double r = 0.1;
            Shape s = new Ellipse2D.Double(x + r, y + r, 1 - 2 * r, 1 - 2 * r);
            g.fill(s);
            g.setColor(fg);
            drawString(g, x, y, text.toString());
        }
        else if (m instanceof MarkupModel.Stone)
        {
            MarkupModel.Stone stone = (MarkupModel.Stone) m;
            logger.fine("Stone at " + x + ", " + y);
            drawStone(g, x, y, stone.getColor());
        }
	else throw new RuntimeException("unknown markup: " + m);
    }
    */

    /**
     * Insert the method's description here. Creation date: (03/24/00 17:12:52)
     * @param x int
     * @param y int
     * @param c goban.BoardType
     */
    /*
    protected void drawStone(Graphics2D g, short x, short y, BoardType c)
    {
        logger.fine("drawStone: " + c);

        if (c == BoardType.White)
            drawWhiteStone(g, x, y);
        else if (c == BoardType.Black)
            drawBlackStone(g, x, y);
        else if (gobanBackground != null)
        {
            //g.drawImage(gobanBackground, x, y, x+1, y+1, this);
        }
    }
    */

    /**
     * Insert the method's description here. Creation date: (03/30/00 14:28:25)
     * @param x short
     * @param y short
     */
    public void drawBlackStone(Graphics2D g, short x, short y)
    {
        //g.drawImage(bsi, t.x - gridWidth, t.y - gridWidth, this);
        Shape c = new Ellipse2D.Double(x, y, 1, 1);
        g.setPaint(Color.black);
        g.draw(c);
        g.fill(c);
    }

    /**
     * Insert the method's description here. Creation date: (03/30/00 14:28:25)
     * @param x short
     * @param y short
     */
    public void drawWhiteStone(Graphics2D g, short x, short y)
    {
        //g.drawImage(wsi, t.x - gridWidth, t.y - gridWidth, this);
        Shape c = new Ellipse2D.Double(x, y, 1, 1);
        g.setPaint(Color.white);
        g.fill(c);
        g.setPaint(Color.black);
        g.draw(c);
        logger.fine("drawWhiteStone");
    }

    /**
     * Insert the method's description here. Creation date: (03/23/00 21:28:36)
     * @return java.awt.Image
     */
    public Image getBsi()
    {
        return bsi;
    }

    /**
     * Insert the method's description here. Creation date: (03/22/00 23:47:43)
     * @return java.awt.Image[]
     */
    public Image[] getWhiteStoneImages()
    {
        return whiteStoneImages;
    }

    /**
     * Insert the method's description here. Creation date: (03/23/00 21:26:59)
     * @return java.awt.Image
     */
    public Image getWsi()
    {
        return wsi;
    }

    /**
     * Insert the method's description here. Creation date: (03/27/00 21:39:00)
     * @param p goban.Point
     */
    protected void highlight(Graphics g, Point p)
    {
        g = scaleGraphics(g, goban);

        if (g != null)
        {
            logger.fine("Highlight " + p.getX() + ", " + p.getY());
            Color c = g.getColor();
            g.setColor(Color.red);
            g.drawRect(p.getX(), p.getY(), 1, 1);
            g.setColor(c);
        }
    }

    /*
    public boolean imageUpdate(java.awt.Image img, int flags,
			       int x, int y, int w, int h)
    {
	logger.fine("ImageUpdate");
	return super.imageUpdate(img, flags, x, y, w, h);
    }
    */

    /** Insert the method's description here. Creation date: (03/25/00 12:47:26) */
    protected void initBackground(JGoban goban)
    {
        //int borderWidth = (getWidth() - 2*(getBoardSize()-1)*gridWidth)/2;
        gobanBackground = goban.createImage(goban.getWidth(), goban.getHeight());
        if (gobanBackground == null)
            return;
        Graphics2D g = scaleGraphics(gobanBackground.getGraphics(), goban);
        if (g != null)
            drawBackground(g, goban);
    }

    /** Insert the method's description here. Creation date: (20.11.99 22:34:36) */
    protected void initImages(JGoban goban)
    {
        int radius = 1; //gridWidth;
        int pixel[];
        int x, y;
        int i = 0;

        pixel = new int[4 * radius * radius + 4 * radius + 1];
        int d;
        for (y = -radius; y <= radius; y++)
            for (x = -radius; x <= radius; x++)
            {
                d = x * x + y * y - radius * radius;
                if (d > 1)
                    pixel[i] = 0;
                else
                {
                    int opaque = d < 0 ? 255 : 0;
                    double lx = 0.35355339, ly = 0.35355339, lz = 0.8660254;
                    double s = Math.sin(0.3);
                    double c = Math.cos(0.3);
                    double l = x * s - y * c;
                    double nz = Math.sqrt(radius * radius - x * x - y * y);
                    double z = 1.0 - (nz / radius);
                    double co = Math.IEEEremainder(l + 8 * z * z, 4) / 4;
                    double nDotL = (x * lx + y * ly + nz * lz) / radius;
                    double bright = (2.0 * nz * nDotL) / radius - lz;
                    if (bright < 0) bright = 0;
                    double lambertian = nDotL;
                    bright *= bright;
                    bright *= bright;
                    bright *= bright;
                    bright *= bright;

                    if (co < 0) co *= -2;
                    co = 0.8 + 0.2 * co;
                    co = 0.4 * bright * co + co * (0.3 * lambertian + 0.7);
                    if (co > 1) co = 1;

                    int grey = new Double(255 * co).intValue();
                    pixel[i] = (opaque << 24) | (grey << 16) | (grey << 8) | (grey);
                }
                i++;
            }
        wsi = goban.createImage(new MemoryImageSource(2 * radius + 1, 2 * radius + 1, pixel, 0, 2 * radius + 1));
        i = 0;
        pixel = new int[4 * radius * radius + 4 * radius + 1];
        for (y = -radius; y <= radius; y++)
            for (x = -radius; x <= radius; x++)
            {
                d = x * x + y * y - radius * radius;
                if (d > 1)
                    pixel[i] = 0;
                else
                {
                    int opaque = d < 0 ? 255 : 100;
                    double lx = 0.35355339, ly = 0.35355339, lz = 0.8660254;
                    double nz = Math.sqrt(radius * radius - x * x - y * y);
                    double z = 1.0 - (nz / radius);
                    double nDotL = (x * lx + y * ly + nz * lz) / radius;
                    double bright = (2.0 * nz * nDotL) / radius - lz;
                    if (bright < 0) bright = 0;
                    double lambertian = nDotL;
                    bright *= bright;

                    double co = 0.3;
                    co = 0.3 * bright + co * (0.3 * lambertian + 0.7);
                    if (co > 1) co = 1;

                    int grey = new Double(255 * co).intValue();
                    pixel[i] = (opaque << 24) | (grey << 16) | (grey << 8) | (grey);
                }
                i++;
            }
        bsi = goban.createImage(new MemoryImageSource(2 * radius + 1, 2 * radius + 1, pixel, 0, 2 * radius + 1));
    }

    public void mouseClicked(MouseEvent e)
    {
        logger.fine("MouseEvent");
        String command;

        if (e.isPopupTrigger())
            command = "Menu";
        else
            command = goban.getActionCommand();
        processActionEvent(
            new GobanActionEvent(this, ActionEvent.ACTION_PERFORMED, command, toBoard(e.getPoint()), ((MouseEvent)e).getModifiers()));
    }

    /**
     * Paint a Goban.
     * @todo Currently a GobanUI does not expect to be installed on more than one <code>Goban</code>.
     * @param g The <code>Graphics2D</code> to draw on.
     */
    public void paint(Graphics g, JComponent c)
    {
	assert c == goban;

	if (goban == null)
	    return;

        Graphics2D g2d = scaleGraphics(g, goban);

        renderer.paint(g2d, model);
        //doHighlights();
        int boardSize = model.getBoardSize();
        if (model instanceof MarkupModel)
        {
            MarkupModel m = (MarkupModel)model;
            Region region = m.getRegion();
            if (region != null)
            {
                logger.info("Region: " + region);
                Area area = new Area(new Rectangle(0, 0, boardSize, boardSize));
                area.subtract(new Area(SimpleRegion.getShape(region)));
                Color color = new Color(0.4f, 0.4f, 0.4f, 0.5f);
                g2d.setPaint(color);
                g2d.fill(area);
            }
        }

	/*
	assert c == goban;

	logger.info("GobanUI.paint");
	int i, j;
	int boardSize = goban.getBoardSize();
	GobanModel model = getModel();

	//super.paint(g);
	Graphics2D g2d = scaleGraphics(g, goban);
	if (g != null) {
	    drawBackground(g2d, goban);
	    if (model != null) {
		short x, y;
		for (y=0; y<boardSize; y++)
		    for (x=0; x<boardSize; x++) {
			if (model.getStone(x, y) == BoardType.White) {
			    logger.fine(x + ", " + y + ": " + "White");
			    drawWhiteStone(g2d, x, y);
			}
			else if (model.getStone(x, y) == BoardType.Black) {
			    logger.fine(x + ", " + y + ": " + "Black");
			    drawBlackStone(g2d, x, y);
			}
			if (model instanceof MarkupModel) {
			    drawMarkup((Graphics2D) g2d, x, y, (MarkupModel) model);
			}
		    }
	    }
	    else {
		logger.fine("null model");
	    }
	}
	
	//doHighlights();
	//super.paint(g);
	if (getModel() instanceof MarkupModel) {
	    MarkupModel m = (MarkupModel) getModel();
	    Region region = m.getRegion();
	    if (region != null) {
		logger.info("Region: " + region);
		Area area = new Area(new Rectangle(0, 0, boardSize, boardSize));
		area.subtract(new Area(region.getShape()));
		Color color = new Color(0.4f, 0.4f, 0.4f, 0.5f);
		g2d.setPaint(color);
		g2d.fill(area);
	    }
	}
	*/
    }

    protected void processActionEvent(ActionEvent e)
    {
        if (actionListener != null)
        {
            actionListener.actionPerformed(e);
        }
    }

    public void removeActionListener(ActionListener l)
    {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    /**
     * Insert the method's description here. Creation date: (03/23/00 21:28:36)
     * @param newBsi java.awt.Image
     */
    public void setBsi(Image newBsi)
    {
        bsi = newBsi;
    }

    /**
     * Insert the method's description here. Creation date: (03/22/00 23:47:43)
     * @param newWhiteStoneImages java.awt.Image[]
     */
    public void setWhiteStoneImages(Image[] newWhiteStoneImages)
    {
        whiteStoneImages = newWhiteStoneImages;
    }

    /**
     * Insert the method's description here. Creation date: (03/23/00 21:26:59)
     * @param newWsi java.awt.Image
     */
    public void setWsi(Image newWsi)
    {
        wsi = newWsi;
    }

    /** stoneAdded method comment. */
    public void stonesRemoved(GobanEvent event)
    {
        logger.fine("stoneRemoved");
        if (goban.isVisible())
        {
	    /*
	    Graphics2D g = scaleGraphics(getGraphics());
	    java.util.Enumeration en = event.getPoints().elements();
	    while(en.hasMoreElements()) {
		Point p = (Point) en.nextElement();
		drawStone(g, p.x, p.y, BoardType.Empty);
	    }
	    doHighlights();
	    repaint();
	    */
        }
    }

    /**
     * Get the value of model.
     * @return value of model.
     */
    public Goban getModel()
    {
        return model;
    }

    /**
     * Set the value of model.
     * @param model  Value to assign to model.
     */
    public void setModel(Goban model)
    {
        this.model = model;
        if (model instanceof MarkupModel)
        {
            MarkupModel m = (MarkupModel)model;
            setRegion(m.getRegion());
        }
        else
            setRegion(null);
    }

    /**
     * Get the value of region.
     * @return value of region.
     */
    public Region getRegion()
    {
        return region;
    }

    /**
     * Set the value of region.
     * @param region  Value to assign to region.
     */
    public void setRegion(Region region)
    {
        this.region = region;
        regionResizer.setRegion(region);
    }

    void setView()
    {
        if (goban.getModel() instanceof MarkupModel)
        {
            Region region = selectRegion();
            setRegion(region);
            ((MarkupModel)model).setRegion(region);
        }
    }


    /** valueChanged method comment. */
    public void valueChanged(GobanSelectionEvent e)
    {
        Enumeration enu;
        Point p;

	/*
	Graphics2D g = scaleGraphics(getGraphics());
	enu = e.getDeselectedPoints().elements();

	while(enu.hasMoreElements()) {
	    p = (Point) enu.nextElement();
	    drawStone(g, p.x, p.y, BoardType.Empty);
	    logger.fine("Deselected: " + p.x + ", " + p.y);
	    drawStone(g, p.x, p.y, getModel().getStone(p));
	}
	doHighlights();
	*/
    }

    public void propertyChange(PropertyChangeEvent e)
    {
        assert e.getSource() == goban;

        if (e.getPropertyName().equals("model"))
        {
            setModel(goban.getModel());
        }
    }
}
