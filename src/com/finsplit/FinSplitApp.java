package com.finsplit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FinSplitApp extends JFrame {
    private final JLabel balanceLabel = new JLabel("Balance: ₹0.00");
    private final DefaultTableModel incomeModel = new DefaultTableModel(new String[]{"Description", "Amount (₹)"}, 0);
    private final DefaultTableModel expenseModel = new DefaultTableModel(new String[]{"Description", "Amount (₹)"}, 0);
    private final DefaultTableModel billModel = new DefaultTableModel(new String[]{"Bill", "Total (₹)", "People", "Each pays (₹)"}, 0);
    private final DefaultTableModel budgetModel = new DefaultTableModel(new String[]{"Category", "Limit (₹)"}, 0);
    private final DefaultTableModel savingsModel = new DefaultTableModel(new String[]{"Goal", "Target (₹)", "Saved (₹)"}, 0);
    private double totalIncome = 0, totalExpenses = 0;
    private static final String DATA_FILE = "finsplit.dat";

    public FinSplitApp() {
        super("FinSplit — Smart Bill Splitter & Personal Finance Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600); setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        JPanel dashboard = new JPanel(new BorderLayout(12,12));
        dashboard.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JLabel welcome = new JLabel("Welcome to FinSplit", SwingConstants.CENTER); welcome.setFont(new Font("SansSerif", Font.BOLD, 26));
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 20)); balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dashboard.add(welcome, BorderLayout.NORTH); dashboard.add(balanceLabel, BorderLayout.CENTER);
        JLabel note = new JLabel("Track income, expenses, budgets, savings goals, and shared bills.", SwingConstants.CENTER); dashboard.add(note, BorderLayout.SOUTH);
        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Income", tablePanel(incomeModel, () -> addAmount(incomeModel, true)));
        tabs.addTab("Expenses", tablePanel(expenseModel, () -> addAmount(expenseModel, false)));
        tabs.addTab("Budgets", tablePanel(budgetModel, () -> addSimple(budgetModel, "Budget category", "Budget limit (₹)")));
        tabs.addTab("Bill Split", tablePanel(billModel, this::addBill));
        tabs.addTab("Savings", tablePanel(savingsModel, this::addSavings));
        tabs.addTab("Reports", reportsPanel());
        setContentPane(tabs);
        loadData(); refreshBalance();
        addWindowListener(new java.awt.event.WindowAdapter() { @Override public void windowClosing(java.awt.event.WindowEvent e) { saveData(); } });
    }

    private JPanel tablePanel(DefaultTableModel model, Runnable action) {
        JPanel p = new JPanel(new BorderLayout(8,8)); p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        JButton add = new JButton("Add entry"); add.addActionListener(e -> action.run());
        JButton remove = new JButton("Remove selected"); remove.addActionListener(e -> { JTable t=(JTable)((JScrollPane)p.getComponent(0)).getViewport().getView(); int r=t.getSelectedRow(); if(r>=0){model.removeRow(t.convertRowIndexToModel(r)); recalculate(); saveData();} });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(add); buttons.add(remove); p.add(buttons, BorderLayout.SOUTH); return p;
    }
    private void addAmount(DefaultTableModel model, boolean income) {
        JTextField desc = new JTextField(), amount = new JTextField(); Object[] fields={"Description",desc,"Amount (₹)",amount};
        if(JOptionPane.showConfirmDialog(this, fields, income?"Add income":"Add expense", JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try { double a=Double.parseDouble(amount.getText().trim()); if(a<=0)throw new NumberFormatException(); model.addRow(new Object[]{desc.getText().trim(),a}); recalculate(); saveData(); }
        catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Enter a valid amount greater than zero."); }
    }
    private void addSimple(DefaultTableModel model, String first, String second) {
        JTextField a=new JTextField(), b=new JTextField(); if(JOptionPane.showConfirmDialog(this,new Object[]{first,a,second,b},"Add entry",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try { double n=Double.parseDouble(b.getText().trim()); if(n<0)throw new NumberFormatException(); model.addRow(new Object[]{a.getText().trim(),n}); saveData(); }
        catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Enter a valid number.");}
    }
    private void addBill() {
        JTextField name=new JTextField(), total=new JTextField(), people=new JTextField();
        if(JOptionPane.showConfirmDialog(this,new Object[]{"Bill description",name,"Total amount (₹)",total,"Number of people",people},"Split a bill",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try { double t=Double.parseDouble(total.getText().trim()); int n=Integer.parseInt(people.getText().trim()); if(t<=0||n<=0)throw new NumberFormatException(); billModel.addRow(new Object[]{name.getText().trim(),t,n,String.format("%.2f",t/n)}); saveData(); }
        catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Enter a positive total and a whole number of people.");}
    }
    private void addSavings() {
        JTextField goal=new JTextField(), target=new JTextField(), saved=new JTextField("0");
        if(JOptionPane.showConfirmDialog(this,new Object[]{"Goal name",goal,"Target amount (₹)",target,"Amount saved (₹)",saved},"Savings goal",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try {double t=Double.parseDouble(target.getText().trim()),s=Double.parseDouble(saved.getText().trim());if(t<=0||s<0)throw new NumberFormatException();savingsModel.addRow(new Object[]{goal.getText().trim(),t,s});saveData();}
        catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Enter a positive target and a non-negative saved amount.");}
    }
    private JPanel reportsPanel(){ JPanel p=new JPanel(new GridLayout(0,1,8,8));p.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));JLabel i=new JLabel(),e=new JLabel(),b=new JLabel();p.add(i);p.add(e);p.add(b); JButton refresh=new JButton("Refresh report");refresh.addActionListener(x->{recalculate();i.setText(String.format("Total income: ₹%.2f",totalIncome));e.setText(String.format("Total expenses: ₹%.2f",totalExpenses));b.setText(String.format("Net balance: ₹%.2f",totalIncome-totalExpenses));});p.add(refresh);refresh.doClick();return p;}
    private void recalculate(){totalIncome=sum(incomeModel);totalExpenses=sum(expenseModel);refreshBalance();}
    private double sum(DefaultTableModel m){double s=0;for(int r=0;r<m.getRowCount();r++)try{s+=Double.parseDouble(m.getValueAt(r,1).toString());}catch(Exception ignored){}return s;}
    private void refreshBalance(){balanceLabel.setText(String.format("Income: ₹%.2f     Expenses: ₹%.2f     Balance: ₹%.2f",totalIncome,totalExpenses,totalIncome-totalExpenses));}
    private void saveData(){try(ObjectOutputStream o=new ObjectOutputStream(new FileOutputStream(DATA_FILE))){o.writeObject(new Object[]{rows(incomeModel),rows(expenseModel),rows(budgetModel),rows(billModel),rows(savingsModel)});}catch(IOException ex){JOptionPane.showMessageDialog(this,"Could not save local data: "+ex.getMessage());}}
    private Object[][] rows(DefaultTableModel m){Object[][] a=new Object[m.getRowCount()][m.getColumnCount()];for(int r=0;r<m.getRowCount();r++)for(int c=0;c<m.getColumnCount();c++)a[r][c]=m.getValueAt(r,c);return a;}
    private void loadData(){File f=new File(DATA_FILE);if(!f.exists())return;try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(f))){Object[] all=(Object[])in.readObject();DefaultTableModel[] models={incomeModel,expenseModel,budgetModel,billModel,savingsModel};for(int i=0;i<models.length;i++){Object[][] a=(Object[][])all[i];for(Object[] row:a)models[i].addRow(row);}recalculate();}catch(Exception ex){System.err.println("Could not load saved data: "+ex.getMessage());}}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new FinSplitApp().setVisible(true));}
}
