package ui;

import service.ReportService;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administrator reporting desk: three printable reports on the left, live
 * charts of campus activity on the right.
 */
public class ReportsPanel extends JPanel implements Refreshable {

    private final ReportService reports = new ReportService();

    private final JTextArea output = new JTextArea();
    private final JPanel charts = Theme.clear(new GridLayout(2, 1, 0, 16));

    private String currentTitle = "User activity";
    private String currentFile = "user-activity";

    public ReportsPanel(MainFrame frame) {
        super(new BorderLayout(16, 0));
        setOpaque(false);

        add(left(), BorderLayout.CENTER);

        charts.setPreferredSize(new Dimension(400, 400));
        add(charts, BorderLayout.EAST);
    }

    private JPanel left() {
        JPanel column = Theme.clear(new BorderLayout(0, 14));

        Theme.Card picker = new Theme.Card(new BorderLayout(0, 12));
        picker.pad(18, 20, 18, 20);

        JPanel head = Theme.clear(new BorderLayout(0, 4));
        head.add(Theme.h2("Generate a report"), BorderLayout.NORTH);
        head.add(Theme.label("Pick a report, read it here, then save it to the reports folder.",
                12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        picker.add(head, BorderLayout.NORTH);

        JPanel buttons = Theme.clear(new GridLayout(1, 4, 10, 0));
        buttons.add(Theme.solid("User activity")
                .onClick(() -> load("User activity", "user-activity", reports.userActivityReport())));
        buttons.add(Theme.ghost("Credit report", Theme.AMBER)
                .onClick(() -> load("Credit report", "credit-report", reports.creditReport())));
        buttons.add(Theme.ghost("Top skills", Theme.TEAL)
                .onClick(() -> load("Popular skills", "popular-skills", reports.popularSkillsReport())));
        buttons.add(Theme.quiet("Save to file").onClick(this::save));
        picker.add(buttons, BorderLayout.SOUTH);
        column.add(picker, BorderLayout.NORTH);

        output.setEditable(false);
        output.setFont(Theme.mono(12));
        output.setForeground(Theme.TEXT);
        output.setBackground(Theme.PANEL);
        output.setBorder(new javax.swing.border.EmptyBorder(14, 16, 14, 16));
        output.setCaretPosition(0);

        Theme.Card paper = new Theme.Card(new BorderLayout());
        paper.fill(Theme.PANEL).pad(6, 6, 6, 6);
        paper.add(Theme.scroll(output), BorderLayout.CENTER);
        column.add(paper, BorderLayout.CENTER);
        return column;
    }

    private void load(String title, String fileStem, String contents) {
        currentTitle = title;
        currentFile = fileStem;
        output.setText(contents);
        output.setCaretPosition(0);
    }

    private void save() {
        try {
            File file = reports.export(currentFile + "-" + System.currentTimeMillis() + ".txt",
                    output.getText());
            Dialogs.success(this, "Report saved",
                    currentTitle + " was written to\n" + file.getPath());
        } catch (Exception e) {
            Dialogs.error(this, "Could not save", String.valueOf(e.getMessage()));
        }
    }

    @Override
    public void refresh() {
        load(currentTitle, currentFile, reportFor(currentFile));

        charts.removeAll();

        Map<String, Integer> byCategory = new LinkedHashMap<>(reports.sessionsByCategory());
        Charts.Bars bars = new Charts.Bars(byCategory)
                .tones(Theme::categoryColor)
                .labelWidth(120)
                .emptyMessage("No completed sessions yet");
        charts.add(chartCard("Sessions by category", "Completed sessions across campus", bars));

        Charts.Columns columns = new Charts.Columns(reports.sessionsByMonth(6), Theme.VIOLET);
        charts.add(chartCard("Last six months", "Completed sessions per month", columns));

        charts.revalidate();
        charts.repaint();
    }

    private String reportFor(String stem) {
        switch (stem) {
            case "credit-report":  return reports.creditReport();
            case "popular-skills": return reports.popularSkillsReport();
            default:               return reports.userActivityReport();
        }
    }

    private Theme.Card chartCard(String title, String caption, javax.swing.JComponent chart) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.pad(18, 20, 18, 20);
        JPanel head = Theme.clear(new BorderLayout(0, 3));
        head.add(Theme.h2(title), BorderLayout.NORTH);
        head.add(Theme.label(caption, 12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }
}
