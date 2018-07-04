//
// Copyright 2011-2012 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package waveview;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JColorChooser;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.FontMetrics;

class ColorButton extends JPanel {
    private Color color;
    private final String label;

    ColorButton(String label, Color initialColor) {
        setPreferredSize(new Dimension(180, DrawMetrics.COLOR_BUTTON_HEIGHT));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                showColorChooser();
            }
        });

        this.label = label;
        color = initialColor;
    }

    Color getColor() {
        return color;
    }

    private void showColorChooser() {
        Color newColor = JColorChooser.showDialog(this, "Color", color);
        if (newColor != null) {
            color = newColor;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        FontMetrics metrics = g.getFontMetrics();
        int fontBaseline = (DrawMetrics.COLOR_BUTTON_HEIGHT + metrics.getHeight()) / 2 - metrics.getDescent();

        g.setColor(color);
        g.fillRect(DrawMetrics.COLOR_BUTTON_INSET * 2, DrawMetrics.COLOR_BUTTON_INSET * 2,
                DrawMetrics.COLOR_BUTTON_WIDTH - DrawMetrics.COLOR_BUTTON_INSET * 4,
                DrawMetrics.COLOR_BUTTON_HEIGHT - DrawMetrics.COLOR_BUTTON_INSET * 4);

        g.setColor(Color.black);
        g.drawRect(DrawMetrics.COLOR_BUTTON_INSET * 2, DrawMetrics.COLOR_BUTTON_INSET * 2,
                DrawMetrics.COLOR_BUTTON_WIDTH - DrawMetrics.COLOR_BUTTON_INSET * 4,
                DrawMetrics.COLOR_BUTTON_HEIGHT - DrawMetrics.COLOR_BUTTON_INSET * 4);
        g.drawRect(DrawMetrics.COLOR_BUTTON_INSET, DrawMetrics.COLOR_BUTTON_INSET,
                DrawMetrics.COLOR_BUTTON_WIDTH - DrawMetrics.COLOR_BUTTON_INSET * 2,
                DrawMetrics.COLOR_BUTTON_HEIGHT - DrawMetrics.COLOR_BUTTON_INSET * 2);
        g.drawString(label, DrawMetrics.COLOR_BUTTON_INSET + DrawMetrics.COLOR_BUTTON_WIDTH, fontBaseline);
    }
}
