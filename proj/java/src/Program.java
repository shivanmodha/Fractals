import studios.vanish.engine.Window;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import studios.vanish.engine.Button;
import studios.vanish.engine.Color;
import studios.vanish.engine.GraphicsUnit;
import studios.vanish.engine.Point;
import studios.vanish.engine.Size;
public class Program
{
    /**
     * Variables
     */
    private Window wnd = new Window("Fractals", new Size(900 + 330, 700), true);
    private int panelSize = 330;
    private Button[] knobs = new Button[10];
    private int[] knobVals = new int[] {  +2,  +80,   +0, +110, +200, +200, +100, +100,    +0,    +0};
    private int[] knobMin  = new int[] {  +1, +100,   +0,   +0,   +0,   +0, -200, -200,  +800,  +600};
    private int[] knobMax  = new int[] {+500, +600, +360, +100, +100, +100, +200, +200, +2000, +2000};
    private int[] knobPrint = new int[10];
    private BufferedImage fractal;
    private Size content = new Size();
    private Size resolution = new Size(800, 600);
    public static void main(String[] args)
    {
        new Program();
    }
    public Program()
    {
        Initialize(10000);
        while (true)
        {
            Update();
            wnd.Wait(2);
        }
    }
    public void Initialize(int FPS)
    {
        // Initialize Window
        wnd.OnPaint.Add(this, "Render");
        wnd.Initialize(FPS);
        wnd.Show();
        // Initialize GUI Elements
        for (int i = 0; i < knobs.length; i++)
        {
            knobs[i] = new Button(wnd, "");
            knobs[i].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
            knobs[i].Size = new Size(10, 20);
        }
        content = wnd.Size.subtract(new Size(panelSize, 0));
        resolution = new Size(800, 600);
        // Initialize Fractal
        fractal = RenderFractal();
    }
    public void Update()
    {
        content = wnd.Size.subtract(new Size(panelSize, 0));
        resolution = new Size(knobPrint[8], knobPrint[9]);
        for (int i = 0; i < knobs.length; i++)
        {
            if (knobs[i].GetState() == 2)
            {
                int leftboundary = wnd.Size.Width - (panelSize * 3 / 4) + 12;
                int rightboundary = leftboundary + 200;
                Point realMousePosition = wnd.GetMousePosition().subtract(wnd.Location);
                if (realMousePosition.X > leftboundary && realMousePosition.X < rightboundary)
                {
                    int delta = (int)realMousePosition.X - leftboundary;
                    knobVals[i] = delta;
                }
                else if (realMousePosition.X < leftboundary)
                {
                    knobVals[i] = 0;
                }
                else if (realMousePosition.X > rightboundary)
                {
                    knobVals[i] = 200;
                }
            }
            int knobRange = knobMax[i] - knobMin[i];
            int newVal = (int)(knobVals[i] * knobRange / 200);
            knobPrint[i] = newVal + knobMin[i];
        }
        fractal = RenderFractal();
    }
    public BufferedImage RenderFractal()
    {
        if (resolution.Width == 0)
        {
            resolution.Width = content.Width;
            resolution.Height = content.Height;
        }
        BufferedImage _return = new BufferedImage(resolution.Width, resolution.Height, BufferedImage.TYPE_INT_RGB);
        double zoom = knobPrint[0] / 10.0;
        int iterations = knobPrint[1];
        for (int j = 0; j < resolution.Height; j++)
        {
            for (int i = 0; i < resolution.Width; i++)
            {
                double x = ((double)resolution.Width / (double)resolution.Height) * (i - resolution.Width / 2) / (0.5 * zoom * resolution.Width);
                double y = (j - resolution.Height / 2) / (0.5 * zoom * resolution.Height);
                float counter = iterations;
                while (x * x + y * y < 100 && counter > 0)
                {
                    double newX = x * x - y * y + (knobPrint[7] / 100.0);
                    y = 2.0 * x * y + (knobPrint[6] / 100.0);
                    x = newX;
                    counter--;
                }
                int c = 0;
                if (counter > 0)
                {
                    c = java.awt.Color.HSBtoRGB((iterations / counter) % 1 + (knobPrint[3] / 100.0f), (knobPrint[4] / 100.0f), (knobPrint[5] / 100.0f));
                }
                else
                {
                    c = java.awt.Color.HSBtoRGB(0, (knobPrint[4] / 100.0f), 0);
                }
                _return.setRGB(i, j, c);
            }
        }
        return _return;
    }
    public void Render(GraphicsUnit Graphics)
    {
        // Clear
        Graphics.FillRectangle(Color.WhiteSmoke, new Point(0, 0), wnd.Size);
        // Fractal
        Graphics.graphics.drawImage(fractal, 0, 0, null);
        // Layout
        Graphics.FillRectangle(Color.Snow, new Point(wnd.Size.Width - panelSize, 0), new Size(panelSize, wnd.Size.Height));
        int y = 10;
        y = DrawHeader(Graphics, "View", y);
        y = DrawItem(Graphics, "Zoom", y, 0);
        y = DrawItem(Graphics, "Rotation", y, 2);
        y = DrawHeader(Graphics, "Quality and Resolution", y);
        y = DrawItem(Graphics, "Iterations", y, 1);
        y = DrawItem(Graphics, "Width", y, 8);
        y = DrawItem(Graphics, "Height", y, 9);
        y = DrawHeader(Graphics, "Color", y);
        y = DrawItem(Graphics, "Hue", y, 3);
        y = DrawItem(Graphics, "Saturation", y, 4);
        y = DrawItem(Graphics, "Brightness", y, 5);
        y = DrawHeader(Graphics, "Fractal Equation (a + bi)", y);
        y = DrawItem(Graphics, "a", y, 6);
        y = DrawItem(Graphics, "b", y, 7);
        // Debugging Statistics
        Graphics.DrawString(content.toString(), Color.Black, content.subtract(Graphics.GetTextSize(content.toString(), new Font("Consolas", Font.PLAIN, 12))).toPoint().subtract(new Point(10, 5)), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("" + wnd.FPS, Color.Black, new Point(0, 0), new Font("Consolas", Font.PLAIN, 12));
    }
    public int DrawHeader(GraphicsUnit Graphics, String text, int y)
    {
        Graphics.DrawString(text, Color.Black, new Point(wnd.Size.Width - panelSize + 10, y), new Font("Century Gothic", Font.BOLD, 18));
        return y + 30;
    }
    public int DrawItem(GraphicsUnit Graphics, String text, int y, int knob)
    {
        Font fnt = new Font("Century Gothic", Font.PLAIN, 12);
        Graphics.DrawString(text, Color.Gray, new Point(wnd.Size.Width - (panelSize * 3 / 4) - Graphics.GetTextSize(text, fnt).Width - 10, y), fnt);
        Graphics.DrawRectangle(Color.Black, new Point(wnd.Size.Width - (panelSize * 3 / 4), y + (Graphics.GetTextSize(text, fnt).Height / 2) + 1), new Size(210, 1));
        knobs[knob].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4) + knobVals[knob], y);
        knobs[knob].Render(Graphics);
        Graphics.DrawString("" + knobPrint[knob], Color.Gray, new Point(wnd.Size.Width - (panelSize * 3 / 4) + 215, y), new Font("Century Gothic", Font.PLAIN, 12));
        return y + 25;
    }
}
