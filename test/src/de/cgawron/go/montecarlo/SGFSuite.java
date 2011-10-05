package de.cgawron.go.montecarlo;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import de.cgawron.go.sgf.GameTree;
import de.cgawron.go.Point;

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

	public static class SGFTestParameters
	{
	    private File baseDir = new File("test/sgf");
	    private File inputFile;
		private Goban goban;
		private BoardType movingColor;
		private double komi; 
		private Point expectedMove; 
		private double expectedScore;

		public SGFTestParameters(String sgfFile, BoardType movingColor, 
				                 double komi, Point expectedMove, double expectedScore) throws Exception
		{
			this.movingColor = movingColor;
			this.komi = komi;
			this.expectedMove = expectedMove;
			this.expectedScore = expectedScore;
	    	this.inputFile = new File(baseDir, sgfFile);
	    	GameTree gameTree = new GameTree(inputFile);
	    	goban = gameTree.getLeafs().get(0).getGoban();
		}
		
		@Override
		public String toString() {
			return "[file=" + inputFile + ", movingColor="
					+ movingColor + ", komi=" + komi + ", expectedMove="
					+ expectedMove + ", expectedScore=" + expectedScore + "]";
		}
		
		public Goban getGoban() {
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
	}
	
	private class TestClassRunnerForSGFSuite extends BlockJUnit4ClassRunner 
	{
		private final SGFTestParameters parameters;

		TestClassRunnerForSGFSuite(Class<?> type, SGFTestParameters parameters) throws InitializationError 
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
			return String.format("[%s]", parameters.toString());
		}

		@Override
		protected String testName(final FrameworkMethod method) {
			return String.format("%s", method.getName());
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
	
	}

	private final ArrayList<Runner> runners = new ArrayList<Runner>();

	/**
	 * Only called reflectively. Do not use programmatically.
	 */
	public SGFSuite(Class<?> testClass) throws Throwable 
	{
		super(testClass, Collections.<Runner> emptyList());
		List<SGFTestParameters> parametersList = getParametersList(getTestClass());
		for (SGFTestParameters parameters : parametersList) {
			runners.add(new TestClassRunnerForSGFSuite(getTestClass().getJavaClass(), parameters));
		}
	}

	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	private List<SGFTestParameters> getParametersList(TestClass testClass) throws Throwable {
		ArrayList<SGFTestParameters> parameters = new ArrayList<SGFTestParameters>();
		SuiteConfig config = testClass.getJavaClass().getAnnotation(SuiteConfig.class);

		parameters.add(new SGFTestParameters("ko1.sgf", BoardType.BLACK, 10, new Point(3, 1), 15 ));
		//parameters.add(new SGFTestParameters("ko1.sgf", BoardType.WHITE, 10, new Point(3, 1), 15 ));
		return parameters;
	}

}
