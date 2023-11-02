package comul01.eve;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.geom.Line2D;
import javax.swing.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.*;

import javax.media.*;
import java.net.*;

/**
 * This class handles the User Interface functions for the
 * Tuner SubSystem of the ToneMap
 *
 * @version 1.0 01/01/01
 * @author Jim O'Mulloy
 */

public class EJSettings implements EJConstants , Serializable{

	static final int PRE_PROCESS = 0;
	static final int POST_PROCESS = 1;
	static final int FILTER_PROCESS = 2;

	static final int INTERP_NEAREST = 0;
	static final int INTERP_BILINEAR = 1;
	static final int INTERP_BICUBIC = 2;

	static final int NUM_MODULATORS = 10;
	static final int NUM_EFFECTS = 5;
	static final int NUM_PARAMS = 10;
	static final int NUM_RANDOMISERS = 10;


	static final String[] effectNames = { "none", "deltaframes", "passtime", "timeframes", "timeframework",
			"maxtime", "mintime", "imagine", "jaiscale", "jaiscaleaffine", "j2dscale", "jaiscaleaffine2",
			"scale", "jaiinvert", "framemean", "zebra", "passfilter", "dft", "idft", "dct", "dctfilter", "idct",
			"dctidct", "bandcombine", "intervene","magnitude",
			"colourconvert", "triplethreshold", "jailaplace", "sharpen", "emboss", "robertsedge", "freichenedge",
			"prewittdge", "sobeledge", "median", "multiplyconst", "add", "subtract", "subtractmean", "multiply",
			"max", "min", "timeconvolve", "convolve", "convolvespread", "brownian", "texturemix", "equalise",
			"histogram", "zerox", "movie", "hough", "ihough", "threshold", "localhistogram", "contrast", "lin",
			"sin", "dolps", "crackdetect", "centroid", "grassfire", "limb", "junction", "chamfer", "zerocrossing",
			"clearwhite", "dilation", "erosion", "opening", "closing", "internalgradient", "externalgradient",
			"morphgradient", "whitetophat", "blacktophat", "reconstruct", "watermark", "walkline", "fill",
			"leader", "compose", "warppath", "waterripple", "coco", "normalise", "thinning", "band", "bits",
			"warpgen", "border", "dither", "deco", "reco", "crop", "laplace", "derive", "boundary",
			"disconnect", "pointillism", "waves", "yiq", "yiq2", "rgbtohsi", "hsitorgb", "invert", "hsi",
			"timeflow", "timecorelate", "gaussian", "sobelmax", "hysteresis", "coco2", "mexican",
			"marrhill", "coco2", "dummy", "subtractconst", "addconst", "divideconst" ,"pointy2", "timemix",
			"translate", "enhance", "jaitranslate", "jaiwarp", "shade"};

	static final String[] comboEffects = { "add", "subtract", "subtractmean", "multiply", "max", "min", "dummy"  };

	static final String[] convolveNames = { "anon", "matrix1", "matrix2","edgematrix", "edgematrix1",
			"edgematrix2","embossmatrix","freichen1","prewitt1" ,"roberts1","normaldata","blurdata",
			"blurmoredata","sharpendata", "sharpenmoreata", "laplacematrix","edgedata", "embossdata",
		 	"timeconvolve1", "timeprewitt", "timeblur", "timemean", "dilationmatrix", "erosionmatrix",
			 "tophatmatrix", "bcmatrix1", "bcmatrix2", "bcmatrixr", "bcmatrixg", "bcmatrixb", "bcmatrix4", "bcmatrix5"};
	static {
		Arrays.sort(effectNames);
		Arrays.sort(comboEffects);
		Arrays.sort(convolveNames);
	}

	class ESelectControl extends JPanel   {

		private int index;
		private EffectContext effectContext;
		public ParamControl paramControl;
		JButton button;
		JComboBox effectSCB, convolveSCB, countCB, depthCB, stepCB, seqCB;
		JCheckBox option1CB, option2CB, option3CB, option4CB, option5CB, option6CB;
		JRadioButton preRB, postRB, filterRB, interpNNRB, interpBLRB, interpBCRB;

