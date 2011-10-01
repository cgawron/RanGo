package de.cgawron.go.montecarlo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.GobanMap;
import de.cgawron.go.Point;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;
import de.cgawron.go.montecarlo.AnalysisGoban.Group;
import de.cgawron.go.montecarlo.Evaluator.AnalysisResult;
import de.cgawron.go.montecarlo.Evaluator.RandomSimulator;

class AnalysisNode implements Comparable<AnalysisNode>
{
	private static final int STEEPNESS = 1;



	static Logger logger = Logger.getLogger(AnalysisNode.class.getName());

	
	
	AnalysisNode parent;
	Set<AnalysisNode> children;

	AnalysisGoban goban;
	Map<Point, Miai> miaiMap;

	int boardSize;
	double komi;
	
	Point move;
	int moveNo;

	double score;
	double score2;
	double suitability;
	double depth;
	double value;
	int visits;

	int blackAtari = -1;
	int whiteAtari = -1;

	

	
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
		this.goban = new AnalysisGoban(goban, movingColor);
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
		BoardType color = goban.getStone(move);
		
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
		Group group = eye.group;
		if (group != null)
		{
			if (group.isAlive()) return 0;
			else {
				// FIXME
				if (eye.size < 7 && eye.isVitalPoint(move))
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
		return child;
	}

	public AnalysisNode createChild(Point p) 
	{
		AnalysisNode child = createChild();
		child.move = p;
		child.goban.move(p);
		updateMiai();
		child.suitability = child.calculateStaticSuitability();
		//logger.info("createChild: \n" + child);
		child.goban.movingColor = goban.movingColor.opposite();
		return child;
	}
	
	public AnalysisNode createPassNode() 
	{
		//logger.info("creating pass move");
		AnalysisNode child = createChild();
		child.move = null;
		child.goban.movingColor = goban.movingColor.opposite();
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
	
	public void evaluateByMC(AnalysisNode[] sequence, int n, double[][] territory)
	{
		AnalysisNode currentNode = this; 
		int depth = 0;
		
		int i = n;
		while (true) {
			sequence[++i] = currentNode = currentNode.selectRandomMCMove(sequence, i);
			depth++;
			// logger.info("evaluate: " + currentNode);

			if (currentNode.isPass() && currentNode.parent.isPass()) {
				break;
			}
			
			if (currentNode.moveNo > Evaluator.MAX_MOVES) {
				throw new RuntimeException("no result");
			}
		} 
		evaluateByScoring(currentNode, territory);
		this.depth += depth;
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
	
	public void evaluateByScoring(AnalysisNode leaf, double[][] territory)
	{
		double chineseScore = Evaluator.chineseScore(leaf, goban.movingColor, territory);
		if (goban.movingColor == BoardType.BLACK)
			chineseScore -= komi;
		else
			chineseScore += komi;
	
		/* All or nothing ...
		if (result.score < 0)
			value = 1;
		else
			value = 0;
		*/
		
		// Sigmoid exp(x)/(1+exp(x))
		double exp = Math.exp(STEEPNESS*chineseScore);
		value = exp / (1+exp);
		
		score -= chineseScore;
		score2 += chineseScore*chineseScore;

		//logger.info("score=" + chineseScore + ", value=" + value);
	}
	
	
	private int getSavedStones() 
	{
		int parentAtari = parent.getAtariCount(parent.goban.movingColor);		
		int myAtari = getAtariCount(parent.goban.movingColor);
		
		// logger.info("getSavedStones: " + move + ": " + parentAtari + " - " + myAtari);
		return parentAtari - myAtari;
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
	protected AnalysisNode selectRandomMCMove(AnalysisNode[] sequence, int n)
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
				boolean illegalKo = false;
				for (int i=n-2; i>=0; i--)
				{
					if (sequence[i].goban.equals(node.goban))
						illegalKo = true;
				}
				if (!illegalKo)
					return node;
			}
		}
		AnalysisNode node = createPassNode();
		//if (node.moveNo >= node.hashCodes.size()) node.hashCodes.setSize(node.moveNo + 1);
		//node.hashCodes.set(node.moveNo, node.goban.hashCode());
		return node;
	}

	protected AnalysisNode selectRandomUCTMove(AnalysisNode[] sequence, int n) 
	{
		int visits = 0;
		AnalysisNode best = null;
		double max = -1;
		
		for (AnalysisNode child : children)
		{
			visits += child.visits;
		}
		for (AnalysisNode child : children)
		{
			//logger.info("child=" + child);
			double value;
			if (child.visits == 0) 
				value = 1000 + child.value;
			else {
				value = 1 + child.value + Math.sqrt(2*Math.log(visits)/child.visits);
			}
			
			if (value > max) {
				boolean illegalKo = false;
				for (int i=n-2; i>=0; i--)
				{
					if (sequence[i].goban.equals(child.goban))
						illegalKo = true;
				}
				if (!illegalKo) {
					best = child;
					max = value;
				}
			}
		}
		//logger.info("final max=" + max + ", best=" + best);
		return best;
	}

	@Override
	public String toString() {
		return "AnalysisNode [id=" + hashCode()
				+ ", parent=" + (parent != null ? parent.hashCode() : "null")
				+ "\nmove=" + move
				+ ", moveNo=" + moveNo + ", value=" + value 
				+ ", score=" + getScore()
			    + ", variance=" + getVariance()
				+ ", movingColor=" + goban.movingColor
				+ "\nvisits=" + visits
				+ ", blackAtari=" + blackAtari + ", whiteAatari=" + whiteAtari
				+ ", suitability=" + suitability + "\n" + goban + "]";
	}
	
	public double getScore()
	{
		return score / visits;
	}

	public double getVariance() {
		return Math.sqrt((score2 - score*score/visits) / (visits - 1));
	}

	private void updateMiai() 
	{
		// TODO Look for miai pairs and add them.	
	}
}