package de.cgawron.go.gtp;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;

import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.gtp.GtpCallback;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpEngine;
import net.sf.gogui.gtp.GtpError;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.AnalysisGoban;
import de.cgawron.go.montecarlo.AnalysisNode;
import de.cgawron.go.montecarlo.Evaluator;

public class RanGoEngine extends GtpEngine
{

	private static final String OK = "ok"; //$NON-NLS-1$
	private static final String ENGINE_VERSION = "0.1"; //$NON-NLS-1$
	private static final String ENGINE_NAME = "RanGoEngine"; //$NON-NLS-1$

	private AnalysisGoban goban = null;
	private Evaluator evaluator = new Evaluator();

	public RanGoEngine(PrintStream log)
	{
		super(log);
		setName(ENGINE_NAME);
		setVersion(ENGINE_VERSION);
		registerCommands(this);
	}

	private void registerCommands(final Object o)
	{
		for (final Method m : o.getClass().getMethods()) {
			if (m.isAnnotationPresent(GtpCmd.class)) {
				register(commandName(m),
				         new GtpCallback() {
					         public void run(GtpCommand cmd) throws GtpError
					         {
						         try {
							         m.invoke(o, cmd);
						         } catch (Exception e) {
							         throw new GtpError(e.getLocalizedMessage());
						         }
					         }
				         });
			}
		}

	}

	private String commandName(Method m)
	{
		return m.getName();
	}

	@GtpCmd
	public void boardsize(GtpCommand cmd) throws GtpError
	{
		int size = cmd.getIntArg();
		goban = new AnalysisGoban(size);
		cmd.setResponse(OK);
	}

	@GtpCmd
	public void clear_board(GtpCommand cmd) throws GtpError
	{
		goban.clear();
		cmd.setResponse(OK);
	}

	@GtpCmd
	public void play(GtpCommand cmd) throws GtpError
	{
		GoColor c = cmd.getColorArg(0);
		GoPoint p = cmd.getPointArg(1, goban.getBoardSize());

		BoardType color = c == GoColor.BLACK ? BoardType.BLACK : BoardType.WHITE;
		goban.move(p.getX(), p.getY(), color);
		cmd.setResponse(OK);
	}

	@GtpCmd
	public void genmove(GtpCommand cmd) throws GtpError
	{
		GoColor c = cmd.getColorArg();
		AnalysisNode node = new AnalysisNode(goban, c == GoColor.BLACK ? BoardType.BLACK : BoardType.WHITE, 1.5);
		evaluator.evaluate(node);
		AnalysisNode best = node.getBestChild();
		goban = best.getGoban();
		Point p = best.getMove();
		cmd.getResponse().append(String.format("%c%d", 'A' + p.getX(), 1 + p.getY()));
	}

	public static void main(String args[]) throws IOException
	{
		GtpEngine engine = new RanGoEngine(System.err);
		engine.mainLoop(System.in, System.out);
	}
}
