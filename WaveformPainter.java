// Draws waveforms 

import java.awt.*;

interface WaveformPainter
{
	public void paint(Graphics g, TraceDataModel model, int netId, 
		int y, Rectangle visibleRect, double horizontalScale,
		ValueFormatter formatter);
}
