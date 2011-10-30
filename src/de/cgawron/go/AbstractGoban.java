/*
 * Copyright (C) 2010 Christian Gawron
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

package de.cgawron.go;

import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link Goban} providing <code>PropertyChangeSupport</code>.
 * 
 * @author Christian Gawron
 * @version $Id: AbstractGoban.java 155 2005-01-04 10:47:49Z cgawron $
 * @see PropetrtyChangeSupport
 */
public abstract class AbstractGoban implements Goban
{
	protected static Logger logger = Logger.getLogger(AbstractGoban.class.getName());
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	protected Collection<GobanListener> listeners = new java.util.ArrayList<GobanListener>();
	protected int blackCaptured;
	protected int whiteCaptured;
	protected Point lastMove;
	protected int boardSize = 0;
	protected Vector<Point> removed = new Vector<Point>();

	public int _hash(Symmetry s) {
		Point.BoardIterator it = new Point.BoardIterator(getBoardSize());
		int h = 0;
		int n = 0;
	
		while (it.hasNext()) {
			Point p = (Point) it.next();
			BoardType stone = getStone(p);
			if (stone != BoardType.EMPTY) {
				n++;
				Point pt = s.transform(p, getBoardSize());
				int z = zobrist[pt.getY() * getBoardSize() + pt.getX()];
				if (s.transform(stone) == BoardType.BLACK)
					h += z;
				else
					h -= z;
			}
		}
		// if (sym & 8 != 0) h ^= 0xffffffff;
		h = (h & 0x01ffffff) | ((n & 0xfe) << (32 - 7));
		return h;
	}

	protected void addCaptureStones(BoardType color, int size) {
		switch(color) {
		case WHITE: 
			whiteCaptured += size;
			break;
		case BLACK: 
			blackCaptured += size;
			break;
		default:
			throw new IllegalArgumentException("Can't capure " + color);
		}
	}

	/** addGobanListener method comment. */
	public void addGobanListener(GobanListener l) {
		listeners.add(l);
	}

	/**
	 * Calculate the chinese score of the position.
	 * This method assumes that all dead stones are already removed, i.e. all 
	 * stones on the board are considered alive, and territories containing stones of both colors are neutral.
	 * @return The chinese score of the position.
	 */
	public abstract int chineseScore(double[][] territory);

	@Override
	abstract public Goban clone();

	public boolean equals(Object o, Symmetry s) {
		if (o instanceof Goban) {
			Goban goban = (Goban) o;
			Point.BoardIterator it = new Point.BoardIterator(boardSize);
	
			while (it.hasNext()) {
				Point p = (Point) it.next();
				Point pt = s.transform(p, boardSize);
				if (goban.getStone(p) != s.transform(getStone(pt)))
					return false;
			}
			return true;
		}
		return false;
	}
	
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o instanceof Goban) {
			return o.hashCode() == hashCode();
		}
		else return false;
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 16:07:59)
	 * 
	 * @param x
	 *            int
	 * @param y
	 *            int
	 * @param c
	 *            goban.BoardType
	 */
	protected void fireModelChanged() {
		GobanEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (GobanListener listener : listeners) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("Notifying listener ...");
			// Lazily create the event:
			if (e == null)
				e = new GobanEvent(this);
			listener.modelChanged(e);
		}
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 16:07:59)
	 * 
	 * @param x
	 *            int
	 * @param y
	 *            int
	 * @param c
	 *            goban.BoardType
	 */
	protected void fireStoneAdded(int x, int y, BoardType c) {
		GobanEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (GobanListener listener : listeners) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("Notifying listener ...");
			// Lazily create the event:
			if (e == null)
				e = new GobanEvent(this, x, y, c);
			listener.stoneAdded(e);
		}
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 16:07:59)
	 * 
	 * @param x
	 *            int
	 * @param y
	 *            int
	 * @param c
	 *            goban.BoardType
	 */
	protected void fireStonesRemoved(Collection<Point> removed) {
		// Guaranteed to return a non-null array
		GobanEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (GobanListener listener : listeners) {
			// Lazily create the event:
			if (e == null)
				e = new GobanEvent(this, removed);
			listener.stonesRemoved(e);
		}
	}

	@Override
	public int getBlackCaptured() {
		return blackCaptured;
	}

	/** getBoardSize method comment. */
	public final int getBoardSize() {
		return boardSize;
	}

	public Point getLastMove() {
		return lastMove;
	}

	public Vector<Point> getRemoved() {
		return removed;
	}

	public BoardType getStone(Point p) {
		return getStone(p.getX(), p.getY());
	}

	@Override
	public int getWhiteCaptured() {
		return whiteCaptured;
	}

	public int hashCode() {
		return zobristHash();
	}

	/**
	 * Checks if there are any listeners for a specific property.
	 * 
	 * @param propertyName
	 *            The name of the property
	 * @return <code>true</code>if there are one or more listeners for the given
	 *         property
	 */
	public boolean hasListeners(String propertyName)
	{
		return pcs.hasListeners(propertyName);
	}

	@Override
	public boolean move(Point p, BoardType color) {
		return move(p.getX(), p.getY(), color);
	}

	@Override
	public boolean move(Point p, BoardType color, int moveNo) {
		return move(p.getX(), p.getY(), color);
	}

	@Override
	public void putStone(Point p, BoardType color) {
		putStone(p.getX(), p.getY(), color);
	}

	/** addGobanListener method comment. */
	public void removeGobanListener(GobanListener l) {
		listeners.remove(l);
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer(512);
		int i, j;
		s.append("\n");
		BoardType p;
		for (i = 0; i < getBoardSize(); i++) {
			for (j = 0; j < getBoardSize(); j++) {
				p = getStone(i, j);
				if (p == BoardType.WHITE)
					s.append("O ");
				else if (p == BoardType.BLACK)
					s.append("X ");
				else
					s.append(". ");
			}
			s.append('\n');
		}
		return s.toString();
	}


}