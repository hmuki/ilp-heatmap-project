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

	public static String readFile(String path, Charset encoding) throws IOException {
		return Files.readString(Paths.get(path));
	}

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

		String filePath = basePath.concat(fileName);
		System.out.println(filePath);

		String textContent = null;

		try {
			textContent = readFile(filePath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("An error occurred during the file reading.");
		}

		System.out.println(textContent);

		// Split the strings over the commas
		String[] readings = textContent.split(",");

		// Convert to an array of integers
		ArrayList<Integer> values = new ArrayList<>();

		for (int i = 0; i < readings.length; i++) {
			values.add(Integer.parseInt(readings[i].strip()));
		}

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				System.out.print(values.get(10 * i + j) + " ");
			}
			System.out.println();
		}

		System.out.println();
		int gridSize = 10;

		// Initialise constants

		BigDecimal leftLongitude = BigDecimal.valueOf(-3.192473);
		BigDecimal rightLongitude = BigDecimal.valueOf(-3.184319);
		BigDecimal topLatitude = BigDecimal.valueOf(55.946233);
		BigDecimal bottomLatitude = BigDecimal.valueOf(55.942617);
		BigDecimal width = rightLongitude.subtract(leftLongitude);
		BigDecimal height = topLatitude.subtract(bottomLatitude);
		BigDecimal widthStepSize = width.divide(BigDecimal.valueOf(gridSize));
		BigDecimal heightStepSize = height.divide(BigDecimal.valueOf(gridSize));

		ArrayList<BigDecimal> horizontalPoints = new ArrayList<>();
		ArrayList<BigDecimal> verticalPoints = new ArrayList<>();

		// Compute evenly spaced points along the width (horizontal direction)
		for (int i = 1; i < gridSize; i++) {
			BigDecimal point = leftLongitude.add(BigDecimal.valueOf(i).multiply(widthStepSize));
			horizontalPoints.add(point);
		}

		// Compute evenly spaced points along the height (vertical direction)
		for (int i = 1; i < gridSize; i++) {
			BigDecimal point = topLatitude.subtract(BigDecimal.valueOf(i).multiply(heightStepSize));
			verticalPoints.add(point);
		}

		for (BigDecimal e : horizontalPoints) {
			System.out.println(e.round(new MathContext(8)));
		}

		System.out.println();

		for (BigDecimal e : verticalPoints) {
			System.out.println(e.round(new MathContext(8)));
		}

		System.out.println();

		// Create an array representing the 10x10 grid
		ArrayList<Polygon> grid = new ArrayList<>();

		for (int i = 0; i < gridSize; i++) {
			// Compute top and bottom latitudes of a given rectangle
			BigDecimal top = topLatitude.subtract(BigDecimal.valueOf(i).multiply(heightStepSize));
			BigDecimal bottom = topLatitude.subtract(BigDecimal.valueOf(i + 1).multiply(heightStepSize));

			for (int j = 0; j < gridSize; j++) {
				// ...and do likewise for the left and right longitudes
				BigDecimal left = leftLongitude.add(BigDecimal.valueOf(j).multiply(widthStepSize));
				BigDecimal right = leftLongitude.add(BigDecimal.valueOf(j + 1).multiply(widthStepSize));

				/*
				 * Create an array of Point objects that can later on be used to instantiate a
				 * Polygon object.
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

		System.out.println(featureCollection.toJson());
		ArrayList<String> lines = new ArrayList<>();
		lines.add(featureCollection.toJson());

		// Write to text file for testing
		Path outputFilePath = Paths.get(new File("").getAbsolutePath().concat("/heatmap.geojson"));
		try {
		Files.write(outputFilePath, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("THe given file cannot be created");
		}
	}
}
