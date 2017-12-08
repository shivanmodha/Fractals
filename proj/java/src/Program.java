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
    private boolean k8md = false;
    private boolean k9md = false;
    private boolean md = false;
    private int first = 0;
    private BufferedImage[] swapchain = new BufferedImage[2];
    private Size content = new Size();
    private Size resolution = new Size(800, 600);
    private volatile boolean THREAD_01_WORKING = false;
    private volatile boolean THREAD_02_WORKING = false;
    private volatile boolean THREAD_03_WORKING = false;
    private volatile boolean THREAD_04_WORKING = false;
    private volatile Graphics2D THREAD_01_RENDERER;
    private volatile Graphics2D THREAD_02_RENDERER;
    private volatile Graphics2D THREAD_03_RENDERER;
    private volatile Graphics2D THREAD_04_RENDERER;
    private volatile long THREAD_01_TIME = 0;
    private volatile long THREAD_02_TIME = 0;
    private volatile long THREAD_03_TIME = 0;
    private volatile long THREAD_04_TIME = 0;
    private volatile long THREAD_00_TIME = 0;
    public static void main(String[] args)
    {
        new Program();
    }
    public Program()
    {
        Initialize(60);
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
        wnd.OnResize.Add(this, "Resize");
        wnd.OnMouseUp.Add(this, "MouseUp");
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
        RenderFractal();
    }
    public void RefreshSize(Point MousePosition, int Button)
    {
        swapchain[0] = new BufferedImage(resolution.Width, resolution.Height, BufferedImage.TYPE_INT_RGB);
        THREAD_01_RENDERER = swapchain[0].createGraphics();
        THREAD_02_RENDERER = swapchain[0].createGraphics();
        THREAD_03_RENDERER = swapchain[0].createGraphics();
        THREAD_04_RENDERER = swapchain[0].createGraphics();
        md = true;
    }
    public void Resize(Size oldSize, Size newSize)
    {
        swapchain[0] = new BufferedImage(resolution.Width, resolution.Height, BufferedImage.TYPE_INT_RGB);
        THREAD_01_RENDERER = swapchain[0].createGraphics();
        THREAD_02_RENDERER = swapchain[0].createGraphics();
        THREAD_03_RENDERER = swapchain[0].createGraphics();
        THREAD_04_RENDERER = swapchain[0].createGraphics();
        md = true;
    }
    public void MouseUp(Point MousePosition, int Button)
    {
        if (k8md || k9md)
        {
            RefreshSize(new Point(), 0);
            k8md = false;
            k9md = false;
            md = true;
        }
    }
    public void Update()
    {
        long startTime = System.currentTimeMillis();    
        md = false;
        content = wnd.Size.subtract(new Size(panelSize, 0));
        resolution = new Size(knobPrint[8], knobPrint[9]);
        for (int i = 0; i < knobs.length; i++)
        {
            if (knobs[i].GetState() == 2)
            {
                md = true;
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
                if (i == 8)
                {
                    k8md = true;
                }
                else if (i == 9)
                {
                    k9md = true;
                }
            }
            int knobRange = knobMax[i] - knobMin[i];
            int newVal = (int)(knobVals[i] * knobRange / 200);
            knobPrint[i] = newVal + knobMin[i];
        }
        if ((md || first < 10) && !THREAD_01_WORKING && !THREAD_02_WORKING && !THREAD_03_WORKING && !THREAD_04_WORKING)
        {
            first++;
            RenderFractal();
        }
        long endTime = System.currentTimeMillis();  
        THREAD_00_TIME = (endTime - startTime);
    }
    public BufferedImage RenderFractalRegion(int xStart, int xEnd, int yStart, int yEnd)
    {
        BufferedImage _return = new BufferedImage(xEnd - xStart, yEnd - yStart, BufferedImage.TYPE_INT_RGB);
        double zoom = knobPrint[0] / 10.0;
        int iterations = knobPrint[1];
        for (int j = yStart; j < yEnd; j++)
        {
            for (int i = xStart; i < xEnd; i++)
            {
                double x = ((double)resolution.Width / (double)resolution.Height) * (i - resolution.Width / 2) / (0.5 * zoom * resolution.Width);
                double y = (j - resolution.Height / 2) / (0.5 * zoom * resolution.Height);
                float counter = iterations;
                while (x * x + y * y < 6 && counter > 0)
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
                _return.setRGB(i - xStart, j - yStart, c);
            }
        }
        return _return;
    }
    public void RenderFractal()
    {
        if (resolution.Width == 0)
        {
            resolution.Width = content.Width;
            resolution.Height = content.Height;
        }
        if (swapchain[0] != null)
        {
            swapchain[1] = swapchain[0];
        }
        Thread t1 = new Thread(new Runnable()
        {            
            public void run()
            {
                long startTime = System.currentTimeMillis();
                THREAD_01_WORKING = true;
                BufferedImage q1 = RenderFractalRegion(0, resolution.Width / 2, 0, resolution.Height / 2);       
                THREAD_01_RENDERER.drawImage(q1, 0, 0, null);   
                THREAD_01_WORKING = false;    
                long endTime = System.currentTimeMillis();  
                THREAD_01_TIME = (endTime - startTime);
            }
        });
        Thread t2 = new Thread(new Runnable()
        {            
            public void run()
            {
                long startTime = System.currentTimeMillis();
                THREAD_02_WORKING = true;
                BufferedImage q2 = RenderFractalRegion(resolution.Width / 2, resolution.Width, 0, resolution.Height / 2);    
                THREAD_02_RENDERER.drawImage(q2, resolution.Width / 2, 0, null);
                THREAD_02_WORKING = false;   
                long endTime = System.currentTimeMillis();  
                THREAD_02_TIME = (endTime - startTime);   
            }
        });
        Thread t3 = new Thread(new Runnable()
        {            
            public void run()
            {
                long startTime = System.currentTimeMillis();
                THREAD_03_WORKING = true;
                BufferedImage q3 = RenderFractalRegion(0, resolution.Width / 2, resolution.Height / 2, resolution.Height);
                THREAD_03_RENDERER.drawImage(q3, 0, resolution.Height / 2, null);  
                THREAD_03_WORKING = false;    
                long endTime = System.currentTimeMillis();  
                THREAD_03_TIME = (endTime - startTime);
            }
        });
        Thread t4 = new Thread(new Runnable()
        {            
            public void run()
            {
                long startTime = System.currentTimeMillis();
                THREAD_04_WORKING = true;
                BufferedImage q4 = RenderFractalRegion(resolution.Width / 2, resolution.Width, resolution.Height / 2, resolution.Height);
                THREAD_04_RENDERER.drawImage(q4, resolution.Width / 2, resolution.Height / 2, null);   
                THREAD_04_WORKING = false;   
                long endTime = System.currentTimeMillis();  
                THREAD_04_TIME = (endTime - startTime);
            }
        });  
        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }
    public void Render(GraphicsUnit Graphics)
    {
        // Clear
        Graphics.FillRectangle(Color.WhiteSmoke, new Point(0, 0), wnd.Size);
        // Fractal
        Graphics.graphics.drawImage(swapchain[1], 0, 0, null);
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
        Graphics.DrawString("T0~ME: " + wnd.FPS + "fps", Color.Black, new Point(0, 0), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T0~UI: " + THREAD_00_TIME + "ms", Color.Black, new Point(0, 15), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T1~Q1: " + THREAD_01_TIME + "ms, " + THREAD_01_WORKING, Color.Black, new Point(0, 30), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T2~Q2: " + THREAD_02_TIME + "ms, " + THREAD_02_WORKING, Color.Black, new Point(0, 45), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T3~Q3: " + THREAD_03_TIME + "ms, " + THREAD_03_WORKING, Color.Black, new Point(0, 60), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T4~Q4: " + THREAD_04_TIME + "ms, " + THREAD_04_WORKING, Color.Black, new Point(0, 75), new Font("Consolas", Font.PLAIN, 12));
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
