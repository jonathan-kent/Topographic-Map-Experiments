import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TerrainViewer extends Frame implements MouseMotionListener {
	
	static JLabel label1;
	static JLabel map;
	
	static File input = new File("grand_canyon_topography.png");
	static String outputFileName = "generated_output_terrain_view";
	
	static int xPosition = 25;
	static int yPosition = 25;
	static int scaleRange = 1965-605;
	static float distancePerPixel = (float) 3000 / (float) 146;
	
	static float[][][] pixels = createPixels(input);
	static float[][] heightMap = pixelsToHeight(pixels, scaleRange);
	static float[][] noScaleHeightMap = noScaleHeightMap(pixels);

	
	TerrainViewer(){
		
	}
	
	//================================================================================================================
	//
	//                               helper methods for generating the view image
	//
	//================================================================================================================
	
	public static float[][][] createPixels(File file){
		BufferedImage image;
		try {
			image = ImageIO.read(file);
			//stores pixel color into an array
			float[][][] pixels = new float[image.getWidth()][image.getHeight()][];
			for(int x=0; x< image.getWidth(); x++) {
				for(int y=0; y< image.getHeight(); y++) {
					int color = image.getRGB(x,y);
					int  red   = (color & 0x00ff0000) >> 16;
				  	int  green = (color & 0x0000ff00) >> 8;
				  	int  blue  =  color & 0x000000ff;
				  	
				  	float[] hsb = Color.RGBtoHSB(red,green,blue,null);
				  	pixels[x][y] = hsb;
				}
			}
			return pixels;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static float[][] pixelsToHeight(float[][][] pixels, int scaleRange){
		float[][] heightMap = new float[pixels.length][pixels[0].length];
		for(int x=0; x< pixels.length; x++) {
			for(int y=0; y< pixels[x].length; y++) {
				if(pixels[x][y][0]>(float)(0.8)) {
					pixels[x][y][0] = (float)1.0 - pixels[x][y][0];
				}
				float height = Math.abs(((float)0.75 * (1-(pixels[x][y][0]/(float)(2.0/3.0)))) + ((float)0.25 * (1-(pixels[x][y][1]/(float)0.55))));
				heightMap[x][y] = (height*scaleRange);
			}
		}
		return heightMap;
	}
	
	public static float distance(int viewerX, int viewerY, int x, int y, float distancePerPixel) {
		float a = Math.abs(viewerX-x);
		float b = Math.abs(viewerY-y);
		float distance = (float) Math.sqrt( (Math.pow(a,2)) + (Math.pow(b, 2)) );
		return distance * distancePerPixel;
	}
	
	public static float[][] relativeHeight(float[][] heightMap, int viewerX, int viewerY, float distancePerPixel){
		float[][] relativeHeightMap = new float[heightMap.length][heightMap[0].length];
		for(int x=0; x< heightMap.length; x++) {
			for(int y=0; y< heightMap[x].length; y++) {
				float relativeHeight = ((heightMap[x][y])-heightMap[viewerX][viewerY]) * ((float) 1.0/ distance(viewerX,viewerY,x,y, distancePerPixel));
				relativeHeightMap[x][y] = relativeHeight;
			}
		}
		return relativeHeightMap;
	}
	
	public static float[] maxHeights(float[][] relativeHeightMap, int yPosition) {
		float[] maxHeights = new float[relativeHeightMap.length];
		int initY = 0;
		for(int x=0; x< relativeHeightMap.length; x++) {
			float highest = relativeHeightMap[x][initY];
			for(int y=0; y<yPosition; y++) {
				if(relativeHeightMap[x][y] >= highest) {
					highest = relativeHeightMap[x][y];
				}
				maxHeights[x] = highest;
			}
		}
		return maxHeights;
	}
	
	public static float findLowest(float[] maxHeights) {
		float lowest = maxHeights[0];
		for(int i=0; i<maxHeights.length; i++) {
			if(maxHeights[i]<lowest) {
				lowest = maxHeights[i];
			}
		}
		return lowest;
	}
	
	public static float findHighest(float[] maxHeights) {
		float highest = maxHeights[0];
		for(int i=0; i<maxHeights.length; i++) {
			if(maxHeights[i]>highest) {
				highest = maxHeights[i];
			}
		}
		return highest;
	}
	
	public static BufferedImage drawOutput(float[] maxHeights, int outputWidth, int outputHeight, float highest, float lowest) {
		BufferedImage outputImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
		int rgb = new Color(34,117,43).getRGB();
		for(int x=0; x<maxHeights.length; x++) {
			if(highest > 0) {
				for(int y= (int) (outputHeight - (( (maxHeights[x]/(highest*1.1))*(outputHeight/2) ) + (outputHeight/2))); y<outputHeight; y++) {
					outputImage.setRGB(x, y, rgb);
				}
			} else {
				for(int y= (int) ((maxHeights[x]/(lowest*1.1))*(outputHeight/2)) + (outputHeight/2); y<outputHeight; y++) {
					outputImage.setRGB(x, y, rgb);
				}
			}
		}
		return outputImage;
	}
	
	//===============================================================================================================
	//
	//                                           generating an view image
	//
	//===============================================================================================================
	
	public static void topographyToView(float[][] heightMap, int xPosition, int yPosition, float distancePerPixel, int outputWidth, int outputHeight, String fileName, int generationNumber) {
		float[][] relativeHeightMap = relativeHeight(heightMap, xPosition, yPosition, distancePerPixel);
		float[] maxHeights = maxHeights(relativeHeightMap, yPosition);
		float highest = findHighest(maxHeights);
		float lowest = findLowest(maxHeights);
		
		BufferedImage outputImage = drawOutput(maxHeights, outputWidth, outputHeight, highest, lowest);
		File outputFile = new File(fileName + Integer.toString(generationNumber) + ".png");
		try {
			ImageIO.write(outputImage, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//===============================================================================================================
	//
	//                                        generating a new topographic map
	//
	//===============================================================================================================
	
	public static BufferedImage newTopographyMap(float[][] heightMap) {
		BufferedImage outputImage = new BufferedImage(heightMap.length, heightMap[0].length, BufferedImage.TYPE_INT_RGB);
		for(int x=0; x<heightMap.length; x++) {
			for(int y=0; y<heightMap[0].length; y++) {
				outputImage.setRGB(x, y, new Color((int)(200*(heightMap[x][y])),(int)(255*(heightMap[x][y])),(int)(150*(heightMap[x][y]))).getRGB());
			}
		}
		return outputImage;
	}
	
	public static float[][] noScaleHeightMap(float[][][] pixels){
		float[][] heightMap = new float[pixels.length][pixels[0].length];
		for(int x=0; x< pixels.length; x++) {
			for(int y=0; y< pixels[x].length; y++) {
				if(pixels[x][y][0]>(float)(0.8)) {
					pixels[x][y][0] = (float)1.0 - pixels[x][y][0];
				}
				float height = Math.abs(((float)0.75 * (1-(pixels[x][y][0]/(float)(2.0/3.0)))) + ((float)0.25 * (1-(pixels[x][y][1]/(float)0.55))));
				heightMap[x][y] = height;
			}
		}
		return heightMap;
	}
	
	//================================================================================================================
	//
	//                                        generating a relative topographic map
	//
	//================================================================================================================
	
	public static BufferedImage relativeTopographyMap(float[][] heightMap, int viewerX, int viewerY, float distancePerPixel) {
		float[][] relativeHeightMap = relativeHeight(heightMap, viewerX, viewerY, distancePerPixel);
		BufferedImage outputImage = new BufferedImage(relativeHeightMap.length, relativeHeightMap[0].length, BufferedImage.TYPE_INT_RGB);
		float range = findMaxAbsValue(relativeHeightMap);
		for(int x=0; x<relativeHeightMap.length; x++) {
			for(int y=0; y<relativeHeightMap[0].length; y++) {
				if(relativeHeightMap[x][y]>=0) {
					outputImage.setRGB(x, y, new Color((int)(255*((relativeHeightMap[x][y])/range)),50,0).getRGB());
				} else {
					outputImage.setRGB(x, y, new Color(0,50,(int)(255*((Math.abs(relativeHeightMap[x][y]))/range))).getRGB());
				}
			}
		}
		return outputImage;
	}
	
	public static float findMaxAbsValue(float[][] relativeHeightMap) {
		float highest = relativeHeightMap[0][0];
		for(int i=0; i<relativeHeightMap.length; i++) {
			for(int j=0; j<relativeHeightMap[0].length; j++) {
				if(Math.abs(relativeHeightMap[i][j])>highest) {
					highest = Math.abs(relativeHeightMap[i][j]);
				}
			}
		}
		return highest;
	}
	
	//================================================================================================================
	//
	//                                     faster relative topographic map
	//
	//================================================================================================================
	
	public static void fastRelativeTopographicMap(File input, String outputName, int xPosition, int yPosition, int scaleRange, float distancePerPixel) throws IOException {
		BufferedImage image;
		image = ImageIO.read(input);
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		
		float[][] heightMap = new float[imageWidth][imageHeight];
		for(int x=0; x< imageWidth; x++) {
			for(int y=0; y< imageHeight; y++) {
				int color = image.getRGB(x,y);
				int  red   = (color & 0x00ff0000) >> 16;
			  	int  green = (color & 0x0000ff00) >> 8;
			  	int  blue  =  color & 0x000000ff;
			  	
			  	float[] hsb = Color.RGBtoHSB(red,green,blue,null);

			  	if(hsb[0]>(float)(0.8)) {
					hsb[0] = (float)1.0 - hsb[0];
				}
				float height = Math.abs(((float)0.75 * (1-(hsb[0]/(float)(2.0/3.0)))) + ((float)0.25 * (1-(hsb[1]/(float)0.55))));
				heightMap[x][y] = (height*scaleRange);
			}
		}
		BufferedImage outputImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		float[][] relativeHeightMap = new float[imageWidth][imageHeight];
		for(int x=0; x<imageWidth; x++) {
			for(int y=0; y<imageHeight; y++) {
				relativeHeightMap[x][y] = ((heightMap[x][y])-heightMap[xPosition][yPosition]) * ((float) 1.0/ distance(xPosition,yPosition,x,y, distancePerPixel));
			}
		}
		float range = findMaxAbsValue(relativeHeightMap);
		for(int x=0; x<relativeHeightMap.length; x++) {
			for(int y=0; y<relativeHeightMap[0].length; y++) {
				if(relativeHeightMap[x][y]>=0) {
					outputImage.setRGB(x, y, new Color((int)(255*((relativeHeightMap[x][y])/range)),50,0).getRGB());
				} else {
					outputImage.setRGB(x, y, new Color(0,50,(int)(255*((Math.abs(relativeHeightMap[x][y]))/range))).getRGB());
				}
			}
		}
		File outputFastRelativeMapFile = new File(outputName + ".png");
		try {
			ImageIO.write(outputImage, "png", outputFastRelativeMapFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	//================================================================================================================
	//
	//                                    Mouse Listener Implementation
	//
	//================================================================================================================

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		//label1.setText("mouse moved to " + e.getX() + ", " + e.getY());
		ImageIcon image = new ImageIcon(relativeTopographyMap(heightMap, e.getX(), e.getY(), distancePerPixel));
		//ImageIcon image = new ImageIcon(pathToLowest(heightMap, e.getX(), e.getY()));
		map.setIcon(image);
	}
	
	//================================================================================================================
	//
	//                                        path to lowest point
	//
	//================================================================================================================
	
	public static BufferedImage pathToLowest(float[][] heightMap, int startX, int startY) {
		BufferedImage output = new BufferedImage(heightMap.length, heightMap[0].length, BufferedImage.TYPE_INT_RGB);
		int x = startX;
		int y = startY;
		float lowestPoint = heightMap[startX][startY];
		boolean foundLowest = false;
		int tempX = x;
		int tempY = y;
		
		while(foundLowest == false) {
			output.setRGB(x, y, new Color(0,0,255).getRGB());
			if(x>=1 && x<heightMap.length -2 && y>=1 && y<heightMap[0].length -2) {
				for(int xCount=x-1; xCount<=x+1; xCount++) {
					for(int yCount=y-1; yCount<=y+1; yCount++) {
						if(heightMap[xCount][yCount]<heightMap[x][y]) {
							lowestPoint = heightMap[xCount][yCount];
							tempX = xCount;
							tempY = yCount;
						}
					}
				}
				if(tempX==x && tempY == y) {
					foundLowest = true;
				}
				x = tempX;
				y = tempY;
			}
		}
		return output;
	}
	
	//================================================================================================================
	//
	//                                             main method
	//
	//================================================================================================================
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		//screenshot of carto/light mode
		//Long startTime = System.currentTimeMillis();
		
		int outputWidth = pixels.length;
		int outputHeight = pixels[0].length;
		
	    JFrame f = new JFrame("Auto-Terrain"); 
        f.setSize(outputWidth, outputHeight); 
        //f.setResizable(false);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        JPanel p = new JPanel(); 
        p.setLayout(new FlowLayout());
        label1 = new JLabel("no event");
        ImageIcon image = new ImageIcon(relativeTopographyMap(heightMap, xPosition, yPosition, distancePerPixel));
        //ImageIcon image = new ImageIcon(pathToLowest(heightMap, xPosition, yPosition));
		map = new JLabel(image);
        TerrainViewer mouse = new TerrainViewer();
        f.addMouseMotionListener(mouse); 
        //p.add(label1);
        p.add(map);
        f.add(p); 
        f.show(); 
		
		/*int screenWidth =  1920;
		int screenHeight = 1080;
		String FOVFileName = "FOV_output";
		Long timeTaken = System.currentTimeMillis() - startTime;
		
		System.out.println("Setup complete: " + timeTaken + " milliseconds");
		startTime = System.currentTimeMillis();
		
		topographyToView(heightMap, xPosition, yPosition, distancePerPixel, outputWidth, outputHeight, outputFileName, 0);
		
		timeTaken = System.currentTimeMillis() - startTime;
		System.out.println("Regular view complete: " + timeTaken + " milliseconds");
		startTime = System.currentTimeMillis();
		
		BufferedImage outputMap = newTopographyMap(noScaleHeightMap);
		File outputMapFile = new File("generated_topography_map.png");
		try {
			ImageIO.write(outputMap, "png", outputMapFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		timeTaken = System.currentTimeMillis() - startTime;
		System.out.println("New topography map complete: " + timeTaken + " milliseconds");
		startTime = System.currentTimeMillis();
		
		BufferedImage outputRelativeMap = relativeTopographyMap(heightMap, xPosition, yPosition, distancePerPixel);
		File outputRelativeMapFile = new File("generated_relative_topography_map.png");
		try {
			ImageIO.write(outputRelativeMap, "png", outputRelativeMapFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		timeTaken = System.currentTimeMillis() - startTime;
		System.out.println("Relative topography map complete: " + timeTaken + " milliseconds");
		startTime = System.currentTimeMillis();
		
		try {
			fastRelativeTopographicMap(input, "fast_relative_map", xPosition, yPosition, scaleRange, distancePerPixel);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		timeTaken = System.currentTimeMillis() - startTime;
		System.out.println("Faster topography map complete: " + timeTaken + " milliseconds");
		
		System.out.println("Done");
		*/
		
	}
	
}
