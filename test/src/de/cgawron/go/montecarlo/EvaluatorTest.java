/**
 * 
 */
package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.cgawron.go.montecarlo.SGFSuite.SGFTestCase;
import de.cgawron.go.montecarlo.SGFSuite.SuiteConfig;

/**
 * Test class for (static) chinese scoring, i.e. assuming all stones are alive.
 * @author Christian Gawron
 */
@RunWith(SGFSuite.class)
@SuiteConfig("test/testcases.xml")
public class EvaluatorTest 
{
	private static Logger logger = Logger.getLogger(EvaluatorTest.class.getName());
	
    SGFTestCase testCase;
      
    public EvaluatorTest(SGFTestCase parameters) throws Exception {
    	this.testCase = parameters;
    	logger.info("Parameters: " + parameters);
    }
    
    @Test
	public void testEvaluateUCT() throws Exception {
		Evaluator evaluator = new Evaluator();
		if (testCase.evaluatorParameters != null)
			evaluator.setParameters(testCase.evaluatorParameters);
		AnalysisNode root = new AnalysisNode(testCase.getGoban(), 
											 testCase.getMovingColor(), testCase.getKomi());
		evaluator.evaluate(root);
		AnalysisNode best = root.getBestChild();
		assertEquals("Testing expected move", testCase.getExpectedMove(), best.getMove());
		if (testCase.getExpectedScore() != null)
			assertEquals("Testing expected score", testCase.getExpectedScore(), best.getScore(), best.getVariance());
		assertEquals("Testing number of iterations", Evaluator.parameters.numSimulations, root.getVisits());
	}

}
