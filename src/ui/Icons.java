package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Small vector icons drawn by hand so the app needs no image assets. */
public final class Icons {

    private Icons() { }

    /** Draws icon {@code name} in a size x size box with its top left at (x, y). */
    public static void draw(Graphics2D g, String name, double x, double y, double size, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        g2.scale(size / 24.0, size / 24.0);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (name) {
            case "home": {
                GeneralPath p = new GeneralPath();
                p.moveTo(3, 11); p.lineTo(12, 3.5); p.lineTo(21, 11);
                g2.draw(p);
                g2.draw(new RoundRectangle2D.Double(5.5, 11, 13, 9.5, 2.5, 2.5));
                g2.draw(new RoundRectangle2D.Double(10, 14.5, 4, 6, 1, 1));
                break;
            }
            case "grid": {
                g2.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 7, 2, 2));
                g2.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 7, 2, 2));
                g2.draw(new RoundRectangle2D.Double(3.5, 13.5, 7, 7, 2, 2));
                g2.draw(new RoundRectangle2D.Double(13.5, 13.5, 7, 7, 2, 2));
                break;
            }
            case "calendar": {
                g2.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 3, 3));
                line(g2, 3, 10, 21, 10);
                line(g2, 8, 3, 8, 6);
                line(g2, 16, 3, 16, 6);
                g2.fill(new Ellipse2D.Double(8, 13, 2.4, 2.4));
                g2.fill(new Ellipse2D.Double(14, 13, 2.4, 2.4));
                break;
            }
            case "coin": {
                g2.draw(new Ellipse2D.Double(3.2, 3.2, 17.6, 17.6));
                g2.draw(new Ellipse2D.Double(6.8, 6.8, 10.4, 10.4));
                line(g2, 12, 8, 12, 16);
                break;
            }
            case "trophy": {
                GeneralPath cup = new GeneralPath();
                cup.moveTo(7, 3.5); cup.lineTo(17, 3.5); cup.lineTo(16.4, 10.5);
                cup.curveTo(16.4, 14, 13.8, 15.2, 12, 15.2);
                cup.curveTo(10.2, 15.2, 7.6, 14, 7.6, 10.5);
                cup.closePath();
                g2.draw(cup);
                g2.draw(new java.awt.geom.Arc2D.Double(2.8, 4.4, 4.6, 5.6, 90, 180, Path2D.WIND_NON_ZERO));
                g2.draw(new java.awt.geom.Arc2D.Double(16.6, 4.4, 4.6, 5.6, 270, 180, Path2D.WIND_NON_ZERO));
                line(g2, 12, 15, 12, 18);
                line(g2, 8, 20.5, 16, 20.5);
                line(g2, 9.5, 18, 14.5, 18);
                break;
            }
            case "user": {
                g2.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                g2.draw(new java.awt.geom.Arc2D.Double(3.8, 13.5, 16.4, 15, 0, 180, Path2D.WIND_NON_ZERO));
                break;
            }
            case "chart": {
                line(g2, 3.5, 20.5, 20.5, 20.5);
                g2.fill(new RoundRectangle2D.Double(5.5, 12, 3.6, 7, 1.4, 1.4));
                g2.fill(new RoundRectangle2D.Double(10.2, 7, 3.6, 12, 1.4, 1.4));
                g2.fill(new RoundRectangle2D.Double(14.9, 3.5, 3.6, 15.5, 1.4, 1.4));
                break;
            }
            case "shield": {
                GeneralPath p = new GeneralPath();
                p.moveTo(12, 3); p.lineTo(20, 6); p.lineTo(20, 12);
                p.curveTo(20, 17, 16, 20, 12, 21.2);
                p.curveTo(8, 20, 4, 17, 4, 12);
                p.lineTo(4, 6);
                p.closePath();
                g2.draw(p);
                GeneralPath tick = new GeneralPath();
                tick.moveTo(8.6, 12); tick.lineTo(11, 14.4); tick.lineTo(15.6, 9.2);
                g2.draw(tick);
                break;
            }
            case "search": {
                g2.draw(new Ellipse2D.Double(4, 4, 12.5, 12.5));
                line(g2, 16, 16, 20.5, 20.5);
                break;
            }
            case "plus": {
                line(g2, 12, 5, 12, 19);
                line(g2, 5, 12, 19, 12);
                break;
            }
            case "logout": {
                GeneralPath p = new GeneralPath();
                p.moveTo(14, 4); p.lineTo(5.5, 4); p.lineTo(5.5, 20); p.lineTo(14, 20);
                g2.draw(p);
                line(g2, 11, 12, 20.5, 12);
                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(17, 8.2); arrow.lineTo(20.8, 12); arrow.lineTo(17, 15.8);
                g2.draw(arrow);
                break;
            }
            case "spark": {
                GeneralPath p = new GeneralPath();
                p.moveTo(13.4, 2.4); p.lineTo(5.2, 13.4); p.lineTo(11.2, 13.4);
                p.lineTo(10.2, 21.6); p.lineTo(18.6, 10.4); p.lineTo(12.6, 10.4);
                p.closePath();
                g2.fill(p);
                break;
            }
            case "star": {
                g2.fill(star(12, 12, 9.4, 4.2, 5));
                break;
            }
            case "clock": {
                g2.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                line(g2, 12, 7.5, 12, 12.4);
                line(g2, 12, 12.4, 15.8, 14.6);
                break;
            }
            case "book": {
                GeneralPath p = new GeneralPath();
                p.moveTo(4, 4.5); p.lineTo(10, 4.5); p.curveTo(11.2, 4.5, 12, 5.4, 12, 6.6);
                p.lineTo(12, 20); p.curveTo(12, 18.8, 11.2, 18, 10, 18); p.lineTo(4, 18);
                p.closePath();
                g2.draw(p);
                GeneralPath q = new GeneralPath();
                q.moveTo(20, 4.5); q.lineTo(14, 4.5); q.curveTo(12.8, 4.5, 12, 5.4, 12, 6.6);
                q.lineTo(12, 20); q.curveTo(12, 18.8, 12.8, 18, 14, 18); q.lineTo(20, 18);
                q.closePath();
                g2.draw(q);
                break;
            }
            default:
                g2.draw(new Ellipse2D.Double(4, 4, 16, 16));
        }
        g2.dispose();
    }

    private static void line(Graphics2D g2, double x1, double y1, double x2, double y2) {
        g2.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }

    /** A filled star polygon, used by ratings and badges. */
    public static GeneralPath star(double cx, double cy, double outer, double inner, int points) {
        GeneralPath p = new GeneralPath();
        double angle = -Math.PI / 2;
        double step = Math.PI / points;
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double px = cx + Math.cos(angle) * r;
            double py = cy + Math.sin(angle) * r;
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
            angle += step;
        }
        p.closePath();
        return p;
    }
}
