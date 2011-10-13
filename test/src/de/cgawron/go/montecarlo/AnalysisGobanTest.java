/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.montecarlo.AnalysisGoban.Chain;
import de.cgawron.go.montecarlo.AnalysisGoban.Cluster;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;

/**
 * Test class for AnalysisGoban
 * @author Christian Gawron
 */
public class AnalysisGobanTest {
	private static Logger logger = Logger.getLogger(AnalysisGobanTest.class.getName());
	
  
    public AnalysisGobanTest() {
    }
    
 	@Test
	public void testMove() 
	{
 		AnalysisGoban goban = new AnalysisGoban(19);
 		goban.move(0, 0, BoardType.BLACK);
 		assertEquals("Test color after move", BoardType.BLACK, goban.getStone(0, 0));
 		Cluster cluster00 = goban.getBoardRep(0, 0).cluster;
 		assertTrue("Check that there is now a chain at [0, 0]", cluster00 instanceof Chain);
 		Chain chain00 = (Chain) cluster00;
		logger.info("[0, 0]: " + chain00.toString(true));
 		assertEquals("Chain should have size 1", 1, chain00.size());
 		assertEquals("Chain at [0, 0] should have 2 liberties", 2, chain00.liberties.size());
		goban.move(1, 0, BoardType.WHITE);
		Cluster cluster10 = goban.getBoardRep(1, 0).cluster;
		Chain chain10 = (Chain) cluster10;
		logger.info("[0, 0]: " + chain00.toString(true));
		logger.info("[1, 0]: " + chain10.toString(true));
		assertEquals("Chain at [0, 0] should have 1 liberties", 1, chain00.liberties.size());
		assertEquals("Chain at [1, 0] should have 2 liberties", 2, chain10.liberties.size());
		goban.move(0, 1, BoardType.WHITE);
		assertEquals("Chain at [0, 0] should have 0 liberties", 0, chain00.liberties.size());
		cluster00 = goban.getBoardRep(0, 0).cluster;
		assertEquals("Check number of captured stones", 1, goban.getBlackCaptured());
		assertEquals("Chain at [1, 0] should have 3 liberties", 3, chain10.liberties.size());
		assertTrue("Check that there is now no chain at [0, 0]", cluster00 instanceof Eye);
		Eye eye00 = (Eye) cluster00;
		logger.info("Eye: " + eye00.toString(true));
		assertEquals("Eye at [0, 0] should have 2 neighbors", 2, eye00.getNeighbors().size());
	}

}
