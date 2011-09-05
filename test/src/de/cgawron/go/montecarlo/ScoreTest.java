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
public class ScoreTest {
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { { "simpleScore1.sgf", 9.0 }, { "simpleScore2.sgf", 5.0 } });
    }
    
    private AnalysisGoban goban;
    private double expectedScore;
    private File baseDir = new File("test/sgf");
    
    public ScoreTest(String inputSGF, double expectedScore) throws Exception {
    	this.expectedScore = expectedScore;
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = new AnalysisGoban(gameTree.getLeafs().get(0).getGoban());
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testChineseScore() {
		int size = goban.getBoardSize();
		double[][] territory = new double[size][size]; 
		double score = Evaluator.chineseScore(goban, BoardType.BLACK, territory);
		assertEquals("Testing expected score", expectedScore, score, 0.2);
	}

}
