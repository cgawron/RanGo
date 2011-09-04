/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class EvaluatorTest {
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 
            			{ "simpleScore1.sgf", 9.0 }, 
            		    { "simpleScore2.sgf", 5.0 }, 
            		    { "score1.sgf", 9.0 }, 
            		    { "score2.sgf", 5.0 }   
            });
    }
    
    private Goban goban;
    private double expectedScore;
    private File baseDir = new File("test/sgf");
    
    public EvaluatorTest(String inputSGF, double expectedScore) throws Exception {
    	this.expectedScore = expectedScore;
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testEvaluate() {
		double score = Evaluator.evaluate(goban, BoardType.BLACK);
		assertEquals("Testing expected score", expectedScore, score, 0.2);
	}

}
