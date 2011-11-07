package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertTrue;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

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
			super(color, p);
			liberties = new TreeSet<Point>();
			for (Point q : p.neighbors(boardSize)) {
				addLiberty(q);
			}
		}
		
		protected Chain(BoardType color, Point p, Collection<Point> libs) {
			super(color, p);
			liberties = new TreeSet<Point>(libs);
		}
		
		protected Chain(Chain parent)
		{
			super(parent);
			assert ((Chain) (this.parent)).liberties != null;
		}

		public void addLiberty(Point p) {
			if (parent != null) copy();
			liberties.add(p);
		}

		public void addLiberties(Collection<Point> points) {
			if (parent != null) copy();
			liberties.addAll(points);
		}
		
		public Cluster clone(AnalysisGoban goban)
		{
			return goban.new Chain(this);
		}

		@Override
		protected void copy() {
			if (parent != null) {
				liberties = new TreeSet<Point>(((Chain) parent).liberties);
				super.copy();
			}
		}

		public final Set<Point> getLiberties() {
			Set<Point> result;
			if (parent != null)
				result = ((Chain) parent).liberties;
			else
				result = liberties;
			
			assert result != null;

			return Collections.unmodifiableSet(result);
		}

		public void join(AnalysisGoban goban, Chain chain) {
			if (parent != null) copy();
			for (Point p : chain.getPoints()) {
				addPoint(p);
				goban.setBoardRep(p, this);
			}
			for (Cluster neighbor : chain.getNeighbors()) {
				neighbor.removeNeighbor((Cluster) chain);
				neighbor.addNeighbor(this);
				addNeighbor(neighbor);
			}
			removeNeighbor(chain);
			liberties.addAll(chain.getLiberties());
			goban.clusters.remove(chain);
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
					+ ", size=" + size() + ", rep=" + getRep()
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
	public abstract class Cluster
	{
		private class NeighborCollection extends AbstractCollection<Cluster> {
			
			@Override
			public boolean contains(Object obj) {
				Cluster cluster = (Cluster) obj;
				return getNeighborReps().contains(cluster.getRep());
			}

			@Override
			public Iterator<Cluster> iterator() {
				return new Iterator<Cluster>() {
					Iterator<Point> it = getNeighborReps().iterator();
					
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public Cluster next() {
						return getBoardRep(it.next());
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
					
				};
			}

			@Override
			public int size() {
				return getNeighborReps().size();	
			}
			
		}
		
		/**
		 * If not null, delegate all operations to the parent. If an operation changes the state of the cluster, it should copy the state of 
		 * the parent and set parent to null. 
		 */
		protected Cluster parent;
		protected Group group;
		protected int boardSize;
		
		private Point rep;
		private BoardType color;
		private Set<Point> points;
		private Set<Point> neighbors;
		private NeighborCollection neighborCollection;

		protected Cluster(Cluster parent) 
		{
			while (parent.parent != null) parent = parent.parent;
			this.parent = parent;
		}

		protected Cluster(BoardType color, List<Point> points) {
			this.color = color;
			this.neighbors = new TreeSet<Point>();
			this.points = new TreeSet<Point>(points);
			this.rep = points.get(0);
		}

		protected Cluster(BoardType color, List<Point> points, Set<Point> neighbors) {
			this.color = color;
			this.points = new TreeSet<Point>(points);
			this.neighbors = new TreeSet<Point>(neighbors);
			this.rep = points.get(0);
		}

		protected Cluster(BoardType color, Set<Point> points) {
			this.color = color;
			this.neighbors = new TreeSet<Point>();
			this.points = new TreeSet<Point>(points);
			this.rep = points.iterator().next();
		}

		protected Cluster(BoardType color, Set<Point> points, Set<Point> neighbors) {
			this.color = color;
			this.points = new TreeSet<Point>(points);
			this.neighbors = new TreeSet<Point>(neighbors);
			this.rep = points.iterator().next();
		}
		
		public Cluster(BoardType color, Point p) {
			this.color = color;
			this.points = new TreeSet<Point>();
			this.points.add(p);
			this.rep = p;
			this.neighbors = new TreeSet<Point>();
		}

		public void addNeighbor(Cluster cluster)
		{
			if (parent != null)
				copy();
			neighbors.add(cluster.getRep());
		}
		
		public void addPoint(Point p) {
			if (parent != null)
				copy();
			points.add(p);
		}
		
		public abstract Cluster clone(AnalysisGoban goban);
		
		protected void copy() 
		{
			if (parent != null) {
				this.color = parent.color;
				this.points = new TreeSet<Point>(parent.points);
				this.neighbors = new TreeSet<Point>(parent.neighbors);
				this.rep = parent.rep;
				parent = null;
			}
		}
		
		public final BoardType getColor() {
			if (parent != null)
				return parent.color;
			else 
				return color;
		}

		public final Collection<Cluster> getNeighbors() {
			if (neighborCollection == null) neighborCollection = new NeighborCollection();
			return neighborCollection;
		}

		private final Set<Point> getNeighborReps() {
			if (parent != null) return parent.neighbors;
			else return neighbors;
		}
		
		public final Set<Point> getPoints() {
			Set<Point> result;
			if (parent != null)
				result =  parent.points;
			else
				result = points;
			
			return Collections.unmodifiableSet(result);
		}
		
		protected final Point getRep() {
			if (parent != null)
				return parent.rep;
			else
				return rep;
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

		public void removePoint(AnalysisGoban goban, Point p) {
			if (parent != null) copy();
			points.remove(p);
			if (points.size() == 0) {
				goban.clusters.remove(this);
				for (Point q : neighbors) {
					Cluster c = goban.getBoardRep(q);
					c.removeNeighbor(this);
				}
			}
			else if (rep.equals(p)) {
				updateRep(goban);
			}
		}

		private void updateNeighbor(Point oldRep, Point newRep) 
		{
			if (parent != null) copy();
			neighbors.remove(oldRep);
			neighbors.add(newRep);
		}

		abstract public String toString(boolean expand);

		public void removeNeighbor(Cluster cluster) {
			if (parent != null)
				copy();
			neighbors.remove(cluster.getRep());
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
			else
				return false;
		}

		public void updateRep(AnalysisGoban goban) {
			Point oldRep = rep;
			rep = points.iterator().next();
			for (Point q : neighbors) {
				Cluster c = goban.getBoardRep(q);
				c.updateNeighbor(oldRep, rep);
			}
		}

	}
	
	public class Eye extends Cluster  
	{
		/** This eye is definitely a real eye for a group */
		private boolean real;
		private BoardType eyeColor;
		
		protected Eye(Eye parent)
		{
			super(parent);
		}
		
		public Eye(List<Point> points) {
			super(BoardType.EMPTY, points);
		}

		public Eye(List<Point> points, Set<Point> neighbors) {
			super(BoardType.EMPTY, points, neighbors);
			initialAnalysis();
		}
		
		public Eye(Set<Point> points) {
			super(BoardType.EMPTY, points);
			initialAnalysis();
		}

		public Eye(Set<Point> points, Set<Point> neighbors) {
			super(BoardType.EMPTY, points, neighbors);
			initialAnalysis();
		}
		
		private void initialAnalysis() 
		{
			eyeColor = null;
			boolean touchWhite = false;
			boolean touchBlack = false;
			for (Cluster n : getNeighbors()) {
				if (n.getColor() == BoardType.WHITE) touchWhite = true;
				else if (n.getColor() == BoardType.BLACK) touchBlack = true;
			}
			
			if (touchBlack && touchWhite) {
				group = null;
			}
			else if (touchWhite) {
				eyeColor = BoardType.WHITE;	
			}
			else if (touchBlack) {
				eyeColor = BoardType.BLACK;
			}
			if (eyeColor != null) {
				group = new Group(this, getNeighbors(), eyeColor);
			}
				
		}

		public final BoardType getEyeColor() {
			if (parent != null)
				return ((Eye) parent).eyeColor;
			else
				return eyeColor;
		}

		protected boolean isReal() {
			return real;
		}

		protected void copy() 
		{
			super.copy();
			initialAnalysis();
		}
		
		public Cluster clone(AnalysisGoban goban)
		{
			return goban.new Eye(this);
		}

		protected Group getGroup() 
		{
			if (parent != null)
				return ((Eye) parent).group;
			else
				return group;
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
			return "Eye [size=" + size() + ", real=" + real + ", color=" + getColor() + ", rep=" + getRep()
					+ (expand ? ", neighbors=" + getNeighbors().toString() + "]" :  "]");
		}

	}
	
	public static class Group 
	{
		BoardType color;
		Set<Point> chains;
		Set<Eye> eyes;
		
		public Group(Eye eye, Collection<Cluster> chains, BoardType color)
		{
			this.chains = new TreeSet<Point>();
			eyes = new TreeSet<Eye>();
			eyes.add(eye);
			this.color = color;
			for (Cluster c : chains) {
				this.chains.add(c.getRep());
			}
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
	private List<Point> allPoints;

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
			logger.info("chineseScore: \n" + this + ", clusters: " + clusters);
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
				switch (cluster.getColor()) {
				case BLACK:
					v = 1;
					break;
					
				case WHITE:
					v = -1;
					break;

				case EMPTY:
					v = Math.signum(scoreEmpty(cluster));
					break;
				}
				
				for (Point p : cluster.getPoints()) {
					territory[p.getX()][p.getY()] += v;
				}
			}
		}
		
		return score;
	}
	
	/** 
	 * Check some important invariants. 
	 */
	boolean checkGoban() {
		for (Cluster c : clusters) {
			assert c.size() > 0 : "cluster has size 0";
			assert c.getPoints().contains(c.getRep()) : "rep does not belong to cluster";
			assert getBoardRep(c.getRep()) == c : "rep does not represent the cluster";
			for (Point p : c.getPoints()) {
				assert getBoardRep(p) == c : "check cluster " + c + " vs. boardRep at " + p;
			}
			for (Cluster n : c.getNeighbors()) {
				assert c.getColor() != n.getColor() : "check that neighbors are of different color";
				assert n.getNeighbors().contains(c) : "asymmetric neighborship of " + c + " and " + n;
				assert clusters.contains(n) : "check that neighbor " + n.toString(true) + " of cluster " + c.toString(true) + " is a valid cluster";
			}
			if (c instanceof Chain) {
				Chain chain = (Chain) c;
				for (Point p : chain.getLiberties()) {
					Cluster d = getBoardRep(p);
					assert d.getColor() == BoardType.EMPTY : "chain " + chain + ": bogus liberty at " + p;
					assert c.getNeighbors().contains(d) : "chain " + chain + ": liberty at " + p + ", but no corresponding neighbor";
				}
			}
		}
		for (Point p : allPoints) {
			assert clusters.contains(getBoardRep(p)) : "cluster missing in clusters";
		}
		return true;
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
			AnalysisGoban goban = (AnalysisGoban) m;
			assert goban.checkGoban() : "copy called with inconsistent goban";
			this.allPoints = goban.allPoints;
			Map<Cluster, Cluster> clones = new HashMap<Cluster, Cluster>();
			// TODO copy also _hash, but that means that move() etc. need to update _hash
			this.boardRep = new Cluster[boardSize*boardSize];
			for (Cluster c : goban.clusters) {
				Cluster nc = c.clone(this);
				clones.put(c, nc);
				clusters.add(nc);
				for (Point p : c.getPoints()) {
					setBoardRep(p, nc);
				}
			}
			assert checkGoban() : "goban inconsistent after copy";
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
					//logger.info("atariCount: " + chain.toString(true) + " is atari");
				}
			}
		}
		//logger.info("atariCount([" + this + "]): " + atariCount);
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
		clusters.add(empty);
		Arrays.fill(boardRep, empty);
	}

	public boolean isOneSpaceEye(Point p) {
		Cluster c = getBoardRep(p);
		if (!(c instanceof Eye)) return false;
		Eye e = (Eye) c;
		return e.getEyeColor() != null && c.size() == 1;
	}
	
	@Override
	public boolean move(int x, int y, BoardType color) {
		return move(new Point(x, y), color);
	}

	@Override
	public boolean move(Point p, BoardType color) {
		Cluster stone = getBoardRep(p);
		
		// don't move ontop another stone
		if (stone.getColor() != BoardType.EMPTY)
			return false;
		else
			stone.removePoint(this, p);
		lastMove = p;	
		
		
		List<Point> emptyNeighbors = new ArrayList<Point>(4);
		List<Chain> friendlyNeighbors = new ArrayList<Chain>(4);
		List<Chain> enemyNeighbors = new ArrayList<Chain>(4);
		
		for (Point q : p.neighbors(this)) {
			Cluster cluster = getBoardRep(q);
			if (cluster.getColor() == BoardType.EMPTY) { 
				emptyNeighbors.add(q);
			}
			else if (cluster.getColor() == color) {
				if (!friendlyNeighbors.contains(cluster)) friendlyNeighbors.add((Chain) cluster);
			}
			else {
				if (!enemyNeighbors.contains(cluster)) enemyNeighbors.add((Chain) cluster);
			}
		}
		
		// place stone
		Chain myChain = null;
		if (friendlyNeighbors.size() == 0) {
			// create new Chain
			myChain = new Chain(color, p, emptyNeighbors);
			clusters.add(myChain);
			setBoardRep(p, myChain);
		}
		else {
			myChain = friendlyNeighbors.remove(0);
			myChain.addPoint(p);
			myChain.addLiberties(emptyNeighbors);
			setBoardRep(p, myChain);
			for (Chain chain : friendlyNeighbors) {
				myChain.join(this, chain);
			}
		}
		
		for (Cluster cluster : enemyNeighbors) {
			if (!(cluster instanceof Chain)) logger.info("p: " + p + "\ncluster: " + cluster.toString(true) + "\npoints: " + cluster.getPoints());
			Chain chain = (Chain) cluster;
			chain.removeLiberty(p);
			if (chain.liberties.size() == 0) {
				myChain.addNeighbor(removeChain(chain));
			}
			else {
				//myChain.removeLiberty(q);
				myChain.addNeighbor(chain);
				chain.addNeighbor(myChain);
			}
		}

		// there is no liberty on p any more
		myChain.removeLiberty(p);
		
		for (Point q : emptyNeighbors) {
			Cluster eye = getBoardRep(q);
			eye.addNeighbor(myChain);
			myChain.addNeighbor(eye);
			// Does not work for points at the edge!!
			//if (emptyNeighbors.size() + friendlyNeighbors.size() + enemyNeighbors.size() < 4) {
				Collection<Cluster> newEyes = checkIfPartioned(eye, p);
				for (Cluster newEye : newEyes) {
					myChain.addNeighbor(newEye);
				}
			//}
		}

		// suicide is not allowed
		if (myChain.getLiberties().size() == 0) {
			removeChain(myChain);
			return false;
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
		stone.removePoint(this, p);
		
		List<Point> emptyNeighbors = new ArrayList<Point>(4);
		List<Chain> friendlyNeighbors = new ArrayList<Chain>(4);
		List<Chain> enemyNeighbors = new ArrayList<Chain>(4);
		
		for (Point q : p.neighbors(this)) {
			Cluster cluster = getBoardRep(q);
			if (cluster.getColor() == BoardType.EMPTY) { 
				emptyNeighbors.add(q);
			}
			else if (cluster.getColor() == color) {
				if (!friendlyNeighbors.contains(cluster)) friendlyNeighbors.add((Chain) cluster);
			}
			else {
				if (!enemyNeighbors.contains(cluster)) enemyNeighbors.add((Chain) cluster);
			}
		}
		
		// place stone
		Chain myChain = null;
		if (friendlyNeighbors.size() == 0) {
			// create new Chain
			myChain = new Chain(color, p, emptyNeighbors);
			clusters.add(myChain);
			setBoardRep(p, myChain);
		}
		else {
			myChain = friendlyNeighbors.remove(0);
			myChain.addPoint(p);
			setBoardRep(p, myChain);
			for (Chain chain : friendlyNeighbors) {
				myChain.join(this, chain);
			}
		}
		
		// there is no liberty on p any more
		myChain.removeLiberty(p);
		
		for (Cluster cluster : enemyNeighbors) {
			Chain chain = (Chain) cluster;
			chain.removeLiberty(p);
			// myChain.removeLiberty(q);
			myChain.addNeighbor(chain);
			chain.addNeighbor(myChain);
		}
		
		if (emptyNeighbors.size() + friendlyNeighbors.size() + enemyNeighbors.size() < 4) {
			for (Point q : emptyNeighbors) {	
				Cluster eye = getBoardRep(q);
				eye.addNeighbor(myChain);
				Collection<Cluster> newEyes = checkIfPartioned(eye, p);
				for (Cluster newEye : newEyes) {
					myChain.addNeighbor(newEye);
				}
			}
		}
		
		myChain.addLiberties(emptyNeighbors);
	}

	private Collection<Cluster> checkIfPartioned(Cluster cluster, Point p) {
		Collection<Cluster> newEyes = new ArrayList<Cluster>(4);
	    Set<Point> points = new HashSet<Point>();
		Queue<Point> queue = new PriorityQueue<Point>();
		Set<Point> neighbors; 
		
		//newEyes.add(cluster);
		if (cluster.parent != null)
			cluster.copy();
		do {
			neighbors = new TreeSet<Point>();
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
							neighbors.add(neighbor.getRep());
						}
					}
				}
			}
			if (points.size() != cluster.getPoints().size()) {
				cluster.copy();
				cluster.points.removeAll(points);
				if (!cluster.points.contains(cluster.rep)) {
					cluster.updateRep(this);
				}
				Cluster empty = new Eye(points, neighbors);
				clusters.add(empty);
				for (Point r : neighbors) {
					Cluster n = getBoardRep(r);
					n.addNeighbor(empty);
				}
				newEyes.add(empty);
				for (Point r : points) {
					setBoardRep(r, empty);
				}
			}
			else {
				cluster.neighbors = neighbors;
				for (Cluster c : clusters) {
					if (!neighbors.contains(c.getRep())) 
						c.removeNeighbor(cluster);
				}
			}
		} while (points.size() != cluster.getPoints().size());
		
		return newEyes;
	}

	private Cluster removeChain(Chain chain) {
		Eye eye = new Eye(chain.getPoints());
		for (Cluster cluster : chain.getNeighbors()) {
			cluster.removeNeighbor(chain);
		}
		for (Point p : chain.getPoints()) {
			removed.add(p);
			setBoardRep(p, eye);
			for (Point q : p.neighbors(this)) {
				Cluster cluster = getBoardRep(q);
				if (cluster.getColor() == chain.getColor().opposite()) {
					Chain other = (Chain) cluster;
					other.addLiberty(p);
					other.addNeighbor(eye);
					eye.addNeighbor(other);
				}
			}
		}

		addCaptureStones(chain.getColor(), chain.size());
		clusters.remove(chain);
		clusters.add(eye);
		return eye;
	}

	private int scoreEmpty(Cluster eye) {
		// logger.info("scoreEmpty: " + eye.toString(true));
		boolean touchBlack = false;
		boolean touchWhite = false;
		
		for (Cluster c : eye.getNeighbors()) {
			if (c.getColor() == BoardType.BLACK) touchBlack = true;
			if (c.getColor() == BoardType.WHITE) touchWhite = true;
		}
		if (touchWhite && touchBlack) return 0;
		else if (touchBlack) return  eye.getPoints().size();
		else if (touchWhite) return -eye.getPoints().size();
		else return 0;
	}

	private final void setBoardRep(Point p, Cluster rep) {
		assert rep != null : "setBoardRep called with null rep!";
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
