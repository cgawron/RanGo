package de.cgawron.go.montecarlo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import de.cgawron.go.AbstractGoban;
import de.cgawron.go.Goban;
import de.cgawron.go.Point;
import de.cgawron.go.Symmetry;

public class AnalysisGoban extends AbstractGoban 
{

	/**
	 * A connected chain of stones.
	 *  
	 * @author Christian Gawron
	 *
	 */
	public class Chain extends Cluster
	{
		Set<Point> liberties; 
		
		protected Chain(BoardType color, Point p) {
			super(color);
			liberties = new TreeSet<Point>();
			addPoint(p);
			for (Point q : p.neighbors(AnalysisGoban.this)) {
				addLiberty(q);
			}
		}
		
		protected Chain(Chain parent)
		{
			super(parent);
		}

		public void addLiberty(Point p) {
			if (parent != null) copy();
			liberties.add(p);
		}

		public void addLiberties(Collection<Point> points) {
			if (parent != null) copy();
			liberties.addAll(points);
		}
		
		public Cluster clone()
		{
			return new Chain(this);
		}

		@Override
		protected void copy() {
			if (parent != null) {
				this.liberties = new TreeSet<Point>(((Chain) parent).liberties);
				super.copy();
			}
		}

		public final Set<Point> getLiberties() {
			Set<Point> result;
			if (parent != null)
				result = ((Chain) parent).liberties;
			else
				result = liberties;
			
			return Collections.unmodifiableSet(result);
		}

		public void join(Chain chain) {
			if (parent != null) copy();
			for (Point p : chain.getPoints()) {
				addPoint(p);
				setBoardRep(p, this);
			}
			for (Cluster neighbor : chain.getNeighbors()) {
				addNeighbor(neighbor);
			}
			removeNeighbor(chain);
			clusters.remove(chain);
			liberties.addAll(chain.getLiberties());
		}

		public void removeLiberty(Point p) {
			if (parent != null) copy();
			liberties.remove(p);
		}

		@Override
		public String toString() {
			return toString(false);
		}
		
		public String toString(boolean expand) {
			return "Chain [color=" + getColor() + ", numLiberties=" + getLiberties().size()
					+ ", size=" + size() 
					+ (expand ? ", neighbors=" + getNeighbors().toString() + "]" :  "]");
		}

	}

	/**
	 * A connected cluster of points of same BoardType.
	 * This class uses a "copy on write" approach for updates as one move will usually only affect few clusters (indeed, at maximum four). 
	 *  
	 * @author Christian Gawron
	 *
	 */
	public abstract class Cluster implements Cloneable
	{
		/**
		 * If not null, delegate all operations to the parent. If an operation changes the state of the cluster, it should copy the state of 
		 * the parent and set parent to null. 
		 */
		protected Cluster parent;
		
		private BoardType color;
		private Set<Point> points;
		private Set<Cluster> neighbors;

		private int myHash;
		
		protected Cluster(BoardType color) 
		{
			this.color = color;
			this.points = new TreeSet<Point>();
			this.neighbors = new HashSet<Cluster>();
			myHash = 31 * color.hashCode();
		}

		protected Cluster(BoardType color, Collection<Point> points) {
			this(color);
			this.points.addAll(points);
			myHash = 31 * color.hashCode();
			for (Point p : points) 
				myHash += zobrist[p.getX()*boardSize + p.getY()];
		}

		protected Cluster(Cluster parent) 
		{
			this.parent = parent;
		}

		protected Cluster(BoardType color, Collection<Point> points, Set<Cluster> neighbors) {
			this.color = color;
			this.points = new TreeSet<Point>(points);
			this.neighbors = new HashSet<Cluster>(neighbors);
			myHash = 31 * color.hashCode();
			for (Point p : points) 
				myHash += zobrist[p.getX()*boardSize + p.getY()];
		}

		public void addNeighbor(Cluster cluster)
		{
			if (parent != null)
				copy();
			neighbors.add(cluster);
		}
		
		public void addPoint(Point p) {
			if (parent != null)
				copy();
			points.add(p);
			myHash += zobrist[p.getX()*boardSize + p.getY()];
		}
		
		public abstract Cluster clone();

		protected void copy() 
		{
			if (parent != null) {
				this.color = parent.color;
				this.points = new TreeSet<Point>(parent.points);
				this.neighbors = new HashSet<Cluster>(parent.neighbors);
				parent = null;
			}
		}
		
		public final BoardType getColor() {
			if (parent != null)
				return parent.color;
			else 
				return color;
		}

		public final Set<Cluster> getNeighbors() {
			Set<Cluster> result;
			if (parent != null)
				result = parent.neighbors;
			else
				result = neighbors;
			
			return Collections.unmodifiableSet(result);
		}

