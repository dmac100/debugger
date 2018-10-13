package debugger.ui.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import debugger.ui.common.TabbedView;
import debugger.ui.common.layout.GridLayoutBuilder;

public class CodeView implements TabbedView {
	private final Composite composite;
	
	public CodeView(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		
		composite.setLayout(new GridLayoutBuilder().marginWidth(10).marginHeight(10).build());
		
		FontData[] fontData = Display.getDefault().getSystemFont().getFontData();
		for(FontData data:fontData) {
			data.setHeight(15);
		}
		
		Font font = new Font(Display.getDefault(), fontData);
		
		Label label = new Label(composite, SWT.NONE);
		label.setFont(font);
		label.setText("Code View");
		
		composite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
	}
	
	public Control getControl() {
		return composite;
	}
}
