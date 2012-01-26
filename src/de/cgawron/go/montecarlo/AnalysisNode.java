package de.cgawron.go.montecarlo;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.GobanMap;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;
import de.cgawron.go.montecarlo.AnalysisGoban.Group;

/** 
 * A node in the analysis tree. It contains the move, the @code{AnalysisGoban} and data needed for the UCT algorithm.
 * 
 * @author Christian Gawron
 *
 */
class AnalysisNode implements Comparable<AnalysisNode>
{
	static Logger logger = Logger.getLogger(AnalysisNode.class.getName());	
	
	private int visits = 0;
	int moveNo;
	int whiteAtari = -1;
	int blackAtari = -1;
	int boardSize;

	double depth;
	double komi;
	private double score;
	private double score2;
	double suitability;
	double value;

	AnalysisNode parent;
	AnalysisGoban goban;
	Point move;

	Set<AnalysisNode> children;
	Map<Point, Miai> miaiMap;

	BoardType movingColor;
	
	public AnalysisNode(AnalysisNode analysisNode) 
	{
		this.parent = analysisNode;
		this.boardSize = analysisNode.boardSize;
		this.goban = analysisNode.goban.clone();
		this.moveNo = analysisNode.moveNo;
		this.komi = analysisNode.komi;
		this.miaiMap = analysisNode.miaiMap;
	}
	
	public AnalysisNode(Goban goban, BoardType movingColor)
	{
		this(goban, movingColor, 6.5);
	}
	
	public AnalysisNode(Goban goban, BoardType movingColor, double komi) 
	{
		//this.goban = new OldAnalysisGoban(goban, movingColor);
		this.goban = new AnalysisGoban(goban);
		this.movingColor = movingColor;
		this.boardSize = goban.getBoardSize();
		this.komi = komi;
		this.miaiMap = new GobanMap<Miai>(this.boardSize);
		this.moveNo = 0;
		
		initializeMiai();
	}
	
	public void addMiai(Miai miai)
	{
		miaiMap.put(miai.p1, miai);
		miaiMap.put(miai.p2, miai);
	}

		
	/**
	 * Calculate the suitability of a move. 
	 * The suitability will be used as a probability when choosing a move.
	 * @return the suitability of this @code{AnalysisNode}
	 */
	double calculateStaticSuitability() 
	{
		double suitability = 1;
		
		// Only play on empty fields
		if (parent.goban.getStone(move) != BoardType.EMPTY) return 0;
		
		// Don't play Suicide
		if (goban.getStone(move) == BoardType.EMPTY) return 0;
					
		Vector<Point> removed = goban.getRemoved();
		int saved = getSavedStones();

		// don't fill an eye
		if (parent.goban.isOneSpaceEye(move) && removed.size() == 0 && saved <= 0) return 0;
		
		// Capture is good
		if (removed.size() > 0) {
			//logger.info("Capturing: " + move + " captures " + removed.size());
			suitability += 2 * removed.size();
		}
					
		// Saving stones is good
		if (saved > 0) {
			//logger.info("Saving: " + move + " saves " + saved);
			suitability += 2*saved;
		}

		Miai miai = null;
		if (parent.move != null) miai = miaiMap.get(parent.move);
		if (miai != null && miai.other(parent.move).equals(move)) {
			//logger.info("miai found!!" + miai);
			suitability += miai.value;
		}
					
		// Self-Atari is discouraged
		if (saved < 0) 
			suitability *= -1.0 / saved;
		
		
		Eye eye = parent.goban.getEye(move);
		Group group = eye.getGroup();
		if (group != null)
		{
			if (group.isAlive()) {
				if (eye.getPoints().size() > 1 && eye.getPoints().size() < 7) 
					return 0;
				else
					return suitability /= eye.getPoints().size();
			}
			else {
				// FIXME
				if (eye.getPoints().size() < 7 && eye.isVitalPoint(move))
					suitability *= 5;
			}
		}
			
		return suitability;
	}

