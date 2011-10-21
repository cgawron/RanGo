package de.cgawron.go.montecarlo;

import java.io.File;

import de.cgawron.go.Goban;
import de.cgawron.go.sgf.GameTree;

public class GobanTest {

	private File baseDir = new File("test/sgf");

	protected Goban getGoban(String inputSGF) throws Exception {
		File inputFile = new File(baseDir, inputSGF);
		GameTree gameTree = new GameTree(inputFile);
		return gameTree.getLeafs().get(0).getGoban();
	}

}
