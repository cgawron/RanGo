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
public class EyeTest {
	private static Logger logger = Logger.getLogger(EyeTest.class.getName());
	
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 
                { "lifeAndDeath1.sgf", BoardType.WHITE }
            });
    }
    
    private Goban goban;
    private BoardType movingColor;
    private File baseDir = new File("test/sgf");
    
    public EyeTest(String inputSGF, BoardType movingColor) throws Exception {
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    	this.movingColor = movingColor;
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testEyes() 
	{
		Evaluator.AnalysisNode parent = new Evaluator.AnalysisNode(goban, movingColor);
		Evaluator.AnalysisNode node = parent;
		int size = goban.getBoardSize();
		int maxId = 0;
		
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size; i++) {
			sb.append("\n");
			for (int j=0; j<size; j++) {
				int id = node.goban.eyeMap[i*size + j];
				if (id >= 0) {
					Eye eye = node.goban.getEye(new Point(i, j));
					assertEquals(String.format("Testing eye %d@[%d, %d]", id, i, j), id, eye.id);
				}
				if (id > maxId) maxId = id;
				sb.append(String.format("%4d ", id));
			}
		}
		logger.info("eyes: " + movingColor + "\n" + goban + sb.toString());
		for (int i=0; i<maxId; i++) {
			logger.info(String.format("Eye %d: %s", i, node.goban.eyes[i]));
		}
		//assertEquals("Testing expected score", expectedScore, score, 0.2);
	}

}
