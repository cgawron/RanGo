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

package de.cgawron.go;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple implementation of a GobanModel, using a two-dimensional array to
 * represent the board. 
 * @author Christian Gawron
 */
public class SimpleGoban extends AbstractGoban
{
	protected BoardType[][] boardRep;
	protected int[][] tmpBoard;
	private int[] _hash;

	protected int visited;
	int numStones = 0;
	protected static Logger logger = Logger.getLogger(SimpleGoban.class.getName());

	/** Create a SimpleGoban with default board size of 19x19. */
	public SimpleGoban()
	{
		this(19);
	}

	/**
	 * Create a SimpleGoban which copies the board of another Goban
	 * 
	 * @param m
	 *            gawron.go.goban.Goban
	 */
	public SimpleGoban(Goban m)
	{
		super();
		numStones = 0;
		if (m != null)
			copy(m);
		else
			setBoardSize((int) 19);
	}

	/** Create a SimpleGoban with a given board size. */
	public SimpleGoban(int size)
	{
		super();
		numStones = 0;
		setBoardSize((int) size);
	}

	/**
	 * Calculate the chinese score of the position.
	 * This method assumes that all dead stones are already removed, i.e. all 
	 * stones on the board are considered alive, and territories containing stones of both colors are neutral.
	 * @return The chinese score of the position.
	 */
	public int chineseScore(double[][] territory) 
	{
		/*
		if (logger.isLoggable(Level.INFO))
			logger.info("chineseScore: \n" + this);
	    */
		int score = 0;
		visited++;
		int i, j;
		for (i = 0; i < boardSize; i++) {
			for (j = 0; j < boardSize; j++) {
				if (tmpBoard[i][j] == visited) {
					continue;
				}
				else {
					switch(boardRep[i][j]) {
					case BLACK:
						score++;
						if (territory != null)
							territory[i][j] += 1;
						break;
					case WHITE:
						score--;
						if (territory != null)
							territory[i][j] -= 1;
						break;
					case EMPTY:
						score += scoreEmpty(new Point(i, j), territory);
						break;
					}
				}
			}
		}
		return score;
	}
	
	public void clear()
	{
		int i;
		int j;
		for (i = 0; i < boardSize; i++)
			for (j = 0; j < boardSize; j++)
				setStone(i, j, BoardType.EMPTY);
		numStones = 0;
		fireModelChanged();
	}
	
	public Goban clone() 
	{
		Goban model = new SimpleGoban();
		model.copy(this);
		return model;
	}

	/**
	 * Insert the method's description here. Creation date: (04/09/00 18:33:15)
	 * 
	 * @param m
	 *            gawron.go.goban.Goban
	 */
	public void copy(Goban m)
	{
		int i;
		int j;
		if (m instanceof SimpleGoban) {
			SimpleGoban sm = (SimpleGoban) m;
			setBoardSizeNoInit(sm.boardSize);

			for (i = 0; i < boardSize; i++)
				System.arraycopy(sm.boardRep[i], 0, boardRep[i], 0, boardSize);

			System.arraycopy(sm._hash, 0, _hash, 0, 16);
			whiteCaptured = sm.whiteCaptured;
			blackCaptured = sm.blackCaptured;
			numStones = sm.numStones;
		} else {
			setBoardSize(m.getBoardSize());
			for (i = 0; i < boardSize; i++)
				for (j = 0; j < boardSize; j++)
					setStone(i, j, m.getStone(i, j));

			whiteCaptured = m.getWhiteCaptured();
			blackCaptured = m.getBlackCaptured();
		}
		fireModelChanged();
	}

	public int countLiberties(int x, int y, boolean incrementVisited)
	{
		if (incrementVisited) visited++;
		
		tmpBoard[x][y] = visited;
		int liberties = (int) 0;

		Point p = new Point(x, y);
		for (Point q : p.neighbors(this)) {
			if (tmpBoard[q.getX()][q.getY()] != visited) {
				tmpBoard[q.getX()][q.getY()] = visited;
				if (getStone(q) == BoardType.EMPTY)
					liberties++;
				else if (getStone(q) == getStone(x, y))
					liberties += countLiberties(q, false);
			}
		}
		return liberties;
	}

	public int countLiberties(Point p)
	{
		return countLiberties(p, true);
	}
	
