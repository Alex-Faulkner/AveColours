import java.awt.Color;

public class kMeansPlusThread implements Runnable {

	private Thread t;
	private String threadName;
	private int width;
	private int[] pixels;
	private Color colour;
	private double[] dists2;
	private boolean done = false;
	private double totalDist2 = 0;



	kMeansPlusThread(Color colourIn, int[] pixelsIn, double[] dists2In){
		pixels = pixelsIn;
		width = pixels.length;
		colour = colourIn;
		dists2 = dists2In;
	}

	public void start (String name) {
		threadName = name;
		//System.out.println("Starting " +  threadName );
		if (t == null) {
			t = new Thread (this, threadName);
			t.start ();
		}
	}

	@Override
	public void run() {

		for(int j = 0; j < width; j++){
			Color pixel = new Color(pixels[j]);
			double minDist = Math.sqrt(dists2[j]);
			
			double dist = Main.getDist(pixel, colour);
			if(dist < minDist){
				minDist = dist;
			}
			dists2[j] = minDist * minDist;
			totalDist2 += minDist * minDist;
		}
		done = true;
	}

	public double[] getDists(){
		return this.dists2;
	}

	public boolean getDone() {
		return this.done;
	}

	public double getTotalDist(){
		return this.totalDist2;
	}
}


