/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Cluster;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class ScoreTest {
	private static Logger logger = Logger.getLogger(ScoreTest.class.getName());

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
		StringBuffer sb = new StringBuffer();
		int size = goban.getBoardSize();
		double[][] territory = new double[size][size]; 
		double score = goban.chineseScore(territory);
		Cluster cluster = goban.getBoardRep(6, 6);
		for (int i=0; i<size; i++) {
			sb.append("\n");
			for (int j=0; j<size; j++) {
				sb.append(String.format(" %4.0f", territory[i][j]));
			}
		}
		for (Point p : Point.all(size)) {
			Cluster cp = goban.getBoardRep(p);
			assertTrue("Testing boardRep at " + p, cp.getPoints().contains(p));
			for (Cluster c : goban.clusters) {
				if (cp != c) {
					assertFalse("Testing boardRep at " + p, c.getPoints().contains(p));
				}
			}
		}

		logger.info("score: " + score + "\n" + goban.toString());
		logger.info(sb.toString());
		logger.info("[6, 6]: " + cluster.toString(true));
		assertEquals("Testing expected score", expectedScore, score, 0.2);
	}

}
