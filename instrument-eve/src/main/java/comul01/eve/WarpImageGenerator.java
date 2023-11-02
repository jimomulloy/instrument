/** WarpImageGenerator Class
   * @version 1.0  18 apr 2000
   * @author Lawrence Rodrigues
   **/
package comul01.eve;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.geom.*;
import javax.media.jai.*;
import java.awt.image.renderable.*;

/** Generates a warped image using the "Warp" operator and related APIs
  * @version 1.0 18 Apr 2000


  * @author Lawrence Rodrigues


  **/

public class WarpImageGenerator implements MouseListener{
   protected float srcCoord[];
   protected float destCoord[];
   protected int polyDegree = 1;
   protected int numPointsNeeded = (polyDegree + 1)*(polyDegree + 2)/2;
   protected int currentNumPoints =0;
   protected RenderedImage sourceImage = null;
   protected PlanarImage destImage = null;
   protected int imageWidth, imageHeight;
   protected WarpPolynomial warp;

   public WarpImageGenerator(){
      setPolyDegree(polyDegree);
   }

   public WarpImageGenerator(RenderedImage sourceImage){
      this.sourceImage = sourceImage;
   }

   public void setSourceImage(RenderedImage sourceImage){
      if(sourceImage == null) return;
      this.sourceImage = sourceImage;
      imageWidth = sourceImage.getWidth();
      imageHeight = sourceImage.getHeight();
   }

   public RenderedImage getSourceImage(){
      return sourceImage;
   }

   public PlanarImage getDestImage(){ 
   
 		return destImage;
		}

   public void setPolyDegree(int degree){
      polyDegree = degree;
      numPointsNeeded = computeNumPoints(degree);
      srcCoord = new float[2*(numPointsNeeded+1)];
      destCoord = new float[2*(numPointsNeeded+1)];
   }

   public int getPolyDegree() { return polyDegree; }

   protected PlanarImage generateWarpImage() {
      if(sourceImage == null) return null;
      if (currentNumPoints >= numPointsNeeded) {
	  	
          WarpPolynomial warp = WarpPolynomial.createWarp(srcCoord, 0,
                                             destCoord, 0,
                                             2*currentNumPoints,
                                             1.0f/imageWidth,
                                             1.0f/imageHeight,
                                             (float)imageWidth,
                                             (float)imageHeight,
                                             polyDegree);
          return createDestImage(sourceImage, warp);
      }
      return null;
    }

    public int computeNumPoints(int degree) {
       if(degree <0) return -1;
       return (degree + 1)*(degree + 2)/2;
    }

    public static RenderedOp createDestImage(RenderedImage img,
                                      Warp warp){
       ParameterBlock pb = new ParameterBlock();
       pb.addSource(img);
       pb.add(warp);
       pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
       return JAI.create("warp", pb);
    }

	public void setPoints(int[] points) {
		currentNumPoints = 0;
		int x,y;
		System.out.println("warpgen set points needed - "+numPointsNeeded);
	  
		for (int i=0; i < points.length && currentNumPoints < numPointsNeeded; i++ ) {
	       x = points[i];
		   y = points[i+1];
	       srcCoord[2*currentNumPoints] = (float)x;
    	   srcCoord[2*currentNumPoints+1] = (float)y;
       	   destCoord[2*currentNumPoints] = (float)x + (float)Math.random()*10f;
           destCoord[2*currentNumPoints+1] = (float)y + (float)Math.random()*10f;
           currentNumPoints++;
		}
	}
	
	public void generateImage() {
	     if(currentNumPoints >=numPointsNeeded ) {
		 	System.out.println("warpgen OK ");
	          destImage = generateWarpImage();
			   	System.out.println("goit warp "+srcCoord.length+", "+destCoord.length+", "
			 				+srcCoord[0]+", "+srcCoord[1]+", "+destCoord[0]+
			 				", "+destCoord[1]+", "+currentNumPoints+", "+polyDegree);
	
	          currentNumPoints = 0;
	          srcCoord = new float[2*(numPointsNeeded+1)];
	          destCoord = new float[2*(numPointsNeeded+1)];
		}
  }

    public void addPoints(int x, int y) {
       srcCoord[2*currentNumPoints] = (float)x;
       srcCoord[2*currentNumPoints+1] = (float)y;
       destCoord[2*currentNumPoints] = (float)x + (float)Math.random()*10f;
       destCoord[2*currentNumPoints+1] = (float)y + (float)Math.random()*10f;
       if(currentNumPoints >=numPointsNeeded ) {
          destImage = generateWarpImage();
          currentNumPoints = 0;
          srcCoord = new float[2*(numPointsNeeded+1)];
          destCoord = new float[2*(numPointsNeeded+1)];
       }else currentNumPoints++;
    }

    public void mousePressed(MouseEvent e) {
       addPoints(e.getX(), e.getY());
    }
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
}