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
import de.cgawron.go.montecarlo.AnalysisGoban.Chain;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class LibertyTest {
	private static Logger logger = Logger.getLogger(LibertyTest.class.getName());
	
	@Parameters
    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 
            			{ "suitability1.sgf", BoardType.BLACK, new Point(1, 2), 6 },
            			{ "suitability1.sgf", BoardType.BLACK, new Point(0, 0), 1 }
             });
    }
    
    private Goban goban;
    private BoardType movingColor;
    private File baseDir = new File("test/sgf");
	private Point move;
	private int expectedLiberties;
    
    public LibertyTest(String inputSGF, BoardType movingColor, Point move, int expectedLiberties) throws Exception {
    	File inputFile = new File(baseDir, inputSGF);
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    	this.movingColor = movingColor;
    	this.move = move;
    	this.expectedLiberties = expectedLiberties;
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testLiberties() 
	{
		Evaluator.AnalysisNode parent = new Evaluator.AnalysisNode(goban, BoardType.BLACK);
		Evaluator.AnalysisNode node;
		// logger.info(parent.toString());
		node = parent.createChild(move);
		// logger.info(node.toString());
		
		Chain chain = node.goban.getChain(move);
		assertEquals("Testing expected liberties", expectedLiberties, chain.numLiberties);
	}

}