		public ESelectControl(int index) {

			this.index = index;
			this.effectContext = (EffectContext)effectList.get(index);

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

			//JPanel p = new JPanel();
			//p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Effect "+(index+1));
			setBorder(tb);

			effectSCB = new JComboBox(effectNames);
			effectSCB.setPreferredSize(new Dimension(120,25));
			effectSCB.setMaximumSize(new Dimension(120,25));
			effectSCB.addActionListener(new EffectSCBListener());
			effectSCB.setSelectedIndex(0);

			add(effectSCB);
			String effectValue = (String)effectSCB.getSelectedItem();
			effectContext.name = effectValue;
			button = createButton("ON", this);

			JPanel seqPanel = new JPanel();
			JLabel seqLabel = new JLabel("Sequence");
			seqCB = new JComboBox();
			String s;
			seqCB.addItem("N");
			for (int j = 1; j <= NUM_EFFECTS; j++) {
				s = Integer.toString(j);
				seqCB.addItem(s);
			}
			seqCB.addActionListener(new SeqCBListener());
			seqCB.setSelectedIndex(0);

			add(seqLabel);
			add(seqCB);

			JPanel panel1 = new JPanel();
			panel1.setLayout(new BorderLayout());
			JPanel panel2 = new JPanel();

			filterRB = new JRadioButton("Filter");
			filterRB.setActionCommand("Filter");
			filterRB.setSelected(true);

			preRB = new JRadioButton("Pre");
			preRB.setActionCommand("Pre");
			preRB.setSelected(false);

			postRB = new JRadioButton("Post");
			postRB.setActionCommand("Post");
			postRB.setSelected(false);


			ButtonGroup typeGroup = new ButtonGroup();
			typeGroup.add(preRB);
			typeGroup.add(postRB);
			typeGroup.add(filterRB);

			TypeRBListener typeRBListener = new TypeRBListener();
			preRB.addActionListener(typeRBListener);
			postRB.addActionListener(typeRBListener);
			filterRB.addActionListener(typeRBListener);
			effectContext.type = FILTER_PROCESS;

			JPanel typePanel = new JPanel();
			typePanel.setLayout(new GridLayout(1, 0));
			typePanel.add(preRB);
			typePanel.add(postRB);
			typePanel.add(filterRB);

			panel2.add(typePanel);

			interpNNRB = new JRadioButton("Mode 1");
			interpNNRB.setActionCommand("Mode 1");
			interpNNRB.setSelected(true);

			interpBLRB = new JRadioButton("Mode 2");
			interpBLRB.setActionCommand("Mode 2");
			interpBLRB.setSelected(false);

			interpBCRB = new JRadioButton("Mode 3");
			interpBCRB.setActionCommand("Mode 3");
			interpBCRB.setSelected(false);

			ButtonGroup interpGroup = new ButtonGroup();
			interpGroup.add(interpNNRB);
			interpGroup.add(interpBLRB);
			interpGroup.add(interpBCRB);

			InterpRBListener interpRBListener = new InterpRBListener();
			interpNNRB.addActionListener(interpRBListener);
			interpBLRB.addActionListener(interpRBListener);
			interpBCRB.addActionListener(interpRBListener);
			effectContext.mode = INTERP_NEAREST;

			JPanel interpPanel = new JPanel();
			interpPanel.setLayout(new GridLayout(1, 0));
			interpPanel.add(interpNNRB);
			interpPanel.add(interpBLRB);
			interpPanel.add(interpBCRB);

			panel2.add(interpPanel);

			option1CB = new JCheckBox("Option 1");
			option1CB.addItemListener(new OptionCBItemListener());
			option2CB = new JCheckBox("Option 2");
			option2CB.addItemListener(new OptionCBItemListener());
			option3CB = new JCheckBox("Option 3");
			option3CB.addItemListener(new OptionCBItemListener());
			option4CB = new JCheckBox("Option 4");
			option4CB.addItemListener(new OptionCBItemListener());
			option5CB = new JCheckBox("Option 5");
			option5CB.addItemListener(new OptionCBItemListener());
			option6CB = new JCheckBox("Option 6");
			option6CB.addItemListener(new OptionCBItemListener());

			JPanel optionP = new JPanel();
			optionP.add(option1CB);
			optionP.add(option2CB);
			optionP.add(option3CB);
			optionP.add(option4CB);
			optionP.add(option5CB);
			optionP.add(option6CB);

			panel1.add("North", panel2);
			panel1.add("South", optionP);
			add(panel1);

			convolveSCB = new JComboBox(convolveNames);
			convolveSCB.setPreferredSize(new Dimension(120,25));
			convolveSCB.setMaximumSize(new Dimension(120,25));
			convolveSCB.addActionListener(new ConvolveSCBListener());
			convolveSCB.setSelectedIndex(0);

			add(convolveSCB);

			String convolveValue = (String)convolveSCB.getSelectedItem();
			effectContext.convolve = convolveValue;
			effectContext.compo = false;
			for (int i=0; i < comboEffects.length; i++) {
				if (effectContext.name.equals(comboEffects[i])) {
					effectContext.compo = true;
					break;
				}
			}

			JPanel countPanel = new JPanel();
			JLabel countLabel = new JLabel("Count");
			countCB = new JComboBox();
			for (int i = 0; i < 50; i++) {
			   countCB.addItem("" + i);
			}
			countCB.addActionListener(new CountCBListener());
			countCB.setSelectedIndex(0);

			add(countLabel);
			add(countCB);

			JPanel depthPanel = new JPanel();
			JLabel depthLabel = new JLabel("Depth");
			depthCB = new JComboBox();
			for (int i = 0; i < 50; i++) {
			   depthCB.addItem("" + i);
			}
			depthCB.addActionListener(new DepthCBListener());
			depthCB.setSelectedIndex(0);

			add(depthLabel);
			add(depthCB);

			JPanel stepPanel = new JPanel();
			JLabel stepLabel = new JLabel("Step");
			stepCB = new JComboBox();
			for (int i = 0; i < 50; i++) {
			   stepCB.addItem("" + i);
			}
			stepCB.addActionListener(new StepCBListener());
			stepCB.setSelectedIndex(0);

			add(stepLabel);
			add(stepCB);

			ArrayList paramList;
			paramList = new ArrayList(NUM_PARAMS);
			for (int j=0; j<NUM_PARAMS; j++) {
				effectParam = new EffectParam();
				effectParam.index = j;
				paramList.add(effectParam);
			}
			effectContext.params = paramList;
			paramControl = new ParamControl(index);
			add(paramControl);


		}

