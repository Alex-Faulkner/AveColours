import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.util.*;

public class Main {
	private static int numberOfColors = 3;
	private static double w = 1.9;
	private static double threshold = 0.5;
	private static int width;
	private static int height;
	private static BufferedImage in = null;
	private static double totalError = 0;
	private static int AA = 2;
	private static boolean dither = false;
	private static boolean chooseColour = false;
	private static int customColours = 0;
	private static Color customColour1 = new Color(180, 80, 108);
	private static boolean frame = true;
	private static int frameWidth = 718;
	private static int frameHeight = 404;
	private static String name = "sunset";
	//private static boolean d2 = true;
	
	public static void main(String[] args) {
		try {
			in = ImageIO.read(new File(name+".jpg"));
		} catch (IOException e) {
			System.out.println("FUCK");
		}

		width = in.getWidth();
		height = in.getHeight();
		
		
		//Method to find the starting cluster points using k-means++
		Color[] startColours = kMeansPlus();
		//Method to find the cluster points using k-means
		Color[] meanColours = kMeansColours(startColours);
		//Method to create output image by setting each pixel from in to one of the cluster points
		BufferedImage out = assignPixels(meanColours);
		//Method to smooth image with AA
		//out = AA(out);
		
		
		try {
			File outputfile = null;
			if(dither){
				if(frame){
					outputfile = new File(name+"DF"+numberOfColors+", "+(totalError/(width * height))+".png");
				}else{
					outputfile = new File(name+"D"+numberOfColors+", "+(totalError/(width * height))+".png");
				}
			}else{
				outputfile = new File(name+numberOfColors+", "+(totalError/(width * height))+".png");
			}
			ImageIO.write(out, "png", outputfile);
		} catch (IOException e) {
		}
		System.out.print(""+(int)totalError+", "+(totalError/(width * height)));
	}


	private static BufferedImage AA(BufferedImage out) {
		BufferedImage outAA = new BufferedImage(width/AA, height/AA, BufferedImage.TYPE_INT_ARGB);
		for(int j = 0; j < height; j += AA){
			for(int i = 0; i < width; i += AA){
				int redAve = 0;
				int greenAve = 0;
				int blueAve = 0;
				for(int x = 0; x < AA; x++){
					for(int y = 0; y < AA; y++){
						Color pix = new Color(out.getRGB(i+x, j+y));
						redAve += pix.getRed();
						greenAve += pix.getGreen();
						blueAve += pix.getBlue();
					}
				}
				redAve = redAve/(AA * AA);
				greenAve = greenAve/(AA * AA);
				blueAve = blueAve/(AA * AA);
				Color ave = new Color(redAve, greenAve, blueAve);
				outAA.setRGB(i/AA, j/AA, ave.getRGB());
			}
		}
		return outAA;
	}


