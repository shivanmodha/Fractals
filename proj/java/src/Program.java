/**
 * 
 * Java Fractal Viewer
 * 
 * @author Shivan Modha
 * @version 2.1
 * 
 * (van|sh) stud10s 2017
 */
/**
 * Java Swing Dependencies
 */
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
/**
 * Matrix Engine Dependencies
 * @version 1.0.1
 */
import studios.vanish.engine.Window;
import studios.vanish.engine.Button;
import studios.vanish.engine.Color;
import studios.vanish.engine.GraphicsUnit;
import studios.vanish.engine.Point;
import studios.vanish.engine.Rectangle;
import studios.vanish.engine.Size;
public class Program
{
    /**
     * GUI Interface
     */
    private Window wnd; // Matrix Engine Window
    private int panelSize = 330; // Right panel size
    private Button[] knobs = new Button[13]; // Knob objects
    private int[] knobVals = new int[] {  +5,  +80, +100, +110, +200, +200, +100, +100,   +50,   +50, +105, +105, 100}; // Current knob value in terms of 200 (track size is 200px)
    private int[] knobMin  = new int[] {  +1, +100,   +0,   +0,   +0,   +0, -200, -200,  +200,  +200,  -20,  -20,   0}; // Physical minimum size (custom scale)
    private int[] knobMax  = new int[] {+500, +600, +360, +100, +100, +100, +200, +200, +5000, +5000,  +20,  +20, 100}; // Physical maximum size (custom scale)
    private int[] knobPrint = new int[13]; // Physical value (custom scale between knobMin[i] and knobMax[i]
    private boolean k8md = false; // knob[8] down
    private boolean k9md = false; // knob[9] down
    private boolean md = false; // knob[i] down
    private Size content = new Size(); // Fractal content size
    private int first = 0; // Initial load render used to render the image a couple of times during startup while variables are being set and application is starting
    private Button[] regionsbtn = new Button[4]; // Render region control GUI object
    private boolean[] enabledPP = new boolean[] {false, false}; // Post Processing effects boolean vals
    private Button[] ppbtn = new Button[2]; // Post Processing toggle button GUI objects
    private Button[] savebtn = new Button[2]; // Save button GUI object
    private Point StartMousePosition; // Previous Mouse Position Variable
    private volatile Point fractalOffset = new Point(0, 0); // Fractal movement offset for mouse move
    private boolean cursoroverride = false; // Waiting cursor
    /**
     * Threaded Rendering
     * 
     * Swapchain: Top level, shared resource used for graphics buffering and rendering.
     * swapchain[0]: Image being currently drawn through the threads. This is the most up to date image, but may not be completed during rendering. This way, if
     * something fails swapchain[0] can be set to a new instance of BufferedImage without impacting the image rendered to the screen, preventing random black flashes.
     * swapchain[1]: Previous drawn, completely up to date image. After swapchain[0] is finished, swapchain[1] = swapchain[0]
     * 
     * T01: Handles Quadrant 2 of Fractal
     * T02: Handles Quadrant 1 of Fractal
     * T03: Handles Quadrant 3 of Fractal
     * T04: Handles Quadrant 4 of Fractal
     * 
     * Thread hierarchy
     *   | - JAVA: Main java thread
     *     | - T0~ME (main): Matrix Engine Rendering Thread (Render), rendering fractalImage to screen
     *     | - T0~UI (AWT-EventQueue-0): Main Java Swing Handle (Update)
     *       | - T1~TL: Renders 1/4 Fractal
     *       | - T2~TR: Renders 1/4 Fractal
     *       | - T3~BL: Renders 1/4 Fractal
     *       | - T4~BR: Renders 1/4 Fractal
     *     | - T5~PP: Post Processing thread, rendering swapchain[1] to fractalImage
     */
    private volatile BufferedImage[] swapchain = new BufferedImage[2];
    private volatile int resolutionWidth = 1400; // Shared resolution width variable (not using studios.vanish.engine.Size, ensuring minimum memory allocation)
    private volatile int resolutionHeight = 1400; // Shared resolution height variable (not using studios.vanish.engine.Size, ensuring minimum memory allocation)
    private volatile boolean THREAD_01_WORKING = false; // Shared thread isworking variable
    private volatile boolean THREAD_02_WORKING = false; // Shared thread isworking variable
    private volatile boolean THREAD_03_WORKING = false; // Shared thread isworking variable
    private volatile boolean THREAD_04_WORKING = false; // Shared thread isworking variable (at this point I should've just made this an array lol)
    private volatile long THREAD_01_TIME = 0; // Shared thread time variable
    private volatile long THREAD_02_TIME = 0; // Shared thread time variable
    private volatile long THREAD_03_TIME = 0; // Shared thread time variable
    private volatile long THREAD_04_TIME = 0; // Shared thread time variable
    private volatile long THREAD_05_TIME = 0; // Shared thread time variable
    private volatile long THREAD_00_TIME = 0; // Shared thread time variable (definitely using an array next time)
    private Graphics2D THREAD_01_RENDERER; // External renderer object
    private Graphics2D THREAD_02_RENDERER; // External renderer object
    private Graphics2D THREAD_03_RENDERER; // External renderer object
    private Graphics2D THREAD_04_RENDERER; // External renderer object
    private volatile boolean[] enabledRegions = new boolean[] {true, true, true, true}; // Thread render control
    private BufferedImage fractalImage; // Post Processing thread will render here. Shared resource for main ME Render thread and post processing thread
    private Thread pp; // Post Processing Thread
    /**
     * Java Entry Point
     * @param args entry arguments
     */
    public static void main(String[] args)
    {
        try
        {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e)
        {
            
        }
        new Program();
    }
    /**
     * Constructor
     */
    public Program()
    {
        // Initialize Matrix Engine
        wnd = new Window("Fractals", new Size(900 + 330, 700), true);
        Initialize(60);
        // Keep T0~UI Running
        while (true)
        {
            Update();
            wnd.Wait(2); // Otherwise, its WAYY to fast, and the CPU gets overloaded
        }
    }
    /**
     * Initialize the program completely
     * @param FPS FPS Cap
     */
    public void Initialize(int FPS)
    {
        // Initialize window and the event handlers
        wnd.OnPaint.Add(this, "Render");
        wnd.OnResize.Add(this, "Resize");
        wnd.OnMouseDown.Add(this, "MouseDown");
        wnd.OnMouseUp.Add(this, "MouseUp");
        wnd.OnMouseWheel.Add(this, "Wheel");
        wnd.OnMouseDrag.Add(this, "MouseDrag");
        wnd.OnMouseMove.Add(this, "MouseMove");
        wnd.Initialize(FPS);
        // Initialize GUI Elements
        try
        {
            wnd.GetInnerWindow().setIconImage(ImageIO.read(new File("icon.png")));
        }
        catch (IOException e)
        {            
            e.printStackTrace();
        }
        for (int i = 0; i < knobs.length; i++)
        {
            knobs[i] = new Button(wnd, "");
            knobs[i].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
            knobs[i].Size = new Size(10, 20);
        }
        content = wnd.Size.subtract(new Size(panelSize, 0));
        for (int i = 1; i < 5; i++)
        {
            regionsbtn[i - 1] = new Button(wnd, "0" + i, new Point(0, 0), new Size(24, 24));
            regionsbtn[i - 1].SetBackColor(Color.Orange, Color.DarkOrange, Color.Gray);
            regionsbtn[i - 1].SetForeColor(Color.White, Color.Black, Color.Black);
            regionsbtn[i - 1].OnClick.Add(this, "RegionBTN");
        }
        for (int i = 0; i < ppbtn.length; i++)
        {
            ppbtn[i] = new Button(wnd, "Disabled", new Point(0, 0), new Size(75, 24));
            ppbtn[i].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
            ppbtn[i].SetForeColor(Color.White, Color.Black, Color.Black);
            ppbtn[i].OnClick.Add(this, "PPBTN");
        }
        for (int i = 0; i < savebtn.length; i++)
        {
            savebtn[i] = new Button(wnd, "", new Point(0, 0), new Size(75, 24));
            savebtn[i].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
            savebtn[i].SetForeColor(Color.White, Color.Black, Color.Black);
            savebtn[i].OnClick.Add(this, "SaveBTN");
        }
        savebtn[0].Text = "Viewport";
        savebtn[1].Text = "Image";
        // Initialize Fractal Post Processing Thread. This thread draws the completed fractal in the content
        pp = new Thread(() ->
        {
            while (true)
            {
                // Time the thread   
                long startTime = System.currentTimeMillis();             
                // In case swapchain[0] has been reset, update swapchain[1]
                if (!THREAD_01_WORKING && !THREAD_02_WORKING && !THREAD_03_WORKING && !THREAD_04_WORKING)
                {
                    if (swapchain[0] != null)
                    {
                        swapchain[1] = swapchain[0];
                    }
                }
                // Construct the image. We don't want to draw directly to fractalImage because fractalImage may be rendered right now
                BufferedImage image = new BufferedImage(content.Width, content.Height, BufferedImage.TYPE_INT_RGB);
                Graphics2D iG = image.createGraphics();
                // Post Processing effects
                if (enabledPP[0])
                {
                    iG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                }
                else
                {
                    iG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);                    
                }
                if (enabledPP[1])
                {
                    iG.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
                }
                else
                {
                    iG.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);                    
                }
                // Transform the image based on rotation and translation offsets
                AffineTransform at = new AffineTransform();
                double angle = knobPrint[2] * Math.PI / 180;
                at.translate((content.Width / 2) - (resolutionWidth / 2), (content.Height / 2) - (resolutionHeight / 2));
                at.translate(fractalOffset.X, fractalOffset.Y);
                at.rotate(angle, resolutionWidth / 2, resolutionHeight / 2);
                // Clear the background
                iG.setColor(new java.awt.Color(Color.WhiteSmoke.R, Color.WhiteSmoke.G, Color.WhiteSmoke.B));
                iG.fillRect(0, 0, content.Width, content.Height);
                // Draw swapchain[1]
                iG.drawImage(swapchain[1], at, null);                
                // Add contrast post processing effect
                float factor = knobPrint[12] / 50.0f;                
                RescaleOp contrastFilter = new RescaleOp(factor, 0.0f, null);
                image = contrastFilter.filter(image, null);                
                // Set fractalImage, since it is now ready to be drawn
                fractalImage = image;
                // Get the time
                long endTime = System.currentTimeMillis();  
                THREAD_05_TIME = (endTime - startTime);
            }
        }, "PP");
        // Start the post processing thread and show the window
        pp.start();        
        wnd.Show();
    }
    /**
     * Wheel event handler, called from the Matrix Engine
     * 
     * Change the zoom level of the fractal only if the mouse is on top of the fractal image
     * @param amount Scroll Delta Amount
     */
    public void Wheel(int amount)
    {
        Point ml = wnd.GetMousePosition().subtract(wnd.Location);
        Rectangle rect = new Rectangle(Color.Black, new Point(0, 30), content);
        if (rect.Intersects(ml))
        {
            knobVals[0] -= amount;
            int knobRange = knobMax[0] - knobMin[0];
            int newVal = (int)(knobVals[0] * knobRange / 200);
            knobPrint[0] = newVal + knobMin[0];
            if (knobPrint[0] > knobMax[0])
            {
                knobVals[0] = 200;
            }
            else if (knobPrint[0] < knobMin[0])
            {
                knobVals[0] = 0;
            }
            RenderFractal();
        }
    }
    /**
     * Mouse Drag event handler, called from the Matrix Engine
     * 
     * Change the translation offset of the fractal only if the mouse is on top of the fractal image
     * @param MouseLocation Current Mouse Location
     * @param Button Mouse Down Buttons
     */
    public void MouseDrag(Point MouseLocation, int Button)
    {
        Point ml = wnd.GetMousePosition().subtract(wnd.Location);
        Rectangle rect = new Rectangle(Color.Black, new Point(0, 30), content);
        if (rect.Intersects(ml))
        {
            Point delta = StartMousePosition.subtract(MouseLocation);
            fractalOffset = fractalOffset.subtract(delta);
            StartMousePosition = MouseLocation;
        }
    }
    /**
     * Mouse Move event handler, called from the Matrix Engine
     * 
     * Change the cursor of the mouse only if the mouse is on top of the fractal image
     * @param MouseLocation Current Mouse Location
     * @param Button Mouse Down Buttons
     */
    public void MouseMove(Point MouseLocation, int Button)
    {
        Point ml = wnd.GetMousePosition().subtract(wnd.Location);
        Rectangle rect = new Rectangle(Color.Black, new Point(0, 30), content);
        if (!cursoroverride)
        {
            if (rect.Intersects(ml))
            {
                wnd.Cursor_Set(Cursor.MOVE_CURSOR);
            }
            else
            {
                wnd.Cursor_Reset();
            }
        }
    }
    /**
     * OnClick event handler, called from the save buttons
     * @param sender Handle to the object sending the event
     * @param MouseLocation Current Mouse Location
     * @param MouseButton Current Mouse Down Buttons
     */
    public void SaveBTN(Button sender, Point MouseLocation, int MouseButton)
    {
        // Get Save Button
        int idx = -1;
        for (int i = 0; i < savebtn.length; i++)
        {
            if (sender == savebtn[i])
            {
                idx = i;
            }
        }
        // Prompt a save file dialog
        JFileChooser sfd = new JFileChooser();
        if (sfd.showSaveDialog(wnd.GetInnerWindow()) == JFileChooser.APPROVE_OPTION)
        {
            File file = sfd.getSelectedFile();
            if (idx == 0)
            {
                // Save rendered image
                try
                {
                    ImageIO.write(fractalImage, "png", file);
                }
                catch (Exception e)
                {
                    
                }
            }
            else if (idx == 1)
            {
                // Save RAW image
                try
                {
                    BufferedImage image = new BufferedImage(resolutionWidth, resolutionHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D iG = image.createGraphics();
                    AffineTransform at = new AffineTransform();
                    double angle = knobPrint[2] * Math.PI / 180;
                    at.rotate(angle, resolutionWidth / 2, resolutionHeight / 2);
                    iG.drawImage(swapchain[1], at, null);                    
                    float factor = knobPrint[12] / 50.0f;                
                    RescaleOp contrastFilter = new RescaleOp(factor, 0.0f, null);
                    image = contrastFilter.filter(image, null);                    
                    ImageIO.write(image, "png", file);
                }
                catch (Exception e)
                {
                    
                }
            }
        }
    }
    /**
     * OnClick event handler, called from the post processing buttons
     * @param sender Handle to the object sending the event
     * @param MouseLocation Current Mouse Location
     * @param MouseButton Current Mouse Down Buttons
     */
    public void PPBTN(Button sender, Point MouseLocation, int MouseButton)
    {
        // Get PP Button
        int idx = -1;
        for (int i = 0; i < ppbtn.length; i++)
        {
            if (sender == ppbtn[i])
            {
                idx = i;
            }
        }
        // Enable correct PP effect
        enabledPP[idx] = !enabledPP[idx];
        if (enabledPP[idx])
        {
            ppbtn[idx].SetBackColor(Color.Orange, Color.DarkOrange, Color.Gray);
            ppbtn[idx].Text = "Enabled";
        }
        else
        {
            ppbtn[idx].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
            ppbtn[idx].Text = "Disabled";
        }
    }
    /**
     * OnClick event handler, called from the region buttons
     * @param sender Handle to the object sending the event
     * @param MouseLocation Current Mouse Location
     * @param MouseButton Current Mouse Down Buttons
     */
    public void RegionBTN(Button sender, Point MouseLocation, int MouseButton)
    {
        // Get Region Button
        int idx = -1;
        for (int i = 0; i < regionsbtn.length; i++)
        {
            if (sender == regionsbtn[i])
            {
                idx = i;
            }
        }
        // Enable correct rendering region
        enabledRegions[idx] = !enabledRegions[idx];
        if (enabledRegions[idx])
        {
            regionsbtn[idx].SetBackColor(Color.Orange, Color.DarkOrange, Color.Gray);            
        }
        else
        {
            regionsbtn[idx].SetBackColor(Color.SkyBlue, Color.DeepSkyBlue, Color.Gray);
        }
    }
    /**
     * Form Resize event handler, called from the Matrix Engine
     * 
     * Changes the resolution and refreshes the drawing points
     * @param oldSize old window size
     * @param newSize new window size
     */
    public void Resize(Size oldSize, Size newSize)
    {
        swapchain[0] = new BufferedImage(resolutionWidth, resolutionHeight, BufferedImage.TYPE_INT_RGB);
        THREAD_01_RENDERER = swapchain[0].createGraphics();
        THREAD_02_RENDERER = swapchain[0].createGraphics();
        THREAD_03_RENDERER = swapchain[0].createGraphics();
        THREAD_04_RENDERER = swapchain[0].createGraphics();
        RenderFractal();
        md = true;
    }
    /**
     * Mouse Down event handler, called from the Matrix Engine
     * @param MousePosition Current Mouse Position
     * @param Button Current Selected Button
     */
    public void MouseDown(Point MousePosition, int Button)
    {
        // Set previous mouse position to current mouse position
        StartMousePosition = MousePosition;
    }
    /**
     * Mouse Up event handler, called from the Matrix Engine
     * @param MousePosition Current Mouse Position
     * @param Button Current Selected Button
     */
    public void MouseUp(Point MousePosition, int Button)
    {
        // Force size reset when the resolution changes
        if (k8md || k9md)
        {
            k8md = false;
            k9md = false;
            Resize(new Size(0, 0), new Size(0, 0));
        }
    }
    /**
     * Basically the T0~UI Thread
     * 
     * This thread, theoretically, shouldn't be handling any of the rendering. It is for the UI updates only!
     */
    public void Update()
    {
        // Time thread
        long startTime = System.currentTimeMillis();
        // Make sure these variables are accurate at all times
        content = wnd.Size.subtract(new Size(panelSize, 0));
        resolutionWidth = knobPrint[8];
        resolutionHeight = knobPrint[9];
        // Loop through the knob buttons
        for (int i = 0; i < knobs.length; i++)
        {
            // Something is being changed, lets handle it
            if (knobs[i].GetState() == 2)
            {
                // Make sure the knob doesn't move out of the track
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
            // Set the knob value
            int knobRange = knobMax[i] - knobMin[i];
            int newVal = (int)(knobVals[i] * knobRange / 200);
            knobPrint[i] = newVal + knobMin[i];
        }
        // If a value is being changed, and the threads aren't currently rendering, trigger a render
        if ((md || first < 3) && !THREAD_01_WORKING && !THREAD_02_WORKING && !THREAD_03_WORKING && !THREAD_04_WORKING)
        {
            first++;
            RenderFractal();
            md = false;            
        }
        // Set the waiting cursor accordingly
        if (!THREAD_01_WORKING && !THREAD_02_WORKING && !THREAD_03_WORKING && !THREAD_04_WORKING)
        {
            if (cursoroverride)
            {
                wnd.Cursor_Reset();
            }
            cursoroverride = false;
        }
        else
        {
            cursoroverride = true;
            if (!md)
            {
                wnd.Cursor_Set(Cursor.WAIT_CURSOR);
            }
            else
            {
                wnd.Cursor_Reset();
            }
        }
        // End time
        long endTime = System.currentTimeMillis();  
        THREAD_00_TIME = (endTime - startTime);
    }
    /**
     * Actual Fractal rendering method, however this will only render a region of the image
     * @param xStart Left most x
     * @param xEnd Right most x
     * @param yStart Top most y
     * @param yEnd Bottom most y
     * @return
     */
    public BufferedImage RenderFractalRegion(int xStart, int xEnd, int yStart, int yEnd)
    {
        // resolutionWidth must not equal 0
        if (resolutionWidth == 0 || resolutionHeight == 0)
        {
            resolutionWidth = content.Width;
            resolutionHeight = content.Height;
        }
        // Create rendering image
        int w = xEnd - xStart;
        if (w <= 0)
        {
            w = 1;
        }
        int h = yEnd - yStart;
        if (h <= 0)
        {
            h = 1;
        }
        BufferedImage _return = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // Set variables
        double zoom = knobPrint[0] / 50.0;
        int iterations = knobPrint[1];
        double xMul = knobPrint[10];
        double yMul = knobPrint[11];
        // Render fractal
        for (int j = yStart; j < yEnd; j++)
        {
            for (int i = xStart; i < xEnd; i++)
            {
                double x = ((double)resolutionWidth / (double)resolutionHeight) * (i - resolutionWidth / 2) / (0.5 * zoom * resolutionWidth);
                double y = (j - resolutionHeight / 2) / (0.5 * zoom * resolutionHeight);
                float counter = iterations;
                while (x * x * xMul + y * y * yMul < 6 && counter > 0)
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
    /**
     * Method used to trigger a multi-threaded render event 
     */
    public void RenderFractal()
    {
        // Update swapchain[1] if swapchain[0] has been reset
        if (swapchain[0] != null)
        {
            swapchain[1] = swapchain[0];
        }
        else
        {
            // This has never been initialized, so initialize it
            swapchain[0] = new BufferedImage(resolutionWidth, resolutionHeight, BufferedImage.TYPE_INT_RGB);
        }        
        if (!THREAD_01_WORKING && !THREAD_02_WORKING && !THREAD_03_WORKING && !THREAD_04_WORKING) // WE DO NOT WANT TO SPAWN A MILLION THREADS
        {
            // Create every single thread
            Thread t1 = new Thread(() ->
            {
                // Only run if the region is enabled
                if (enabledRegions[0])
                {
                    // Time the thread
                    long startTime = System.currentTimeMillis();
                    THREAD_01_WORKING = true;
                    // Generate the region
                    BufferedImage q1 = RenderFractalRegion(0, resolutionWidth / 2, 0, resolutionHeight / 2);      
                    // Render onto swapchain[0]
                    if (THREAD_01_RENDERER != null)
                    {
                        THREAD_01_RENDERER.drawImage(q1, 0, 0, null);   
                    }
                    // End time
                    THREAD_01_WORKING = false;    
                    long endTime = System.currentTimeMillis();  
                    THREAD_01_TIME = (endTime - startTime);
                }
            }, "t1");
            // T2-4 are generated the same way as t1 (Definitely should have used a loop here)
            Thread t2 = new Thread(() ->
            {
                if (enabledRegions[1])
                {
                    long startTime = System.currentTimeMillis();
                    THREAD_02_WORKING = true;
                    BufferedImage q2 = RenderFractalRegion(resolutionWidth / 2, resolutionWidth, 0, resolutionHeight / 2); 
                    if (THREAD_02_RENDERER != null)
                    {
                        THREAD_02_RENDERER.drawImage(q2, resolutionWidth / 2, 0, null);
                    }
                    THREAD_02_WORKING = false;   
                    long endTime = System.currentTimeMillis();  
                    THREAD_02_TIME = (endTime - startTime);   
                }
            }, "t2");
            Thread t3 = new Thread(() ->
            {
                if (enabledRegions[2])
                {
                    long startTime = System.currentTimeMillis();
                    THREAD_03_WORKING = true;
                    BufferedImage q3 = RenderFractalRegion(0, resolutionWidth / 2, resolutionHeight / 2, resolutionHeight);
                    if (THREAD_03_RENDERER != null)
                    {
                        THREAD_03_RENDERER.drawImage(q3, 0, resolutionHeight / 2, null);  
                    }
                    THREAD_03_WORKING = false;    
                    long endTime = System.currentTimeMillis();  
                    THREAD_03_TIME = (endTime - startTime);
                }
            }, "t3");
            Thread t4 = new Thread(() -> 
            {
                if (enabledRegions[3])
                {
                    long startTime = System.currentTimeMillis();
                    THREAD_04_WORKING = true;
                    BufferedImage q4 = RenderFractalRegion(resolutionWidth / 2, resolutionWidth, resolutionHeight / 2, resolutionHeight);
                    if (THREAD_04_RENDERER != null)
                    {
                        THREAD_04_RENDERER.drawImage(q4, resolutionWidth / 2, resolutionHeight / 2, null);   
                    }
                    THREAD_04_WORKING = false;   
                    long endTime = System.currentTimeMillis();  
                    THREAD_04_TIME = (endTime - startTime);
                }
            }, "t4");  
            // 1 2 3, RACE!
            t1.start();
            t2.start();
            t3.start();
            t4.start();
        }
    }
    /**
     * Java Matrix Rendering Thread Event. This is where the main rendering is done.
     * @param Graphics
     */
    public void Render(GraphicsUnit Graphics)
    {
        // Clear the rendering scene
        Graphics.FillRectangle(Color.WhiteSmoke, new Point(0, 0), wnd.Size);
        // Render the fractal
        if (fractalImage != null)
        {
            Graphics.graphics.drawImage(fractalImage, 0, 0, null);
        }
        // Render the layout
        Graphics.FillRectangle(Color.Snow, new Point(wnd.Size.Width - panelSize, 0), new Size(panelSize, wnd.Size.Height));
        int y = 10; // ima genius. I wish I thought of this solution before, because now i only have to change this value to change the location of every single one of these elements
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
        y = DrawHeader(Graphics, "Equation", y);
        y = DrawItem(Graphics, "X Multiplier", y, 10);
        y = DrawItem(Graphics, "Y Multiplier", y, 11);
        y = DrawItem(Graphics, "a", y, 6);
        y = DrawItem(Graphics, "b", y, 7);
        y = DrawHeader(Graphics, "Post Processing", y);
        y = DrawItem(Graphics, "Contrast", y, 12);
        y = DrawItem(Graphics, "AA", y);
        ppbtn[0].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4), y - 27);
        ppbtn[0].Render(Graphics);
        y = DrawItem(Graphics, "Dithering", y);
        ppbtn[1].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4), y - 27);
        ppbtn[1].Render(Graphics);
        y = DrawHeader(Graphics, "Rendering", y);
        y = DrawItem(Graphics, "Regions", y);
        regionsbtn[0].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4), y - 27);
        regionsbtn[0].Render(Graphics);
        regionsbtn[1].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4) + 25, y - 27);
        regionsbtn[1].Render(Graphics);
        regionsbtn[2].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4), y - 2);
        regionsbtn[2].Render(Graphics);
        regionsbtn[3].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4) + 25, y - 2);
        regionsbtn[3].Render(Graphics);
        y = DrawItem(Graphics, "", y);
        y = DrawItem(Graphics, "Output", y);
        savebtn[0].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4), y - 27);
        savebtn[0].Render(Graphics);
        savebtn[1].Location = new Point(wnd.Size.Width - (panelSize * 3 / 4) + 76, y - 27);
        savebtn[1].Render(Graphics);
        // Render debugging statistics
        Graphics.DrawString(content.toString(), Color.Black, content.subtract(Graphics.GetTextSize(content.toString(), new Font("Consolas", Font.PLAIN, 12))).toPoint().subtract(new Point(10, 5)), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T0~ME: " + wnd.FPS + "fps", Color.Black, new Point(0, 0), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T0~UI: " + THREAD_00_TIME + "ms", Color.Black, new Point(0, 15), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T1~TL: " + THREAD_01_TIME + "ms, " + THREAD_01_WORKING, Color.Black, new Point(0, 30), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T2~TR: " + THREAD_02_TIME + "ms, " + THREAD_02_WORKING, Color.Black, new Point(0, 45), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T3~BL: " + THREAD_03_TIME + "ms, " + THREAD_03_WORKING, Color.Black, new Point(0, 60), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T4~BR: " + THREAD_04_TIME + "ms, " + THREAD_04_WORKING, Color.Black, new Point(0, 75), new Font("Consolas", Font.PLAIN, 12));
        Graphics.DrawString("T5~PP: " + THREAD_05_TIME + "ms", Color.Black, new Point(0, 90), new Font("Consolas", Font.PLAIN, 12));
    }
    /**
     * Rendering Helper Methods From Here On!
     */
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
    public int DrawItem(GraphicsUnit Graphics, String text, int y)
    {
        Font fnt = new Font("Century Gothic", Font.PLAIN, 12);
        Graphics.DrawString(text, Color.Gray, new Point(wnd.Size.Width - (panelSize * 3 / 4) - Graphics.GetTextSize(text, fnt).Width - 10, y), fnt);
        return y + 25;
    }
}