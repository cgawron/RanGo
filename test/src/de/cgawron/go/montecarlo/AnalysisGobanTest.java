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
public class AnalysisGobanTest extends GobanTest 
{
	private static Logger logger = Logger.getLogger(AnalysisGobanTest.class.getName());
	
  
    public AnalysisGobanTest() {
    }
    
 	@Test
	public void testMove() 
	{
 		AnalysisGoban goban = new AnalysisGoban(19);
 		goban.move(0, 0, BoardType.BLACK);
 		assertEquals("Test color after move", BoardType.BLACK, goban.getStone(0, 0));
 		Cluster cluster00 = goban.getBoardRep(0, 0);
 		assertTrue("Check that there is now a chain at [0, 0]", cluster00 instanceof Chain);
 		Chain chain00 = (Chain) cluster00;
		logger.info("[0, 0]: " + chain00.toString(true));
 		assertEquals("Chain should have size 1", 1, chain00.size());
 		assertEquals("Chain at [0, 0] should have 2 liberties", 2, chain00.liberties.size());
		goban.move(1, 0, BoardType.WHITE);
		Cluster cluster10 = goban.getBoardRep(1, 0);
		Chain chain10 = (Chain) cluster10;
		logger.info("[0, 0]: " + chain00.toString(true));
		logger.info("[1, 0]: " + chain10.toString(true));
		assertEquals("Chain at [0, 0] should have 1 liberties", 1, chain00.liberties.size());
		assertEquals("Chain at [1, 0] should have 2 liberties", 2, chain10.liberties.size());
		goban.move(0, 1, BoardType.WHITE);
		assertEquals("Chain at [0, 0] should have 0 liberties", 0, chain00.liberties.size());
		cluster00 = goban.getBoardRep(0, 0);
		assertEquals("Check number of captured stones", 1, goban.getBlackCaptured());
		assertEquals("Chain at [1, 0] should have 3 liberties", 3, chain10.liberties.size());
		assertTrue("Check that there is now no chain at [0, 0]", cluster00 instanceof Eye);
		Eye eye00 = (Eye) cluster00;
		logger.info("Eye: " + eye00.toString(true));
		assertEquals("Eye at [0, 0] should have 2 neighbors", 2, eye00.getNeighbors().size());
	}

 	@Test
 	public void testLiberties() throws Exception 
 	{
		AnalysisGoban goban = new AnalysisGoban(9);
 		goban.putStone(0, 0, BoardType.BLACK);
 		Chain chain00 = (Chain) goban.getBoardRep(0, 0);
 		assertEquals("Chain at [0, 0] should have 2 liberties", 2, chain00.getLiberties().size());
 		goban.putStone(0, 1, BoardType.WHITE);
		assertEquals("Chain at [0, 0] should have 1 liberties", 1, chain00.getLiberties().size());
		goban.putStone(1, 0, BoardType.BLACK);
		logger.info("chain: " + chain00.toString(true));
		assertEquals("Chain at [0, 0] should have 2 liberties", 2, chain00.getLiberties().size());
 	}
 	
 	@Test
 	public void testSetup() throws Exception
 	{
 		AnalysisGoban goban = new AnalysisGoban(getGoban("suitability1.sgf"));
 		logger.info("goban: \n" + goban);
 		Cluster cluster00 = goban.getBoardRep(0, 0);
		Cluster cluster12 = goban.getBoardRep(1, 2);
		logger.info(cluster00.toString(true));
 		logger.info(cluster12.toString(true));
		assertTrue("Check that there is a chain at [0, 0]", cluster00 instanceof Chain);
		assertTrue("Check that there is an eye at [1, 2]", cluster12 instanceof Eye);
		for (Cluster c : cluster00.getNeighbors())
		{
			logger.info("cluster " + c + ": " + c.hashCode());
		}
		assertEquals("Check that cluster00 has three neighbors", 3, cluster00.getNeighbors().size());
		Chain chain00 = (Chain) cluster00;
		Eye eye12 = (Eye) cluster12;
		assertEquals("Check number of liberties of chain00", 1, chain00.getLiberties().size());
 	}
 	
}
