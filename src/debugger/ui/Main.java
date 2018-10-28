package debugger.ui;

import java.io.File;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.jdom2.Element;

import debugger.event.EventLogger;
import debugger.instrumentation.Instrumentor;
import debugger.model.EventLog;
import debugger.model.QuickSort;
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
		
		EventLog eventLog = createEventLog();
		
		tabbedViewFactory = new TabbedViewFactory(tabbedViewLayout);
		tabbedViewFactory.registerView(ThreadView.class, "Threads", FolderPosition.LEFT, ThreadView::new);
		tabbedViewFactory.registerView(CallView.class, "Calls", FolderPosition.BOTTOM, CallView::new);
		tabbedViewFactory.registerView(CodeView.class, "Code", FolderPosition.RIGHT, parent -> new CodeView(parent, eventLog));
		
		tabbedViewFactory.getRegisteredViews().forEach(viewInfo -> {
			tabbedViewFactory.addView(viewInfo.getType());
		});
		
		Element element = new Element("Element");
		tabbedViewLayout.serialize(element);
		
		createMenuBar(shell);
	}
	
	private static EventLog createEventLog() {
		EventLogger.clear();
		new Instrumentor().instrumentClass(QuickSort.class);
		QuickSort.sort(Arrays.asList(5, 2, 3, 8, 7, 3, 8, 6, 3));
		
		EventLog eventLog = new EventLog(EventLogger.getEvents());
		eventLog.setSourceFile(new File(Main.class.getResource(".").getFile(), "../../../test/debugger/model/QuickSort.java"));
		return eventLog;
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
		
		shell.setSize(1200, 900);
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