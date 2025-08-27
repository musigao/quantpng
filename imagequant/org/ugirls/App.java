package org.ugirls;

import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJCompressor;
import org.libjpegturbo.turbojpeg.TJException;
import org.pngquant.PngQuant;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        if(args.length<2){
            return;
        }

        String input = args[0];
        String output = args[1];

        System.out.println("input file:"+input);
        System.out.println("output file:"+output);




        try {
            BufferedImage bufferedImage = ImageIO.read(new File(input));
            PngQuant pngQuant = new PngQuant();
            pngQuant.setMaxColors(256);
            pngQuant.setQuality(100);
            pngQuant.setSpeed(1);
            pngQuant.setMinPosterization(0);
            bufferedImage = pngQuant.getRemapped(bufferedImage);


            File outputfile = new File(output);
            ImageIO.write(bufferedImage, "png", outputfile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
