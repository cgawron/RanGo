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
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.Evaluator.EvaluatorParameters;
import de.cgawron.go.montecarlo.SGFSuite.TestCases;
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.sgf.Node;
import de.cgawron.go.sgf.Value.Result;

public class SGFSuite extends Suite {
	private static Logger logger = Logger.getLogger(SGFSuite.class.getName());


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
		
		@XmlElement(name="sgfFile")
		List<SGFFile> sgfFiles;
		
		@XmlElement(name="testcase")
		List<SGFTestCase> testCases = new ArrayList<SGFTestCase>();
	}

	public static class SGFFile
	{
		@XmlTransient
	    private File baseDir = new File("test/sgf");
	    
		@XmlTransient
		GameTree gameTree;

		@XmlID 
		@XmlAttribute
		public String gameID;
		
	    @XmlAttribute(name="sgfFile")
	    public String sgfFileName;

	    @XmlElement(name="testcase")
		List<SGFTestCase> testCases = null;
		
	    public
	    SGFFile()
	    {
	    }
	    
		SGFFile(String sgfFileName)
		{
			this.sgfFileName = sgfFileName;
			init();
		}

		void afterUnmarshal(javax.xml.bind.Unmarshaller unmarshaller, Object parent) 
		{
			logger.info("unmarshalling " + this);
			init();
		}
		
		public void init()
		{
			File inputFile = new File(baseDir, sgfFileName);
	    	try {
				gameTree = new GameTree(inputFile);
			} catch (Exception e) {
				throw new RuntimeException("error loading GameTree", e);
			}	
		}
		
		public Node getNode(String nodeName)
		{
			if (nodeName != null && nodeName.length() != 0) {
				return gameTree.getNodeByName(nodeName);
			}
			else {
		    	return gameTree.getLeafs().get(0);
			}
		}

		@Override
		public String toString()
		{
			return "SGFFile[" + sgfFileName + "]";
		}
		
		public GameTree getGameTree()
		{
			if (gameTree == null) 
				init();
			return gameTree;
		}
		
	}
	
	public class SGFFileRunner extends ParentRunner<Runner>
	{
		SGFFile sgfFile;
		List<Runner> runners = new ArrayList<Runner>();
		
		SGFFileRunner(Class<?> testClass, SGFFile sgfFile) throws InitializationError
		{
			super(testClass);
			this.sgfFile = sgfFile;
			if (sgfFile.testCases == null) {
				GameTree gt = sgfFile.getGameTree();
				for (Node node : gt.getNamedNodes()) {
					SGFTestCase tc = new SGFTestCase(sgfFile, node);
					tc.evaluatorParameters = testCases.evaluatorParameters;
					runners.add(new SGFRunner(testClass, tc));
				}
			}
			else {
				for (SGFTestCase tc : sgfFile.testCases) {
					runners.add(new SGFRunner(testClass, tc));
				}
			}
		}

		
		@Override
		protected String getName()
		{
			return sgfFile.toString();
		}


		@Override
		protected List<Runner> getChildren()
		{
			return runners;
		}

		@Override
		protected Description describeChild(Runner runner)
		{
			return runner.getDescription();
			//return Description.createTestDescription(testClass.class, runner.getName());
		}

		@Override
		protected void runChild(Runner runner, RunNotifier notifier)
		{
			runner.run(notifier);
		}

	}

	
	public static class SGFTestCase
	{
	    @XmlIDREF
	    @XmlAttribute
	    public SGFFile sgfFile;
	    
	    @XmlAttribute
	    public String sgfFileName;
	       
	    @XmlAttribute
		public BoardType movingColor;
	    
	    @XmlAttribute
	    public String nodeName;
	    
	    @XmlAttribute
	    public double komi;
		
	    @XmlElement(name="expected")
	    public Point expectedMove;
		
	    @XmlAttribute
	    public Double expectedScore;

	    @XmlAttribute
	    public double tolerance = 3;
	    
	    @XmlAttribute
	    public String name;

		@XmlElement(name="evaluatorParameters")
		EvaluatorParameters evaluatorParameters;
		
		@XmlTransient 
		protected Node node;
		
		public SGFTestCase()
		{
		}
		
		public SGFTestCase(SGFFile sgfFile, Node node)
		{
			this.sgfFile = sgfFile;
			this.node = node;
			this.name = node.getName();
			this.nodeName = node.getName();
		}
		
		@Override
		public String toString() {
			return "[file=" + sgfFile + ", name=" + name + ", node=" + nodeName + ", movingColor="
					+ getMovingColor() + ", komi=" + getKomi() + ", expectedMove="
					+ getExpectedMove() + ", expectedScore=" + getExpectedScore() + ", evaluatorParameters=" + evaluatorParameters + "]";
		}
		
		public Goban getGoban() throws Exception {
			Goban goban = getNode().getGoban();
			return goban.clone();
		}

		protected Node getNode()
		{
			if (node == null && sgfFile != null)
				node = sgfFile.getNode(nodeName);
			return node;
		}

		public BoardType getMovingColor() {
			if (movingColor == null) {
				Node node = getNode();
				movingColor = node.getColor().opposite();
			}
			return movingColor;
		}

		public double getKomi() {
			return komi;
		}

		public Point getExpectedMove() {
			if (expectedMove == null) {
				Node child = null;
				Node node = getNode();
				if (node != null)
					child = node.getChildAt(0);
				if (child != null)
					expectedMove = child.getPoint();
			}
			return expectedMove;
		}

		public Double getExpectedScore() {
			if (expectedScore == null) {
				Result result = sgfFile.getGameTree().getResult();
				logger.info("expecting" + result);
			}
			return expectedScore;
		}

		public double getTolerance() {
			return tolerance;
		}
		
		void afterUnmarshal(javax.xml.bind.Unmarshaller unmarshaller, Object parent) 
		{
			logger.info("unmarshalling " + this);
			if (sgfFileName != null) {
				sgfFile = new SGFFile(sgfFileName);
			}
			if (parent instanceof SGFFile)
				sgfFile = (SGFFile) parent;
		}

		public Object getName()
		{
			if (name != null) 
				return name;
			else 
				return toString();
		}
	}
	
	private class SGFRunner extends BlockJUnit4ClassRunner 
	{
		private final SGFTestCase testCase;

		SGFRunner(Class<?> type, SGFTestCase testCase) throws InitializationError 
		{
			super(type);
			this.testCase = testCase;
		}

		@Override
		public Object createTest() throws Exception {
			return getTestClass().getOnlyConstructor().newInstance(testCase);	
		}

		@Override
		protected String getName() {
			return String.format("%s", testCase.getName());
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s[%s]", method.getName(), testCase.toString());
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
			return "TestClassRunnerForSGFSuite [parameters=" + testCase
					+ ", testClass=" + getTestClass().getJavaClass() + "]";
		}

	}

	private final ArrayList<Runner> runners = new ArrayList<Runner>();
	private TestCases testCases;

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public SGFSuite(Class<?> testClass) throws Throwable 
	{
		super(testClass, Collections.<Runner> emptyList());
		runners.addAll(getRunners(getTestClass()));
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	private List<Runner> getRunners(TestClass testClass) throws Throwable {
		List<Runner> runners = new ArrayList<Runner>();
		SuiteConfig config = testClass.getJavaClass().getAnnotation(SuiteConfig.class);
		InputStream is = null;
		try {
			is = new FileInputStream(config.value());
			JAXBContext jc = JAXBContext.newInstance(TestCases.class);
			Unmarshaller u = jc.createUnmarshaller();
			testCases = (TestCases) u.unmarshal(is);
		}
		catch (IOException ex) {
			throw new Exception("Could not parse suite configuration file " + config.value(), ex);
		}
		finally {
			if (is != null)
				is.close();
		}

		if (testCases != null) {
			if (testCases.sgfFiles != null) {
				for (SGFFile sgfFile : testCases.sgfFiles) {
					runners.add(new SGFFileRunner(testClass.getJavaClass(), sgfFile));
				}
			}
			if (testCases.testCases != null) {
				for (SGFTestCase tc : testCases.testCases) {
					if (tc.evaluatorParameters == null) {
						tc.evaluatorParameters = testCases.evaluatorParameters;
					}	
					runners.add(new SGFRunner(testClass.getJavaClass(), tc));
				}
			}
	    }	
		return runners;
	}

}
