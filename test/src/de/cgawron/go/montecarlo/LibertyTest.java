/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.*;

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

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(Parameterized.class)
public class LibertyTest extends GobanTest {
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
    private Point move;
	private int expectedLiberties;
    
    public LibertyTest(String inputSGF, BoardType movingColor, Point move, int expectedLiberties) throws Exception {
    	goban = getGoban(inputSGF);

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
		AnalysisNode parent = new AnalysisNode(goban, BoardType.BLACK);
		AnalysisNode node;
		node = parent.createChild(move);
		
		Chain chain = node.goban.getChain(move);
		assertEquals("Testing expected liberties of chain " + chain, expectedLiberties, chain.getLiberties().size());
	}

}
