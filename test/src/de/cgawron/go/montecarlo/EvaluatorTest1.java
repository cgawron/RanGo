/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class EvaluatorTest1 {
	private static Logger logger = Logger.getLogger(EvaluatorTest1.class.getName());
	
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 	
                    { "ko1.sgf", BoardType.BLACK, 10, new Point(3, 1), 15 },
                    { "seki1.sgf", BoardType.BLACK, 0, null, 0 },
                    { "seki1.sgf", BoardType.WHITE, 0, null, 0 },
                    { "lifeAndDeath1.sgf", BoardType.BLACK, 30, new Point(5, 0), 11 },
                    { "lifeAndDeath1.sgf", BoardType.WHITE, 30, new Point(5, 0), 19 },
            });
    }
    
    private Goban goban;
    private BoardType movingColor;
    private File baseDir = new File("test/sgf");
    private double komi;
    private double expectedScore;
	private Point expectedMove;
    
    public EvaluatorTest1(String inputSGF, BoardType movingColor, double komi, Point expectedMove, double expectedScore) throws Exception {
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    	this.movingColor = movingColor;
    	this.komi = komi;
    	this.expectedMove = expectedMove;
       	this.expectedScore = expectedScore;
    }
    
    @Test
	public void testEvaluateUCT() {
		Evaluator evaluator = new Evaluator();
		AnalysisNode root = new AnalysisNode(goban, movingColor, komi);
		double score = evaluator.evaluate(root);
		AnalysisNode best = root.getBestChild();
		assertEquals("Testing number of iterations", Evaluator.NUM_SIMULATIONS, root.visits);
		assertEquals("Testing expected move", expectedMove, best.move);
		assertEquals("Testing expected score", expectedScore, -best.getScore(), 2);
	}

}
