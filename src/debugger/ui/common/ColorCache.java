package debugger.ui.common;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Stores used colors so they can be cached, and disposed when needed.
 */
public class ColorCache implements DisposeListener {
	private final Display display;
	private final Map<RGB, Color> colors = new HashMap<>();

	public ColorCache(Display display) {
		this.display = display;
	}

	/**
	 * Returns a color from an AWT color.
	 */
	public Color getColor(java.awt.Color color) {
		RGB rgb = new RGB(
			color.getRed(),
			color.getGreen(),
			color.getBlue()
		);
		
		return colors.computeIfAbsent(rgb, key -> new Color(display, rgb));
	}

	/**
	 * Returns a color from r, g, b values from 0-255.
	 */
	public Color getColor(int r, int g, int b) {
		return getColor(new java.awt.Color(r, g, b));
	}
	
	/**
	 * Returns a color from a hex code as in the form: "rrggbb".
	 */
	public Color getHexColor(String color) {
		int[] v = new int[3];
		for(int x = 0; x < 3; x++) {
			String part = color.substring(x*2, x*2+2);
			v[x] = Integer.parseInt(part, 16);
		}
		return getColor(v[0], v[1], v[2]);
	}
	
	/**
	 * Returns a color from an SWT rgb value.
	 */
	public Color getColor(RGB rgb) {
		return getColor(rgb.red, rgb.green, rgb.blue);
	}
	
	/**
	 * Disposes of the cached colors.
	 */
	public void dispose() {
		for(Color color:colors.values()) {
			color.dispose();
		}
	}

	public void widgetDisposed(DisposeEvent event) {
		dispose();
	}
}