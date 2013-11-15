/*
    DrMIPS - Educational MIPS simulator
    Copyright (C) 2013 Bruno Nova <ei08109@fe.up.pt>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.feup.brunonova.drmips.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import org.feup.brunonova.drmips.mips.CPU;
import org.feup.brunonova.drmips.mips.Component;
import org.feup.brunonova.drmips.mips.Input;
import org.feup.brunonova.drmips.mips.IsSynchronous;
import org.feup.brunonova.drmips.mips.Output;
import org.feup.brunonova.drmips.mips.components.Concatenator;
import org.feup.brunonova.drmips.mips.components.Constant;
import org.feup.brunonova.drmips.mips.components.Distributor;
import org.feup.brunonova.drmips.mips.components.ExtendedALU;
import org.feup.brunonova.drmips.mips.components.Fork;

/**
 * Graphical component that displays a CPU component.
 * 
 * @author Bruno Nova
 */
public final class DatapathComponent extends JPanel implements MouseListener {
	/** The font used for the text. */
	private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 8);
	
	/** The graphical datapath this component is in. */
	private DatapathPanel datapath;
	/** The respective CPU component. */
	private Component component;
	/** The label that displays the component name. */
	private JLabel lblName;
	
	/**
	 * Creates a graphical CPU component.
	 * @param datapath The datapath this component is being added to.
	 * @param component The respective CPU component.
	 */
	public DatapathComponent(DatapathPanel datapath, Component component) {
		super();
		this.datapath = datapath;
		this.component = component;
		boolean dark = DrMIPS.prefs.getBoolean(DrMIPS.DARK_THEME_PREF, DrMIPS.DEFAULT_DARK_THEME);
		
		setBorder(new LineBorder(Color.BLACK));
		setBackground(Color.WHITE);
		setLayout(new BorderLayout());
		setLocation(component.getPosition().x, component.getPosition().y);
		setSize(component.getSize().width, component.getSize().height);
		
		lblName = new JLabel("<html><pre>" + component.getDisplayName() + "</pre></html>", JLabel.CENTER);
		lblName.setFont(FONT);
		lblName.setForeground(Color.BLACK);
		add(lblName, BorderLayout.CENTER);
		
		if(component instanceof Fork || component instanceof Concatenator || component instanceof Distributor) {
			Color color = component.isInControlPath() ? DatapathPanel.CONTROL_COLOR : (dark ? Color.WHITE : Color.BLACK);
			setBackground(color);
			setBorder(BorderFactory.createLineBorder(color));
		}
		else if(component instanceof Constant) {
			setOpaque(false);
			setBorder(null);
			lblName.setForeground(component.isInControlPath() ? DatapathPanel.CONTROL_COLOR : (dark ? Color.WHITE : Color.BLACK));
		}
		else {
			if(component.isInControlPath()) {
				setBorder(BorderFactory.createLineBorder(DatapathPanel.CONTROL_COLOR));
				lblName.setForeground(DatapathPanel.CONTROL_COLOR);
			}
		}
		
		refresh();
		addMouseListener(this);
	}
	
	/**
	 * Returns the respective CPU component.
	 * @return The CPU component.
	 */
	public Component getComponent() {
		return component;
	}
	
	/**
	 * Refreshes the component tooltip with the current information, and possibly other things.
	 */
	public void refresh() {
		// Set fork gray if irrelevant
		if(getComponent() instanceof Fork) {
			boolean dark = DrMIPS.prefs.getBoolean(DrMIPS.DARK_THEME_PREF, DrMIPS.DEFAULT_DARK_THEME);
			Color color;
			if(!datapath.isInPerformanceMode() && !((Fork)component).getInput().isRelevant())
				color = Color.GRAY;
			else
				color = component.isInControlPath() ? DatapathPanel.CONTROL_COLOR : (dark ? Color.WHITE : Color.BLACK);
			setBackground(color);
			setBorder(BorderFactory.createLineBorder(color));
		}
		
		
		// Refresh the tooltip
		String tip = "<html><table cellspacing=0 cellpadding=0>";
		String controlStyle = "style='color: rgb(" + DatapathPanel.CONTROL_COLOR.getRed() + "," + DatapathPanel.CONTROL_COLOR.getGreen() + "," + DatapathPanel.CONTROL_COLOR.getBlue() + ")'";
		
		// Name
		tip += "<tr><th><u>" + Lang.t(component.getNameKey()) + "</u></th></tr>";

		// Identifier and synchronous?
		tip += "<tr><td align='center'><i><tt>" + component.getId() + "</tt></i>";
		if(component instanceof IsSynchronous)
			tip += " (" + Lang.t("synchronous") + ")";
		tip += "</td></tr>";
		
		// Description
		String desc = component.getCustomDescription(Lang.getLanguage());
		if(desc != null) 
			desc = desc.replace("\n", "<br />");
		else
			desc = Lang.t(component.getDescriptionKey()).replace("\n", "<br />");
		tip += "<tr><td align='center'>" + desc + "</td></tr>";
		
		// HI and LO registers if extended ALU
		if(!datapath.isInPerformanceMode() && component instanceof ExtendedALU) {
			ExtendedALU alu = (ExtendedALU)component;
			tip += "<tr><td align='center'><table>";
			tip += "<tr><td><tt>HI:</tt></td><td><tt>" + Util.formatDataAccordingToFormat(alu.getHI(), datapath.getDataFormat()) + "</tt></td></tr>";
			tip += "<tr><td><tt>LO:</tt></td><td><tt>" + Util.formatDataAccordingToFormat(alu.getLO(), datapath.getDataFormat()) + "</tt></td></tr>";
			tip += "</table></td></tr>";
		}
		
		// Latency
		if(datapath.isInPerformanceMode()) {
			tip += "<tr><td align='center'>" + Lang.t("latency") + ": " + component.getLatency() + " " + CPU.LATENCY_UNIT
				+ " <i>(" + Lang.t("double_click_to_change") + ")</i></td></tr>";
		}
		
		// Inputs
		tip += "<tr><td align='center'><table>";
		tip += "<tr><th colspan=2>" + Lang.t("inputs") + "</th></tr>";
		for(Input in: component.getInputs()) {
			if(in.isConnected()) {
				tip += in.isInControlPath() ? ("<tr " + controlStyle + ">") : "<tr>";
				tip += "<td><tt>" + in.getId() + ":</tt></td><td align='right'><tt>";
				if(datapath.isInPerformanceMode())
					tip += in.getAccumulatedLatency() + " " + CPU.LATENCY_UNIT + "</tt></td></tr>";
				else
					tip += Util.formatDataAccordingToFormat(in.getData(), datapath.getDataFormat())  + "</tt></td></tr>";
			}
		}
		// Outputs
		tip += "<tr><th colspan=2>" + Lang.t("outputs") + "</th></tr>";
		for(Output out: component.getOutputs()) {
			if(out.isConnected()) {
				tip += out.isInControlPath() ? ("<tr " + controlStyle + ">") : "<tr>";
				tip += "<td><tt>" + out.getId() + ":</tt></td><td align='right'><tt>";
				if(datapath.isInPerformanceMode())
					tip += component.getAccumulatedLatency() + " " + CPU.LATENCY_UNIT + "</tt></td></tr>";
				else
					tip += Util.formatDataAccordingToFormat(out.getData(), datapath.getDataFormat()) + "</tt></td></tr>";
			}
		}
		tip += "</table></td></tr>";
		
		setToolTipText(tip + "</table></html>");
	}

	@Override
	public void mouseClicked(MouseEvent e) { }

	@Override
	public void mousePressed(MouseEvent e) {
		if(datapath.isInPerformanceMode() && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			String res = (String)JOptionPane.showInputDialog(datapath, Lang.t("latency_of_x", component.getId()), DrMIPS.PROGRAM_NAME, JOptionPane.QUESTION_MESSAGE, null, null, component.getLatency());
			if(res != null) {
				try {
					int lat = Integer.parseInt(res);
					if(lat >= 0) {
						component.setLatency(lat);
						datapath.getCPU().calculatePerformance();
						datapath.refresh();
						datapath.repaint();
					}
					else
						JOptionPane.showMessageDialog(this.getParent(), Lang.t("invalid_value"), DrMIPS.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
				}
				catch(NumberFormatException ex) {
					JOptionPane.showMessageDialog(this.getParent(), Lang.t("invalid_value"), DrMIPS.PROGRAM_NAME, JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }
}