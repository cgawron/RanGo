/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertTrue;
import java.util.logging.Logger;

import org.junit.Test;

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Cluster;
import de.cgawron.go.montecarlo.AnalysisGoban.Group;

/**
 * Test class for suitability.
 * @author Christian Gawron
 */
public class SuitabilityTest extends GobanTest {
	private static Logger logger = Logger.getLogger(SuitabilityTest.class.getName());
	
      
    public SuitabilityTest() throws Exception {
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testSuitability() 
	{
 		AnalysisGoban goban = new AnalysisGoban(7);
 		AnalysisNode root = new AnalysisNode(goban, BoardType.BLACK);
 		AnalysisNode child = root.createChild(new Point(0, 1));
		child = child.createPassNode();
		child = child.createChild(new Point(1, 0));
		child = child.createPassNode();
		child = child.createChild(new Point(1, 1));
		child = child.createPassNode();
		child = child.createChild(new Point(1, 2));
		child = child.createPassNode();
		child = child.createChild(new Point(1, 3));
		child = child.createPassNode();
		child = child.createChild(new Point(0, 3));
		child = child.createPassNode();

 		logSuitability(child);
 		assertUnsuitable(child, 0, 0);
 		assertUnsuitable(child, 0, 2);
 		Group g = child.goban.getBoardRep(1, 0).getGroup();
 		logger.info("group: " + g);
 		assertSuitable(child, 5, 5);
	}
	
 	@Test 
 	public void testEyeFill() throws Exception
 	{
 		Cluster c;
 		AnalysisGoban goban = new AnalysisGoban(getGoban("ko1.sgf"));
 		AnalysisNode child= new AnalysisNode(goban, BoardType.WHITE);
 		logger.info("testEyeFill: " + child);
 		child = child.createChild(new Point(3, 1));
 		child = child.createChild(new Point(0, 1));
 		logger.info("testEyeFill: " + child);
 		logSuitability(child);
 		c = child.goban.getBoardRep(new Point(4, 0));
 		logger.info("cluster at (4, 0): " + c);
 		assertUnsuitable(child, 4, 0);
  	}

 	@Test 
 	public void testKo() throws Exception
 	{
 		AnalysisGoban goban = new AnalysisGoban(getGoban("ko1.sgf"));
 		AnalysisNode child= new AnalysisNode(goban, BoardType.BLACK);
 		logSuitability(child);
 		assertSuitable(child, 3, 1);
  	}

 	
	private void assertUnsuitable(AnalysisNode child, int i, int j) 
	{
		Point p = new Point(i, j);
		double suitability = child.createChild(p).calculateStaticSuitability();
		assertTrue("move on " + p + " for " + child + " should have suitability 0", suitability == 0);
	}

	private void assertSuitable(AnalysisNode child, int i, int j) 
	{
		Point p = new Point(i, j);
		double suitability = child.createChild(p).calculateStaticSuitability();
		assertTrue("move on " + p + " for " + child + " should have suitability > 0", suitability > 0);
	}
	private void logSuitability(AnalysisNode parent) 
	{
		AnalysisNode node;
		int size = parent.goban.getBoardSize();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<size; i++) {
			sb.append("\n");
			for (int j=0; j<size; j++) {
				node = parent.createChild(new Point(i, j));
				sb.append(String.format("%4.1f ", node.calculateStaticSuitability()));
			}
		}
		logger.info("suitability: " + parent.movingColor + "\n" + parent.goban + sb.toString());

	}

}