	/**
	 * Insert the method's description here. Creation date: (03/26/00 17:12:54)
	 * 
	 * @return int
	 * @param p
	 *            goban.Point
	 */
	public int countLiberties(Point p, boolean incrementVisited)
	{
		return countLiberties(p.getX(), p.getY(), false);
	}

	public boolean equals(Object o)
	{
		if (o == this)
			return true;
		else if (o instanceof SimpleGoban) {
			SimpleGoban goban = (SimpleGoban) o;
			if (goban.boardSize != this.boardSize) return false;
			int i, j;
			for (i = 0; i < boardSize; i++)
				for (j = 0; j < boardSize; j++)
					if (boardRep[i][j] != goban.boardRep[i][j])
						return false;
			return true;
		} else if (o instanceof Goban) {
			Goban goban = (Goban) o;
			/*
			 * Point.BoardIterator it = new Point.BoardIterator(size);
			 * 
			 * while (it.hasNext()) { Point p = (Point) it.next(); if
			 * (getStone(p) != goban.getStone(p)) return false; }
			 */
			int i, j;
			for (i = 0; i < boardSize; i++)
				for (j = 0; j < boardSize; j++)
					if (getStone(i, j) != goban.getStone(i, j))
						return false;
			return true;
		}
		return false;
	}

	/** getStone method comment. */
	public final BoardType getStone(int x, int y)
	{
		return boardRep[x][y];
	}

	/** getStone method comment. */
	public BoardType getStone(Point p)
	{
		return boardRep[p.getX()][p.getY()];
	}

	public boolean move(int x, int y, BoardType color)
	{
		if (x < 0 || y < 0 || x >= getBoardSize() || y >= getBoardSize())
			return false;

		BoardType enemy = color.opposite();
		if (getStone(x, y) != BoardType.EMPTY)
			return false;

		setStone(x, y, color);

		Point p = new Point(x, y);
		lastMove = p;
		for (Point q : p.neighbors(this)) {
			visited++;
			if (getStone(q) == enemy)
				if (countLiberties(q) == 0) {
					removeChain(q, removed);
				}
		}
		visited++;

		boolean legal = true;
		if (countLiberties(p) == 0) {
			removeChain(p, removed);
			legal = false;
		}
		fireStonesRemoved(removed);
		fireStoneAdded(x, y, color);
		return legal;
	}

	/** move method comment. */
	public boolean move(int x, int y, BoardType color, int moveNo)
	{
		return move(x, y, color);
	}

	public Goban newInstance()
	{
		return new SimpleGoban(getBoardSize());
	}

	/** putStone method comment. */
	public void putStone(int x, int y, BoardType color)
	{
		setStone(x, y, color);
		fireStoneAdded(x, y, color);
	}

	/**
	 * Insert the method's description here. Creation date: (03/26/00 17:13:44)
	 * 
	 * @return int
	 */
	public int removeChain(Point p, Vector<Point> removed)
	{
		removed.addElement(p);
		BoardType c = getStone(p);
		setStone(p, BoardType.EMPTY);
		int r = 1;

		for (Point q : p.neighbors(this)) {
			if (getStone(q) == c)
				r += removeChain(q, removed);
		}

		if (c == BoardType.BLACK)
			whiteCaptured += r;
		else
			blackCaptured += r;

		return r;
	}

	public int scoreEmpty(Point p, double[][] territory) 
	{
		int score=0;
		boolean touchBlack = false;
		boolean touchWhite = false;
		Queue<Point> queue = new LinkedList<Point>();
		List<Point> area = new ArrayList<Point>();
		queue.add(p);
		
		while (!queue.isEmpty()) {
			p = queue.poll();
			area.add(p);
			score++;
			tmpBoard[p.getX()][p.getY()] = visited;
			for (Point n : new Neighborhood(this.getBoardSize(), p)) {
				if (tmpBoard[n.getX()][n.getY()] == visited) continue;
				else {
					switch(boardRep[n.getX()][n.getY()]) {
					case BLACK:
						touchBlack = true;
						break;
					case WHITE:
						touchWhite = true;
						break;
					case EMPTY:
						queue.add(n);
						tmpBoard[n.getX()][n.getY()] = visited;
						break;
					}
				}
			}
		}
		//logger.info("scoreEmpty " + p + ": " + score);
		if (touchBlack && touchWhite)
			return 0;
		else if (touchBlack) {
			for (Point q : area) {
				if (territory != null)
					territory[q.getX()][q.getY()] += 1.0;
			}
			return score;
		}
		else if (touchWhite) {
			for (Point q : area) {
				if (territory != null)
					territory[q.getX()][q.getY()] -= 1.0;
			}
			return -score;
		}
		else throw new IllegalStateException("This should not happen: \n" + toString());
	}
	
