/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.SGFSuite.SGFTestParameters;
import de.cgawron.go.montecarlo.SGFSuite.SuiteConfig;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(SGFSuite.class)
@SuiteConfig("config.xml")
public class EvaluatorTest 
{
	private static Logger logger = Logger.getLogger(EvaluatorTest.class.getName());
	

    public static List<Object[]> data() {
            return Arrays.asList(new Object[][] { 	
                    { "ko1.sgf", BoardType.BLACK, 10, new Point(3, 1), 15 },
                    { "seki1.sgf", BoardType.BLACK, 0, null, 0 },
                    { "seki1.sgf", BoardType.WHITE, 0, null, 0 },
                    { "lifeAndDeath1.sgf", BoardType.BLACK, 30, new Point(5, 0), 11 },
                    { "lifeAndDeath1.sgf", BoardType.WHITE, 30, new Point(5, 0), 19 },
            });
    }
    
    SGFTestParameters parameters;
      
    public EvaluatorTest(SGFTestParameters parameters) throws Exception {
    	this.parameters = parameters;
      }
    
    @Test
	public void testEvaluateUCT() {
		Evaluator evaluator = new Evaluator();
		AnalysisNode root = new AnalysisNode(parameters.getGoban(), 
											 parameters.getMovingColor(), parameters.getKomi());
		double score = evaluator.evaluate(root);
		AnalysisNode best = root.getBestChild();
		assertEquals("Testing number of iterations", Evaluator.NUM_SIMULATIONS, root.getVisits());
		assertEquals("Testing expected move", parameters.getExpectedMove(), best.move);
		assertEquals("Testing expected score", parameters.getExpectedScore(), -best.getScore(), 2);
	}

}
