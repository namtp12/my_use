/*
 * USE - UML based specification environment
 * Copyright (C) 1999-2010 Mark Richters, University of Bremen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

// $Id$

package org.tzi.use.gui.views;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.tzi.use.config.Options;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.uml.mm.MMetricEvaluationSetting;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.expr.MultiplicityViolationException;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MSystem;

/**
 * TODO
 * @author KHANHHOANG
 *
 */
public class ModelMetricsEvaluation extends JPanel implements View{
	
	private MSystem mSystem;
	private Evaluator evaluator;
	private JTable tblMetricsEvaluation;
	MetricsEvaluationTableModel tableModel = new MetricsEvaluationTableModel();
	
	/**
	 * Create the panel.
	 */
	
	public ModelMetricsEvaluation(final MainWindow parent, final MSystem metaSystem) {
		
		mSystem = metaSystem;
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton btnLoadConfigFile = new JButton("Load the Setting");
		btnLoadConfigFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try{
					String readLine = null;
					String evaluationInv = "";
					//Open the file browser dialog
					JFileChooser fChooser = new JFileChooser(Options.getLastDirectory().toFile());
		            ExtFileFilter filter = new ExtFileFilter("met", "Metrics configuration");
		            fChooser.setFileFilter(filter);
		            fChooser.setDialogTitle("Open the metric configuration file ...");
		            		            
		            int returnVal = fChooser.showOpenDialog(ModelMetricsEvaluation.this);
		            if (returnVal != JFileChooser.APPROVE_OPTION)
		                return;

		            Path path = fChooser.getCurrentDirectory().toPath();
		            Options.setLastDirectory(path);
		            Path f = fChooser.getSelectedFile().toPath();
		            File confFile = new File(f.toAbsolutePath().toString());
		            
					//File confFile = new File("E:\\test.txt");
					FileReader reader = new FileReader(confFile);
					@SuppressWarnings("resource")
					BufferedReader bufReader = new BufferedReader(reader);
					
					List<MMetricEvaluationSetting> configList = new ArrayList<MMetricEvaluationSetting>();
					while((readLine = bufReader.readLine()) != null)
					{
						String[] data = readLine.split("\\s+");
						MMetricEvaluationSetting config = new MMetricEvaluationSetting();
						config.setScope(data[0].trim().equals("0")?"Class":"Model");
						config.setName(data[1]);
						try{
							config.setMinValue(Double.parseDouble(data[2]));
							config.setMaxValue(Double.parseDouble(data[3]));
						}
						catch(NumberFormatException ex)
						{
							
						}
						evaluationInv = config.createEvaluationInvariant();
						String errFilename = Paths.get(System.getProperty("user.dir")).resolve("OCLEvaluationLog.txt").toAbsolutePath().toString();
								
						PrintWriter out = new PrintWriter(errFilename);

				        
				        // compile invariant
				        Expression expr = OCLCompiler.compileExpression(
				        		metaSystem.model(),
				        		metaSystem.state(),
				        		evaluationInv, 
				                "Error", 
				                out, 
				                metaSystem.varBindings());
				        	        
				        out.flush();
				        
				        try {
				            // evaluate it with current system state
				            evaluator = new Evaluator(true);
				            Value val = evaluator.eval(expr, metaSystem.state(), metaSystem.varBindings());
				            // print result
				            config.setSatisfaction(Boolean.parseBoolean(val.toString()));
				        } catch (MultiplicityViolationException e) {
				            
				        }
				        configList.add(config);
					}
					
					tableModel.setList(configList);
					
				}catch(IOException ex){}
			}
		});
		
		tblMetricsEvaluation = new JTable();
		
		tblMetricsEvaluation.setModel(tableModel);
		
		tblMetricsEvaluation.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = tblMetricsEvaluation.getSelectedRow();
				String value = tblMetricsEvaluation.getModel().getValueAt(row, 4).toString();
				String scope = tblMetricsEvaluation.getModel().getValueAt(row, 0).toString();
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2 && row >= 0 && scope.equals("Class") && value.equals("false")){
					MetricEvaluationDetailedView dlg = 
							new MetricEvaluationDetailedView(mSystem, parent, tableModel.getDataItem(row));
		            dlg.setVisible(true);
				}
			}
		});
		
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment( JLabel.CENTER );
		for(int i=0;i<tblMetricsEvaluation.getColumnCount();i++)
			tblMetricsEvaluation.getColumnModel().getColumn(i).setCellRenderer( centerRenderer );
		
		//A JTable must be in a JScrollPane so that the Header will be shown
		JScrollPane scrollPane = new JScrollPane(tblMetricsEvaluation, 
                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(350, 200));
		add(scrollPane);

		add(btnLoadConfigFile);

	}
	
	@Override
	public void detachModel() {
		// TODO Auto-generated method stub
		
	}
}
	
/*
 * The Table model for metrics evaluation
 */
@SuppressWarnings("serial")
class MetricsEvaluationTableModel extends AbstractTableModel {
    private List<MMetricEvaluationSetting> list = new ArrayList<MMetricEvaluationSetting>();
	private final String[] columnNames = { "Scope", "Metric", "Min Value", "Max Value", "Satisfied" };
    private final int[] columnWidths =   {  80,         80,       50,         50,          80};

    public void setList(List<MMetricEvaluationSetting> data) {
        this.list = data;
        fireTableDataChanged();
    }
    
    public MMetricEvaluationSetting getDataItem(int row){
    	return list.get(row);
    }
    
    @Override
	public String getColumnName(int col) {
        return columnNames[col];
    }
    public int getColumnWidth(int col){
    	return columnWidths[col];
    }
    public int getRowCount() {
        return list.size();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return list.get(rowIndex).getScope();
        case 1:
            return list.get(rowIndex).getName();
        case 2:
            return list.get(rowIndex).getMinValue();
        case 3:
            return list.get(rowIndex).getMaxValue();
        case 4:
            return list.get(rowIndex).getSatisfaction();
        default:
            return null;
        }
    }
    
}