	/**
	 * Compare two AnalysisNode by value.
	 */
	@Override
	public int compareTo(AnalysisNode node) {
		return move.compareTo(node.move);
	}

	protected AnalysisNode createChild() 
	{
		AnalysisNode child = new AnalysisNode(this);
		child.moveNo = moveNo + 1;
		child.parent = this;
		child.movingColor = movingColor.opposite();
		return child;
	}

	public AnalysisNode createChild(Point p) 
	{
		AnalysisNode child = createChild();
		//child.goban.checkGoban();
		child.move = p;
		child.goban.move(p, movingColor);
		//logger.info("createChild: " + p + "\n[" + goban + "]\n[" + child.goban + "]");
		updateMiai();
		child.suitability = child.calculateStaticSuitability();

		return child;
	}
	
	public AnalysisNode createPassNode() 
	{
		//logger.info("creating pass move");
		AnalysisNode child = createChild();
		child.move = null;
		child.movingColor = movingColor.opposite();
		return child;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisNode other = (AnalysisNode) obj;
		if (!goban.equals(other.goban))
			return false;
		return true;
	}
	
	public double evaluateByMC(AnalysisNode[] sequence, int n, double[][] territory)
	{
		AnalysisNode currentNode = this; 
		
		int i = n;
		while (true) {
			sequence[++i] = currentNode = currentNode.selectRandomMCMove();
			if (currentNode.isPass() && currentNode.parent.isPass()) {
				break;
			}
			
			if (currentNode.moveNo >= Evaluator.parameters.maxMoves) {
				throw new RuntimeException("no result");
			}
		} 
		//logger.info("MC: calling evaluateByScoring");
		return currentNode.evaluateByScoring(territory);
	}

	public double evaluateByScoring(double[][] territory)
	{
		double chineseScore = Evaluator.chineseScore(this, territory);
		chineseScore -= komi;
	
		return chineseScore;
	}

	int getAtariCount(BoardType movingColor) 
	{
		switch (movingColor) {
		case BLACK:
			if (blackAtari < 0) {
				blackAtari = goban.getAtariCount(movingColor);
				//logger.info("getAtariCount(BLACK): " + this);
			}
			return blackAtari;

		case WHITE:
			if (whiteAtari < 0) {
				whiteAtari = goban.getAtariCount(movingColor);
				//logger.info("getAtariCount(WHITE): " + this);
			}
			return whiteAtari;
			
		case EMPTY:
		default:	
			return 0;	
		}
	}
	
	AnalysisNode getBestChild()
	{
		double max = -1;
		AnalysisNode best = null;
		for (AnalysisNode node : children)
		{
			//logger.info("move: " + node.move + ", value=" + node.value + ", visits=" + node.visits);
			if (node.value > max) {
				max = node.value;
				best = node;
			}
		}
		return best;
	}
	
	
	private int getSavedStones() 
	{
		int parentAtari = parent.getAtariCount(parent.movingColor);		
		int myAtari = getAtariCount(parent.movingColor);
		
	    // logger.info("getSavedStones: " + move + ": " + parentAtari + " - " + myAtari);
		return parentAtari - myAtari;
	}
	
	public final double getScore()
	{
		return score / getVisits();
	}

	synchronized public final double getValue() {
		return value / getVisits();
	}

	public final double getVariance() {
		return Math.sqrt((score2 - score*score/getVisits()) / (getVisits() - 1));
	}
	
	@Override
	public int hashCode() {
		if (move != null)
			return goban.hashCode();
		else if (parent != null) {
			return 31 * parent.hashCode() + 1;
		}
		else return 0;
	}


	
	private void initializeMiai() 
	{
		updateMiai();
	}

	public boolean isPass() 
	{
		return move == null;
	}
	
