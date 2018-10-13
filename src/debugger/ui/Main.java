package debugger.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.Element;

import debugger.ui.common.CommandList;
import debugger.ui.common.MenuBuilder;
import debugger.ui.common.RunCommand;
import debugger.ui.common.TabbedViewFactory;
import debugger.ui.common.TabbedViewLayout;
import debugger.ui.common.TabbedViewLayout.FolderPosition;
import debugger.ui.view.CallView;
import debugger.ui.view.CodeView;
import debugger.ui.view.ThreadView;

public class Main {
	private final CommandList commandList = new CommandList();
	private final TabbedViewFactory tabbedViewFactory;
	
	private final Shell shell;

	public Main(Shell shell) {
		this.shell = shell;
		
		TabbedViewLayout tabbedViewLayout = new TabbedViewLayout(shell);
		
		tabbedViewFactory = new TabbedViewFactory(tabbedViewLayout);
		tabbedViewFactory.registerView(ThreadView.class, "Threads", FolderPosition.LEFT, ThreadView::new);
		tabbedViewFactory.registerView(CallView.class, "Calls", FolderPosition.BOTTOM, CallView::new);
		tabbedViewFactory.registerView(CodeView.class, "Code", FolderPosition.RIGHT, CodeView::new);
		
		tabbedViewFactory.getRegisteredViews().forEach(viewInfo -> {
			tabbedViewFactory.addView(viewInfo.getType());
		});
		
		Element element = new Element("Element");
		tabbedViewLayout.serialize(element);
		
		createMenuBar(shell);
	}
	
	private void createMenuBar(final Shell shell) {
		MenuBuilder menuBuilder = new MenuBuilder(shell, commandList);
		
		menuBuilder.addMenu("File")
			.addItem("Run Command...\tCtrl+3").addSelectionListener(() -> runCommand()).setAccelerator(SWT.CONTROL | '3')
			.addSeparator()
			.addItem("Exit").addSelectionListener(() -> shell.dispose());
		
		MenuBuilder view = menuBuilder.addMenu("View");
		tabbedViewFactory.getRegisteredViews().forEach(viewInfo -> {
			view.addItem(viewInfo.getDefaultTitle()).addSelectionListener(() -> {
				tabbedViewFactory.addView(viewInfo.getType());
			});
		});

		menuBuilder.build();
	}
	
	private void runCommand() {
		RunCommand runCommand = new RunCommand(shell);
		runCommand.setSearchFunction(findText -> commandList.findCommands(findText));
		String result = runCommand.open();
		if(result != null) {
			commandList.runCommand(result);
		}
	}
	
	private void displayException(Exception e) {
		MessageBox messageBox = new MessageBox(shell);
		messageBox.setText("Error");
		messageBox.setMessage(e.getMessage() == null ? e.toString() : e.getMessage());
		e.printStackTrace();
		
		messageBox.open();
	}

	public static void main(String[] args) {
		Display display = new Display();
		
		Shell shell = new Shell(display);
		
		Main main = new Main(shell);
		
		shell.setSize(600, 500);
		shell.setText("Debugger");
		shell.setVisible(true);
		
		while(!shell.isDisposed()) {
			while(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		display.dispose();
	}
}