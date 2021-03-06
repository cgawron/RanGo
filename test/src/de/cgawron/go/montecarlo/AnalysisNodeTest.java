/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.junit.Test;

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Chain;
import de.cgawron.go.montecarlo.AnalysisGoban.Cluster;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;

/**
 * Test class for AnalysisGoban
 * @author Christian Gawron
 */
public class AnalysisNodeTest extends GobanTest 
{
	private static Logger logger = Logger.getLogger(AnalysisNodeTest.class.getName());
	
  
    public AnalysisNodeTest() {
    }
    
 	@Test
	public void testMove() 
	{
 		AnalysisGoban goban = new AnalysisGoban(7);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(0, 0));
 		checkGoban(child.goban);
 		child = child.createChild(new Point(1, 0));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(0, 1));
 		child = child.createChild(new Point(0, 0));
		goban = child.goban;
 		
 		Cluster cluster00 = goban.getBoardRep(0, 0);
		Cluster cluster10 = goban.getBoardRep(1, 0);
		Chain chain10 = (Chain) cluster10;
		Cluster cluster01 = goban.getBoardRep(0, 1);
		Chain chain01 = (Chain) cluster01;
		
		assertEquals("Check number of captured stones", 1, goban.getBlackCaptured());
		assertEquals("Chain at [1, 0] should have 3 liberties", 3, chain10.liberties.size());
		assertEquals("Chain at [0, 1] should have 3 liberties", 3, chain01.liberties.size());
		assertTrue("Check that there is now no chain at [0, 0]", cluster00 instanceof Eye);
		Eye eye00 = (Eye) cluster00;
		assertTrue("Check the neighbors of cluste01", cluster01.getNeighbors().contains(eye00));
		assertTrue("Check the neighbors of eye00", eye00.getNeighbors().contains(cluster01));
		assertTrue("Check the neighbors of eye00", eye00.getNeighbors().contains(cluster10));
		logger.info("Eye: " + eye00.toString(true));
		assertEquals("Eye at [0, 0] should have 2 neighbors", 2, eye00.getNeighbors().size());
		goban.move(0, 0, BoardType.BLACK);
		logger.info(goban.toString());
	}
 	
 	@Test
	public void testJoin() 
	{
 		AnalysisGoban goban = new AnalysisGoban(7);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(0, 0));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(2, 0));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(2, 2));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(0, 2));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(0, 1));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(1, 0));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(2, 1));
 		checkGoban(child.goban);
		child = child.createPassNode();
 		child = child.createChild(new Point(1, 2));
 		checkGoban(child.goban);
 	}
 
 	@Test 
 	public void testKo() throws Exception
 	{
 		AnalysisGoban goban = new AnalysisGoban(getGoban("ko1.sgf"));
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(3, 1));
 		logger.info("testKo: " + child);
 		assertTrue("check that move is not illegal ko capture", !child.isIllegalKo());
 		child = child.createChild(new Point(2, 1));
 		assertTrue("check that move is illegal ko capture", child.isIllegalKo());
 	}
 	
	@Test
	public void testEyePartion() 
	{
 		AnalysisGoban goban = new AnalysisGoban(7);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(0, 0));
 		checkGoban(child.goban);
 		child = child.createChild(new Point(5, 0));
 		checkGoban(child.goban);
 		child = child.createChild(new Point(6, 1));
 		checkGoban(child.goban);
	}
	
 	@Test
	public void testJoin1() 
	{
 		AnalysisGoban goban = new AnalysisGoban(7);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(3, 4));
 		checkGoban(child.goban);
		child = child.createChild(new Point(0, 0));
 		child = child.createChild(new Point(5, 4));
 		checkGoban(child.goban);
		child = child.createChild(new Point(0, 1));
 		child = child.createChild(new Point(4, 3));
 		checkGoban(child.goban);
		child = child.createChild(new Point(0, 2));
 		child = child.createChild(new Point(5, 4));
 		checkGoban(child.goban);
		child = child.createChild(new Point(3, 3));
 		child = child.createChild(new Point(4, 4));
 		checkGoban(child.goban);
 	}

 	@Test 
 	public void testChild() throws Exception
 	{
 		AnalysisGoban goban = new AnalysisGoban(getGoban("lifeAndDeath1.sgf"));
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild();
 		child.goban.checkGoban();
 		
		for (Cluster c : child.goban.clusters) {
 			assertTrue("Child cluster has no parent: " + c, c.parent != null);
 		}
		
 		child = child.createChild(new Point(5, 0));
 		child.goban.checkGoban();
		child = child.createChild();
 		child.goban.checkGoban();
 		{
 			int size = goban.getBoardSize();
 			StringBuffer sb = new StringBuffer();
 			for (int x=0; x<size; x++) {
 				sb.append("\n");
 				for (int y=0; y<size; y++) {
 					AnalysisNode node = child.createChild(new Point(x, y));
 					sb.append(String.format("%4.1f ", node.calculateStaticSuitability()));
 				}
 			}
		}
 		
		for (Cluster c : child.goban.clusters) {
 			assertTrue("Child cluster has no parent: " + c, c.parent != null);
 		}
 	}
 	
 	@Test
	public void testSimulation() 
	{
 		int size = 7;
 		AnalysisNode[] sequence = new AnalysisNode[200];
 		AnalysisGoban goban = new AnalysisGoban(size);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode currentNode = root.createChild(new Point(0, 0));
 		
		int i = 0;
		sequence[0] = currentNode;
		while (true) {
			sequence[++i] = currentNode = currentNode.selectRandomMCMove();
			goban = currentNode.goban;
			logger.info("move " + i + ": " + currentNode);			
			checkGoban(goban);
			if (currentNode.isPass() && currentNode.parent.isPass()) {
				StringBuffer sb = new StringBuffer();
				for (int x=0; x<size; x++) {
					sb.append("\n");
					for (int y=0; y<size; y++) {
						AnalysisNode node = currentNode.createChild(new Point(x, y));
						sb.append(String.format("%4.1f ", node.calculateStaticSuitability()));
					}
				}
				logger.info("suitability: " + currentNode.movingColor + "\n" + goban + sb.toString());				
				break;
			}
		} 
	}
 	

 	@Test
	public void testSimulation3() throws Exception
	{
 		int size = 9;
 		AnalysisNode[] sequence = new AnalysisNode[200];
 		AnalysisGoban goban = new AnalysisGoban(getGoban("lifeAndDeath1.sgf"));
 		AnalysisNode root = new AnalysisNode(goban, BoardType.WHITE);
 		AnalysisNode currentNode = root.createChild(new Point(5, 0));
 		
 		
		int i = 0;
		sequence[0] = currentNode;
		while (true) {
			StringBuffer sb = new StringBuffer();
			for (int x=0; x<size; x++) {
				sb.append("\n");
				for (int y=0; y<size; y++) {
					AnalysisNode node = currentNode.createChild(new Point(x, y));
					sb.append(String.format("%4.1f ", node.calculateStaticSuitability()));
				}
			}
			logger.info("suitability: " + currentNode.movingColor + "\n" + currentNode.goban + sb.toString());				

			sequence[++i] = currentNode = currentNode.selectRandomMCMove();
			goban = currentNode.goban;
			logger.info("move " + i + ": " + currentNode);			
			checkGoban(goban);
			if (currentNode.isPass() && currentNode.parent.isPass()) {	
				logger.info("score is " + currentNode.evaluateByScoring(null));
				break;
			}
		} 
	}
	
}
