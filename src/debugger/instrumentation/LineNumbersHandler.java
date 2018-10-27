package debugger.instrumentation;

import java.util.Map;

import org.objectweb.asm.Label;

public interface LineNumbersHandler {
	public void setLineNumbers(Map<Label, Integer> lineNumbers);
}