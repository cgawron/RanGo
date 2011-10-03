/*
 * Copyright (C) 2011 Christian Gawron
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cgawron.go.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.MemoryImageSource;
import java.util.logging.Logger;

import de.cgawron.go.Goban;
import de.cgawron.go.Goban.BoardType;
import de.cgawron.go.sgf.MarkupModel;

/**
 * The UI delegate for a Goban component.
 * 
 * @author Christian Gawron
 */
public class SimpleGobanRenderer implements GobanRenderer {
	protected Image[] whiteStoneImages;
	protected Image wsi;
	protected Image gobanBackground;
	protected Image bsi;

	protected float scale = 1;
	protected double drw = 0.02;
	protected double drb = 0.01;
	private double drc = 0.005;

	private Color background = new Color(209, 181, 135);

	private static Logger logger = Logger.getLogger(SimpleGobanRenderer.class
			.getName());

	private Font font = null;

	public SimpleGobanRenderer() {
		try {
			logger.info("Loading font GillSans");
			java.net.URL url = Goban.class.getResource("gillsans.ttf");
			font = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
		} catch (Exception ex) {
			logger.warning("Could not load font: " + ex);
			logger.info("Using font SanSerif");
			font = new Font("SansSerif", Font.PLAIN, 1);// .deriveFont(0.5f);
			//throw new RuntimeException(ex);
		}
	}

	protected Color getBackground() {
		return background;
	}

	protected Color getForeground() {
		return Color.black;
	}

	protected void drawBoard(Graphics2D g, Goban model) {
		int boardSize = model.getBoardSize();
		int max = boardSize;
		//logger.info("SimpleGobanRender: Size is " + boardSize);

		g.setColor(getBackground());
		g.fill(new Rectangle2D.Double(0, 0, scale * max, scale * max));
		// g.fillRect(0, 0, scale*max, scale*max);
		g.setColor(getForeground());
	}

	protected void drawBackground(Graphics2D g, Goban model) {
		short i, j;
		java.awt.Point p, q;
		int boardSize = model.getBoardSize();
		int max = boardSize - 1;
		logger.info("SimpleGobanRender: Size is " + boardSize);

		drawBoard(g, model);
		g.setColor(getForeground());

		Line2D.Double line1 = new Line2D.Double();
		Line2D.Double line2 = new Line2D.Double();
		for (i = 0; i < boardSize; i++) {
			line1.setLine(scale * (i + 0.5), scale * (0.5), scale * (i + 0.5),
					scale * (max + 0.5));
			line2.setLine(scale * (0.5), scale * (i + 0.5),
					scale * (max + 0.5), scale * (i + 0.5));
			g.draw(line1);
			g.draw(line2);
		}

		BasicStroke stroke = (BasicStroke) g.getStroke();
		Shape boundary = new Rectangle2D.Double(scale * 0.5, scale * 0.5, scale
				* max, scale * max);
		g.setStroke(new BasicStroke(2 * stroke.getLineWidth()));
		g.draw(boundary);
		g.setStroke(stroke);

		int m = boardSize >> 1;
		int h = boardSize > 9 ? 3 : 2;

		if (boardSize % 2 != 0) {
			drawHoshi(g, m, m);

			if (boardSize > 9) {
				drawHoshi(g, h, m);
				drawHoshi(g, boardSize - h - 1, m);
				drawHoshi(g, m, h);
				drawHoshi(g, m, boardSize - h - 1);
			}
		}

		if (boardSize > 7) {
			drawHoshi(g, h, h);
			drawHoshi(g, h, boardSize - h - 1);
			drawHoshi(g, boardSize - h - 1, h);
			drawHoshi(g, boardSize - h - 1, boardSize - h - 1);
		}
	}

	protected void drawHoshi(Graphics2D g, int x, int y) {
		double r = 0.1;
		Shape c = new Ellipse2D.Double(scale * (x + 0.5 - r), scale
				* (y + 0.5 - r), scale * 2 * r, scale * 2 * r);
		g.draw(c);
		g.fill(c);
	}

	protected void drawEmpty(Graphics2D g, int x, int y) {
		double r = 0.75 * 0.5;
		Shape s = new Ellipse2D.Double(scale * (x + 0.5 - r), scale
				* (y + 0.5 - r), scale * (2 * r), scale * (2 * r));
		Color c = g.getColor();
		g.setColor(getBackground());
		g.fill(s);
		g.setColor(c);
	}

