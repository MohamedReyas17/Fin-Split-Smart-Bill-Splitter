package com.finsplit;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.io.*;
import java.util.Locale;

public class FinSplitApp extends JFrame {
    // Keep the same table models, column order, and finsplit.dat format for compatibility.
    private final JLabel balanceLabel = new JLabel("₹0.00");
    private final JLabel incomeSummary = new JLabel("₹0.00");
    private final JLabel expenseSummary = new JLabel("₹0.00");
    private final JLabel savingsSummary = new JLabel("0 goals");
    private final DefaultTableModel incomeModel = model("Description", "Amount (₹)");
    private final DefaultTableModel expenseModel = model("Description", "Amount (₹)");
    private final DefaultTableModel billModel = model("Bill", "Total (₹)", "People", "Each pays (₹)");
    private final DefaultTableModel budgetModel = model("Category", "Limit (₹)");
    private final DefaultTableModel savingsModel = model("Goal", "Target (₹)", "Saved (₹)");
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pageHost = new JPanel(cardLayout);
    private final JLabel pageTitle = new JLabel("Overview");
    private final JLabel pageSubtitle = new JLabel("Your money, clearly organised.");
    private final JButton[] navButtons = new JButton[7];
    private double totalIncome = 0, totalExpenses = 0;
    private static final String DATA_FILE = "finsplit.dat";

