/*
 * Alumno:   Rodriguez Bocanegra, Juan Daniel
 * Profesor: Aviña Mendez, Jose Antonio
 * Materia:  Seminario de Solucion de Problemas de Inteligencia Artificial I
 * Seccion:  D02
 * Centro Universitario de Ciencias Exactas e Ingeniería
 * División de Electrónica y Computación
 */

package myAgents.handson2;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
  @author Giovanni Caire - TILAB
 */
class BookSellerGui extends JFrame {
	private AmazonBrokerAgent myAgent;

	private JTextField titleField, priceField;

	BookSellerGui(AmazonBrokerAgent a) {
		super(a.getLocalName());

		myAgent = a;

		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Book title:"));
		titleField = new JTextField(15);
		p.add(titleField);
		p.add(new JLabel("Price:"));
		priceField = new JTextField(15);
		p.add(priceField);
		getContentPane().add(p, BorderLayout.CENTER);

		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String title = titleField.getText().trim();
					String price = priceField.getText().trim();
					myAgent.connDB.updateCatalogue(title, Float.parseFloat(price));
					titleField.setText("");
					priceField.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(BookSellerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);

		// Make the agent terminate when the user closes
		// the GUI using the button on the upper right corner
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );

		setResizable(false);
	}

	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}
}