	protected void drawString(Graphics2D graphics, short x, short y, String s) {
		Graphics2D g = (Graphics2D) graphics.create();

		g.setFont(font);
		FontRenderContext frc = g.getFontRenderContext();
		TextLayout layout = new TextLayout(s, font, frc);
		Rectangle2D bounds = layout.getBounds();
		Rectangle2D maxBounds = font.getMaxCharBounds(frc);
		double width = layout.getAdvance();
		double height = bounds.getHeight();
		double maxHeight = maxBounds.getHeight();
		double h = 0.7;
		double w = 0.7;
		double textscale = (h / maxHeight < w / width ? h / maxHeight : w
				/ width);
		// double scale = w/width;
		g.translate(scale * (x + 0.5 - 0.5 * textscale * width), scale
				* (y + 0.5 - 0.5 * textscale * height));
		g.scale(scale * textscale, scale * textscale);
		// g.draw(new Rectangle2D.Double(0, 0, width, height));
		layout.draw(g, 0f, (float) height);
	}

	protected void drawMarkup(Graphics2D g, short x, short y, MarkupModel model) {
		MarkupModel.Markup m = model.getMarkup(x, y);
		BoardType color = model.getStone(x, y);
		Color bg;
		Color fg;
		if (color == BoardType.BLACK) {
			bg = Color.black;
			fg = Color.white;
		} else if (color == BoardType.WHITE) {
			bg = Color.white;
			fg = Color.black;
		} else {
			fg = Color.black;
			bg = getBackground();
		}
		if (m instanceof MarkupModel.Move) {
			MarkupModel.Move move = (MarkupModel.Move) m;
			logger.fine("Move at " + x + ", " + y);
			if (move.getColor() == BoardType.BLACK) {
				drawBlackStone(g, x, y);
				g.setColor(Color.white);
			} else {
				drawWhiteStone(g, x, y);
				g.setColor(Color.black);
			}

			int modulus = Integer.getInteger("de.cgawron.go.goban.modulus", 0)
					.intValue();
			int moveNo = move.getMoveNo();
			if (modulus > 0)
				while (moveNo > modulus)
					moveNo -= modulus;
			String text = Integer.toString(moveNo);
			drawString(g, x, y, text);
		} else if (m instanceof MarkupModel.Text) {
			MarkupModel.Text text = (MarkupModel.Text) m;
			logger.fine("Text " + text + " at " + x + ", " + y);
			if (color == BoardType.EMPTY) {
				drawEmpty(g, x, y);
				g.setColor(Color.black);
			} else {
				drawStone(g, x, y, color);
				if (color == BoardType.BLACK)
					g.setColor(Color.white);
				else
					g.setColor(Color.black);
			}
			drawString(g, x, y, text.toString());
		} else if (m instanceof MarkupModel.ConflictMark) {
			MarkupModel.ConflictMark c = (MarkupModel.ConflictMark) m;
			logger.fine("Conflict at " + x + ", " + y);
			drawStone(g, x, y, c.getColor());
			if (c.getColor() == BoardType.BLACK)
				g.setColor(Color.white);
			else
				g.setColor(Color.black);
			drawString(g, x, y, c.toString());
		} else if (m instanceof MarkupModel.Stone) {
			MarkupModel.Stone stone = (MarkupModel.Stone) m;
			logger.fine("Stone at " + x + ", " + y);
			drawStone(g, x, y, stone.getColor());
		} else if (m instanceof MarkupModel.Triangle) {
			logger.fine("Triangle at " + x + ", " + y);
			drawStone(g, x, y, color);
			float r = 0.445f;
			float c30 = 0.8660254f;
			float s30 = 0.5f;
			GeneralPath p = new GeneralPath();
			p.moveTo(scale * (x + 0.5f - c30 * r), scale * (y + 0.5f + r * s30));
			p.lineTo(scale * (x + 0.5f), scale * (y + 0.5f - r));
			p.lineTo(scale * (x + 0.5f + c30 * r), scale * (y + 0.5f + r * s30));
			p.closePath();
			Graphics2D g1 = (Graphics2D) g.create();
			g1.setStroke(new BasicStroke(0.05f));
			g1.setColor(fg);
			g1.draw(p);
		} else if (m instanceof MarkupModel.Square) {
			logger.fine("Square at " + x + ", " + y);
			drawStone(g, x, y, color);
			float r = 0.445f * 0.70710678f;
			GeneralPath p = new GeneralPath();
			p.moveTo(scale * (x + 0.5f - r), scale * (y + 0.5f - r));
			p.lineTo(scale * (x + 0.5f - r), scale * (y + 0.5f + r));
			p.lineTo(scale * (x + 0.5f + r), scale * (y + 0.5f + r));
			p.lineTo(scale * (x + 0.5f + r), scale * (y + 0.5f - r));
			p.closePath();

			Graphics2D g1 = (Graphics2D) g.create();
			g1.setStroke(new BasicStroke(0.05f));
			g1.setColor(fg);
			g1.draw(p);
		} else if (m instanceof MarkupModel.Mark) {
			logger.info("Mark at " + x + ", " + y);
			drawStone(g, x, y, color);
			float r = 0.45f * 0.70710678f;
			GeneralPath p = new GeneralPath();
			p.moveTo(scale * (x + 0.5f - r), scale * (y + 0.5f - r));
			p.lineTo(scale * (x + 0.5f + r), scale * (y + 0.5f + r));
			p.moveTo(scale * (x + 0.5f + r), scale * (y + 0.5f - r));
			p.lineTo(scale * (x + 0.5f - r), scale * (y + 0.5f + r));

			Graphics2D g1 = (Graphics2D) g.create();
			g1.setStroke(new BasicStroke(0.05f));
			g1.setColor(fg);
			g1.draw(p);
		} else if (m instanceof MarkupModel.Circle) {
			logger.info("Circle at " + x + ", " + y);
			drawStone(g, x, y, color);
			float r = 0.2f;
			Shape p = new Ellipse2D.Double(scale * (x + r), scale * (y + r),
					scale * (1 - 2 * r), scale * (1 - 2 * r));

			Graphics2D g1 = (Graphics2D) g.create();
			g1.setStroke(new BasicStroke(0.05f));
			g1.setColor(fg);
			g1.draw(p);
		} else if (m instanceof MarkupModel.WhiteTerritory) {
			logger.info("WhiteTerritory at " + x + ", " + y);
			float r = 0.2f;
			Shape p = new Rectangle2D.Double(scale * x, scale * y, scale * 1,
					scale * 1);

			Graphics2D g1 = (Graphics2D) g.create();
			g1.setColor(new Color(1.0f, 1.0f, 1.0f, 0.5f));
			g1.fill(p);
		} else if (m instanceof MarkupModel.BlackTerritory) {
			logger.info("BlackTerritory at " + x + ", " + y);
			float r = 0.2f;
			Shape p = new Rectangle2D.Double(scale * x, scale * y, scale * 1,
					scale * 1);

			Graphics2D g1 = (Graphics2D) g.create();
			g1.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
			g1.fill(p);
		} else if (m != null)
			logger.warning("unknown markup: " + m + " (" + m.getClass() + ")");
	}