		public final Set<Point> getPoints() {
			Set<Point> result;
			if (parent != null)
				result =  parent.points;
			else
				result = points;
			
			return Collections.unmodifiableSet(result);
		}

		protected int size() {
			if (parent != null)
				return parent.points.size();
			else
				return points.size();
		}

		public Point getPoint() {
			return points.iterator().next();
		}

		public void removePoint(Point p) {
			if (parent != null) copy();
			points.remove(p);
			myHash += zobrist[p.getX()*boardSize + p.getY()];
		}

		abstract public String toString(boolean expand);

		public void removeNeighbor(Cluster cluster) {
			if (parent != null)
				copy();
			neighbors.remove(cluster);
		}

		public void clearNeighbors() {
			neighbors.clear();	
		}

		/*
		@Override
		public int hashCode() {
			if (parent != null)
				return parent.myHash;
			else
				return myHash;
		}
  	    */

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Cluster))
				return false;
			Cluster other = (Cluster) obj;
			logger.info("equals: " + hashCode() + " " + other.hashCode());
			return other.hashCode() == hashCode();
		}

	}
	
	public class Eye extends Cluster  
	{
		/** This eye is definitely a real eye for a group */
		public boolean real;
		public Group group;
		
		public Eye() 
		{
			super(BoardType.EMPTY);
		}

		protected Eye(Eye parent)
		{
			super(parent);
		}
		
		public Eye(Collection<Point> points) {
			super(BoardType.EMPTY, points);
		}

		public Eye(Collection<Point> points, Set<Cluster> neighbors) {
			super(BoardType.EMPTY, points, neighbors);
		}
		
		public Cluster clone()
		{
			return new Eye(this);
		}

		public boolean isVitalPoint(Point move) {
			// FIXME
			return true;
		}

		@Override
		public String toString() {
			return toString(false);
		}
		
		
		public String toString(boolean expand) {
			return "Eye [size=" + size() + ", real=" + real + ", color=" + getColor() 
					+ (expand ? ", neighbors=" + getNeighbors().toString() + "]" :  "]");
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
		
		public boolean isAlive() {
			if (eyes.size() >= 2)
				return true;
			else 
				return false;
		}

		@Override
		public String toString() {
			return "Group [chains=" + chains + ", eyes=" + eyes + "]";
		}
	}

	protected Set<Cluster> clusters;
	Cluster[] boardRep;
	private int _hash;
	private Collection<Point> allPoints;

	public AnalysisGoban() {
		clusters = new HashSet<Cluster>();
	}
	
	public AnalysisGoban(Goban goban) {
		this();
		copy(goban);
	}

	public AnalysisGoban(int boardSize) {
		this();
		this.boardSize = boardSize;
		initBoard();
	}

	@Override
	public int chineseScore(double[][] territory) 
	{
		/*
			if (logger.isLoggable(Level.INFO))
				logger.info("chineseScore: \n" + this);
		 */
		int score = 0;
		
		for (Cluster cluster : clusters) {
			switch (cluster.getColor()) {
			case BLACK:
				score += cluster.size();
				break;
				
			case WHITE:
				score -= cluster.size();
				break;

			case EMPTY:
				score += scoreEmpty(cluster);
				break;
			}
		}
		
		if (territory != null) {
			for (Cluster cluster : clusters) {
				double v = 0;
				switch (cluster.color) {
				case BLACK:
					v = cluster.size();
					break;
					
				case WHITE:
					v = -cluster.size();
					break;

				case EMPTY:
					v = Math.signum(scoreEmpty(cluster)) * cluster.size();
					break;
				}
				
				for (Point p : cluster.getPoints()) {
					territory[p.getX()][p.getY()] = v;
				}
			}
		}
		
		return score;
	}

	@Override
	public void clear() {
		
	}
	
	@Override
	public AnalysisGoban clone() {
		AnalysisGoban model = new AnalysisGoban();
		model.copy(this);
		return model;
	}

	@Override
	public void copy(Goban m) {
		this.boardSize = m.getBoardSize();
		if (m instanceof AnalysisGoban) {
			// TODO copy also _hash, but that means that move() etc. need to update _hash
			this.boardRep = new Cluster[boardSize*boardSize];
			for (int i=0; i<boardSize*boardSize; i++) {
				boardRep[i] = ((AnalysisGoban) m).boardRep[i].clone();
				clusters.add(boardRep[i]);
			}
		}
		else {
			init(m);
		}
	}

	public int getAtariCount(BoardType movingColor) {
		int atariCount = 0;
		for (Cluster c : clusters) {
			if (c.getColor() == movingColor) {
				Chain chain = (Chain) c;
				if (chain.getLiberties().size() == 1) {
					atariCount += chain.size();
					logger.info("atariCount: " + chain.toString(true) + " is atari");
				}
			}
		}
		logger.info("atariCount([" + this + "]): " + atariCount);
		return atariCount;
	}

	final Cluster getBoardRep(int x, int y) {
		return boardRep[x*boardSize+y];
	}

	final Cluster getBoardRep(Point p) {
		return boardRep[p.getX()*boardSize+p.getY()];
	}

	public Chain getChain(Point p) {
		return (Chain) boardRep[p.getX()*boardSize + p.getY()];
	}
	
	public Eye getEye(Point p) {
		return (Eye) boardRep[p.getX()*boardSize + p.getY()];
	}

	@Override
	public BoardType getStone(int x, int y) {
		return getBoardRep(x, y).getColor();
	}

	private void init(Goban m) {
		this.boardSize = m.getBoardSize();
		initBoard();
		
		for (int x=0; x<boardSize; x++) {
			for (int y=0; y<boardSize; y++) {
				BoardType c = m.getStone(x, y);
				if (c != BoardType.EMPTY) putStone(x, y, c);
			}
		}
	}
	
	
	private void initBoard() {
		allPoints = new ArrayList<Point>(boardSize*boardSize);
		for (Point p : Point.all(boardSize)) {
			allPoints.add(p);
		}
		boardRep = new Cluster[boardSize*boardSize];
		Cluster empty = new Eye(allPoints);
		Arrays.fill(boardRep, empty);
	}

	public boolean isOneSpaceEye(Point p) {
		Cluster c = getBoardRep(p);
		return c.getColor() == BoardType.EMPTY && c.getPoints().size() == 1;
	}
	
	@Override
	public boolean move(int x, int y, BoardType color) {
		return move(new Point(x, y), color);
	}

	@Override
	public boolean move(Point p, BoardType color) {
		Cluster stone = getBoardRep(p);
		if (stone.getColor() != BoardType.EMPTY)
			return false;
		else
			stone.removePoint(p);
		lastMove = p;	
		
		
		List<Point> emptyNeighbors = new ArrayList<Point>(4);
		List<Chain> friendlyNeighbors = new ArrayList<Chain>(4);
		List<Point> enemyNeighbors = new ArrayList<Point>(4);
		
		for (Point q : p.neighbors(this)) {
			Cluster cluster = getBoardRep(q);
			if (cluster.getColor() == BoardType.EMPTY) { 
				emptyNeighbors.add(q);
			}
			else if (cluster.getColor() == color) {
				if (!friendlyNeighbors.contains(cluster)) friendlyNeighbors.add((Chain) cluster);
			}
			else {
				enemyNeighbors.add(q);
			}
		}
		
		// place stone
		Chain myChain = null;
		if (friendlyNeighbors.size() == 0) {
			// create new Chain
			myChain = new Chain(color, p);
			setBoardRep(p, myChain);
		}
		else {
			myChain = friendlyNeighbors.remove(0);
			myChain.addPoint(p);
			myChain.addLiberties(emptyNeighbors);
			setBoardRep(p, myChain);
			for (Chain chain : friendlyNeighbors) {
				myChain.join(chain);
			}
		}
		
		// there is no liberty on p any more
		myChain.removeLiberty(p);

		for (Point q : enemyNeighbors) {
			Chain chain = (Chain) getBoardRep(q);
			chain.removeLiberty(p);
			if (chain.liberties.size() == 0) {
				myChain.addNeighbor(removeChain(chain));
			}
			else {
				myChain.removeLiberty(q);
				myChain.addNeighbor(chain);
				chain.addNeighbor(myChain);
			}
		}
		
		if (emptyNeighbors.size() + friendlyNeighbors.size() + enemyNeighbors.size() < 4) {
			for (Point q : emptyNeighbors) {
				Cluster eye = getBoardRep(q);
				Collection<Cluster> newEyes = checkIfPartioned(eye, p);
				for (Cluster newEye : newEyes) {
					myChain.addNeighbor(newEye);
				}
			}
		}
		
		return true;
	}

	@Override
	public Goban newInstance() {
		return new AnalysisGoban(getBoardSize());
	}

	@Override
	public void putStone(int x, int y, BoardType color) {
		putStone(new Point(x, y), color);
	}
	
	@Override
	public void putStone(Point p, BoardType color) {
		Cluster stone = getBoardRep(p);
		stone.removePoint(p);
		
		List<Point> emptyNeighbors = new ArrayList<Point>(4);
		List<Chain> friendlyNeighbors = new ArrayList<Chain>(4);
		List<Point> enemyNeighbors = new ArrayList<Point>(4);
		
		for (Point q : p.neighbors(this)) {
			Cluster cluster = getBoardRep(q);
			if (cluster.getColor() == BoardType.EMPTY) { 
				emptyNeighbors.add(q);
			}
			else if (cluster.getColor() == color) {
				if (!friendlyNeighbors.contains(cluster)) friendlyNeighbors.add((Chain) cluster);
			}
			else {
				enemyNeighbors.add(q);
			}
		}
		
		// place stone
		Chain myChain = null;
		if (friendlyNeighbors.size() == 0) {
			// create new Chain
			myChain = new Chain(color, p);
			setBoardRep(p, myChain);
		}
		else {
			myChain = friendlyNeighbors.remove(0);
			myChain.addPoint(p);
			setBoardRep(p, myChain);
			for (Chain chain : friendlyNeighbors) {
				myChain.join(chain);
			}
		}
		
		// there is no liberty on p any more
		myChain.removeLiberty(p);
		
		for (Point q : enemyNeighbors) {
			Chain chain = (Chain) getBoardRep(q);
			chain.removeLiberty(p);
			myChain.removeLiberty(q);
			myChain.addNeighbor(chain);
			chain.addNeighbor(myChain);
		}
		
		// if (emptyNeighbors.size() + friendlyNeighbors.size() + enemyNeighbors.size() < 4) {
			for (Point q : emptyNeighbors) {	
				Cluster eye = getBoardRep(q);
				Collection<Cluster> newEyes = checkIfPartioned(eye, p);
				for (Cluster newEye : newEyes) {
					myChain.addNeighbor(newEye);
				}
			}
		// }
		
		logger.info(p + "empty neighbors: " + emptyNeighbors + ", myChain: " + myChain);
		myChain.addLiberties(emptyNeighbors);
	}

	private Collection<Cluster> checkIfPartioned(Cluster cluster, Point p) {
		Collection<Cluster> newEyes = new ArrayList<Cluster>(4);
		Collection<Point> points = new HashSet<Point>();
		Queue<Point> queue = new PriorityQueue<Point>();
		Set<Cluster> neighbors; 
		
		newEyes.add(cluster);
		if (cluster.parent != null)
			cluster.copy();
		do {
			neighbors = new HashSet<Cluster>();
			points.clear();
			if (cluster.getPoints().size() > 0)
				queue.add(cluster.getPoint());
			while (queue.size() > 0) {
				Point q = queue.remove();
				points.add(q);
				for (Point r : q.neighbors(this)) {
					if (cluster.getPoints().contains(r) && !points.contains(r) && !queue.contains(r)) queue.add(r);
					else {
						Cluster neighbor = getBoardRep(r);
						if (neighbor.getColor() != BoardType.EMPTY) {
							neighbors.add(neighbor);
						}
					}
				}
			}
			if (points.size() != cluster.getPoints().size()) {
				cluster.copy();
				cluster.points.removeAll(points);
				Cluster empty = new Eye(points, neighbors);
				newEyes.add(empty);
				for (Point r : points) {
					setBoardRep(r, empty);
				}
			}
			else {
				cluster.neighbors = neighbors;
			}
		} while (points.size() != cluster.getPoints().size());
		
		return newEyes;
	}

	private Cluster removeChain(Chain chain) {
		Eye eye = new Eye(chain.getPoints(), chain.getNeighbors());
		for (Point p : chain.getPoints()) {
			removed.add(p);
			setBoardRep(p, eye);
			for (Point q : p.neighbors(this)) {
				Cluster cluster = getBoardRep(q);
				cluster.removeNeighbor(chain);
				if (cluster.getColor() == chain.getColor().opposite()) {
					Chain other = (Chain) cluster;
					other.addLiberty(q);
				}
			}
		}
		addCaptureStones(chain.getColor(), chain.size());
		clusters.remove(chain);
		return eye;
	}

	private int scoreEmpty(Cluster eye) {
		boolean touchBlack = false;
		boolean touchWhite = false;
		
		for (Cluster c : eye.neighbors) {
			if (c.getColor() == BoardType.BLACK) touchBlack = true;
			if (c.getColor() == BoardType.WHITE) touchWhite = true;
		}
		if (touchWhite && touchBlack) return 0;
		else if (touchBlack) return  eye.getPoints().size();
		else if (touchWhite) return -eye.getPoints().size();
		else return 0;
	}

	private final void setBoardRep(Point p, Cluster rep) {
		boardRep[p.getX()*boardSize+p.getY()] = rep;
	}

	@Override
	public void setBoardSize(int size) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public Goban transform(Symmetry s) {
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

	@Override
	public
	int zobristHash() {
		if (_hash == 0) {
			int n = 0;

			for (int i=0; i<boardSize*boardSize; i++) {
				BoardType stone = boardRep[i].getColor();
				if (stone != BoardType.EMPTY) {
					n++;
					if (stone == BoardType.BLACK)
						_hash += zobrist[i];
					else
						_hash -= zobrist[i];
				}
			}
			_hash = (_hash & 0x01ffffff) | ((n & 0xfe) << (32 - 7));
		}
		return _hash;
	}	
}