		public void setValues() {

			this.effectContext = (EffectContext)effectList.get(index);
			effectSCB.setSelectedIndex(effectContext.seIndex);
			seqCB.setSelectedIndex(effectContext.sqIndex);
			if(effectContext.type == PRE_PROCESS) {
				postRB.setSelected(false);
				filterRB.setSelected(false);
				preRB.setSelected(true);
				
			}
			if(effectContext.type == POST_PROCESS) {
				preRB.setSelected(false);
				filterRB.setSelected(false);
				postRB.setSelected(true);
				
			}
			if(effectContext.type == FILTER_PROCESS) {
				preRB.setSelected(false);
				postRB.setSelected(false);
				filterRB.setSelected(true);
			}

		
			if(effectContext.mode == INTERP_NEAREST) {
				interpBLRB.setSelected(false);
				interpBCRB.setSelected(false);
				interpNNRB.setSelected(true);
				
			} else
			if(effectContext.mode == INTERP_BICUBIC) {
				interpNNRB.setSelected(false);
				interpBLRB.setSelected(false);
				interpBCRB.setSelected(true);
				

			} else
			if(effectContext.type == INTERP_BILINEAR) {
				interpNNRB.setSelected(false);
				interpBCRB.setSelected(false);
				interpBLRB.setSelected(true);
				

			} else {
				interpBLRB.setSelected(false);
				interpBCRB.setSelected(false);
				interpNNRB.setSelected(true);
			
			}

			convolveSCB.setSelectedIndex(effectContext.coIndex);
			countCB.setSelectedIndex(effectContext.ctIndex);
			depthCB.setSelectedIndex(effectContext.deIndex);
			stepCB.setSelectedIndex(effectContext.stIndex);

			if (effectContext.enabled) {
				button.setText("OFF");
			} else {
				button.setText("ON");
			}
		
			if (effectContext.option1) {
				option1CB.setSelected(true);
			} else {
				option1CB.setSelected(false);
					
			}
			if (effectContext.option2) {
				option2CB.setSelected(true);
			} else {
				option2CB.setSelected(false);

			}
			if (effectContext.option3) {
				option3CB.setSelected(true);
			} else {
				option3CB.setSelected(false);

			}
			if (effectContext.option4) {
				option4CB.setSelected(true);
			} else {
				option4CB.setSelected(false);

			}
			if (effectContext.option5) {
				option5CB.setSelected(true);
			} else {
				option5CB.setSelected(false);

			}
			if (effectContext.option6) {
				option6CB.setSelected(true);
			} else {
				option6CB.setSelected(false);

			}

			paramControl.setValues(index);

		}

		public JButton createButton(String name, JPanel p) {
			JButton b = new JButton();
			b.setPreferredSize(new Dimension(60, 25));
			b.setMinimumSize(new Dimension(60, 25));
			b.setMaximumSize(new Dimension(60, 25));
			b.setText("ON");
			b.addActionListener(new ControlButtonListener());
			p.add(b);
			return b;
		}

