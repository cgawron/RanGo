package de.cgawron.go.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import de.cgawron.go.AbstractGoban;
import de.cgawron.go.Goban;
import de.cgawron.go.Neighborhood;
import de.cgawron.go.Point;
import de.cgawron.go.SimpleGoban;

public class OldAnalysisGoban extends SimpleGoban 
{
	/**
	 * A connected chain of stones.
	 *  
	 * @author Christian Gawron
	 *
	 */
	public class Chain extends Cluster
	{
		int numLiberties;
		
		public Chain(BoardType color) {
			super(lastChainId++, color);
		}

		public void addLiberty(Point p) 
		{
			numLiberties++;
		}

		@Override
		public int compareTo(Cluster cluster) 
		{
			if (cluster instanceof Chain) {
				if (this.id < cluster.id) return -1;
				else if (this.id > cluster.id) return 1;
				else return 0;
			}
			else return +1;
		}

		@Override
		public String toString() {
			return "Chain [color=" + color + ", numLiberties=" + numLiberties
					+ ", size=" + size + ", id=" + id + "]";
		}
	}

	public abstract class Cluster implements Comparable<Cluster>
	{
		protected BoardType color;
		protected int size;
		protected int id;
		
		protected Set<Cluster> neighbors; 
		
		Cluster(int id, BoardType color) 
		{
			this.id = id;
			this.color = color;
			this. neighbors = new TreeSet<Cluster>();
		}
		
		public void addNeighbor(Cluster cluster)
		{
			neighbors.add(cluster);
		}

	}

	public class Eye extends Cluster  
	{
		/** This eye is definitely a real eye for a group */
		public boolean real;
		public BoardType color;
		public Group group;
		
		public Eye() 
		{
			super(lastEyeId++, BoardType.EMPTY);
		}

		@Override
		public int compareTo(Cluster cluster) 
		{
			if (cluster instanceof Eye) {
				if (this.id < cluster.id) return -1;
				else if (this.id > cluster.id) return 1;
				else return 0;
			}
			else return -1;
		}

		@Override
		public String toString() {
			return "Eye [size=" + size + ", id=" + id + ", real=" + real + ", color=" + color + "]";
		}

		public boolean isVitalPoint(Point move) {
			// FIXME
			return true;
		}

	}

	public class Group 
	{
		Set<Chain> chains;
		Set<Eye> eyes;
		
		public Group()
		{
			chains = new TreeSet<Chain>();
			eyes = new TreeSet<Eye>();
		}
		
		@Override
		public String toString() {
			return "Group [chains=" + chains + ", eyes=" + eyes + "]";
		}

		public boolean isAlive() {
			if (eyes.size() >= 2)
				return true;
			else 
				return false;
		}
	}

	private static final int INITIAL_CHAIN_SIZE = 32;
	
	public static Logger logger = Logger.getLogger(OldAnalysisGoban.class.getName());
	int lastChainId;
	int lastEyeId;
	int[] chainMap;
	int[] eyeMap;
	Chain[] chains;
	Eye[] eyes;
	Collection<Group> groups;
	BoardType movingColor;
	
	public OldAnalysisGoban() {
		super();
		this.movingColor = BoardType.BLACK;
	}

	public OldAnalysisGoban(Goban m, BoardType movingColor) {
		super(m);
		this.movingColor = movingColor;
		initAnalysis();
	}
	
	public OldAnalysisGoban(OldAnalysisGoban m) {
		super(m);
		this.movingColor = m.movingColor;
		initAnalysis();
	}

	public OldAnalysisGoban(int size) {
		super(size);
		this.movingColor = BoardType.BLACK;
	}

	private void addAdjacency(Cluster chain1, Cluster chain2) 
	{
		chain1.addNeighbor(chain2);
		chain2.addNeighbor(chain1);
	}
	
	private void addChain(Point point) 
	{
		//logger.info("addChain: " + point);
		BoardType color = getStone(point);
		Chain chain = new Chain(color);
		if (chain.id >= chains.length)
			chains = Arrays.copyOf(chains, 2*chains.length);
		chains[chain.id] = chain;
		Queue<Point> queue = new LinkedList<Point>();
		queue.add(point);
		
		visited++;
		//logger.info("addChain: visited=" + visited);
		while (!queue.isEmpty()) {
			Point q = queue.poll();
			chainMap[q.getX()*boardSize + q.getY()] = chain.id;
			chain.size++;
			setVisited(q, visited);
			for (Point p : new Neighborhood(this, q)) {
				if (getStone(p) == color) {
					if (chainMap[p.getX()*boardSize + p.getY()] != chain.id) {
						queue.add(p);
					}
				}
				else if (getStone(p) == BoardType.EMPTY) {
					if (!isVisited(p, visited)) {
						chain.addLiberty(p);
						setVisited(p, visited);
						if (eyeMap[p.getX()*boardSize + p.getY()] >= 0) {
							addAdjacency(chain, eyes[eyeMap[p.getX()*boardSize + p.getY()]]);
						}
					}
				}
				else if (chainMap[p.getX()*boardSize + p.getY()] >= 0) {
					addAdjacency(chain, chains[chainMap[p.getX()*boardSize + p.getY()]]);
				}
			}
		}
		//logger.info("addChain: chain=" + chain);
	}
	private void addEye(Point point) 
	{
		//logger.info("addChain: " + point);
		Eye eye = new Eye();
		if (eye.id >= eyes.length)
			eyes = Arrays.copyOf(eyes, 2*eyes.length);
		eyes[eye.id] = eye;
		Queue<Point> queue = new LinkedList<Point>();
		queue.add(point);
		
		//visited++;
		//logger.info("addEye: visited=" + visited);
		while (!queue.isEmpty()) {
			Point q = queue.poll();
			eyeMap[q.getX()*boardSize + q.getY()] = eye.id;
			eye.size++;
			//setVisited(q, visited);
			for (Point p : new Neighborhood(this, q)) {
				if (getStone(p) == BoardType.EMPTY) {
					if (eyeMap[p.getX()*boardSize + p.getY()] != eye.id) {
						queue.add(p);
					}
				}
				else if (chainMap[p.getX()*boardSize + p.getY()] >= 0) {
					addAdjacency(chains[chainMap[p.getX()*boardSize + p.getY()]], eye);
				}
			}
		}
		//logger.info("addEye: eye=" + eye);
	}

