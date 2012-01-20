package de.cgawron.go.montecarlo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.Evaluator.EvaluatorParameters;
import de.cgawron.go.sgf.GameTree;

public class SGFSuite extends Suite {

	/**
	 * Annotation for a method which provides parameters to be injected into the
	 * test class constructor by <code>SGFSuite</code>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public static @interface SuiteConfig {
		String value();
	}


	@XmlRootElement(name="testcases")
	static class TestCases {
		@XmlElement
		EvaluatorParameters evaluatorParameters;
		
		@XmlElement(name="testcase")
		List<SGFTestCase> testCases;
	}

	public static class SGFTestCase
	{
	    private File baseDir = new File("test/sgf");
	
	    @XmlAttribute
	    public String sgfFile;

	    private Goban goban;
	    
	    @XmlAttribute
		public BoardType movingColor;
	    
	    @XmlAttribute
	    public double komi;
		
	    @XmlElement(name="expected")
	    public Point expectedMove;
		
	    @XmlAttribute
	    public double expectedScore;

	    @XmlAttribute
	    public double tolerance = 3;

		@XmlElement(name="evaluatorParameters")
		EvaluatorParameters evaluatorParameters;
		
		public SGFTestCase()
		{
		}
		
		public SGFTestCase(String sgfFile, BoardType movingColor, 
				           double komi, Point expectedMove, double expectedScore) throws Exception
		{
			this.movingColor = movingColor;
			this.komi = komi;
			this.expectedMove = expectedMove;
			this.expectedScore = expectedScore;
	    	this.sgfFile = sgfFile;
		}
		
		@Override
		public String toString() {
			return "[file=" + sgfFile + ", movingColor="
					+ movingColor + ", komi=" + komi + ", expectedMove="
					+ expectedMove + ", expectedScore=" + expectedScore + ", evaluatorParameters=" + evaluatorParameters + "]";
		}
		
		public Goban getGoban() throws Exception {
			if (goban == null) {
				File inputFile = new File(baseDir, sgfFile);
		    	GameTree gameTree = new GameTree(inputFile);
		    	goban = gameTree.getLeafs().get(0).getGoban();
			}
			return goban.clone();
		}

		public BoardType getMovingColor() {
			return movingColor;
		}

		public double getKomi() {
			return komi;
		}

		public Point getExpectedMove() {
			return expectedMove;
		}

		public double getExpectedScore() {
			return expectedScore;
		}

		public double getTolerance() {
			return tolerance;
		}
	}
	
	private class TestClassRunnerForSGFSuite extends BlockJUnit4ClassRunner 
	{
		private final SGFTestCase parameters;

		TestClassRunnerForSGFSuite(Class<?> type, SGFTestCase parameters) throws InitializationError 
		{
			super(type);
			this.parameters = parameters;
		}

		@Override
		public Object createTest() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(parameters);	
		}

		@Override
		protected String getName() {
			return String.format("[%s]", parameters.sgfFile);
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s[%s]", method.getName(), parameters.toString());
		}

		@Override
		protected void validateConstructor(List<Throwable> errors) {
			validateOnlyOneConstructor(errors);
		}

		@Override
		protected Statement classBlock(RunNotifier notifier) {
			return childrenInvoker(notifier);
		}

		@Override
		protected Annotation[] getRunnerAnnotations() {
			return new Annotation[0];
		}
		
		@Override
		public String toString() {
			return "TestClassRunnerForSGFSuite [parameters=" + parameters
					+ ", testClass=" + getTestClass().getJavaClass() + "]";
		}

	}

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public SGFSuite(Class<?> testClass) throws Throwable 
	{
		super(testClass, Collections.<Runner> emptyList());
		List<SGFTestCase> parametersList = getParametersList(getTestClass());
		for (SGFTestCase parameters : parametersList) {
			runners.add(new TestClassRunnerForSGFSuite(getTestClass().getJavaClass(), parameters));
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	private List<SGFTestCase> getParametersList(TestClass testClass) throws Throwable {
		ArrayList<SGFTestCase> parameters = new ArrayList<SGFTestCase>();
		SuiteConfig config = testClass.getJavaClass().getAnnotation(SuiteConfig.class);
		TestCases cases = null;
		InputStream is = null;
		try {
			is = new FileInputStream(config.value());
			JAXBContext jc = JAXBContext.newInstance(TestCases.class);
			Unmarshaller u = jc.createUnmarshaller();
			cases = (TestCases) u.unmarshal(is);
		}
		catch (IOException ex) {
			throw new Exception("Could not parse suite configuration file " + config.value(), ex);
		}
		finally {
			if (is != null)
				is.close();
		}

		if (cases != null && cases.testCases != null) {
			for (SGFTestCase tc : cases.testCases) {
				if (tc.evaluatorParameters == null) {
					tc.evaluatorParameters = cases.evaluatorParameters;
				}
				parameters.add(tc);
			}
	    }	
		return parameters;
	}

}