	/** 
	 * Try to make a (sensible) random move. 
	 * A move is considered sensible if it is <ul>
	 * <li> legal,
	 * <li> does not fill an own eye.
	 * </ul>
	 */
	protected AnalysisNode selectRandomMCMove()
	{
		double totalSuitability = 0;
		Map<AnalysisNode, Double> map = new TreeMap<AnalysisNode, Double>();
		int size = boardSize;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (goban.getStone(i, j) != BoardType.EMPTY) continue;
				Point p = new Point(i, j);
				AnalysisNode node = createChild(p);
				// numNodes++;
				if (node.suitability > 0) {
					totalSuitability += node.suitability;
					map.put(node, node.suitability);
				}
			}
		}
		
		double random = Math.random();
		Set<Map.Entry<AnalysisNode, Double>> entries = map.entrySet();
		//logger.info("selectRandomMove: " + parent.movingColor + " " + totalSuitability + " " + random);
		for (Map.Entry<AnalysisNode, Double> entry : entries) {
			random -= entry.getValue() / totalSuitability;
			if (random < 0) {
				AnalysisNode node = entry.getKey();
				if (!node.isIllegalKo())
					return node;
			}
		}
		// logger.info("no suitable move - passing");
		AnalysisNode node = createPassNode();
		//if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
		//node.hashCodes.set(node.moveNo, node.goban.hashCode());
		return node;
	}

	protected AnalysisNode selectRandomUCTMove() 
	{
		int _visits = 0;
		AnalysisNode best = null;
		double max = -1;
		
		synchronized (this) {
			for (AnalysisNode child : children)
			{
				_visits += child.getVisits();
			}
			for (AnalysisNode child : children)
			{
				double value;
				if (child.getVisits() == 0) {
					value = 1000 + child.suitability;
				}
				else {
					value = 1 + child.getValue() + Math.sqrt(2*Math.log(_visits)/child.getVisits());
				}

				//logger.info("child=" + child + ", value=" + value);
				if (value > max) {								
					if (!child.isIllegalKo() || (child.move == null)) {
						best = child;
						max = value;
					}
				}
			}
		}
		//logger.info("final max=" + max + ", best=" + best);
		if (best == null) throw new NullPointerException();
		assert best != null;
		return best;
	}

	boolean isIllegalKo() {
		boolean illegalKo = false;
		AnalysisNode node = this; 
		
		while (node.parent != null) {
			node = node.parent;
			if (node.goban.equals(this.goban)) {
				illegalKo = true;
				break;
			}
		}
		return illegalKo;
	}
	
	/*
	private boolean isIllegalKo(AnalysisNode child, AnalysisNode[] sequence, int n) {
		boolean illegalKo = false;
		for (int i=n-1; i>=0; i--)
		{
			if (sequence[i].goban.equals(child.goban)) {
				// FIXME
				illegalKo = true;
				break;
			}
		}
		AnalysisNode node = sequence[0];
		while (node.parent != null) {
			node = node.parent;
			if (node.goban.equals(child.goban)) {
				// FIXME!
				illegalKo = true;
				break;
			}
		}
		return illegalKo;
	}
	*/

	@Override
	public String toString() {
		return "AnalysisNode [id=" + hashCode()
				+ ", parent=" + (parent != null ? parent.hashCode() : "null")
				+ "\nmove=" + move
				+ ", moveNo=" + moveNo + ", value=" + getValue() 
				+ ", score=" + getScore()
			    + ", variance=" + getVariance()
				+ ", movingColor=" + movingColor
				+ "\nvisits=" + getVisits()
				+ ", blackAtari=" + blackAtari + ", whiteAatari=" + whiteAtari
				+ ", suitability=" + suitability + "\n" + goban + "]";
	}

	private void updateMiai() 
	{
		// TODO Look for miai pairs and add them.	
	}

	synchronized public void update(double value, double score) {
		this.value += value;
		this.score += score;
		this.score2 += score * score;
		this.visits++;
	}

	synchronized public final int getVisits() {
		return visits;
	}
}