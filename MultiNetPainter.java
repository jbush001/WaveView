import java.awt.*;
import java.util.*;

///
/// Delegate that draws the waveform for a single net that has more than one
/// bit in it.
///
class MultiNetPainter implements WaveformPainter
{
	private static final int kWaveformHeight = 20;
	private static final int kTransitionWidth = 3;
	
	public void paint(Graphics g, TraceDataModel model, int netId, 
		int topOffset, Rectangle visibleRect, double horizontalScale,
		ValueFormatter formatter)
	{
		FontMetrics metrics = g.getFontMetrics();
		int fontBaseline = topOffset + (kWaveformHeight + metrics.getHeight()) / 2 - metrics.getDescent();

		boolean lastValueWasZ = false;
		boolean lastValueWasX = false;
		int lastX = visibleRect.x + visibleRect.width;	// Don't draw before the first segment
		String previousValue = "";
		long firstTimestamp = (long)(visibleRect.x * horizontalScale);

		g.setColor(AppPreferences.getInstance().kTraceColor);

		Iterator<Transition> i = model.findTransition(netId, firstTimestamp);
		while (true)
		{	
			// Draw the segment to the left of this transition
			Transition transition = i.next();
			
			boolean isZ = transition.isZ();
			boolean isX = !isZ && transition.isX();

			// Compute the boundaries of this segment
			int x = (int)(transition.getTimestamp() / horizontalScale);

			// Draw transition
			if (x - lastX > kTransitionWidth * 2)
			{
				if (!lastValueWasZ)
				{
					if (lastValueWasX)
					{
						// Fill in transition twiddle with gray
						fPolygonXPoints[0] = x - kTransitionWidth;
						fPolygonYPoints[0] = topOffset;
						fPolygonXPoints[1] = x;
						fPolygonYPoints[1] = topOffset + kWaveformHeight / 2;
						fPolygonXPoints[2] = x - kTransitionWidth;
						fPolygonYPoints[2] = topOffset + kWaveformHeight;
						g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
						g.setColor(AppPreferences.getInstance().kConflictColor);
						g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
						g.setColor(AppPreferences.getInstance().kTraceColor);
					}
				
					// Draw transition twiddle (left half)
					g.drawLine(x - kTransitionWidth, topOffset, x, topOffset + kWaveformHeight / 2);
					g.drawLine(x - kTransitionWidth, topOffset + kWaveformHeight, x, 
						topOffset + kWaveformHeight / 2);
				}
				
				if (!isZ)
				{
					if (isX)
					{
						// Fill in transition with gray
						fPolygonXPoints[0] = x + kTransitionWidth;
						fPolygonYPoints[0] = topOffset;
						fPolygonXPoints[1] = x;
						fPolygonYPoints[1] = topOffset + kWaveformHeight / 2;
						fPolygonXPoints[2] = x + kTransitionWidth;
						fPolygonYPoints[2] = topOffset + kWaveformHeight;
						g.setColor(AppPreferences.getInstance().kConflictColor);
						g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
						g.setColor(AppPreferences.getInstance().kTraceColor);
					}

					// Draw transition twiddle (right half)
					g.drawLine(x + kTransitionWidth, topOffset, x, topOffset + kWaveformHeight / 2);
					g.drawLine(x + kTransitionWidth, topOffset + kWaveformHeight, x, 
						topOffset + kWaveformHeight / 2);
				}
			}
			else if (x > lastX)
			{
				// Draw squished net values
				g.fillRect(x - kTransitionWidth, topOffset, kTransitionWidth * 2, kWaveformHeight);
			}

			drawSpan(g, Math.max(visibleRect.x, lastX + kTransitionWidth), 
				Math.min(visibleRect.x + visibleRect.width, x - kTransitionWidth), 
				topOffset, previousValue, lastValueWasZ, lastValueWasX, fontBaseline,
				metrics);

			// Stop drawing when we've gone past the edge of the viewport
			// (trace is no longer visible).
			if (x > visibleRect.x + visibleRect.width)
				break;		

			previousValue = formatter.format(transition);
			lastValueWasZ = isZ;
			lastValueWasX = isX;
			lastX = x;
			if (!i.hasNext())
			{
				// End of the trace.  Draw remaining span running off to the right...
				drawSpan(g, Math.max(visibleRect.x,lastX + kTransitionWidth), 
					visibleRect.x + visibleRect.width, topOffset, previousValue, 
					lastValueWasZ, lastValueWasX, fontBaseline, metrics);
				break;
			}
		}
	}

	private void drawSpan(Graphics g, int left, int right, int top, String label,
		boolean isZ, boolean isX, int fontBaseline, FontMetrics metrics)
	{
		if (right <= left)
			return; // You'll end up with single pixel boogers in some cases otherwise
	
		if (isZ)
			g.drawLine(left, top + kWaveformHeight / 2, right, top + kWaveformHeight / 2);
		else
		{
			if (isX)
			{
				g.setColor(AppPreferences.getInstance().kConflictColor);
				g.fillRect(left, top + 1, right - left, kWaveformHeight - 1);
				g.setColor(AppPreferences.getInstance().kTraceColor);
			}
			
			g.drawLine(left, top, right, top);
			g.drawLine(left, top + kWaveformHeight, right, top + kWaveformHeight);

			// Draw text label with values
			int stringWidth = metrics.stringWidth(label);
			int visibleWidth = right - left;
			if (stringWidth < visibleWidth)
			{
				// Fits, draw it.
				int fontX = (visibleWidth - stringWidth) / 2 + left;
				g.drawString(label, fontX, fontBaseline);
			}
			else
			{
				// Try to squeeze in an ellipsis
				String ellipsis = "\u2026";
				stringWidth = metrics.stringWidth(ellipsis);
				if (stringWidth < visibleWidth)
				{
					// At least this fits
					int fontX = (visibleWidth - stringWidth) / 2 + left;
					g.drawString("\u2026", fontX, fontBaseline);
				}
				
				// else we draw no label (it won't fit)
			}
		}
	}

	private int[] fPolygonXPoints = new int[3];
	private int[] fPolygonYPoints = new int[3];
}