import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;

class PreferenceWindow extends JFrame
{
	public PreferenceWindow()
	{
		super("Preferences");

		setPreferredSize(new Dimension(500, 400));
		
		JPanel contentPane = new JPanel();

		contentPane.setLayout(new BorderLayout());
		fTabbedPane = new JTabbedPane();
		fTabbedPane.addTab("Colors", null, new ColorPreferencePane(), "");
		fTabbedPane.setOpaque(true);
		contentPane.add(fTabbedPane, BorderLayout.CENTER);

		Container okCancelContainer = new Container();
		okCancelContainer.setLayout(new FlowLayout());
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ok();
			}
		});
		
		okCancelContainer.add(okButton);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				cancel();
			}
		});
		okCancelContainer.add(cancelButton);
		
		contentPane.add(okCancelContainer, BorderLayout.SOUTH);
		setContentPane(contentPane);
		pack();
	}

	protected void cancel()
	{
		hide();
	}
	
	protected void ok()
	{
		for (int i = 0; i < fTabbedPane.getTabCount(); i++)
			((PreferencePane) fTabbedPane.getComponentAt(i)).saveSettings();
	
		hide();
	}
	
	JTabbedPane fTabbedPane;
}