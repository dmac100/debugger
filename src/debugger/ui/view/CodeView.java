package debugger.ui.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import debugger.model.EventLog;
import debugger.ui.common.TabbedView;
import debugger.ui.common.layout.GridDataBuilder;
import debugger.ui.common.layout.GridLayoutBuilder;
import debugger.ui.view.text.EditorText;
import syntaxhighlighter.brush.BrushJava;

public class CodeView implements TabbedView {
	private final Composite composite;
	private final EditorText editorText;
	private final ToolBar toolbar;
	private final EventLog eventLog;
	
	public CodeView(Composite parent, EventLog eventLog) {
		this.eventLog = eventLog;
		
		composite = new Composite(parent, SWT.NONE);
		
		composite.setLayout(new GridLayoutBuilder().numColumns(1).verticalSpacing(0).marginWidth(0).marginHeight(0).build());
		
		toolbar = new ToolBar(composite, SWT.NONE);
		
		addToolbarItem("/debugger/ui/view/icons/stepinto.png", "Step into");
		addToolbarItem("/debugger/ui/view/icons/stepover.png", "Step over");
		addToolbarItem("/debugger/ui/view/icons/stepreturn.png", "Step return");
		
		editorText = new EditorText(composite);
		
		editorText.getControl().setLayoutData(new GridDataBuilder().fillHorizontal().fillVertical().build());
		
		Runnable removeCallback = eventLog.addChangeCallback(this::refresh);
		composite.addDisposeListener(e -> removeCallback.run());
	}
	
	private void addToolbarItem(String imagePath, String tooltip) {
		ToolItem toolItem = new ToolItem(toolbar, SWT.NONE);
		toolItem.setToolTipText(tooltip);
		InputStream inputStream = getClass().getResourceAsStream(imagePath);
		Image image = new Image(Display.getCurrent(), inputStream);
		composite.addDisposeListener(e -> image.dispose());
		toolItem.setImage(image);
	}

	private void refresh() {
		File file = eventLog.getSourceFile();
		
		try {
			String source = (file == null) ? "" : FileUtils.readFileToString(file, "UTF-8");
			editorText.setText(source);
			editorText.setBrush(new BrushJava());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Control getControl() {
		return composite;
	}
}