	private void analyzeEye(Eye eye) 
	{
		// if an eye is only adjacent to one group, it can't be false
		if (eye.neighbors.size() == 1)
		{
			eye.real = true;
		}
		BoardType color = null;
		boolean undefined = false;
		for (Cluster cluster : eye.neighbors) {
			Chain chain = (Chain) cluster;
			if (color == null)
				color = chain.color;
			else if (color != chain.color)
				undefined = true;
		}
		Group group = null;
		if (!undefined) {
			eye.color = color;
			group = eye.group;
			
			if (group == null) {
				group = eye.group = new Group();
				groups.add(group);
			}
			group.eyes.add(eye);
			for (Cluster cluster : eye.neighbors) {
				Chain chain = (Chain) cluster;
				group.chains.add(chain);
			}
		}
		
		// logger.info("eye: " + eye + ", group: " + group);
	}
	
	private void analyzeGroup(Group group) {
		// logger.info("analyzeGroup: " + group);
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

	public OldAnalysisGoban clone() 
	{
		OldAnalysisGoban goban = new OldAnalysisGoban(boardSize);
		goban.copy(this);
		return goban;
	}

	private void copy(OldAnalysisGoban goban)
	{
		super.copy(goban);
		this.movingColor = goban.movingColor;
		initAnalysis();
	}
	
	public int getAtariCount(BoardType movingColor) 
	{
		int count = 0;
		//logger.info("Ataricount: " + lastChainId);
		for (int i=0; i<lastChainId; i++) {
			Chain chain = chains[i];
			//logger.info("Ataricount: " + chain);
			if (chain.color == movingColor && chain.numLiberties == 1) {
				//logger.info("Atari: " + movingColor + " " + chain);
				count += chain.size;
			}
		}
		return count;
	}
	
	public Chain getChain(Point p) {
		int id = chainMap[p.getX()*boardSize + p.getY()];
		if (id < 0) return null;
		else return chains[id];
	}

	public Eye getEye(Point p) 
	{
		int id = eyeMap[p.getX()*boardSize + p.getY()];
		if (id < 0) return null;
		else return eyes[id];
	}

	public Vector<Point> getRemoved() 
	{
		return removed;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((movingColor == null) ? 0 : movingColor.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OldAnalysisGoban other = (OldAnalysisGoban) obj;
		if (movingColor != other.movingColor)
			return false;
		if (!super.equals(other))
			return false;
		return true;
	}

	private void initAnalysis() 
	{
		visited++;
		lastChainId = 0;
		lastEyeId = 0;
		chainMap = new int[getBoardSize()*getBoardSize()];
		eyeMap = new int[getBoardSize()*getBoardSize()];
		Arrays.fill(chainMap, -1);
		Arrays.fill(eyeMap, -1);
		chains = new Chain[INITIAL_CHAIN_SIZE];
		eyes = new Eye[INITIAL_CHAIN_SIZE];
		groups = new ArrayList<Group>();
				
		for (int i=0; i<boardSize; i++) {
			for (int j=0; j<boardSize; j++) {
				if (boardRep[i][j] != BoardType.EMPTY && chainMap[i*boardSize+j] < 0) {
					addChain(new Point(i, j));
				}
				if (boardRep[i][j] == BoardType.EMPTY && eyeMap[i*boardSize+j] < 0) {
					addEye(new Point(i, j));
				}
			}
		}
		
		for (int i=0; i < lastEyeId; i++) {
			Eye eye = eyes[i];
			analyzeEye(eye);
		}
		
		for (Group group : groups) {
			analyzeGroup(group);
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
		AbstractGoban goban = (AbstractGoban) clone();
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
	
	public boolean move(int x, int y)
	{
		boolean result = super.move(x, y, movingColor);
		updateChains(x, y, movingColor);
		return result;
	}

	public boolean move(Point p)
	{
		return move(p.getX(), p.getY());
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

	private void setVisited(Point p, int visited) 
	{
		tmpBoard[p.getX()][p.getY()] = visited;
	}

	private void updateChains(int x, int y, BoardType color) 
	{
		// FIXME - this is not efficient
		// logger.info("updateChains " + x + " " + y + " " + getStone(x, y));
		initAnalysis();
	}

}