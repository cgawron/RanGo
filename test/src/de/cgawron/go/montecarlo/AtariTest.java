/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Chain;
import de.cgawron.go.sgf.GameTree;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
public class AtariTest {
	private static Logger logger = Logger.getLogger(AtariTest.class.getName());
	
    private AnalysisNode node;
    private static Goban goban;
    private BoardType movingColor;
    private List<Point> expectedChains;
    private static File baseDir = new File("test/sgf");
	private int expectedCount;
    
    public AtariTest() {
    	logger.info("AtariTest constructor");
    	this.movingColor = BoardType.BLACK;
     	this.expectedCount = 5;
     	this.expectedChains = Arrays.asList(new Point(4, 0), new Point(0, 0));
    }
    
    @BeforeClass public static void initSGF() throws Exception 
    {
    	File inputFile = new File(baseDir, "suitability1.sgf");
    	GameTree gameTree = new GameTree(inputFile);
    	goban = gameTree.getLeafs().get(0).getGoban();
    }
    
    @Before
    public void initialize() 
    {
		this.node = new AnalysisNode(goban, movingColor);  	
    }
    
	@Test
	public void testAtari() 
	{	
		// logger.info(parent.toString());
		assertEquals("Testing expected atari count for " + movingColor + " " + node, 
					 expectedCount, node.goban.getAtariCount(movingColor));
	}

	@Test
	public void testAtariForChild() 
	{	
		// logger.info(parent.toString());
		Point move = new Point(1, 2);
		node = node.createChild(move);
		Chain c = node.goban.getChain(move);
		assertNotNull("Expecting a chain at " + move, c);
		assertEquals("Expecting chain with 1 liberty at " + move, 6, c.getLiberties().size());
		assertEquals("Testing expected atari count for " + movingColor + " " + node, 
					 1, node.getAtariCount(movingColor));
	}

	@Test
	public void testAtariChains() 
	{
		for (Point p : expectedChains) {
			Chain c = node.goban.getChain(p);
			logger.info("Chain " + c);
			assertNotNull("Expecting a chain at " + p, c);
			assertEquals("Expecting chain with 1 liberty at " + p, 1, c.getLiberties().size());
			//assertTrue("Chain must be in chainList", node.goban.chainList.contains(c));
		}
	}

}
