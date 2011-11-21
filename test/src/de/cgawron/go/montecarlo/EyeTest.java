/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.junit.Test;

import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Eye;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
public class EyeTest extends GobanTest
{
	private static Logger logger = Logger.getLogger(EyeTest.class.getName());
	
    public EyeTest() {
    	super();
    }
    
	/**
	 * Test method for {@link de.cgawron.go.montecarlo.Evaluator#chineseScore(de.cgawron.go.Goban, de.cgawron.go.Goban.BoardType)}.
	 */
	@Test
	public void testEyes() throws Exception
	{
 		AnalysisGoban goban = new AnalysisGoban(getGoban("lifeAndDeath1.sgf"));
 		AnalysisNode root = new AnalysisNode(goban, BoardType.WHITE);
 		AnalysisNode currentNode = root.createChild(new Point(5, 0));

 		goban = currentNode.goban;
 		
 		Eye eye;
 		eye = goban.getEye(new Point(4, 0));
 		logger.info("goban: " + goban);
 		assertTrue(eye.getGroup() != null);
 		logger.info("eye: " + eye.toString(true));
 		// This does not work yet - living group which is not connected
 		assertTrue(eye.isReal());
 		eye = goban.getEye(new Point(5, 1));
 		assertTrue(eye.getGroup() != null);
 		assertTrue(eye.isReal());
	}

}
