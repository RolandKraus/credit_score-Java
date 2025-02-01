package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.toedter.calendar.JDateChooser; // Importiamo JDateChooser dalla libreria JCalendar

public class ClientManager extends JFrame
{
    private JTable table; // Tabella per visualizzare i dati
    private DefaultTableModel tableModel; // Modello della tabella
    private Connection connection; // Connessione al database

    public ClientManager() {
        connectToDatabase(); // Stabilire la connessione al database

        setTitle("Client Manager");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Creazione della tabella
        String[] columnNames = {"Name", "Birthday", "Salary"};
        tableModel = new DefaultTableModel(columnNames, 0);
        table = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column < 3; // Solo le prime tre colonne sono modificabili
            }
        };


        loadData(); // Caricamento iniziale dei dati nella tabella

        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete");
        addButton.addActionListener(e -> showAddDialog()); // Apre la finestra di dialogo per aggiungere un cliente
        deleteButton.addActionListener(e -> onDeleteClicked());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/credit_score", "root", "");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void loadData() {
        tableModel.setRowCount(0);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Name, Birthday, Salary FROM clients")) {
            while (rs.next()) {

                String name = rs.getString("Name");
                Date birthday = rs.getDate("Birthday");
                double salary = rs.getDouble("Salary");
                tableModel.addRow(new Object[]{name, birthday != null ? new SimpleDateFormat("yyyy-MM-dd").format(birthday) : "", salary, "Delete"});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Add Client", true);
        dialog.setLayout(new GridLayout(4, 2));

        JTextField nameField = new JTextField();
        JTextField salaryField = new JTextField();
        JDateChooser birthdayChooser = new JDateChooser();

        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);

        dialog.add(new JLabel("Birthday:"));
        dialog.add(birthdayChooser);

        dialog.add(new JLabel("Salary:"));
        dialog.add(salaryField);

        JButton addButton = new JButton("Add to Table");
        addButton.addActionListener(e -> {
            String name = nameField.getText();
            Date birthday = birthdayChooser.getDate();
            String salaryText = salaryField.getText();

            if (name.isEmpty() || salaryText.isEmpty() || birthday == null) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double salary;
            try {
                salary = Double.parseDouble(salaryText); // Converte lo stipendio in un numero decimale
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid salary format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            addClientToDatabase(name, new java.sql.Date(birthday.getTime()), salary); // Aggiunge il cliente al database
            loadData(); // Ricarica i dati nella tabella
            dialog.dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(addButton);
        dialog.add(cancelButton);

        dialog.setSize(300, 200);
        dialog.setVisible(true);
    }

    private void addClientToDatabase(String name, java.sql.Date birthday, double salary) {
        String query = "INSERT INTO clients (Name, Birthday, Salary) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, birthday);
            pstmt.setDouble(3, salary);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Client added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to add client.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientManager manager = new ClientManager();
            manager.setVisible(true); // Mostra l'interfaccia utente
        });
    }

    class buttonrenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public buttonrenderer() {
            setText("Delete");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            return this;
        }
    }

    public void onDeleteClicked()
    {
        System.out.println("delete clicked");
        int row = table.getSelectedRow();
        int confirmResult = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete this line?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (confirmResult == JOptionPane.YES_OPTION) {
            deleteClientFromDatabase(row);
            loadData();
        }
    }
    private void deleteClientFromDatabase(int row) {
        String nameToDelete = (String) tableModel.getValueAt(row, 0);

        String query = "DELETE FROM clients WHERE Name=?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, nameToDelete);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Client deleted successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to delete client.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
