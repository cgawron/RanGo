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

import de.cgawron.go.*;
import de.cgawron.go.sgf.MarkupModel;

import java.awt.AWTEventMulticaster;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

/**
 * A component displaying a goban.
 * The state of the goban is represented by a {@link GobanModel} which provides methods for making moves and adding or removing stones. 
 * If you want to display board markup (triangles, letters etc.) use a {@link MarkupModel} instead.
 * <p>
 * The rendering of the goban itself is done by an instance of {@link GobanUI}.
 * @author Christian Gawron
 */
public class JGoban extends JComponent implements GobanListener, GobanSelectionListener
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(JGoban.class.getName());
    private static final String uiClassID = "GobanUI";

    private Goban model;

    private GobanSelectionModel selectionModel;
    private ActionListener actionListener = null;

    private GobanAction action = null;
    private JPopupMenu popupMenu = new JPopupMenu();

    private DragSource dragSource;

    class MyTransferable implements Transferable
    {
	DataFlavor[] myFlavors = {DataFlavor.imageFlavor/*, DataFlavor.javaFileListFlavor*/};

	public Object getTransferData(DataFlavor flavor)
	{
	    logger.info("getTransferData: " + flavor);
	    if (flavor.equals(DataFlavor.stringFlavor))
		return "bla";
	    else if (flavor.equals(DataFlavor.imageFlavor))
	    {
		Image image = createVolatileImage(getWidth(), getHeight());
		Graphics g = image.getGraphics();
		getUI().paint(g, JGoban.this);
		return image;
	    }
	    else if (flavor.equals(DataFlavor.javaFileListFlavor))
	    {
		logger.info("returning file list");
		List<File> list = new ArrayList<File>();
		list.add(new File("test"));
		return list;
	    }
	    else 
		return null;
	}
	
	public DataFlavor[] getTransferDataFlavors()
	{
	    logger.info("queried!");
	    return myFlavors;
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor)
	{
	    logger.info("isDataFlavorSupported: " + flavor);
	    return false;
	}
    }


    /**
     * A GobanActionEvent provides functionality to get the position where the event ocurred on the goban.
     */
    public class GobanActionEvent extends ActionEvent
    {
	private Point point;
	private Goban gobanModel;
	
	GobanActionEvent(Object source, String command, Point point)
	{
	    super(source, ActionEvent.ACTION_PERFORMED, command);
	    this.point = point;
	    gobanModel = JGoban.this.getModel();
	    logger.info("GobanActionEvent: " + point);
	}

	GobanActionEvent(Object source, String command, MouseEvent event)
	{
	    super(source, ActionEvent.ACTION_PERFORMED, command, event.getModifiers());
	    point = JGoban.this.toBoard(event.getPoint());
	    gobanModel = JGoban.this.getModel();
	    logger.info("GobanActionEvent: " + point);
	}
	
	/**
	 * Get the position where Action was triggered.
	 * @return the position on the Goban.
	 */
	public Point getPoint()
	{
	    return point;
	}

	/**
	 * Get the GobanModel for which the action was triggered.
	 * @return the GobanModel of the Goban as the event was triggered.
	 */
	public Goban getModel()
	{
	    return gobanModel;
	}
    }

    /**
     * Set an {@link GobanAction} of the goban.
     * The action will be triggered if the user clicks on the goban.
     * @throws ClassCastException if the action is not a GobanAction.
     * @param action the Action to set.
     */
    public void setAction(Action action)
    {
	logger.info("Setting action to " + action);
	this.action = (GobanAction) action;
    }

    /**
     * Get the Action set on this action.
     * @return the Action that will be triggered by clicking on this Goban.
     */
    public Action getAction()
    {
	return this.action;
    }

    /**
     * An Action which can be performed on a goban.
     */
    public abstract static class GobanAction extends UndoableAction
    {
	/**
	 * Default constructor.
	 */
	public GobanAction()
	{
	    super();
	}

	/**
	 * Create a GobanAction with a name.
	 * @param name name of the action.
	 */
	public GobanAction(String name)
	{
	    super(name);
	}
	
	public void actionPerformed(ActionEvent e)
	{
	    actionPerformed((GobanActionEvent) e);
	}

	/**
	 * @see Action#actionPerformed
	 */
	abstract public void actionPerformed(GobanActionEvent e);
    }

    /**
     * Get the command associated with the currently set Action.
     * @return The action command.
     */
    public String getActionCommand()
    {
	if (action != null)
	    return (String) action.getValue(Action.ACTION_COMMAND_KEY);
	else 
	    return "";
    }

    public void doClick(Point p)
    {
	fireActionPerformed(new GobanActionEvent(this, getActionCommand(), p));
    }

    /**
     * Fire an ActionEvent.
     * @param event Event to fire.
     */
    protected void fireActionPerformed(ActionEvent event) 
    {
	if (action != null) {
	    logger.info("action " + action + "(" + event + ")");
	    action.actionPerformed(event);
	}
	else logger.info("no action");
    }
    
    private class MyMouseWheelListener implements MouseWheelListener
    {
	MyMouseWheelListener()
	{
	}

	public void mouseWheelMoved(MouseWheelEvent e) 
	{
	    logger.info("MouseWheel: " + e);
	}
    }

    private MouseListener mouseListener = 
	new MouseAdapter()
	{
	    public void mousePressed(MouseEvent e) 
	    {
		logger.info("mousePressed, button=" + e.getButton());
		maybeShowPopup(e);
	    }
	    
 	    public void mouseReleased(MouseEvent e) 
	    {
		logger.info("mouseReleased, button=" + e.getButton());
		maybeShowPopup(e);
	    }
	    
 	    public void mouseClicked(MouseEvent e) 
	    {
		logger.info("mouseClicked, button=" + e.getButton());
		if (e.getButton() == MouseEvent.BUTTON1) {
		    logger.info("action");
		    fireActionPerformed(new GobanActionEvent(this, getActionCommand(), e));
		}
		else
		    maybeShowPopup(e);
	    }

	    private void maybeShowPopup(MouseEvent e) 
	    {
		if (popupMenu != null) 
		    if (popupMenu.isPopupTrigger(e)) {
			logger.info("popup");
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		    }
	    }
	};


    /** Construct a Goban with a default model. */
    public JGoban()
    {
        this(new SimpleGoban());
    }

    /** Construct a Goban with a given model. */
    public JGoban(Goban m)
    {
        super();
        model = m;
        model.addGobanListener(this);
        selectionModel = new SimpleGobanSelectionModel();
        selectionModel.addGobanSelectionListener(this);
	addMouseListener(mouseListener);
	// addMouseWheelListener(new MyMouseWheelListener());
        updateUI();
    }

    /** 
     * Goban doesn't currently support pluggable L&F. 
     * This method just resets the ui with a new instance of GobanUI. 
     */
    public void updateUI()
    {
        //GobanUI ui = (GobanUI) UIManager.getUI(this);
        GobanUI ui = GobanUI.createUI();
        logger.info("updateUI: " + ui);
        setUI(ui);
    }

    /** 
     * Sets the L&F delegate for this component. 
     * @param newUI The GobanUI to set for this goban.
     */
    public void setUI(GobanUI newUI)
    {
        logger.info("setUI: " + this + ", " + newUI);
        super.setUI(newUI);

	dragSource = DragSource.getDefaultDragSource();
	DragGestureRecognizer recognizer;
	recognizer = dragSource.createDefaultDragGestureRecognizer(this, 
								   DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK, 
								   newUI); 
    }

    public void setRenderer(GobanRenderer renderer)
    {
        ((GobanUI) ui).setRenderer(renderer);
    }

    public Transferable getTransferable()
    {
	return new MyTransferable();
    }

    /** 
     * Returns the L&F delegate of this component. 
     * @return The current UI delegate for this goban.
     */
    public GobanUI getUI()
    {
        return (GobanUI)ui;
    }

    /**
     * Map a <code>java.awt.Point</code> on this goban to the logical goban coordinates.
     * This method is delegated to the {@link GobanUI} of this goban.
     * @return The {@link Point} on the goban.
     * @param p the <code>java.awt.Point</code> on this goban.
     */
    public Point toBoard(java.awt.Point p)
    {
	return getUI().toBoard(p);
    }

    /**
     * Add an ActionListener to this goban.
     * @param l the ActionListener to add.
     */
    public void addActionListener(ActionListener l)
    {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    /** 
     * The method is obsolete.
     * @deprecated This method is deprecated.
     */
    protected void doHighlights()
    {
	/*
	Enumeration enu = getSelectionModel().getSelectedPoints().elements();
	while (enu.hasMoreElements()) {
	    Point p = (Point) enu.nextElement();
	    highlight(p);
	    logger.debug("Selected: " + p);	
	}
	*/
    }

    /**
     * Gets the boardSize property value.
     * @return The boardSize.
     * @see #setBoardSize
     */
    public short getBoardSize()
    {
        return (short)model.getBoardSize();
    }

    
    /**
     * @deprecated Use {@link #getActionCommand} instead.
     */
    public String getCommand()
    {
	return getActionCommand();
    }

    /**
     * Get the {@link #GobanModel} of this component.
     * @return goban.GobanModel
     */
    public Goban getModel()
    {
        return model;
    }

    /**
     * Get the {@link GobanSelectionModel} of this goban.
     * The GobanSelectionModel can be used to get the list of selected points on the goban.
     * @return The GobanSelectionModel of this goban.
     */
    public GobanSelectionModel getSelectionModel()
    {
        return selectionModel;
    }

    /** 
     * Informs this Goban that it's GobanModel has changed and triggers a {@link #repaint}.
     * This methos should not be called directly - the Gobanmodel will do this if necessary.
     * @see GobanModelEvent
     * @param e The GobanModelEvent describing the changes.
     */
    public void modelChanged(GobanEvent e)
    {
        //logger.info("Goban: model changed");
        repaint();
    }

    /**
     * Removes <code>listener</code> from the listener list.
     * @param listener The <code>ActionListener</code> to remove.
     */
    public void removeActionListener(ActionListener listener)
    {
        actionListener = AWTEventMulticaster.remove(actionListener, listener);
    }

    /**
     * Insert the method's description here. Creation date: (03/22/00 23:43:36)
     * @param newModel goban.GobanModel
     */
    public void setModel(Goban newModel)
    {
        Goban oldModel = model;
        if (newModel != oldModel)
        {
            logger.info("Goban: setting model " + oldModel + " to " + newModel);
            model = newModel;
            model.addGobanListener(this);
            firePropertyChange("model", oldModel, newModel);
        }
        repaint();
    }

    /**
     * Set the <code>{@link #GobanSelectionModel}</code> of this goban.
     * The GobanSelectionModel controls the behavior when points are selected on this goban.
     * @param selectionModel The GobanSelectionModel to set.
     */
    public void setSelectionModel(GobanSelectionModel selectionModel)
    {
        this.selectionModel = selectionModel;
    }


    /** 
     * Set the size of this goban.
     * A goban is best viewed when the screen area is square.
     * @param width The new width.
     * @param height The new height.
     */
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        setPreferredSize(new Dimension(Math.min(width, height), Math.min(width, height)));

        //gridWidth = width / (2 * getBoardSize() + 2);
        //gridWidth += gridWidth % 2 == 0 ? 1 : 0;
        //initImages();
        //initBackground();
        revalidate();
        repaint();
    }

    /** stoneAdded method comment. */
    public void stoneAdded(GobanEvent event)
    {
        logger.fine("stoneAdded");
        //Graphics2D g = getGraphics(getGraphics());
        if (isVisible())
        {
	    /*
	    java.util.Enumeration en = event.getPoints().elements();
	    while(en.hasMoreElements()) {
		Point p = (Point) en.nextElement();
		drawStone(g, p.x, p.y, event.getColor());
	    }
	    */

            repaint();
            //doHighlights();
        }
    }

    /** stoneAdded method comment. */
    public void stonesRemoved(GobanEvent event)
    {
        logger.fine("stoneAdded");
        if (isVisible())
        {
	    /*
	    Graphics2D g = getGraphics(getGraphics());
	    java.util.Enumeration en = event.getPoints().elements();
	    while(en.hasMoreElements()) {
		Point p = (Point) en.nextElement();
		drawStone(g, p.x, p.y, BoardType.Empty);
	    }
	    doHighlights();
	    */

            repaint();
        }
    }

    /** valueChanged method comment. */
    public void valueChanged(GobanSelectionEvent e)
    {
        Enumeration enu;
        Point p;

	/*
	
	Graphics2D g = getGraphics(getGraphics());
	enu = e.getDeselectedPoints().elements();

	while(enu.hasMoreElements()) {
	    p = (Point) enu.nextElement();
	    drawStone(g, p.x, p.y, BoardType.Empty);
	    logger.debug("Deselected: " + p.x + ", " + p.y);
	    drawStone(g, p.x, p.y, getModel().getStone(p));
	}
	doHighlights();
	*/
    }

    public void setView()
    {
        getUI().setView();
    }

    /**
     * Sets the boardSize property (int) value.
     * @param boardSize The new value for the property.
     * @see #getBoardSize
     */
    public void setBoardSize(short boardSize)
    {
        model.setBoardSize(boardSize);
        invalidate();
    }

    public String getToolTipText(MouseEvent event)
    {
	Point q = toBoard(event.getPoint());
	String text = null; 
	if (model instanceof MarkupModel)
	    text = (String) ((MarkupModel) model).getToolTipText(q);
	if (text != null)
	    return text;
	else 
	    return (char)('A' + (q.getX() > 9 ? q.getX() + 1 : q.getX())) + "" + ((int)(q.getY() + 1));
    }
}
