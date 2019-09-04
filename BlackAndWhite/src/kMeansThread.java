import java.awt.Color;

public class kMeansThread implements Runnable {
	private Thread t;
	private String threadName;
	private int numberOfColors;
	private int height;
	private int[] pixels;
	private Color[] colours;
	private long[][] rgbn;
	private boolean done = false;
	
	kMeansThread(Color[] coloursIn, int[] pixelsIn){
		pixels = pixelsIn;
		height = pixels.length;
		colours = coloursIn;
		numberOfColors = colours.length;
		
		rgbn = new long[4][numberOfColors];
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
		for(int j = 0; j < height; j++){
			Color pixel = new Color(pixels[j]);

			double minDist = Main.getDist(colours[0], pixel);
			int nearestColour = 0;

			for(int k = 1; k < numberOfColors; k++){
				double dist = Main.getDist(colours[k], pixel);
				//System.out.print(""+k+":"+dist+", ");
				if(dist < minDist){
					minDist = dist;
					nearestColour = k;
				}
			}
			//System.out.print(""+nearestColour+"|");
			int red = pixel.getRed();
			int green = pixel.getGreen();
			int blue = pixel.getBlue();

			rgbn[0][nearestColour] += red;
			rgbn[1][nearestColour] += green;
			rgbn[2][nearestColour] += blue;
			rgbn[3][nearestColour]++;

		}
		done = true;
	}
	
	public long[][] getRGBN(){
		return this.rgbn;
	}

	public boolean getDone() {
		return this.done;
	}
	
}
