package de.cgawron.go.montecarlo;

import static org.junit.Assert.assertTrue;

import java.io.File;

import de.cgawron.go.Goban;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban.Cluster;
import de.cgawron.go.sgf.GameTree;

public class GobanTest {

	private File baseDir = new File("test/sgf");

	protected Goban getGoban(String inputSGF) throws Exception {
		File inputFile = new File(baseDir, inputSGF);
		GameTree gameTree = new GameTree(inputFile);
		return gameTree.getLeafs().get(0).getGoban();
	}

	protected void checkGoban(AnalysisGoban goban) {
		for (Cluster c : goban.clusters) {
			for (Point p : c.getPoints()) {
				assertTrue("check cluster " + c + " vs. boardRep at " + p, goban.getBoardRep(p) == c);
			}
			for (Cluster d : c.getNeighbors()) {
				assertTrue("check that neighbors are of different color", c.getColor() != d.getColor());
				assertTrue("check that cluster has size > 0", c.size() > 0);

			}
			for (Cluster n : c.getNeighbors()) {
				if (!n.getNeighbors().contains(c)) throw new NullPointerException();
				assertTrue("asymmetric neighborship of " + c + " and " + n, n.getNeighbors().contains(c));
				assertTrue("check that neighbor " + n.toString(true) + " of cluster " + c.toString(true) + " is a valid cluster", goban.clusters.contains(n));
			}
			

		}	
		
	}

}