	/**
	 * Insert the method's description here. Creation date: (04/18/00 23:51:16)
	 * 
	 * @param newBlackCaptured
	 *            int
	 */
	protected void setBlackCaptured(int newBlackCaptured)
	{
		blackCaptured = newBlackCaptured;
	}

	/** setBoardSize method comment. */
	public void setBoardSize(int s)
	{
		if (boardSize != s) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("setBoardSize: " + s);
			boardSize = s;
			boardRep = new BoardType[boardSize][boardSize];
			tmpBoard = new int[boardSize][boardSize];
			_hash = new int[16];

			int i;
			for (i = 0; i < boardSize; i++) {
				java.util.Arrays.fill(boardRep[i], BoardType.EMPTY);
				java.util.Arrays.fill(tmpBoard[i], 0);
			}

			for (i = 0; i < 16; i++)
				_hash[i] = 0;

			numStones = 0;
		}
	}

	/** setBoardSize method comment. */
	protected void setBoardSizeNoInit(int s)
	{
		if (boardSize != s) {
			if (logger.isLoggable(Level.FINE))
				logger.fine("setBoardSize: " + s);
			boardSize = s;
			boardRep = new BoardType[boardSize][boardSize];
			tmpBoard = new int[boardSize][boardSize];
			_hash = new int[16];

			int i;
			for (i = 0; i < boardSize; i++) {
				java.util.Arrays.fill(tmpBoard[i], 0);
			}
		}
	}

	/**
	 * Insert the method's description here. Creation date: (03/25/00 18:52:08)
	 * 
	 * @param x
	 *            int
	 * @param y
	 *            int
	 * @param c
	 *            goban.BoardType
	 */
	protected void setStone(int x, int y, BoardType c)
	{
		BoardType oc = boardRep[x][y];
		if (oc != c) {
			boardRep[x][y] = c;

			if (oc != BoardType.EMPTY)
				numStones--;
			if (c != BoardType.EMPTY)
				numStones++;

			Symmetry.Iterator it = new Symmetry.Iterator();
			while (it.hasNext()) {
				Symmetry s = (Symmetry) it.next();
				int si = s.toInt();
				Point pt = s.transform(x, y, boardSize);
				if (oc != BoardType.EMPTY) {
					if (s.transform(oc) == BoardType.BLACK)
						_hash[si] ^= zobrist[pt.getY() * boardSize + pt.getX()];
					else
						_hash[si] ^= ~zobrist[pt.getY() * boardSize + pt.getX()];
				}
				if (c != BoardType.EMPTY) {
					if (s.transform(c) == BoardType.BLACK)
						_hash[si] ^= zobrist[pt.getY() * boardSize + pt.getX()];
					else
						_hash[si] ^= ~zobrist[pt.getY() * boardSize + pt.getX()];
				}
			}
		}
	}
	
	/**
	 * Insert the method's description here. Creation date: (03/25/00 18:52:08)
	 * 
	 * @param x
	 *            int
	 * @param y
	 *            int
	 * @param c
	 *            goban.BoardType
	 */
	public void setStone(Point p, BoardType c)
	{
		setStone(p.getX(), p.getY(), c);
	}

	/**
	 * Insert the method's description here. Creation date: (04/18/00 23:50:17)
	 * 
	 * @param newWhiteCaptured
	 *            int
	 */
	protected void setWhiteCaptured(int newWhiteCaptured)
	{
		whiteCaptured = newWhiteCaptured;
	}

	public Goban transform(Symmetry s)
	{
		int size = getBoardSize();
		Goban m = newInstance();

		Point.BoardIterator it = new Point.BoardIterator(size);

		while (it.hasNext()) {
			Point p = (Point) it.next();
			BoardType stone = getStone(p);
			if (stone != BoardType.EMPTY) {
				Point pt = s.transform(p, size);
				m.putStone(pt, s.transform(stone));
			}
		}
		return m;
	}

	
	public int zobristHash()
	{
		Symmetry.Iterator it = new Symmetry.Iterator();
		int h = 0;

		while (it.hasNext()) {
			Symmetry s = (Symmetry) it.next();
			int _h = _hash(s);
			if (h == 0 || _h > h) {
				h = _h;
			}
		}

		return h;
	}
}
