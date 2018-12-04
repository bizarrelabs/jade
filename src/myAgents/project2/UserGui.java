/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */
package myAgents.project2;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * @author Giovanni Caire - TILAB
 */
class UserGui extends JFrame {

    private LR_User userAgent;

    private JTextField x1Field, x2Field;

    private String x1, x2;;

    UserGui(LR_User a) {
        super(a.getLocalName());

        userAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("      X1:"));
        x1Field = new JTextField(10);
        p.add(x1Field);
        p.add(new JLabel("      X2:"));
        x2Field = new JTextField(10);
        p.add(x2Field);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Calculate");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    x1 = x1Field.getText().trim();
                    x2 = x2Field.getText().trim();
                    System.out.println("\n\n");

                    Double.parseDouble(x1);
                    Double.parseDouble(x2);

                    userAgent.oneShot(x1,x2);

                    x1Field.setText("");
                    x2Field.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserGui.this, "Invalid values. " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                userAgent.doDelete();
            }
        });

        setResizable(false);
    }

    public void showGui() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

}
