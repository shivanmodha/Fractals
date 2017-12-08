import studios.vanish.engine.Window;
import studios.vanish.engine.GraphicsUnit;
import studios.vanish.engine.Object3D;
import studios.vanish.engine.Point;
import studios.vanish.engine.Size;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import studios.vanish.engine.Color;
public class Concept
{
    Object3D cube = new Object3D();
    Window window = new Window("Concept", new Size(800, 600), true);
    BufferedImage image;
    BufferedImage previous;
    volatile boolean UPDATE_FLAG = true;
    double zoom = 0;
    public static void main(String[] args)
    {
        new Concept();
    }
    public Concept()
    {
        Initialize(60);
        while (true)
        {
            Update();
            window.Wait(2);
        }
    }
    public void Initialize(int FPS)
    {
        window.OnPaint.Add(this, "Render");
        window.Initialize(FPS);
        window.Show();
    }
    public void Update()
    {
        UPDATE_FLAG = true;
        previous = image;
        image = new BufferedImage(window.Size.Width, window.Size.Height, BufferedImage.TYPE_INT_RGB);
        zoom += 0.1;
        for (int x = 0; x < window.Size.Width; x++)
        {
            for (int y = 0; y < window.Size.Height; y++)
            {
                double zx = -1.5 * (x - window.Size.Width / 2) / (0.5 * zoom * window.Size.Width);
                double zy = (y - window.Size.Height / 2) / (0.5 * zoom * window.Size.Height);
                float i = 300;
                while (zx * zx + zy * zy < 100 && i > 0)
                {
                    double tmp = zx * zx - zy * zy - 0.162;
                    zy = 2.0 * zx * zy + 1.04;
                    zx = tmp;
                    i--;
                }
                int c = java.awt.Color.HSBtoRGB((300 / i) % 1, 1, i > 0 ? 1 : 0);
                image.setRGB(x, y, c);
            }
        }
        UPDATE_FLAG = false;
    }
    public void Render(GraphicsUnit Graphics)
    {
        Graphics.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graphics.FillRectangle(Color.White, new Point(0, 0), window.Size);    
        if (!UPDATE_FLAG)
        {
            Graphics.graphics.drawImage(image, 0, 0, null);
        }
        else
        {
            Graphics.graphics.drawImage(previous, 0, 0, null);
        }
        Graphics.DrawString("" + window.FPS, Color.Black, new Point(0, 0), new Font("Consolas", Font.PLAIN, 12));
    }
}