import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

class ColorButton extends JPanel
{
	public ColorButton(String label, Color initialColor)
	{
		setPreferredSize(new Dimension(180, 32));
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent event) {
				showColorChooser();
			}
		});
		
		fLabel = label;
		fColor = initialColor;
	}
	
	public Color getColor()
	{
		return fColor;
	}

	private void showColorChooser()
	{
		Color newColor = JColorChooser.showDialog(this, "Color", fColor); 
		if (newColor != null)
			fColor = newColor;
	}

	protected void paintComponent(Graphics g)
	{
		g.setColor(fColor);
		g.fillRect(0, 0, 32, 32);

		g.setColor(Color.black);
		g.drawString(fLabel, 35, 32);
		g.drawLine(0, 0, 32, 0);
		g.drawLine(32, 0, 32, 32);
		g.drawLine(0, 32, 32, 32);
		g.drawLine(0, 0, 0, 32);
	}

	private Color fColor;
	private String fLabel;
}
