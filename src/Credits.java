import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Credits extends JPanel {

    public Credits() {
        setOpaque(true);
        setFocusable(true);
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());

        // Text label
        JLabel textLabel = new JLabel("Made with ♥ by Dy0gu!");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(15, 0, 15, 0);
        add(textLabel, gbc);

        // Back button
        JButton backButton = new JButton("◄");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Router.goBack();
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(30, 10, 15, 0);
        add(backButton, gbc);
    }
}