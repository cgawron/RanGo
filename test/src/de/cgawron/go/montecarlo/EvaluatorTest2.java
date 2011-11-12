/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Test;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class Evaluator.
 * @author Christian Gawron
 */
public class EvaluatorTest2 
{
	private static Logger logger = Logger.getLogger(EvaluatorTest2.class.getName());
    
    private Goban goban;
    private BoardType movingColor;
    private double expectedScore;
    private File baseDir = new File("test/sgf");
    
    public EvaluatorTest2() throws Exception {
    	this.movingColor = BoardType.BLACK;
    	File inputFile = new File(baseDir, "evaluate1.sgf");
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    	//expectedScore = 26;
    }
    

	@Test
	public void testCreateNode() {
		Evaluator evaluator = new Evaluator();
		AnalysisNode root = new AnalysisNode(goban, movingColor);
		evaluator.createNode(root);
		//logger.info("root.children=" + root.children);
		assertEquals("Number of children", 27, root.children.size());
		assertEquals("Tree size", 1, evaluator.workingTree.size());
		AnalysisNode node = null;//root.selectRandomUCTMove();
		evaluator.createNode(node);
		assertEquals("Tree size", 2, evaluator.workingTree.size());
	}

	@Test
	public void testEvaluateUCT() {
		Evaluator evaluator = new Evaluator();
		double score = evaluator.evaluate(goban, movingColor, 5);
		assertEquals("Testing expected score", expectedScore, score, 2);
	}
	
}
