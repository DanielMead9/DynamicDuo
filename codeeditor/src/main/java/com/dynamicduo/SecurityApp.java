package DynamicDuo.codeeditor.src.main.java.com.dynamicduo;

import javax.swing.SwingUtilities;

import main.java.com.dynamicduo.GUI8;

public class SecurityApp 
{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GUI().setVisible(true);
        });
    }
}