	protected void drawStone(Graphics2D g, short x, short y, BoardType c) {
		logger.fine("drawStone: " + c);

		if (c == BoardType.WHITE)
			drawWhiteStone(g, x, y);
		else if (c == BoardType.BLACK)
			drawBlackStone(g, x, y);
		else if (gobanBackground != null) {
			// g.drawImage(gobanBackground, x, y, x+1, y+1, this);
		}
	}

	public void drawBlackStone(Graphics2D g, short x, short y) {
		// g.drawImage(bsi, t.x - gridWidth, t.y - gridWidth, this);
		Shape c = new Ellipse2D.Double(x + drb, y + drb, 1 - 2 * drb,
				1 - 2 * drb);
		g.setPaint(Color.black);
		g.draw(c);
		g.fill(c);
	}

	public void drawWhiteStone(Graphics2D g, short x, short y) {
		// g.drawImage(wsi, t.x - gridWidth, t.y - gridWidth, this);
		Shape c;

		c = new Ellipse2D.Double(scale * (x + drc), scale * (y + drc), scale
				* (1 - 2 * drc), scale * (1 - 2 * drc));
		g.setPaint(getBackground());
		g.fill(c);
		c = new Ellipse2D.Double(scale * (x + drw), scale * (y + drw), scale
				* (1 - 2 * drw), scale * (1 - 2 * drw));
		g.setPaint(Color.black);
		g.draw(c);
		g.setPaint(Color.white);
		g.fill(c);

		logger.fine("drawWhiteStone");
	}

	public Image getBsi() {
		return bsi;
	}

	public Image[] getWhiteStoneImages() {
		return whiteStoneImages;
	}

	public Image getWsi() {
		return wsi;
	}