	private static BufferedImage assignPixels(Color[] meanColours) {
		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		double[][] randFrame = new double[frameWidth][frameHeight];
		if(frame){
			for(int i = 0; i < frameWidth; i++){
				for(int j = 0; j < frameHeight; j++){
					randFrame[i][j] = Math.random();
				}
			}
		}
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				Color pixel = new Color(in.getRGB(i, j));
				double[] dists = new double[numberOfColors];
				double minDist = getDist(pixel, meanColours[0]);
				dists[0] = 1/(minDist*minDist);
				int nearestColour = 0;
				double total = 1/(minDist*minDist);
				for(int k = 1; k < numberOfColors; k++){
					
					double dist = getDist(pixel, meanColours[k]);
					if(!dither){
						dists[k] = dist;
						
						if(dist < minDist){
							minDist = dist;
							nearestColour = k;
						}
					}else{
						dist += 0.01;
						total += 1/(dist*dist);
						dists[k] = 1/(dist*dist);
					}
				}
				if(dither){
					double rand;
					if(frame){//if using the rand frame
						rand = randFrame[i%frameWidth][j%frameHeight] * total;
					}else{//otherwise just random
						rand = Math.random() * total;
					}
					for(int k = 0; k < numberOfColors && rand > 0; k++){
						rand -= dists[k];
						if(rand < 0){
							nearestColour = k;
							minDist = Math.sqrt(1/dists[k]);
							break;
						}
					}
				}
				totalError += minDist;
				out.setRGB(i, j, meanColours[nearestColour].getRGB());
			}
		}
		return out;
	}


	private static Color[] kMeansColours(Color[] colours) {
		//rgbn stores the total red, green and blue values of all points near to each colour as well as how many there are
		long[][] rgbn = new long[4][numberOfColors];

		boolean done = false;
		int kmeansGen = 0;
		while(!done){
			kmeansGen++;

			rgbn = multiThread(colours);
			double totalDist = 0;
			int k = 0;
			if(dither){
				k = 2;
			}
			
			for(; k < numberOfColors; k++){
				int red = colours[k].getRed();
				int green = colours[k].getGreen();
				int blue = colours[k].getBlue();


				int redN = red;
				int greenN = green;
				int blueN = blue;
				if(rgbn[3][k] != 0){
					redN = (int) ((rgbn[0][k])/rgbn[3][k]);
					greenN = (int) ((rgbn[1][k])/rgbn[3][k]);
					blueN = (int) ((rgbn[2][k])/rgbn[3][k]);				
				}
				double redD = redN - red;
				double greenD = greenN - green;
				double blueD = blueN - blue;

				int newRed = (int)(red+(redD*w));
				int newgreen = (int)(green+(greenD*w));
				int newblue = (int)(blue+(blueD*w));

				newRed = Math.min(newRed, 255);
				newgreen = Math.min(newgreen, 255);
				newblue = Math.min(newblue, 255);

				newRed = Math.max(newRed, 0);
				newgreen = Math.max(newgreen, 0);
				newblue = Math.max(newblue, 0);

				Color newColor = new Color(newRed, newgreen, newblue);

				double dist = getDist(colours[k], newColor);
				colours[k] = newColor;
				//System.out.println(""+colours[k]);
				totalDist += dist;
			}
			if((totalDist/numberOfColors) <= threshold){
				done = true;
			}
			System.out.println("k"+kmeansGen+", "+totalDist/numberOfColors);
			
		}
		return colours;
	}


	private static long[][] multiThread(Color[] colours){
		long[][] rgbn = new long[4][numberOfColors];
		kMeansThread[] threads = new kMeansThread[width];
		for(int i = 0; i < width; i++){
			threads[i] = new kMeansThread(colours, in.getRGB(i, 0, 1, height, null, 0, 1));
			threads[i].start(""+i);
			//System.out.println("");
		}
		for(int k = 0; k < width; k++){
			while(!threads[k].getDone()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long[][] rgbnK = threads[k].getRGBN();
			for(int i = 0; i < 4; i++){
				for(int j = 0; j < numberOfColors; j++){
					rgbn[i][j] += rgbnK[i][j];
				}
			}
		}
		return rgbn;
	}


	//Gives the distance between two colours
	public static double getDist(Color color1, Color color2) {
		int red = color1.getRed();
		int green = color1.getGreen();
		int blue = color1.getBlue();

		int red2 = color2.getRed();
		int green2 = color2.getGreen();
		int blue2 = color2.getBlue();

		double redD = Math.abs(red - red2);
		double greenD = Math.abs(green - green2);
		double blueD = Math.abs(blue - blue2);

		//dist:
		double dist = Math.hypot(blueD, Math.hypot(redD, greenD));
		//dist squared:
		//double dist = (blueD * blueD) + ((Math.hypot(redD, greenD)) * (Math.hypot(redD, greenD)));

		return dist;
	}
	
	
	private static Color[] kMeansPlus() {
		Color[] colours = new Color[numberOfColors];
		double[][] dists2 = new double[height][width];
		for(int i = 0; i < height; i++){
			Arrays.fill(dists2[i], 1000000.0);
		}
		
		if(dither){//If dithering, starting colours are black and white
			colours[0] = Color.BLACK;
			System.out.println(colours[0]);
			colours[1] = Color.WHITE;
			System.out.println(colours[1]);
		}else{//otherwise pick random pixel
			colours[0] = new Color(in.getRGB((int)(Math.random() * width), (int)(Math.random() * height)));
		}
		
		double totalDist2 = 0;
		int k = 1;//k = 1 (as one colour is already picked)
		if(dither){//unless dithering then two (black and white), then calc dists for black
			k = 2;
			
			kMeansPlusThread[] threads = new kMeansPlusThread[height];
			
			for(int i = 0; i < height; i++){
				threads[i] = new kMeansPlusThread(colours[0], in.getRGB(0, i, width, 1, null, 0, width), dists2[i]);
				threads[i].start(""+i);
			}
			
			for(int i = 0; i < height; i++){
				while(!threads[i].getDone()){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				dists2[i] = threads[i].getDists();
			}
		}
		for(; k < numberOfColors; k++){
			totalDist2 = 0;
			
			kMeansPlusThread[] threads = new kMeansPlusThread[height];
			
			//first loop through image to get distance^2 of each pixel to nearest colour
			for(int i = 0; i < height; i++){
				threads[i] = new kMeansPlusThread(colours[k-1], in.getRGB(0, i, width, 1, null, 0, width), dists2[i]);
				threads[i].start(""+i);
			}
			
			for(int i = 0; i < height; i++){
				while(!threads[i].getDone()){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				dists2[i] = threads[i].getDists();
				totalDist2 += threads[i].getTotalDist();
			}

			
			//Second loop through to pick the next pixel to be a colour
			double rand = Math.random() * totalDist2;
			for(int i = 0; i < height && rand > 0; i++){
				for(int j = 0; j < width && rand > 0; j++){
					rand -= dists2[i][j];
					if(rand < 0){
						colours[k] = new Color(in.getRGB(j, i));
					}
				}
			}
			System.out.println(colours[k]);
		}
		return colours;
	}
}
