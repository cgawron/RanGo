package de.cgawron.go.montecarlo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.GobanMap;
import de.cgawron.go.Neighborhood;
import de.cgawron.go.Point;
import de.cgawron.go.SimpleGoban;

public class AnalysisGoban extends SimpleGoban 
{
	public static Logger logger = Logger.getLogger(AnalysisGoban.class.getName());
	
	Map<Point, Chain> chainMap;
	Map<Point, Set<Chain>> libertyMap;
	List<Chain> chainList;
	
	public AnalysisGoban() {
		super();
		init();
	}

	public AnalysisGoban(Goban m) {
		super(m);
		// Do not call init() here!
	}

	public AnalysisGoban(int size) {
		super(size);
		init();
	}

	private void addChain(Point point) 
	{
		BoardType color = getStone(point);
		Chain chain = new Chain(color);
		chainList.add(chain);
		Queue<Point> queue = new LinkedList<Point>();
		queue.add(point);
		
		while (!queue.isEmpty()) {
			Point q = queue.poll();
			chain.add(q);
			chainMap.put(q, chain);
			setVisited(q, visited);
			for (Point p : new Neighborhood(this, q)) {
				if (getStone(p) == color) {
					if (!isVisited(p, visited)) {
						queue.add(p);
					}
				}
				else if (getStone(p) == BoardType.EMPTY) {
					if (!isVisited(p, chain.id)) {
						chain.addLiberty(p);
						if (!libertyMap.containsKey(p)) libertyMap.put(p, new TreeSet<Chain>());
						libertyMap.get(p).add(chain);
						setVisited(p, chain.id);
					}
				}
			}
		}
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
		for (i = 0; i < size; i++) {
			for (j = 0; j < size; j++) {
				if (tmpBoard[i][j] == visited) {
					continue;
				}
				else {
					switch(boardRep[i][j]) {
					case BLACK:
						score++;
						territory[i][j] += 1;
						break;
					case WHITE:
						score--;
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
	
	public AnalysisGoban clone() 
	{
		AnalysisGoban goban = new AnalysisGoban();
		init();
		goban.copy(this);
		return goban;
	}
	
	public void copy(Goban goban)
	{
		super.copy(goban);
		initChainMap(goban);
	}

	public int getAtariCount(BoardType movingColor) 
	{
		int count = 0;
		for (Chain chain : chainList) {
			if (chain.color == movingColor && chain.numLiberties == 1) {
				// logger.info("Atari: " + movingColor + " " + chain);
				count += chain.size();
			}
		}
		return count;
	}

	public Vector<Point> getRemoved() 
	{
		return removed;
	}

	private void init()
	{
	}

	private void initChainMap(Goban goban) 
	{
		//logger.info("initChainMap: " + goban);
		// if (chainMap == null) {
			chainMap = new GobanMap<Chain>(getBoardSize());
			libertyMap = new GobanMap<Set<Chain>>(getBoardSize());
			chainList = new ArrayList<Chain>();
		//}
		visited++;
		for (int i=0; i<size; i++) {
			for (int j=0; j<size; j++) {
				if (boardRep[i][j] != BoardType.EMPTY && tmpBoard[i][j] != visited) {
					addChain(new Point(i, j));
				}
			}
		}
	}

	public boolean isCapture(Point p, BoardType movingColor) 
	{
		
		//int captured = 0;
		if (getStone(p) != BoardType.EMPTY) return false;
	
		BoardType enemy = movingColor.opposite();
		setStone(p, movingColor);
		for (Point q : new Neighborhood(this, p)) {
			visited++;
			if (getStone(q) == enemy)
				if (countLiberties(q) == 0) {
					setStone(p, BoardType.EMPTY);
					return true;
				}
		}
		setStone(p, BoardType.EMPTY);
		return false;	
	}
	
	public boolean isIllegalKoCapture(Set<Goban> history, Point p, BoardType movingColor) {
		SimpleGoban goban = (SimpleGoban) clone();
		return history.contains(goban.move(p, movingColor));
	}

	public boolean isOneSpaceEye(Point p) 
	{
		Iterator<Point> n = (new Neighborhood(this, p)).iterator();
		p = n.next();
		BoardType color = getStone(p);
		if (color == BoardType.EMPTY) return false;
		while (n.hasNext()) {
			p = n.next();
			if (getStone(p) != color) 
				return false;	
		}
		return true;
	}

	public boolean isValidMove(Point p, BoardType movingColor) {
		if (getStone(p) != BoardType.EMPTY)
			return false;
		else { 
			if (isCapture(p, movingColor))
				return true;
			
			setStone(p, movingColor);
			int liberties = countLiberties(p);
			setStone(p, BoardType.EMPTY);
			
			return liberties > 0;
		}
	}

	private boolean isVisited(Point p, int visited) 
	{
		return tmpBoard[p.getX()][p.getY()] == visited;
	}

	public boolean move(int x, int y, BoardType color)
	{
		boolean result = super.move(x, y, color);
		updateChains(x, y, color);
		return result;
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
			for (Point n : new Neighborhood(this, p)) {
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
				territory[q.getX()][q.getY()] += 1.0;
			}
			return score;
		}
		else if (touchWhite) {
			for (Point q : area) {
				territory[q.getX()][q.getY()] -= 1.0;
			}
			return -score;
		}
		else throw new IllegalStateException("This should not happen: \n" + toString());
	}

	private void setVisited(Point p, int visited) 
	{
		tmpBoard[p.getX()][p.getY()] = visited;
	}

	private void updateChains(int x, int y, BoardType color) 
	{
		// FIXME - this is not efficient
		// logger.info("updateChains " + x + " " + y + " " + getStone(x, y));
		initChainMap(this);
	}

}
