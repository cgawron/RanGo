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
	
    SGFTestCase parameters;
      
    public EvaluatorTest(SGFTestCase parameters) throws Exception {
    	this.parameters = parameters;
    	logger.info("Parameters: " + parameters);
    }
    
    @Test
	public void testEvaluateUCT() throws Exception {
		Evaluator evaluator = new Evaluator();
		if (parameters.evaluatorParameters != null)
			evaluator.setParameters(parameters.evaluatorParameters);
		AnalysisNode root = new AnalysisNode(parameters.getGoban(), 
											 parameters.getMovingColor(), parameters.getKomi());
		evaluator.evaluate(root);
		AnalysisNode best = root.getBestChild();
		assertEquals("Testing number of iterations", evaluator.parameters.numSimulations, root.getVisits());
		assertEquals("Testing expected move", parameters.getExpectedMove(), best.move);
		assertEquals("Testing expected score", parameters.getExpectedScore(), best.getScore(), best.getVariance());
	}

}
