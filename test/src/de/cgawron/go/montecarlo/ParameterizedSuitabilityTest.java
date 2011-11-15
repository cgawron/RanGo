/**
 * 
 */
package de.cgawron.go.montecarlo;

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
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class ParameterizedSuitabilityTest {
	private static Logger logger = Logger.getLogger(ParameterizedSuitabilityTest.class.getName());
	
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 
            			{ "suitability1.sgf", BoardType.BLACK },
               			{ "suitability1.sgf", BoardType.WHITE },
               			{ "lifeAndDeath1.sgf", BoardType.WHITE }
            });
    }
    
    private Goban goban;
    private BoardType movingColor;
    private File baseDir = new File("test/sgf");
    
    public ParameterizedSuitabilityTest(String inputSGF, BoardType movingColor) throws Exception {
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    	this.movingColor = movingColor;
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testSuitability() 
	{
		AnalysisNode parent = new AnalysisNode(goban, movingColor);
		AnalysisNode node;
		int size = goban.getBoardSize();
		//double[][] suitability = new double[size][size];
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size; i++) {
			sb.append("\n");
			for (int j=0; j<size; j++) {
				node = parent.createChild(new Point(i, j));
				sb.append(String.format("%4.1f ", node.calculateStaticSuitability()));
			}
		}
		logger.info("suitability: " + movingColor + "\n" + goban + sb.toString());
		//assertEquals("Testing expected score", expectedScore, score, 0.2);
	}

}
