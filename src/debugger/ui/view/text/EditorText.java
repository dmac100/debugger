package debugger.ui.view.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import debugger.ui.common.ColorCache;
import debugger.ui.common.FontList;
import syntaxhighlight.ParseResult;
import syntaxhighlight.Style;
import syntaxhighlight.Theme;
import syntaxhighlighter.SyntaxHighlighterParser;
import syntaxhighlighter.brush.Brush;

public class EditorText {
	private final StyledText styledText;
	private final ColorCache colorCache;
	
	private Brush brush = null;
	private final Theme theme = new ThemeSublime();
	private StyleRange[] syntaxHighlightingRanges = new StyleRange[0];
	
	public EditorText(Composite parent) {
		colorCache = new ColorCache(Display.getCurrent());
		
		styledText = new StyledText(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		styledText.setMargins(2, 1, 2, 1);
		styledText.setTabs(4);
		
		styledText.setFont(FontList.MONO_NORMAL);

		styledText.addDisposeListener(colorCache);
		
		styledText.setEditable(false);
		
		styledText.addCaretListener(new CaretListener() {
			public void caretMoved(CaretEvent event) {
				// Delay refreshing line style to ensure the new line count is used when deleting lines.
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						refreshStyle();
						refreshLineStyles();
					}
				});
			}
		});

		refreshStyle();
		refreshLineStyles();
	}
	
	/**
	 * Replace all newline characters with "\n".
	 */
	private static String replaceNewLines(String text) {
		return text.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
	}

	/**
	 * Refresh the line styles including the current line highlight, and line numbers.
	 */
	private void refreshLineStyles() {
		int line = styledText.getLineAtOffset(styledText.getCaretOffset());
		int maxLine = styledText.getLineCount();
		
		int lineCountWidth = Math.max(String.valueOf(maxLine).length(), 3);
		
		// Update line numbers.
		StyleRange style = new StyleRange();
		style.metrics = new GlyphMetrics(0, 0, lineCountWidth * 8 + 5);
		style.foreground = colorCache.getColor(70, 80, 90);
		Bullet bullet = new Bullet(ST.BULLET_NUMBER, style);
		styledText.setLineBullet(0, maxLine, null);
		styledText.setLineBullet(0, maxLine, bullet);
		
		// Update current line highlight.
		int lineCount = styledText.getContent().getLineCount();
		styledText.setLineBackground(0, lineCount, colorCache.getColor(theme.getBackground()));
		styledText.setLineBackground(line, 1, colorCache.getColor(47, 48, 42));
	}
	
	/**
	 * Updates the syntax highlighting styles.
	 */
	private void updateSyntaxHighlightingRanges() {
		if(brush == null) return;
		
		// Set syntax highlighting.
		SyntaxHighlighterParser parser = new SyntaxHighlighterParser(brush);
		List<ParseResult> results = filterResults(parser.parse(null, styledText.getText()));
		
		StyleRange[] styleRanges = new StyleRange[results.size()];
		for(int i = 0; i < styleRanges.length; i++) {
			ParseResult result = results.get(i);
			
			StyleRange range = new StyleRange();
			range.start = result.getOffset();
			range.length = result.getLength();
			range.fontStyle = SWT.NORMAL;
			
			Style foregroundStyle = theme.getStyles().get(result.getStyleKeys().get(0));
			if(foregroundStyle != null) {
				java.awt.Color foreground = foregroundStyle.getColor();
				range.foreground = colorCache.getColor(foreground);
			} else {
				java.awt.Color normal = theme.getPlain().getColor();
				range.foreground = colorCache.getColor(normal);
			}
			
			styleRanges[i] = range;
		}
		
		this.syntaxHighlightingRanges = styleRanges;
	}
	
	/**
	 * Refresh character style including foreground, background, syntax highlighting, and bracket highlighting.
	 */
	private void refreshStyle() {
		// Set background color.
		java.awt.Color background = theme.getBackground();
		styledText.setBackground(colorCache.getColor(background));
		
		// Set foreground color.
		java.awt.Color normal = theme.getPlain().getColor();
		styledText.setForeground(colorCache.getColor(normal));
		
		// Set syntax highlighting.
		styledText.setStyleRanges(syntaxHighlightingRanges);
		
		// Set bracket highlighting.
		if(styledText.getCaretOffset() > 0) {
			String text = styledText.getText();
			
			BracketMatcher bracketMatcher = new BracketMatcher();
			Optional<Integer> match = bracketMatcher.getMatchingParen(text, styledText.getCaretOffset() - 1);
			if(match.isPresent()) {
				int x = match.get();

				StyleRange range = new StyleRange();
				range.start = x;
				range.length = 1;
				range.foreground = getForegroundAt(x);
				range.borderStyle = SWT.BORDER_SOLID;
				range.borderColor = colorCache.getColor(150, 150, 150);
				styledText.setStyleRange(range);
			}
		}
		
		// Set matching word highlighting
		String selected = styledText.getSelectionText();
		if(selected.matches("[\\w]+")) {
			String text = styledText.getText();
			
			int index = -1;
			while((index = text.indexOf(selected, index + 1)) >= 0) {
				StyleRange range = new StyleRange();
				range.start = index;
				range.length = selected.length();
				range.foreground = getForegroundAt(index);
				range.borderStyle = SWT.BORDER_SOLID;
				range.borderColor = colorCache.getColor(150, 150, 150);
				styledText.setStyleRange(range);
			}
		}
	}
	
	private Color getForegroundAt(int index) {
		for(StyleRange range:syntaxHighlightingRanges) {
			if(range.start <= index && range.start + range.length > index) {
				return range.foreground;
			}
		}
		return null;
	}
	
	/**
	 * Returns the list of ParseResults so that it doesn't contain overlapping offsets.
	 */
	private List<ParseResult> filterResults(List<ParseResult> results) {
		List<ParseResult> filtered = new ArrayList<>();
		
		int lastIndex = -1;
		for(ParseResult result:results) {
			if(result.getOffset() <= lastIndex) {
				continue;
			}
			
			filtered.add(result);
			
			lastIndex = result.getOffset() + result.getLength();
		}
		
		return filtered;
	}

	public String getText() {
		return styledText.getText();
	}

	public void setText(String string) {
		if(!replaceNewLines(styledText.getText()).equals(replaceNewLines(string))) {
			Point selection = styledText.getSelection();
			
			styledText.setText(string);

			styledText.setSelection(selection);
		}
	}
	
	public Control getControl() {
		return styledText;
	}
	
	public Brush getBrush() {
		return brush;
	}
	
	public void setBrush(Brush brush) {
		this.brush = brush;
		updateSyntaxHighlightingRanges();
		refreshStyle();
		refreshLineStyles();
	}
	
	public StyledText getStyledText() {
		return styledText;
	}
}