		class ControlButtonListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				button = (JButton) e.getSource();
				if (button.getText().startsWith("ON")) {
					effectContext.enabled = true;
					button.setText("OFF");
				} else {
					effectContext.enabled = false;
					button.setText("ON");
				}
			}
		}

		class EffectSCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)effectSCB.getSelectedItem();
				effectContext.seIndex = effectSCB.getSelectedIndex();
				effectContext.name = value;
				effectContext.compo = false;
				for (int i=0; i < comboEffects.length; i++) {
					if (effectContext.name.equals(comboEffects[i])) {
						effectContext.compo = true;
						break;
					}
				}
			}

		}


		class ConvolveSCBListener implements ActionListener   {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)convolveSCB.getSelectedItem();
				effectContext.convolve = value;
				effectContext.coIndex = convolveSCB.getSelectedIndex();

			}
		}

		class TypeRBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
			    if (s.startsWith("Pre")) {
				   effectContext.type = PRE_PROCESS;
				} else if (s.startsWith("Post")) {
				   effectContext.type = POST_PROCESS;
				} else if (s.startsWith("Filter")) {
				   effectContext.type = FILTER_PROCESS;
			 	}
			}
		}

		class InterpRBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
			    if (s.startsWith("Mode 1")) {
				   effectContext.mode = INTERP_NEAREST;
				} else if (s.startsWith("Mode 2")) {
				   effectContext.mode = INTERP_BILINEAR;
				} else if (s.startsWith("Mode 3")) {
				   effectContext.mode = INTERP_BICUBIC;
			 	}
			}
		}

		class CountCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)countCB.getSelectedItem();
				effectContext.count = Integer.parseInt(value);
				effectContext.ctIndex = countCB.getSelectedIndex();

			}

		}

		class DepthCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)depthCB.getSelectedItem();
				effectContext.depth = Integer.parseInt(value);
				effectContext.deIndex = depthCB.getSelectedIndex();

			}

		}

		class StepCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)stepCB.getSelectedItem();
				effectContext.step = Integer.parseInt(value);
				effectContext.stIndex = stepCB.getSelectedIndex();

			}

		}

		class SeqCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)seqCB.getSelectedItem();
				effectContext.sqIndex = seqCB.getSelectedIndex();

				if (value.equals("N")) {
					effectContext.seq = 0;
				} else {
					effectContext.seq = Integer.parseInt(value);
				}
			}
		}

		class OptionCBItemListener implements ItemListener  {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				String name = cb.getText();
				if (name.startsWith("Option 1")) {
					effectContext.option1 = cb.isSelected();
				} else if(name.startsWith("Option 2")) {
					effectContext.option2 = cb.isSelected();
				} else if(name.startsWith("Option 3")) {
					effectContext.option3 = cb.isSelected();
				} else if(name.startsWith("Option 4")) {
					effectContext.option4 = cb.isSelected();
				} else if(name.startsWith("Option 5")) {
					effectContext.option5 = cb.isSelected();
				} else if(name.startsWith("Option 6")) {
					effectContext.option6 = cb.isSelected();
				}
			}
		}

	}

	class ParamControl extends JPanel {

		private int index;
		private EffectContext effectContext;
		private ArrayList paramList;
		JComboBox[] paramMCB;
		EJSlider[] paramS;


		public ParamControl(int index) {

			this.index = index;
			effectContext = (EffectContext)effectList.get(index);
			this.paramList = effectContext.params;

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			paramS = new EJSlider[NUM_PARAMS];
			paramMCB = new JComboBox[NUM_PARAMS];
			for (int i=0; i<NUM_PARAMS; i++) {

				paramS[i] = new EJSlider(JSlider.HORIZONTAL, 0, 100,
	            		100, "Parameter "+(i+1),new ParamSliderListener(i));
				add(paramS[i]);
				paramMCB[i] = new JComboBox();
				paramMCB[i].setPreferredSize(new Dimension(50,25));
				paramMCB[i].setMaximumSize(new Dimension(50,25));
				String s;
				paramMCB[i].addItem("N");
				for (int j = 1; j <= NUM_MODULATORS; j++) {
					s = Integer.toString(j);
					paramMCB[i].addItem(s);
				}
				paramMCB[i].addActionListener(new ParamMCBListener(i));
				paramMCB[i].setSelectedIndex(0);

				add(paramMCB[i]);
				((EffectParam)paramList.get(i)).cbIndex=0;

			}
		}

		public void setValues(int index) {

			this.index = index;
			effectContext = (EffectContext)effectList.get(index);
			this.paramList = effectContext.params;

			for (int i=0; i<NUM_PARAMS; i++) {

				paramS[i].setValue(((EffectParam)paramList.get(i)).value);
				paramMCB[i].setSelectedIndex(((EffectParam)paramList.get(i)).cbIndex);
			}
		}

		class ParamSliderListener implements ChangeListener  {

			int index;

			ParamSliderListener (int index) {
				this.index = index;
			}

			public void stateChanged(ChangeEvent e) {

				EJSlider slider = (EJSlider) e.getSource();
				int value = slider.getValue();
			  	String s = slider.getName();
				((EffectParam)paramList.get(index)).value = value;
			}
		}

		class ParamMCBListener implements ActionListener  {

			int index;

			ParamMCBListener (int index) {
				this.index = index;
			}

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
			    String value = (String)cb.getSelectedItem();
				((EffectParam)paramList.get(index)).cbIndex=cb.getSelectedIndex();

				if (value.equals("N")) {
					((EffectParam)paramList.get(index)).modulator = null;
				} else {
					((EffectParam)paramList.get(index)).modulator =
						(EffectModulator)modulatorList.get(Integer.parseInt(value)-1);
				}
			}
		}

	}

	class RandomiserControl extends JPanel  {

		private int index;
		private EffectContext effectContext;
		JComboBox randomGNCB, randomINCB, randomIN2CB;
		JCheckBox modCB, paramCB, effectCB;
		private EffectRandomiser effectRandomiser;
		EJSlider degreeS, rateS, rangeS;

		public RandomiserControl(int index) {

			this.index = index;
			this.effectRandomiser = (EffectRandomiser)randomiserList.get(index);

			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Randomiser Control "+(index+1));
			setBorder(tb);

			degreeS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
							100, "Degree",new RandomSliderListener());

			rangeS = new EJSlider(JSlider.HORIZONTAL,0, 100,
									100, "Range",new RandomSliderListener());

			rateS = new EJSlider(JSlider.HORIZONTAL,0, 100,
							100, "Ratio",new RandomSliderListener());

			add(degreeS);
			add(rangeS);
			add(rateS);

			modCB = new JCheckBox("Modulator");
			modCB.addItemListener(new RandomOCBItemListener());
			paramCB = new JCheckBox("Parameter");
			paramCB.addItemListener(new RandomOCBItemListener());
			effectCB = new JCheckBox("Effect");
			effectCB.addItemListener(new RandomOCBItemListener());

			JPanel randomOP = new JPanel();
			randomOP.add(modCB);
			randomOP.add(paramCB);
			randomOP.add(effectCB);

			add(randomOP);

			randomGNCB = new JComboBox();
			randomGNCB.setPreferredSize(new Dimension(50,25));
			randomGNCB.setMaximumSize(new Dimension(50,25));
			String s;
			randomGNCB.addItem("N");
			for (int j = 1; j <= NUM_MODULATORS; j++) {
				s = Integer.toString(j);
				randomGNCB.addItem(s);
			}
			randomGNCB.addActionListener(new RandomGNCBListener(index));
			randomGNCB.setSelectedIndex(0);
			add(randomGNCB);

			randomINCB = new JComboBox();
			randomINCB.setPreferredSize(new Dimension(50,25));
			randomINCB.setMaximumSize(new Dimension(50,25));
			randomINCB.addItem("N");
			for (int j = 1; j <= NUM_PARAMS; j++) {
				s = Integer.toString(j);
				randomINCB.addItem(s);
			}
			randomINCB.addActionListener(new RandomINCBListener(index));
			randomINCB.setSelectedIndex(0);
			add(randomINCB);

			randomIN2CB = new JComboBox();
			randomIN2CB.setPreferredSize(new Dimension(50,25));
			randomIN2CB.setMaximumSize(new Dimension(50,25));
			randomIN2CB.addItem("N");
			for (int j = 1; j <= NUM_EFFECTS; j++) {
				s = Integer.toString(j);
				randomIN2CB.addItem(s);
			}
			randomIN2CB.addActionListener(new RandomIN2CBListener(index));
			randomIN2CB.setSelectedIndex(0);
			add(randomIN2CB);

		}


		public void setValues() {

			this.effectRandomiser = (EffectRandomiser)randomiserList.get(index);

			degreeS.setValue(effectRandomiser.degree);
			rangeS.setValue(effectRandomiser.range);
			rateS.setValue(effectRandomiser.rate);
			if (effectRandomiser.modOpt) {
				modCB.setSelected(false);
				paramCB.setSelected(false);
				effectCB.setSelected(false);
				modCB.setSelected(true);
			}
			if (effectRandomiser.paramOpt) {

				modCB.setSelected(false);
				paramCB.setSelected(false);
				effectCB.setSelected(false);
				paramCB.setSelected(true);
			}
			if (effectRandomiser.effectOpt) {

				modCB.setSelected(false);
				paramCB.setSelected(false);
				effectCB.setSelected(false);
				effectCB.setSelected(true);
			}

			randomGNCB.setSelectedIndex(effectRandomiser.groupNumber);
			randomINCB.setSelectedIndex(effectRandomiser.itemNumber);
			randomIN2CB.setSelectedIndex(effectRandomiser.itemNumber2);
		}



		class RandomSliderListener implements ChangeListener   {

			public void stateChanged(ChangeEvent e) {

				EJSlider slider = (EJSlider) e.getSource();
				int value = slider.getValue();
				String s = slider.getName();
				if (s.startsWith("Degree")) {
				   	effectRandomiser.degree = value;
				} else if (s.startsWith("Range")) {
				   	effectRandomiser.range = value;
				} else if (s.startsWith("Rate")) {
				 	effectRandomiser.rate = value;
				}

			}

		}

		class RandomGNCBListener implements ActionListener  {

			int index;

			RandomGNCBListener (int index) {
				this.index = index;

			}

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
			    String value = (String)cb.getSelectedItem();
				if (value.equals("N")) {
					((EffectRandomiser)randomiserList.get(index)).groupNumber = 0;
				} else {
					((EffectRandomiser)randomiserList.get(index)).groupNumber =
							Integer.parseInt(value);
				}
			}
		}

		class RandomINCBListener implements ActionListener   {

			int index;

			RandomINCBListener (int index) {
				this.index = index;

			}

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
			    String value = (String)cb.getSelectedItem();
				if (value.equals("N")) {
					((EffectRandomiser)randomiserList.get(index)).itemNumber = 0;
				} else {
					((EffectRandomiser)randomiserList.get(index)).itemNumber =
							Integer.parseInt(value);
				}
			}
		}

		class RandomIN2CBListener implements ActionListener  {

			int index;

			RandomIN2CBListener (int index) {
				this.index = index;

			}

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
			    String value = (String)cb.getSelectedItem();
				if (value.equals("N")) {
					((EffectRandomiser)randomiserList.get(index)).itemNumber2 = 0;
				} else {
					((EffectRandomiser)randomiserList.get(index)).itemNumber2 =
							Integer.parseInt(value);
				}
			}
		}

		class RandomOCBItemListener implements ItemListener  {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				String name = cb.getText();
				if (name.startsWith("Modulator")) {
					effectRandomiser.modOpt = cb.isSelected();
				} else if(name.startsWith("Parameter")) {
					effectRandomiser.paramOpt = cb.isSelected();
				} else if(name.startsWith("Effect")) {
					effectRandomiser.effectOpt = cb.isSelected();
				}
			}
		}

	}

	class ModulatorControl extends JPanel  {

		private int index;
		private EffectModulator effectModulator;
		private EJSlider lowValueS, midValueS, mid2ValueS, highValueS, midPointS, mid2PointS, startS, stopS;

		public ModulatorControl(int index) {

			this.index = index;
			this.effectModulator = (EffectModulator)modulatorList.get(index);

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			TitledBorder tb = new TitledBorder(new EtchedBorder());
			tb.setTitle("Modulator Control "+(index+1));
			setBorder(tb);

			lowValueS = new EJSlider(JSlider.HORIZONTAL,0, 100,
						100, "Low Value",new ModulatorSliderListener());

			midValueS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
						100, "Mid Value",new ModulatorSliderListener());

			mid2ValueS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
						100, "Mid2 Value",new ModulatorSliderListener());

			highValueS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
						100, "High Value",new ModulatorSliderListener());

			midPointS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
						50, "Mid Point",new ModulatorSliderListener());

			mid2PointS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
							50, "Mid2 Point",new ModulatorSliderListener());

			startS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
									0, "Start Point",new ModulatorSliderListener());

			stopS = new EJSlider(JSlider.HORIZONTAL, 0, 100,
							100, "Stop Point",new ModulatorSliderListener());

			add(lowValueS);
			add(midValueS);
			add(mid2ValueS);
			add(highValueS);
			add(midPointS);
			add(mid2PointS);
			add(startS);
			add(stopS);


		}

		public void setValues() {

			this.index = index;
			this.effectModulator = (EffectModulator)modulatorList.get(index);

			lowValueS.setValue(effectModulator.lowValue);
			midValueS.setValue(effectModulator.midValue);
			mid2ValueS.setValue(effectModulator.mid2Value);
			highValueS.setValue(effectModulator.highValue);
			midPointS.setValue(effectModulator.midPoint);
			mid2PointS.setValue(effectModulator.mid2Point);
			startS.setValue(effectModulator.start);
			stopS.setValue(effectModulator.stop);

		}

		class ModulatorSliderListener implements ChangeListener  {

			public void stateChanged(ChangeEvent e) {

				EJSlider slider = (EJSlider) e.getSource();
				int value = slider.getValue();
			  	String s = slider.getName();
				if (s.startsWith("Low Value")) {
			 	   	effectModulator.lowValue = value;

				} else if (s.startsWith("Mid Value")) {
				   	effectModulator.midValue = value;
				} else if (s.startsWith("Mid2 Value")) {
				   	effectModulator.mid2Value = value;
				} else if (s.startsWith("High Value")) {
				   	effectModulator.highValue = value;
				} else if (s.startsWith("Mid Point")) {
				   	effectModulator.midPoint = value;
				} else if (s.startsWith("Mid2 Point")) {
				   	effectModulator.mid2Point = value;
				} else if (s.startsWith("Start Point")) {
				   	effectModulator.start = value;
				} else if (s.startsWith("Stop Point")) {
				   	effectModulator.stop = value;
				}
			}
		}

	}

	class CompoControl extends JPanel  {

		JComboBox durationCB, startCB;
		JRadioButton alpha1RB, alpha2RB;

		public CompoControl() {

			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(2, 2));
			panel.setOpaque(false);

			JPanel miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Duration (milliSeconds) "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( durationCB = new JComboBox() );
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Start (seconds) "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( startCB = new JComboBox() );
			panel.add(miniPanel);

			for (int i = 0; i < 10; i++) {
			    durationCB.addItem("" + i);
			}
			durationCB.addActionListener(new DurationCBListener());
			durationCB.setSelectedIndex(0);


			for (int i = 0; i < 10; i++) {
			    startCB.addItem("" + i);
			}
			startCB.addActionListener(new StartCBListener());
			startCB.setSelectedIndex(0);


			add(panel);

			JPanel southPanel = new JPanel();
			southPanel.setOpaque(false);
			southPanel.setLayout( new GridLayout(2, 1));
			compoTimeLabel = new JLabel("Selection", JLabel.CENTER);
			compoTimeLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
			southPanel.add(compoTimeLabel);
			compoTimePanel = new SelectionPanel2( new CompoTimeListener() );
			compoTimePanel.setVisible( true );
			southPanel.add(compoTimePanel);
		    add(southPanel);

			alpha1RB = new JRadioButton("Alpha1");
			alpha1RB.setActionCommand("Alpha1");
			alpha1RB.setSelected(true);

			alpha2RB = new JRadioButton("Alpha2");
			alpha2RB.setActionCommand("Alpha2");
			alpha2RB.setSelected(false);

			ButtonGroup typeGroup = new ButtonGroup();
			typeGroup.add(alpha1RB);
			typeGroup.add(alpha2RB);

			AlphaRBListener alphaRBListener = new AlphaRBListener();
			alpha1RB.addActionListener(alphaRBListener);
			alpha2RB.addActionListener(alphaRBListener);

			JPanel alphaPanel = new JPanel();
			alphaPanel.setLayout(new GridLayout(1, 2));
			alphaPanel.add(alpha1RB);
			alphaPanel.add(alpha2RB);

			add(alphaPanel);


		}

		class DurationCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)durationCB.getSelectedItem();
				effectCompo.duration = (long)(Integer.parseInt(value))*1000000000L;
			}

		}

		class StartCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)startCB.getSelectedItem();
				effectCompo.start = (long)(Integer.parseInt(value))*1000000000L;
			}
		}


		class AlphaRBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {

				String s = e.getActionCommand();
			    if (s.startsWith("Alpha1")) {
				   effectCompo.alphaType = 0;
				} else if (s.startsWith("Alpha2")) {
				   effectCompo.alphaType = 1;
				}
			}
		}

		class CompoTimeListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
				effectCompo.beginTime =
					new Time(compoTimePanel.getStartTimeMillis() * 1000000);
				effectCompo.endTime =
				    new Time(compoTimePanel.getStopTimeMillis() * 1000000);
			   updateCompoTimeLabel();
			}
		}

	}

	class TransControl extends JPanel  {

		JComboBox durationCB, loopCB, framesCB, widthCB, heightCB, frameRateCB;
		JCheckBox audioCB;

		public TransControl() {

			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(2, 2));
			panel.setOpaque(false);
			add(panel);
			JPanel miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Duration (milliSeconds) "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( durationCB = new JComboBox() );
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Loop Count "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( loopCB = new JComboBox() );
			panel.add(miniPanel);

			for (int i = 0; i <= 30000; i+=100) {
			    durationCB.addItem("" + i);
			}
			durationCB.addActionListener(new DurationCBListener());
			durationCB.setSelectedIndex(0);

			for (int i = 0; i <= 10; i++) {
			    loopCB.addItem("" + i);
			}
			loopCB.addActionListener(new LoopCBListener());
			loopCB.setSelectedIndex(0);

			add(panel);


			panel = new JPanel();
			panel.setLayout(new GridLayout(2, 2));
			panel.setOpaque(false);
			add(panel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Width "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( widthCB = new JComboBox() );
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Height "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( heightCB = new JComboBox() );
			panel.add(miniPanel);

			int[] widthValues = {100, 200, 400, 640, 800, 1024, 1152};
			for (int i = 0; i < widthValues.length; i++) {
			    widthCB.addItem("" + widthValues[i]);
			}
			widthCB.addActionListener(new WidthCBListener());
			widthCB.setSelectedIndex(0);


			int[] heightValues = {100, 200, 400, 480, 600, 768, 864};
			for (int i = 0; i < heightValues.length; i++) {
			    heightCB.addItem("" + heightValues[i]);
			}
			heightCB.addActionListener(new HeightCBListener());
			heightCB.setSelectedIndex(0);

			add(panel);

			panel = new JPanel();
			panel.setLayout(new GridLayout(2, 2));
			panel.setOpaque(false);
			add(panel);

			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( new JLabel("Frame Rate "));
			panel.add(miniPanel);
			miniPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			miniPanel.setOpaque(false);
			miniPanel.add( frameRateCB = new JComboBox() );
			panel.add(miniPanel);

			int[] frameRateValues = {2, 4, 6, 8, 10, 12, 15, 20, 24, 25, 30};
			for (int i = 0; i < frameRateValues.length; i++) {
			    frameRateCB.addItem("" + frameRateValues[i]);
			}
			frameRateCB.addActionListener(new FrameRateCBListener());
			frameRateCB.setSelectedIndex(0);

			add(panel);

			audioCB = new JCheckBox("Audio On");
			audioCB.addItemListener(new AudioCBItemListener());
			JPanel audioP = new JPanel();
			audioP.add(audioCB);
			effectTrans.audioOn = false;
			add(audioP);
		}

		public void setValues() {

			durationCB.setSelectedIndex(effectTrans.duIndex);
			//durationCB.setSelectedIndex(12);
			loopCB.setSelectedIndex(effectTrans.loIndex);
			widthCB.setSelectedIndex(effectTrans.wiIndex);
			heightCB.setSelectedIndex(effectTrans.heIndex);
			frameRateCB.setSelectedIndex(effectTrans.frIndex);
			audioCB.setSelected(false);
			if (effectTrans.audioOn) {
				audioCB.setSelected(true);
			} else {
				audioCB.setSelected(false);
			}

		}

		class AudioCBItemListener implements ItemListener  {

			public void itemStateChanged(ItemEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				String name = cb.getText();
				if (name.startsWith("Audio On")) {
					effectTrans.audioOn = cb.isSelected();
				}
			}
		}


		class DurationCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)durationCB.getSelectedItem();
				effectTrans.duration = Integer.parseInt(value);
				effectTrans.duIndex = durationCB.getSelectedIndex();
			}

		}


		class LoopCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)loopCB.getSelectedItem();
				effectTrans.loop = Integer.parseInt(value);
				effectTrans.loIndex = loopCB.getSelectedIndex();

			}

		}

		class WidthCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)widthCB.getSelectedItem();
				effectTrans.width = Integer.parseInt(value);
				effectTrans.wiIndex = widthCB.getSelectedIndex();

			}
		}

		class HeightCBListener implements ActionListener   {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)heightCB.getSelectedItem();
				effectTrans.height = Integer.parseInt(value);
				effectTrans.heIndex = heightCB.getSelectedIndex();

			}
		}

		class FrameRateCBListener implements ActionListener  {

			public void actionPerformed(ActionEvent e) {
			    String value = (String)frameRateCB.getSelectedItem();
				effectTrans.frameRate = Integer.parseInt(value);
				effectTrans.frames = (int)((double)effectTrans.duration/(1000.0*(double)effectTrans.frameRate));
				effectTrans.frIndex = frameRateCB.getSelectedIndex();


			}
		}

	}


	public void updateCompoTimeLabel() {
		if (compoTimeLabel == null)
		    return;
		String begin = formatTime(effectCompo.beginTime);
		String end = formatTime(effectCompo.endTime);
		compoTimeLabel.setText(begin + " - " + end);
	}

	private String formatTime ( Time time ) {
		long    nano;
		int     hours;
		int     minutes;
		int     seconds;
		int     hours10;
		int     minutes10;
		int     seconds10;
		long    nano10;
	    String  strTime = new String ( "<unknown>" );

		if ( time == null  ||  time == Time.TIME_UNKNOWN  ||  time == javax.media.Duration.DURATION_UNKNOWN )
			    return ( strTime );

		nano = time.getNanoseconds();
		seconds = (int) (nano / Time.ONE_SECOND);
		hours = seconds / 3600;
		minutes = ( seconds - hours * 3600 ) / 60;
		seconds = seconds - hours * 3600 - minutes * 60;
		nano = (long) ((nano % Time.ONE_SECOND) / (Time.ONE_SECOND/100));

        hours10 = hours / 10;
        hours = hours % 10;
        minutes10 = minutes / 10;
        minutes = minutes % 10;
        seconds10 = seconds / 10;
        seconds = seconds % 10;
        nano10 = nano / 10;
        nano = nano % 10;

        strTime = new String ( "" + hours10 + hours + ":" + minutes10 +
			       minutes + ":" + seconds10 + seconds + "." + nano10 + nano );
		return ( strTime );
    }

	public JPanel getPanel() {
		return ejSPanel;
	}


	public void setEJData(EJData ejData) {

		effectTrans = ejData.effectTrans;
		transControl.setValues();

		randomiserList = ejData.randomiserList;
		for (int i=0; i<NUM_RANDOMISERS; i++) {
			randomiserControls[i].setValues();
		}

		modulatorList = ejData.modulatorList;
		for (int i=0; i<NUM_MODULATORS; i++) {
			modulatorControls[i].setValues();
		}

		effectList = ejData.effectList;
		for (int i=0; i<NUM_EFFECTS; i++) {
			eSelectControls[i].setValues();
		}

	}


	public EJData getEJData() {

		System.out.println("doing serial get "+effectList.size());

		EJData ejData = new EJData();

		ejData.effectList = effectList;
		ejData.modulatorList = modulatorList;
		ejData.randomiserList = randomiserList;
		ejData.effectTrans = effectTrans;

		return ejData;
	}


	public void sortEffects() {

		Collections.sort(effectList);
	}


	public JPanel ejSPanel;
	public EJMain ejMain;
	public ESelectControl[] eSelectControls;
	public ModulatorControl[] modulatorControls;
	public RandomiserControl[] randomiserControls;
	public CompoControl compoControl;
	public TransControl transControl;
	public JLabel compoTimeLabel;
	public SelectionPanel2 compoTimePanel;
	private String effectName=null;
	public ArrayList effectList;
	public ArrayList modulatorList;
	public ArrayList randomiserList;
	public EffectContext effectContext;
	public EffectParam effectParam;
	public EffectModulator effectModulator;
	public EffectCompo effectCompo;
	public EffectTrans effectTrans;
	public EffectRandomiser effectRandomiser;

	public double duration;

	public void setDuration (double duration) {
		this.duration = duration;
	}

	public double getDuration() {
		return duration;
	}
	public void setEffectName(String effectName) {
		this.effectName = effectName;
	}

	public String getEffectName() {
		return effectName;
	}
	public EJSettings(EJMain ejMain) {

		this.ejMain = ejMain;
		JTabbedPane tabPane = ejMain.getPane();

		JPanel p0 = new JPanel();
		p0.setLayout(new BoxLayout(p0, BoxLayout.X_AXIS));
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
		JPanel p3 = new JPanel();
		p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS));
		JPanel p5 = new JPanel();
		p5.setLayout(new BoxLayout(p5, BoxLayout.Y_AXIS));
		JPanel p6 = new JPanel();
		//p6.setLayout(new BoxLayout(p6, BoxLayout.X_AXIS));
		JPanel p7 = new JPanel();
		p7.setLayout(new BoxLayout(p7, BoxLayout.Y_AXIS));
		JPanel p8 = new JPanel();

		p8.setLayout(new BoxLayout(p8, BoxLayout.Y_AXIS));
		JPanel p9 = new JPanel();
		p9.setLayout(new BoxLayout(p9, BoxLayout.Y_AXIS));

		TitledBorder tb = new TitledBorder(new EtchedBorder());
		tb.setTitle("Effect Control");
		p1.setBorder(tb);

		effectList = new ArrayList(NUM_EFFECTS);
		eSelectControls = new ESelectControl[NUM_EFFECTS];
		for (int i=0; i<NUM_EFFECTS; i++) {

			effectContext = new EffectContext();
			effectContext.index = i;
			effectList.add(effectContext);
			eSelectControls[i] = (new ESelectControl(i));
			p1.add(eSelectControls[i]);
		}

		tb = new TitledBorder(new EtchedBorder());
		tb.setTitle("Effect Parameter Modulators");
		p4.setBorder(tb);
		modulatorList = new ArrayList(NUM_MODULATORS);
		modulatorControls = new ModulatorControl[NUM_MODULATORS];
		for (int i=0; i<NUM_MODULATORS; i++) {

			effectModulator = new EffectModulator();
			effectModulator.index = i;
			modulatorList.add(effectModulator);
			modulatorControls[i] = new ModulatorControl(i);
			p4.add(modulatorControls[i]);
		}

		tb = new TitledBorder(new EtchedBorder());
		tb.setTitle("Effect Parameter Randomisers");
		p9.setBorder(tb);
		randomiserList = new ArrayList(NUM_RANDOMISERS);
		randomiserControls = new RandomiserControl[NUM_RANDOMISERS];
		for (int i=0; i<NUM_RANDOMISERS; i++) {
			effectRandomiser = new EffectRandomiser();
			effectRandomiser.index = i;
			randomiserList.add(effectRandomiser);
			randomiserControls[i] = new RandomiserControl(i);
			p9.add(randomiserControls[i]);
		}

		tb = new TitledBorder(new EtchedBorder());
		tb.setTitle("Effect Composite Parameters");
		p3.setBorder(tb);
		effectCompo = new EffectCompo();
		compoControl = new CompoControl();
		p3.add(compoControl);

		tb = new TitledBorder(new EtchedBorder());
		tb.setTitle("TransCode Parameters");
		p7.setBorder(tb);
		effectTrans = new EffectTrans();
		transControl = new TransControl();
		p7.add(transControl);

		JScrollPane p1Pane = new JScrollPane(p1);
		Dimension minimumSize = new Dimension(100, 50);
		p1Pane.setMinimumSize(minimumSize);
		p1.setMinimumSize(minimumSize);
		p1Pane.setPreferredSize(new Dimension(100,100));

		JScrollPane p4Pane = new JScrollPane(p4);
		minimumSize = new Dimension(500, 500);
		p4Pane.setMinimumSize(minimumSize);
		p4.setMinimumSize(minimumSize);
		p4Pane.setPreferredSize(new Dimension(100,100));

		JScrollPane p7Pane = new JScrollPane(p7);
		minimumSize = new Dimension(500, 500);
		p7Pane.setMinimumSize(minimumSize);
		p7.setMinimumSize(minimumSize);
		p7Pane.setPreferredSize(new Dimension(100,100));

		JScrollPane p9Pane = new JScrollPane(p9);
		minimumSize = new Dimension(500, 500);
		p9Pane.setMinimumSize(minimumSize);
		p9.setMinimumSize(minimumSize);
		p9Pane.setPreferredSize(new Dimension(100,100));

		JScrollPane p3Pane = new JScrollPane(p3);
		minimumSize = new Dimension(500, 500);
		p3Pane.setMinimumSize(minimumSize);
		p3.setMinimumSize(minimumSize);
		p3Pane.setPreferredSize(new Dimension(100,100));

		tabPane.addTab("Effects", p1Pane);
	 	tabPane.addTab("Modulators", p4Pane);
		tabPane.addTab("TransCode", p7Pane);
		tabPane.addTab("Composite", p3Pane);
		tabPane.addTab("Randomiser", p9Pane);

	}
}