	protected void initImages(JGoban goban) {
		int radius = 1; // gridWidth;
		int pixel[];
		int x, y;
		int i = 0;

		pixel = new int[4 * radius * radius + 4 * radius + 1];
		int d;
		for (y = -radius; y <= radius; y++)
			for (x = -radius; x <= radius; x++) {
				d = x * x + y * y - radius * radius;
				if (d > 1)
					pixel[i] = 0;
				else {
					int opaque = d < 0 ? 255 : 0;
					double lx = 0.35355339, ly = 0.35355339, lz = 0.8660254;
					double s = Math.sin(0.3);
					double c = Math.cos(0.3);
					double l = x * s - y * c;
					double nz = Math.sqrt(radius * radius - x * x - y * y);
					double z = 1.0 - (nz / radius);
					double co = Math.IEEEremainder(l + 8 * z * z, 4) / 4;
					double nDotL = (x * lx + y * ly + nz * lz) / radius;
					double bright = (2.0 * nz * nDotL) / radius - lz;
					if (bright < 0)
						bright = 0;
					double lambertian = nDotL;
					bright *= bright;
					bright *= bright;
					bright *= bright;
					bright *= bright;

					if (co < 0)
						co *= -2;
					co = 0.8 + 0.2 * co;
					co = 0.4 * bright * co + co * (0.3 * lambertian + 0.7);
					if (co > 1)
						co = 1;

					int grey = new Double(255 * co).intValue();
					pixel[i] = (opaque << 24) | (grey << 16) | (grey << 8)
							| (grey);
				}
				i++;
			}
		wsi = goban.createImage(new MemoryImageSource(2 * radius + 1,
				2 * radius + 1, pixel, 0, 2 * radius + 1));
		i = 0;
		pixel = new int[4 * radius * radius + 4 * radius + 1];
		for (y = -radius; y <= radius; y++)
			for (x = -radius; x <= radius; x++) {
				d = x * x + y * y - radius * radius;
				if (d > 1)
					pixel[i] = 0;
				else {
					int opaque = d < 0 ? 255 : 100;
					double lx = 0.35355339, ly = 0.35355339, lz = 0.8660254;
					double nz = Math.sqrt(radius * radius - x * x - y * y);
					double z = 1.0 - (nz / radius);
					double nDotL = (x * lx + y * ly + nz * lz) / radius;
					double bright = (2.0 * nz * nDotL) / radius - lz;
					if (bright < 0)
						bright = 0;
					double lambertian = nDotL;
					bright = bright * bright;

					double co = 0.3;
					co = 0.3 * bright + co * (0.3 * lambertian + 0.7);
					if (co > 1)
						co = 1;

					int grey = new Double(255 * co).intValue();
					pixel[i] = (opaque << 24) | (grey << 16) | (grey << 8)
							| (grey);
				}
				i++;
			}
		bsi = goban.createImage(new MemoryImageSource(2 * radius + 1,
				2 * radius + 1, pixel, 0, 2 * radius + 1));
	}

	public void paint(Graphics2D g2d, Goban model) {
		logger.info("SimpleGobanRenderer.paint: start");
		logger.fine("SimpleGobanRender: Model is " + model);
		int i, j;
		int boardSize = model.getBoardSize();

		if (g2d != null) {
			drawBackground(g2d, model);
			if (model != null) {
				short x, y;
				for (y = 0; y < boardSize; y++)
					for (x = 0; x < boardSize; x++) {
						if (model instanceof MarkupModel) {
							logger.fine(x + ", " + y + ": stone "
									+ model.getStone(x, y));
							logger.fine(x + ", " + y + ": markup "
									+ ((MarkupModel) model).getMarkup(x, y));
							if (model.getStone(x, y) != BoardType.EMPTY)
								assert ((MarkupModel) model).getMarkup(x, y) != null : "Markup is null on non-empty field!";
							drawMarkup(g2d, x, y, (MarkupModel) model);
						} else if (model.getStone(x, y) == BoardType.WHITE) {
							logger.fine(x + ", " + y + ": " + "White");
							drawWhiteStone(g2d, x, y);
						} else if (model.getStone(x, y) == BoardType.BLACK) {
							logger.fine(x + ", " + y + ": " + "Black");
							drawBlackStone(g2d, x, y);
						}
					}
			} else {
				logger.fine("null model");
			}
		}
		//logger.info("SimpleGobanRenderer.paint: end");
	}

	public void setBsi(Image newBsi) {
		bsi = newBsi;
	}

	public void setWhiteStoneImages(Image[] newWhiteStoneImages) {
		whiteStoneImages = newWhiteStoneImages;
	}

	public void setWsi(Image newWsi) {
		wsi = newWsi;
	}

}
