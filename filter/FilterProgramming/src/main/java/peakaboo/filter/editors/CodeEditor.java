package peakaboo.filter.editors;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import commonenvironment.Env;
import de.sciss.syntaxpane.DefaultSyntaxKit;
import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.view.swing.editors.AbstractSwingEditor;
import peakaboo.common.PeakabooLog;
import swidget.dialogues.fileio.SimpleFileExtension;
import swidget.dialogues.fileio.SwidgetFileDialogs;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.Spacing;
import swidget.widgets.ToolbarImageButton;

public class CodeEditor extends AbstractSwingEditor<String>
{

	
	public JEditorPane codeEditor;
	public JToolBar toolbar;
	
	private String language;
	private DefaultSyntaxKit syntaxKit;
	public String errorMessage;
	
	private JPanel panel;
	
	public CodeEditor(String language, DefaultSyntaxKit syntaxKit)
	{
		this.language = language;
		this.syntaxKit = syntaxKit;
	}
	
	@Override
	public void initialize(final Parameter<String> param)
	{
	
		this.param = param;
		panel = new JPanel();
		
		DefaultSyntaxKit.initKit();
		
		
		codeEditor = new JEditorPane();
		codeEditor.setEditorKit(syntaxKit);
		codeEditor.setMinimumSize(new Dimension(400, 200));
        JScrollPane scrPane = new JScrollPane(codeEditor);
        
        if (language != null) {
        	codeEditor.setContentType("text/" + language);
        }
        
        setFromParameter();
        param.getValueHook().addListener(v -> this.setFromParameter());
        
        
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);
        toolbar.setBorder(Spacing.bNone());
        
        ToolbarImageButton open = new ToolbarImageButton(StockIcon.DOCUMENT_OPEN, "Open");
        ToolbarImageButton save = new ToolbarImageButton(StockIcon.DOCUMENT_SAVE_AS, "Save");
        ToolbarImageButton apply = new ToolbarImageButton(StockIcon.CHOOSE_OK, "Apply", "Apply any code changes to the filter", true);
        
        toolbar.add(open);
        toolbar.add(save);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(apply);
        
        open.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event)
			{
				SimpleFileExtension extension = new SimpleFileExtension("Java Source Files", "java");
				File file = SwidgetFileDialogs.openFile(null, "Open Java Source File", Env.homeDirectory(), extension);
				if (file == null) { return; }
				try
				{
					Scanner s = new Scanner(new FileInputStream(file)).useDelimiter("\\A");
					String code = s.next();
					s.close();
					codeEditor.setText(code);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
        
        save.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SimpleFileExtension extension = new SimpleFileExtension("Java Source Files", "java");
				File file = SwidgetFileDialogs.saveFile(null, "Save Java Source File", Env.homeDirectory(), extension);
				if (file == null) {
					return;
				}
				
				try
				{
					FileOutputStream os = new FileOutputStream(file);
					os.write(codeEditor.getText().getBytes());
					os.close();
				}
				catch (IOException e1)
				{
					PeakabooLog.get().log(Level.SEVERE, "Failed to save file", e1);
				}
				
			}
		});
        
        
		apply.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				getEditorValueHook().updateListeners(getEditorValue());
				if (!param.setValue(getEditorValue())) {
					validateFailed();
				}
			}
		});
		
        
		
		
		

		
		
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridy=0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(toolbar, c);
		
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		panel.add(scrPane, c);

	}
	
	public boolean expandVertical()
	{
		return true;
	}

	@Override
	public boolean expandHorizontal()
	{
		return true;
	}

	@Override
	public LabelStyle getLabelStyle()
	{
		return LabelStyle.LABEL_HIDDEN;
	}

	@Override
	public JComponent getComponent()
	{
		return panel;
	}

	@Override
	public void setEditorValue(String code)
	{
		codeEditor.setText(code);
	}

	@Override
	public String getEditorValue()
	{
		return codeEditor.getText();
	}

	public void validateFailed() {
		JOptionPane.showMessageDialog(
				panel, 
				errorMessage, 
				"Code Error", 
				JOptionPane.ERROR_MESSAGE,
				StockIcon.BADGE_WARNING.toImageIcon(IconSize.ICON)
			);
		
		errorMessage = "";
	}

	@Override
	protected void setEnabled(boolean enabled) {
		codeEditor.setEnabled(enabled);
		toolbar.setEnabled(enabled);
	}

	
}
