package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

public class App {

	/**
	 * Static method for reading all contents of an input file into a string
	 * by means of its file path and file encoding format
	 * 
	 * @param path : the file path
	 * @param encoding : the file encoding format 
	 * @return a file object
	 */
	public static String readFile(String path, Charset encoding) throws IOException {
		return Files.readString(Paths.get(path), encoding);
	}

	/**
	 * Static method for converting the different ranges of air quality reading values
	 * into hexadecimal RGB strings
	 * 
	 * @param value : the air quality reading value
	 * @return RGB value as a hex string
	 */
	public static String rangeToRGBString(Integer value) {
		if (value >= 0 && value < 32) {
			return "#00ff00";
		} else if (value >= 32 && value < 64) {
			return "#40ff00";
		} else if (value >= 64 && value < 96) {
			return "#80ff00";
		} else if (value >= 96 && value < 128) {
			return "#c0ff00";
		} else if (value >= 128 && value < 160) {
			return "#ffc000";
		} else if (value >= 160 && value < 192) {
			return "#ff8000";
		} else if (value >= 192 && value < 224) {
			return "#ff4000";
		} else if (value >= 224 && value < 256) {
			return "#ff0000";
		} else {
			return "#aaaaaa";
		}
	}

	public static void main(String[] args) {
		String fileName = null;

		if (args.length > 0) {
			fileName = args[0];
		}

		if (fileName == null)
			return;

		String basePath = new File("").getAbsolutePath().concat("/");

		/* 
		 * Obtain the file path of the input file by appending the filename
		 * to the path of the root directory
		*/  
		String filePath = basePath.concat(fileName);

		String textContent = null;

		try {
			textContent = readFile(filePath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("An error occurred during the file reading.");
		}


		// Split the strings over the commas
		String[] readings = textContent.split(",");

		
		ArrayList<Integer> values = new ArrayList<>();

		/*
		 * Convert the array of strings into an array of integers
		 */
		for (int i = 0; i < readings.length; i++) {
			values.add(Integer.parseInt(readings[i].strip()));
		}
		
		final int gridSize = 10;

		// Initialise constants 

		final BigDecimal leftLongitude = BigDecimal.valueOf(-3.192473);
		final BigDecimal rightLongitude = BigDecimal.valueOf(-3.184319);
		final BigDecimal topLatitude = BigDecimal.valueOf(55.946233);
		final BigDecimal bottomLatitude = BigDecimal.valueOf(55.942617);
		
		/*
		 * width represents the fixed distance between the leftmost and rightmost
		 * longitudes that bound the grid
		 */
		final BigDecimal width = rightLongitude.subtract(leftLongitude);
		/*
		 * height represents the fixed distance between the topmost and bottom-most
		 * latitudes that bound the grid
		 */
		final BigDecimal height = topLatitude.subtract(bottomLatitude);
		
		/*
		 *  widthStepSize represents the constant width (difference between the longitudes) of
		 *  each polygon in the grid
		 */
		final BigDecimal widthStepSize = width.divide(BigDecimal.valueOf(gridSize));
		/*
		 *  heightStepSize represents the constant height (difference between the latitudes) of
		 *  each polygon in the grid
		 */
		final BigDecimal heightStepSize = height.divide(BigDecimal.valueOf(gridSize));

		ArrayList<BigDecimal> horizontalPoints = new ArrayList<>();
		ArrayList<BigDecimal> verticalPoints = new ArrayList<>();

		// Compute nine evenly spaced points along the width (horizontal direction)
		for (int i = 1; i < gridSize; i++) {
			BigDecimal point = leftLongitude.add(BigDecimal.valueOf(i).multiply(widthStepSize));
			horizontalPoints.add(point);
		}

		// Compute nine evenly spaced points along the height (vertical direction)
		for (int i = 1; i < gridSize; i++) {
			BigDecimal point = topLatitude.subtract(BigDecimal.valueOf(i).multiply(heightStepSize));
			verticalPoints.add(point);
		}

		// Create an array representing the 10x10 grid
		ArrayList<Polygon> grid = new ArrayList<>();

		// Iterate through each row in the grid
		for (int i = 0; i < gridSize; i++) {
			// Compute top and bottom latitudes of a given polygon
			BigDecimal top = topLatitude.subtract(BigDecimal.valueOf(i).multiply(heightStepSize));
			BigDecimal bottom = topLatitude.subtract(BigDecimal.valueOf(i + 1).multiply(heightStepSize));

			// Iterate through each column in the grid
			for (int j = 0; j < gridSize; j++) {
				// ...and do likewise for the left and right longitudes
				BigDecimal left = leftLongitude.add(BigDecimal.valueOf(j).multiply(widthStepSize));
				BigDecimal right = leftLongitude.add(BigDecimal.valueOf(j + 1).multiply(widthStepSize));

				/*
				 * Create an array of Point objects from the afore-named variables. The Point object is
				 * used later on to instantiate a Polygon object.
				 */
				ArrayList<Point> points = new ArrayList<>();
				points.add(Point.fromLngLat(left.doubleValue(), top.doubleValue()));
				points.add(Point.fromLngLat(right.doubleValue(), top.doubleValue()));
				points.add(Point.fromLngLat(right.doubleValue(), bottom.doubleValue()));
				points.add(Point.fromLngLat(left.doubleValue(), bottom.doubleValue()));
				points.add(Point.fromLngLat(left.doubleValue(), top.doubleValue()));

				ArrayList<List<Point>> coordinates = new ArrayList<>();
				coordinates.add(points);

				Polygon polygon = Polygon.fromLngLats(coordinates);
				grid.add(polygon);

			}
		}

		/*
		 * Initialise an array of features.
		 * This array is used to store the feature properties of each polygon we've created.
		 * The properties are processed from contents of the input file given to this application.
		 */
		ArrayList<Feature> features = new ArrayList<>();

		for (int i = 0; i < grid.size(); i++) {
			Feature feature = Feature.fromGeometry(grid.get(i));
			feature.addStringProperty("rgb-string", rangeToRGBString(values.get(i)));
			feature.addNumberProperty("fill-opacity", 0.75);
			feature.addStringProperty("fill", rangeToRGBString(values.get(i)));
			feature.addNumberProperty("stroke-width", 2);
			feature.addNumberProperty("stroke-opacity", 1);
			features.add(feature);
		}

		FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);

		
		/*
		 * Create a JSON file from the FeatureCollection object and write the output
		 * geojson file to the default output directory, while catching any IO exceptions that
		 * may arise from the operation.
		 */
		ArrayList<String> lines = new ArrayList<>();
		lines.add(featureCollection.toJson());
		Path outputFilePath = Paths.get(new File("").getAbsolutePath().concat("/heatmap.geojson"));
		
		try {
		Files.write(outputFilePath, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("The given file cannot be created");
		}
	}
}
