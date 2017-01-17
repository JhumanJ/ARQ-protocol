/*
 * 
 * Uses JFreeChart to make a panel that behaves like an oscilloscope.
 * The Oscilloscope scans over 10 seconds periods where the voltage on
 * the oscilloscope can be changed by the setVoltage method.
 * 
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU SHOULD NOT NEED TO MODIFY THIS CLASS.
 *  (You will only submit on Moodle two files: DataFrame.java and NetworkCard.java)
 */
package physical_network;

/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 *
 */
import java.util.Date;

import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

class OscilloscopePanel extends ApplicationFrame {

	private static final long serialVersionUID = 1L;
	
	private Date startDate = new Date();
    private long startTime = startDate.getTime();
    private XYSeries voltages = new XYSeries("Voltages");

    public OscilloscopePanel() {

        super("Oscilloscope");

        // Set initial (time, voltage) datapoint of (0.0, 0.0).
        voltages.add(0.0, 0.0);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(voltages);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Oscilloscope",
                "Time (seconds)",
                "Voltage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);

        XYPlot plot = (XYPlot) chart.getPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);

        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setRange(0.0, 10.0);
        domain.setTickUnit(new NumberTickUnit(1.0));
        domain.setVerticalTickLabels(true);

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setRange(-5.0, 5.0);
        range.setTickUnit(new NumberTickUnit(1.0));

        plot.setRenderer(renderer);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 300));

        setContentPane(chartPanel);
    }

    
    /**
     * This sets the voltage value at a particular point in term on the oscilloscope.
     * If it sweeps over the end then it resets and goes back to the start.
     * 
     * @param voltage Value to set on the oscilloscope.
     */
    void setVoltage(double voltage) {
    	
    	Date currentDate = new Date();
    	double currentTime = (currentDate.getTime() - startTime) / 1000.0;
        
        if (currentTime > 10.0) {
        	
        	startTime = currentDate.getTime();
        	currentTime = 0.0;
        	
        	Runnable clearData = new Runnable() {
                public void run() {
                	voltages.clear();
                }
            };        	
        	
        	SwingUtilities.invokeLater(clearData);
        }
        
        voltages.add(currentTime, voltage);
    }
}
