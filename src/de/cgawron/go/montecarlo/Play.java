package de.cgawron.go.montecarlo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.SwingWorker;
import javax.swing.border.CompoundBorder;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.Point;
import de.cgawron.go.montecarlo.Evaluator.EvaluatorEvent;
import de.cgawron.go.render.JGoban;
import de.cgawron.go.sgf.MarkupModel;
import de.cgawron.go.sgf.MarkupModel.Markup;
import de.cgawron.go.sgf.SimpleMarkupModel;

public class Play extends JFrame implements Evaluator.EvaluatorListener
{
	private static final long serialVersionUID = 1L;
	static Logger logger = Logger.getLogger(Play.class.getName());
	
	private JGoban gobanUI;
	private Evaluator evaluator;
	private MarkupModel goban = new SimpleMarkupModel(5);
	private JTextField moveNo;
	private JTextField value;
	private JTextField simulations;

	private class PlayAction extends JGoban.GobanAction
	{
		@Override
		public void actionPerformed(JGoban.GobanActionEvent e) {
			play(e);
		}
	}
	
	public Play() throws HeadlessException {
		super();
		goban.putStone(2, 2, BoardType.BLACK);
		goban.resetMarkup();
		
		setTitle("RanGo");
		evaluator = new Evaluator();
		evaluator.addEvaluatorListener(this);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{387, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0};
		gridBagLayout.rowWeights = new double[]{0.0};
		getContentPane().setLayout(gridBagLayout);
		gobanUI = new JGoban();
		//gobanUI.setBoardSize((short) 5);
		gobanUI.setModel(goban);
		gobanUI.setAction(new PlayAction());

		GridBagConstraints gbc_goban = new GridBagConstraints();
		gbc_goban.anchor = GridBagConstraints.WEST;
		gbc_goban.weighty = 1.0;
		gbc_goban.weightx = 1.0;
		gbc_goban.fill = GridBagConstraints.BOTH;
		gbc_goban.gridx = 0;
		gbc_goban.gridy = 0;
		getContentPane().add(gobanUI, gbc_goban);
		
		JPanel evaluatorPanel = new JPanel();
		evaluatorPanel.setBorder(new CompoundBorder(new CompoundBorder(), null));
		GridBagConstraints gbc_evaluatorPanel = new GridBagConstraints();
		gbc_evaluatorPanel.weightx = 2.0;
		gbc_evaluatorPanel.fill = GridBagConstraints.BOTH;
		gbc_evaluatorPanel.gridx = 1;
		gbc_evaluatorPanel.gridy = 0;
		getContentPane().add(evaluatorPanel, gbc_evaluatorPanel);
		evaluatorPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow(3)"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblMove = new JLabel("Move");
		evaluatorPanel.add(lblMove, "2, 2, right, default");
		
		moveNo = new JTextField();
		moveNo.setEnabled(false);
		moveNo.setEditable(false);
		evaluatorPanel.add(moveNo, "4, 2, fill, default");
		moveNo.setColumns(10);
		
		JLabel lblValue = new JLabel("Value");
		evaluatorPanel.add(lblValue, "2, 4, right, default");
		
		value = new JTextField();
		value.setEnabled(false);
		value.setEditable(false);
		evaluatorPanel.add(value, "4, 4, fill, default");
		value.setColumns(10);
		
		simulations = new JTextField();
		simulations.setEditable(false);
		evaluatorPanel.add(simulations, "4, 20, fill, default");
		simulations.setColumns(10);
		
		JLabel lblSimulations = new JLabel("Simulations");
		evaluatorPanel.add(lblSimulations, "2, 20, right, default");
		
		
		//goban.putStone(5, 5, BoardType.BLACK);
	}

	protected void play(JGoban.GobanActionEvent e) {
		logger.info("event: " + e);
		goban.move(e.getPoint(), BoardType.WHITE);
		gobanUI.setEnabled(false);
		final BoardType movingColor = BoardType.BLACK;
		
		SwingWorker<String, Object> move = new SwingWorker<String, Object>()
				{

					@Override
					protected String doInBackground() throws Exception {
						AnalysisNode node = new AnalysisNode(goban, movingColor, 0.5);
						evaluator.evaluate(node);
						AnalysisNode best = node.getBestChild();
						if (best.value < Evaluator.RESIGN)
							resign();
						else {
							Point p = best.move;
							goban.move(p, movingColor);
							gobanUI.setEnabled(true);
						}
						return "ready";
					}
			
				};

		move.execute();
	}

	protected void resign()
	{
		JOptionPane.showConfirmDialog(rootPane, "I resign", "Game over", JOptionPane.OK_OPTION);
		dispose();
	}

	public static void main(String[] args) throws Exception
	{
		Play play = new Play();
		play.setSize(800, 400);
		play.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		play.setVisible(true);
	}

	@Override
	public void doLayout() {
		super.doLayout();
	}

	@Override
	public void stateChanged(EvaluatorEvent event)
	{
		logger.info("Setting simulations to " + Integer.toString(event.outstanding));
		simulations.setText(Integer.toString(event.outstanding));	
		value.setText(Double.toString(event.root.getBestChild().value));
		goban.resetMarkup();
		for (AnalysisNode child : event.root.children) {
			if (child.move != null) {
				Markup m = new MarkupModel.Text(String.format("%.1f:%d", child.getValue(), child.visits));
				goban.setMarkup(child.move, m);
			}
		}
	}
}