    private static final Color BG = new Color(13, 18, 32);
    private static final Color SIDEBAR = new Color(17, 24, 41);
    private static final Color PANEL = new Color(24, 33, 53);
    private static final Color PANEL_ALT = new Color(30, 41, 64);
    private static final Color BORDER = new Color(47, 60, 86);
    private static final Color TEXT = new Color(238, 243, 252);
    private static final Color MUTED = new Color(151, 165, 190);
    private static final Color BLUE = new Color(83, 139, 255);
    private static final Color GREEN = new Color(63, 207, 150);
    private static final Color RED = new Color(255, 112, 126);
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font SEMIBOLD = new Font("Segoe UI", Font.BOLD, 13);

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    public FinSplitApp() {
        super("FinSplit | Personal Finance");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(940, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);
        buildPages();
        loadData();
        recalculate();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { saveData(); }
        });
        showPage(0);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(220, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setBackground(SIDEBAR);
        brand.setBorder(new EmptyBorder(26, 20, 28, 16));
        JLabel mark = new JLabel("F", SwingConstants.CENTER);
        mark.setOpaque(true); mark.setBackground(BLUE); mark.setForeground(Color.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mark.setPreferredSize(new Dimension(42, 42));
        mark.setBorder(new LineBorder(BLUE, 1, true));
        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 2));
        brandText.setOpaque(false);
        JLabel name = new JLabel("FinSplit"); name.setForeground(TEXT);
        name.setFont(new Font("Segoe UI", Font.BOLD, 20));
        JLabel tag = new JLabel("SMART FINANCE"); tag.setForeground(MUTED);
        tag.setFont(new Font("Segoe UI", Font.BOLD, 9));
        brandText.add(name); brandText.add(tag);
        brand.add(mark, BorderLayout.WEST); brand.add(brandText, BorderLayout.CENTER);
        side.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        String[] labels = {"Overview", "Income", "Expenses", "Budgets", "Bill Split", "Savings", "Reports"};
        String[] icons = {"◈", "↗", "↘", "▦", "⇄", "◎", "▥"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            JButton b = new JButton(icons[i] + "    " + labels[i]);
            b.setFont(SEMIBOLD); b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setBorder(new EmptyBorder(13, 17, 13, 10));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> showPage(index));
            navButtons[i] = b; nav.add(b); nav.add(Box.createVerticalStrut(5));
        }
        JPanel navWrap = new JPanel(new BorderLayout());
        navWrap.setOpaque(false);
        navWrap.setBorder(new EmptyBorder(0, 12, 0, 12));
        navWrap.add(nav, BorderLayout.NORTH);
        side.add(navWrap, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(18, 20, 20, 20));
        JLabel local = new JLabel("●  LOCAL STORAGE"); local.setForeground(GREEN);
        local.setFont(new Font("Segoe UI", Font.BOLD, 10));
        JLabel version = new JLabel("Your finances, your control"); version.setForeground(MUTED);
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        footer.add(local, BorderLayout.NORTH); footer.add(version, BorderLayout.SOUTH);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(25, 30, 22, 30));
        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 5));
        titles.setOpaque(false);
        pageTitle.setForeground(TEXT); pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 25));
        pageSubtitle.setForeground(MUTED); pageSubtitle.setFont(BODY);
        titles.add(pageTitle); titles.add(pageSubtitle);
        JLabel status = new JLabel("●  FINANCES AT A GLANCE");
        status.setForeground(GREEN); status.setFont(new Font("Segoe UI", Font.BOLD, 10));
        status.setBorder(new EmptyBorder(0, 0, 0, 2));
        header.add(titles, BorderLayout.WEST); header.add(status, BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);
        pageHost.setBackground(BG);
        main.add(pageHost, BorderLayout.CENTER);
        return main;
    }

    private void buildPages() {
        pageHost.add(buildDashboard(), "0");
        pageHost.add(tablePage("Income", "Record money received.", incomeModel, () -> addAmount(incomeModel, true)), "1");
        pageHost.add(tablePage("Expenses", "Keep track of money spent.", expenseModel, () -> addAmount(expenseModel, false)), "2");
        pageHost.add(tablePage("Budgets", "Set limits for your spending categories.", budgetModel,
                () -> addSimple(budgetModel, "Budget category", "Budget limit (₹)")), "3");
        pageHost.add(tablePage("Bill Split", "Split shared bills fairly and quickly.", billModel, this::addBill), "4");
        pageHost.add(tablePage("Savings", "Track progress towards your goals.", savingsModel, this::addSavings), "5");
        pageHost.add(buildReports(), "6");
    }

    private JPanel pageShell() {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setBackground(BG); p.setBorder(new EmptyBorder(0, 30, 28, 30));
        return p;
    }

    private JPanel buildDashboard() {
        JPanel p = pageShell();
        JPanel intro = new JPanel(new BorderLayout(0, 5)); intro.setOpaque(false);
        JLabel heading = new JLabel("Your financial snapshot"); heading.setForeground(TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel sub = new JLabel("A simple overview of where your money stands."); sub.setForeground(MUTED); sub.setFont(BODY);
        intro.add(heading, BorderLayout.NORTH); intro.add(sub, BorderLayout.SOUTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0)); metrics.setOpaque(false);
        metrics.add(metricCard("TOTAL INCOME", incomeSummary, "↗  Money received", GREEN));
        metrics.add(metricCard("TOTAL EXPENSES", expenseSummary, "↘  Money spent", RED));
        metrics.add(metricCard("AVAILABLE BALANCE", balanceLabel, "◈  Income minus expenses", BLUE));

        JPanel quick = new JPanel(new BorderLayout(0, 14)); quick.setBackground(PANEL);
        quick.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(20, 20, 20, 20)));
        JLabel quickTitle = new JLabel("Quick actions"); quickTitle.setForeground(TEXT);
        quickTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel quickSub = new JLabel("Add a record or split a bill in a few clicks."); quickSub.setForeground(MUTED); quickSub.setFont(BODY);
        JPanel qhead = new JPanel(new GridLayout(2, 1, 0, 5)); qhead.setOpaque(false); qhead.add(quickTitle); qhead.add(quickSub);
        JPanel actions = new JPanel(new GridLayout(1, 4, 10, 0)); actions.setOpaque(false);
        actions.add(actionButton("+  Add income", GREEN, () -> { showPage(1); addAmount(incomeModel, true); }));
        actions.add(actionButton("+  Add expense", BLUE, () -> { showPage(2); addAmount(expenseModel, false); }));
        actions.add(actionButton("⇄  Split a bill", BLUE, () -> { showPage(4); addBill(); }));
        actions.add(actionButton("◎  Savings goal", BLUE, () -> { showPage(5); addSavings(); }));
        quick.add(qhead, BorderLayout.NORTH); quick.add(actions, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0)); bottom.setOpaque(false);
        bottom.add(infoCard("SAVINGS GOALS", savingsSummary, "Goals currently tracked"));
        bottom.add(infoCard("YOUR DATA", new JLabel("Stored on this device"), "Saved automatically in finsplit.dat"));
        JPanel content = new JPanel(new BorderLayout(0, 18)); content.setOpaque(false);
        content.add(intro, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0, 18)); middle.setOpaque(false);
        middle.add(metrics, BorderLayout.NORTH);
        JPanel lower = new JPanel(new BorderLayout(0, 14)); lower.setOpaque(false);
        lower.add(quick, BorderLayout.CENTER); lower.add(bottom, BorderLayout.SOUTH);
        middle.add(lower, BorderLayout.CENTER);
        content.add(middle, BorderLayout.CENTER);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel metricCard(String title, JLabel value, String caption, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 13)); card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(18, 18, 18, 18)));
        JLabel t = new JLabel(title); t.setForeground(MUTED); t.setFont(new Font("Segoe UI", Font.BOLD, 10));
        value.setForeground(TEXT); value.setFont(new Font("Segoe UI", Font.BOLD, 23));
        JLabel c = new JLabel(caption); c.setForeground(accent); c.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JPanel center = new JPanel(new BorderLayout()); center.setOpaque(false); center.add(value, BorderLayout.CENTER);
        card.add(t, BorderLayout.NORTH); card.add(center, BorderLayout.CENTER); card.add(c, BorderLayout.SOUTH);
        return card;
    }

    private JPanel infoCard(String title, JLabel value, String caption) {
        JPanel card = new JPanel(new BorderLayout(0, 8)); card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(16, 18, 16, 18)));
        JLabel t = new JLabel(title); t.setForeground(MUTED); t.setFont(new Font("Segoe UI", Font.BOLD, 10));
        value.setForeground(TEXT); value.setFont(SEMIBOLD);
        JLabel c = new JLabel(caption); c.setForeground(MUTED); c.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(t, BorderLayout.NORTH); card.add(value, BorderLayout.CENTER); card.add(c, BorderLayout.SOUTH);
        return card;
    }

    private JButton actionButton(String label, Color accent, Runnable action) {
        JButton b = new JButton(label); b.setFont(SEMIBOLD); b.setForeground(accent); b.setBackground(PANEL_ALT);
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(12, 8, 12, 8)));
        b.addActionListener(e -> action.run()); return b;
    }

    private JPanel tablePage(String title, String description, DefaultTableModel model, Runnable action) {
        JPanel p = pageShell();
        JPanel heading = new JPanel(new BorderLayout(0, 5)); heading.setOpaque(false);
        JLabel h = new JLabel(title + " records"); h.setForeground(TEXT); h.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel d = new JLabel(description); d.setForeground(MUTED); d.setFont(BODY);
        heading.add(h, BorderLayout.NORTH); heading.add(d, BorderLayout.SOUTH);
        JTable table = createTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true)); scroll.getViewport().setBackground(PANEL);
        JPanel tableWrap = new JPanel(new BorderLayout()); tableWrap.setBackground(PANEL);
        tableWrap.setBorder(new LineBorder(BORDER, 1, true)); tableWrap.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); buttons.setBackground(BG);
        JButton add = primaryButton("+  Add entry"); add.addActionListener(e -> action.run());
        JButton remove = secondaryButton("Remove selected");
        remove.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) { model.removeRow(table.convertRowIndexToModel(viewRow)); recalculate(); saveData(); }
            else JOptionPane.showMessageDialog(this, "Select a row first.", "Nothing selected", JOptionPane.INFORMATION_MESSAGE);
        });
        buttons.add(add); buttons.add(remove);
        p.add(heading, BorderLayout.NORTH); p.add(tableWrap, BorderLayout.CENTER); p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(BODY); table.setForeground(TEXT); table.setBackground(PANEL);
        table.setGridColor(BORDER); table.setSelectionBackground(new Color(46, 68, 108));
        table.setSelectionForeground(Color.WHITE); table.setRowHeight(36);
        table.setShowVerticalLines(false); table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true); table.setAutoCreateRowSorter(true);
        JTableHeader header = table.getTableHeader();
        header.setBackground(PANEL_ALT); header.setForeground(MUTED); header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        return table;
    }

    private JPanel buildReports() {
        JPanel p = pageShell();
        JLabel h = new JLabel("Your report summary"); h.setForeground(TEXT); h.setFont(new Font("Segoe UI", Font.BOLD, 17));
        JLabel d = new JLabel("A quick summary of income, expenses, and balance."); d.setForeground(MUTED); d.setFont(BODY);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5)); heading.setOpaque(false); heading.add(h); heading.add(d);
        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0)); cards.setOpaque(false);
        JLabel reportIncome = new JLabel(); JLabel reportExpense = new JLabel(); JLabel reportBalance = new JLabel();
        cards.add(metricCard("TOTAL INCOME", reportIncome, "All recorded income", GREEN));
        cards.add(metricCard("TOTAL EXPENSES", reportExpense, "All recorded expenses", RED));
        cards.add(metricCard("NET BALANCE", reportBalance, "Income less expenses", BLUE));
        JButton refresh = primaryButton("Refresh report");
        refresh.addActionListener(e -> updateReportLabels(reportIncome, reportExpense, reportBalance));
        JPanel wrap = new JPanel(new BorderLayout(0, 20)); wrap.setOpaque(false);
        wrap.add(cards, BorderLayout.NORTH); wrap.add(refresh, BorderLayout.SOUTH);
        p.add(heading, BorderLayout.NORTH); p.add(wrap, BorderLayout.CENTER);
        updateReportLabels(reportIncome, reportExpense, reportBalance);
        return p;
    }

    private void updateReportLabels(JLabel i, JLabel e, JLabel b) {
        recalculate();
        i.setText(money(totalIncome)); e.setText(money(totalExpenses)); b.setText(money(totalIncome - totalExpenses));
    }

    private JButton primaryButton(String text) {
        JButton b = new JButton(text); b.setUI(new BasicButtonUI()); b.setFont(SEMIBOLD); b.setForeground(Color.WHITE); b.setBackground(BLUE);
        b.setOpaque(true); b.setContentAreaFilled(true); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(11, 17, 11, 17)); return b;
    }

    private JButton secondaryButton(String text) {
        JButton b = new JButton(text); b.setUI(new BasicButtonUI()); b.setFont(SEMIBOLD); b.setForeground(TEXT); b.setBackground(PANEL_ALT);
        b.setOpaque(true); b.setContentAreaFilled(true); b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(10, 15, 10, 15))); return b;
    }

    private void showPage(int index) {
        String[] titles = {"Overview", "Income", "Expenses", "Budgets", "Bill Split", "Savings", "Reports"};
        String[] subtitles = {"Your money, clearly organised.", "Track money coming in.", "Understand where your money goes.",
                "Plan spending before it happens.", "Share costs without the mental maths.", "Make progress towards your goals.",
                "See the big picture of your finances."};
        pageTitle.setText(titles[index]); pageSubtitle.setText(subtitles[index]);
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = i == index;
            navButtons[i].setBackground(active ? new Color(38, 57, 91) : SIDEBAR);
            navButtons[i].setForeground(active ? Color.WHITE : MUTED);
        }
        cardLayout.show(pageHost, Integer.toString(index));
        if (index == 6) recalculate();
    }

    private void addAmount(DefaultTableModel model, boolean income) {
        JTextField desc = styledField(), amount = styledField();
        Object[] fields = {"Description", desc, "Amount (₹)", amount};
        if (JOptionPane.showConfirmDialog(this, fields, income ? "Add income" : "Add expense",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double a = Double.parseDouble(amount.getText().trim());
            if (a <= 0) throw new NumberFormatException();
            model.addRow(new Object[]{desc.getText().trim(), a}); recalculate(); saveData();
        } catch (NumberFormatException ex) { error("Enter a valid amount greater than zero."); }
    }

    private void addSimple(DefaultTableModel model, String first, String second) {
        JTextField a = styledField(), b = styledField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{first, a, second, b}, "Add entry",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double n = Double.parseDouble(b.getText().trim());
            if (n < 0) throw new NumberFormatException();
            model.addRow(new Object[]{a.getText().trim(), n}); saveData();
        } catch (NumberFormatException ex) { error("Enter a valid number."); }
    }

    private void addBill() {
        JTextField name = styledField(), total = styledField(), people = styledField();
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Bill description", name, "Total amount (₹)", total,
                "Number of people", people}, "Split a bill", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double t = Double.parseDouble(total.getText().trim());
            int n = Integer.parseInt(people.getText().trim());
            if (t <= 0 || n <= 0) throw new NumberFormatException();
            billModel.addRow(new Object[]{name.getText().trim(), t, n, String.format(Locale.US, "%.2f", t / n)});
            saveData();
        } catch (NumberFormatException ex) { error("Enter a positive total and a whole number of people."); }
    }

    private void addSavings() {
        JTextField goal = styledField(), target = styledField(), saved = styledField(); saved.setText("0");
        if (JOptionPane.showConfirmDialog(this, new Object[]{"Goal name", goal, "Target amount (₹)", target,
                "Amount saved (₹)", saved}, "Add savings goal", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            double t = Double.parseDouble(target.getText().trim()), s = Double.parseDouble(saved.getText().trim());
            if (t <= 0 || s < 0) throw new NumberFormatException();
            savingsModel.addRow(new Object[]{goal.getText().trim(), t, s}); updateDashboardSummaries(); saveData();
        } catch (NumberFormatException ex) { error("Enter a positive target and a non-negative saved amount."); }
    }

    private JTextField styledField() {
        JTextField f = new JTextField(18); f.setFont(BODY); f.setBackground(PANEL_ALT); f.setForeground(TEXT);
        f.setCaretColor(TEXT); f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 9, 7, 9)));
        return f;
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Please check your entry", JOptionPane.ERROR_MESSAGE);
    }

    private void recalculate() {
        totalIncome = sum(incomeModel); totalExpenses = sum(expenseModel);
        balanceLabel.setText(money(totalIncome - totalExpenses));
        incomeSummary.setText(money(totalIncome)); expenseSummary.setText(money(totalExpenses));
        updateDashboardSummaries();
    }

    private void updateDashboardSummaries() {
        savingsSummary.setText(savingsModel.getRowCount() + (savingsModel.getRowCount() == 1 ? " goal" : " goals"));
    }

    private String money(double value) { return String.format(Locale.US, "₹%,.2f", value); }

    private double sum(DefaultTableModel m) {
        double s = 0;
        for (int r = 0; r < m.getRowCount(); r++) try {
            s += Double.parseDouble(m.getValueAt(r, 1).toString());
        } catch (Exception ignored) { }
        return s;
    }

    private void saveData() {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            o.writeObject(new Object[]{rows(incomeModel), rows(expenseModel), rows(budgetModel), rows(billModel), rows(savingsModel)});
        } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Could not save local data: " + ex.getMessage()); }
    }

    private Object[][] rows(DefaultTableModel m) {
        Object[][] a = new Object[m.getRowCount()][m.getColumnCount()];
        for (int r = 0; r < m.getRowCount(); r++)
            for (int c = 0; c < m.getColumnCount(); c++) a[r][c] = m.getValueAt(r, c);
        return a;
    }

    private void loadData() {
        File f = new File(DATA_FILE); if (!f.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Object[] all = (Object[]) in.readObject();
            DefaultTableModel[] models = {incomeModel, expenseModel, budgetModel, billModel, savingsModel};
            for (int i = 0; i < models.length; i++) {
                Object[][] a = (Object[][]) all[i];
                for (Object[] row : a) models[i].addRow(row);
            }
        } catch (Exception ex) { System.err.println("Could not load saved data: " + ex.getMessage()); }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new FinSplitApp().setVisible(true));
    }
}
