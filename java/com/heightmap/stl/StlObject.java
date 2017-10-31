package com.heightmap.stl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

public class StlObject extends ModelObject implements Serializable {

	public static int FILE_BINARY = 3;
	public static int FILE_ASCII = 4;

	public String stlType;
	public float[] points;
	public float[] normal;

	public StlObject(String header, long vertexcount){
		super(header, vertexcount, "application/vnd.ms-pki.stl");
	}

	@Override
	public String getFileType(){
		return filetype + " (" + stlType + ")";
	}


	public static ModelObject parseObjectFromFile(File file, boolean checking) throws Exception{

		BufferedReader br = new BufferedReader(new FileReader(file));
		String firstLine = br.readLine();
		br.close();
		if(firstLine.startsWith("solid ")){
			StlObject obj = null;
			try {
				obj = parseASCII(file, checking);
			} catch (Exception e){
				return parseBinary(file, checking);
			}
			if(obj == null){
				return parseBinary( file, checking);
			}
			return obj;
		} else {
			return parseBinary(file, checking);
		}
	}

	private static StlObject parseASCII(File file, boolean checking) throws Exception {

		ArrayList<Float> points = new ArrayList<Float>();
		ArrayList<Float> normals = new ArrayList<Float>();

		String header = "";
		String[] t = new String[3];
		BufferedReader br = new BufferedReader(new FileReader(file), 2048 * 1024);
		String line = "";
		boolean firstLine = true;
		while((line = br.readLine()) != null){
			line = StringUtils.strip(line);
			t = StringUtils.split(line, " ");

			if(firstLine){
				firstLine = false;

				if(StringUtils.equalsIgnoreCase(t[0], "solid")){
					header = t[1].trim();
				} else {
					return null;
				}

			} else {

				if(StringUtils.equalsIgnoreCase(t[0], "vertex")){
					points.add(parseFloat(t[1]));
					points.add(parseFloat(t[2]));
					points.add(parseFloat(t[3]));
				} else if(StringUtils.equalsIgnoreCase(t[0], "facet")){

					float x = parseFloat(t[2]);
					float y = parseFloat(t[3]);
					float z = parseFloat(t[4]);

					normals.add(x);
					normals.add(y);
					normals.add(z);

					normals.add(x);
					normals.add(y);
					normals.add(z);

					normals.add(x);
					normals.add(y);
					normals.add(z);
				}  else if(StringUtils.equalsIgnoreCase(t[0], "endsolid")){
					StlObject obj = new StlObject(header, points.size() / 3);
					obj.facecount = points.size() / 9;
					obj.points = toArray(points);
					obj.normal = toArray(normals);
					obj.filename = file.getName();
					obj.path = file.getAbsolutePath();
					obj.init(checking);
					obj.stlType = "ASCII";
					obj.filetype = "STL";
					br.close();

					return obj;
				}
			}
		}

		br.close();
		return null;
	}

	private static float[] toArray(ArrayList<Float> array){
		float[] farray = new float[array.size()];
		for(int i = 0; i < array.size(); i++){
			farray[i] = array.get(i).floatValue();
		}
		return farray;
	}

	private static StlObject parseBinary(File file, boolean checking) throws Exception{

		String header = "";
		long vertexcount;
		long facecount;
		byte[] b;

		RandomAccessFile in = new RandomAccessFile(file, "r");

		b = new byte[80];
		in.read(b);
		for(int i = 0; i < b.length; i++){
			header += (char)unsignedByteToInt(b[i]);
		}
		header = header.trim();

		b = new byte[4];
		in.read(b);
		vertexcount = ((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff);
		facecount = vertexcount / 3;

		float[] points = new float[(int)vertexcount * 9];
		float[] normal = new float[(int)vertexcount * 9];
		for(int i = 0; i < (int)points.length; i = i+9){
			b = new byte[4];
			normal[i] = readFloat(in, b);
			normal[i+1] = readFloat(in, b);
			normal[i+2] = readFloat(in, b);
			normal[i+3] = normal[i];
			normal[i+4] = normal[i+1];
			normal[i+5] = normal[i+2];
			normal[i+6] = normal[i];
			normal[i+7] = normal[i+1];
			normal[i+8] = normal[i+2];

			points[i] = readFloat(in, b);
			points[i+1] = readFloat(in, b);
			points[i+2] = readFloat(in, b);
			points[i+3] = readFloat(in, b);
			points[i+4] = readFloat(in, b);
			points[i+5] = readFloat(in, b);
			points[i+6] = readFloat(in, b);
			points[i+7] = readFloat(in, b);
			points[i+8] = readFloat(in, b);

			b = new byte[2];
			in.read(b);
		}


		StlObject object = new StlObject(header, vertexcount);
		object.points = points;
		object.normal = normal;
		object.filename = file.getName();
		object.path = file.getAbsolutePath();
		object.facecount = facecount;
		object.init(checking);
		object.stlType = "Binary";
		object.filetype = "STL";
		in.close();
		return object;
	}

	public void writeToAscii() throws Exception {

		new File(this.path).delete();

		BufferedWriter out = null;

		out = new BufferedWriter(new FileWriter(new File(this.path)));

		out.append("solid " + this.filename.substring(0, this.filename.lastIndexOf(".")) + "\r\n");

		for(int i = 0; i < points.length; i += 9){
			out.append(" facet normal " + normal[i] + " " + normal[i+1] + " " + normal[i+2] + "\r\n");
			out.append("  outer loop\r\n");
			out.append("   vertex " + points[i] + " " + points[i+1] + " " + points[i+2] + "\r\n");
			out.append("   vertex " + points[i+3] + " " + points[i+4] + " " + points[i+5] + "\r\n");
			out.append("   vertex " + points[i+6] + " " + points[i+7] + " " + points[i+8] + "\r\n");
			out.append("  endloop\r\n");
			out.append(" endfacet\r\n");
		}

		out.append("endsolid " + this.filename.substring(0, this.filename.lastIndexOf(".")-1) + "\r\n");
		out.close();

		this.filetype = "ASCII";
	}

	public void writeToBinary() throws Exception {

		Date startTime = new Date();

		new File(this.path).delete();

		RandomAccessFile out = null;

		double percent = 0;

		byte[] b;
		out = new RandomAccessFile(new File(this.path), "rw");

		b = new byte[80];
		for(int i = 0; i < header.length(); i++){
			b[i] = (byte)header.charAt(i);
		}
		out.write(b);
		b = ByteBuffer.allocate(4).putInt((points.length / 9)).array();
		for(int i = b.length-1; i >= 0; i--){
			out.write(b[i]);
		}

		for(int i = 0; i < points.length; i += 9){
			if(normal != null){
				writeFloat(out, normal[i]);
				writeFloat(out, normal[i+1]);
				writeFloat(out, normal[i+2]);
			} else {
				writeFloat(out, 0);
				writeFloat(out, 0);
				writeFloat(out, 0);
			}
			

			writeFloat(out, points[i]);
			writeFloat(out, points[i+1]);
			writeFloat(out, points[i+2]);
			writeFloat(out, points[i+3]);
			writeFloat(out, points[i+4]);
			writeFloat(out, points[i+5]);
			writeFloat(out, points[i+6]);
			writeFloat(out, points[i+7]);
			writeFloat(out, points[i+8]);

			out.write(new byte[]{0, 0});

			double p = Math.round(i*1.0 / points.length * 100.0);
			if(p >= percent+2){
				double dur = (new Date().getTime() - startTime.getTime()) / p;
				double t =  dur * (100-p) / 1000;

				percent = p;
				
					System.out.print(percent + "% - ");
					if(t > 60){
						System.out.println(Math.round(t/60) + " mins");
					} else {
						System.out.println(Math.round(t) + " seconds");
					}
				}
		
		}

		this.filetype = "Binary";


		out.close();

	}

	@Override
	public float[] getNormalsArray() {
		return normal;
	}


	public float[] getPointArray(){
		return points;
	}

	@Override
	public void save(int type) throws Exception {
		if(type == FILE_ASCII){
			//writeToAscii();
		} else {
			writeToBinary();
		}
		modified = false;
	}

	public static StlObject fromHeightmap (String filename, int height, int width, float[][] heightmap) {

		try {
			String name = filename;
			if (name.contains(".")) {
				name = name.substring(0, name.lastIndexOf("."));
			}

			StlObject stl = new StlObject(name, 0);

			//float[] points = new float[((height - 1) * (width - 1) * 2 + (width - 1) * 4 + (height - 1) * 4 + 2) * 3 * 3];
			float[] points = new float[(height * width + width + height) * 6 * 3 * 2];

			int i = 0;

			//top surface
			for (int y = 0; y < height - 1; y++) {

				for (int x = 0; x < width - 1; x++) {

					float xy, x1y, xy1, x1y1, z0;
					int x1, y1;
					x1 = x + 1;
					y1 = y + 1;
					z0 = 0.0F;
					xy = heightmap[x][y];
					xy1 = heightmap[x][y1];
					x1y = heightmap[x1][y];
					x1y1 = heightmap[x1][y1];
                    
					// lower left triangle
					if (xy > z0 || x1y > z0 || xy1 > z0) {

						// top
						points[i++] = x;
						points[i++] = y;
						points[i++] = xy;

						points[i++] = x1;
						points[i++] = y;
						points[i++] = x1y;

						points[i++] = x;
						points[i++] = y1;
						points[i++] = xy1;

						// bottom
						points[i++] = x;
						points[i++] = y;
						points[i++] = z0;

						points[i++] = x1;
						points[i++] = y;
						points[i++] = z0;

						points[i++] = x;
						points[i++] = y1;
						points[i++] = z0;

						// left side
						if (x == 0) {
							points[i++] = z0;
							points[i++] = y;
							points[i++] = xy;

							points[i++] = z0;
							points[i++] = y1;
							points[i++] = xy1;

							points[i++] = z0;
							points[i++] = y;
							points[i++] = z0;

							points[i++] = z0;
							points[i++] = y1;
							points[i++] = xy1;

							points[i++] = z0;
							points[i++] = y1;
							points[i++] = z0;

							points[i++] = z0;
							points[i++] = y;
							points[i++] = z0;
						}

						// near side
						if (y == 0) {
							points[i++] = x;
							points[i++] = z0;
							points[i++] = xy;

							points[i++] = x;
							points[i++] = z0;
							points[i++] = z0;

							points[i++] = x1;
							points[i++] = z0;
							points[i++] = x1y;

							points[i++] = x1;
							points[i++] = z0;
							points[i++] = x1y;

							points[i++] = x;
							points[i++] = z0;
							points[i++] = z0;

							points[i++] = x1;
							points[i++] = z0;
							points[i++] = z0;
						}
					} // if lower left

					// upper right triangle
					if (x1y > z0 || x1y1 > z0 || xy1 > z0) {
						// top
						points[i++] = x1;
						points[i++] = y;
						points[i++] = x1y;

						points[i++] = x1;
						points[i++] = y1;
						points[i++] = x1y1;

						points[i++] = x;
						points[i++] = y1;
						points[i++] = xy1;

						//bottom
						points[i++] = x1;
						points[i++] = y;
						points[i++] = z0;

						points[i++] = x1;
						points[i++] = y1;
						points[i++] = z0;

						points[i++] = x;
						points[i++] = y1;
						points[i++] = z0;

						// right side
						if (x == width - 2) {
							points[i++] = x1;
							points[i++] = y;
							points[i++] = x1y;

							points[i++] = x1;
							points[i++] = y;
							points[i++] = z0;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = x1y1;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = x1y1;

							points[i++] = x1;
							points[i++] = y;
							points[i++] = z0;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = z0;
						}

						// far side
						if (y == height - 2) {
							points[i++] = x;
							points[i++] = y1;
							points[i++] = xy1;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = x1y1;

							points[i++] = x;
							points[i++] = y1;
							points[i++] = z0;

							points[i++] = x;
							points[i++] = y1;
							points[i++] = z0;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = x1y1;

							points[i++] = x1;
							points[i++] = y1;
							points[i++] = z0;
						}
					} // if upper right

				} // for x
			} // for y

			stl.normal = null;
			stl.points = points;

			stl.rotate(ModelObject.Z_AXIS, 180d);
			System.out.println("Facecount: " + NumberFormat.getIntegerInstance().format(points.length / 9));

			int filesize = (points.length / 9 * 4 * 3 * 4) / 1024 / 1024;
			if (filesize > 1) {
				System.out.println("Estimated file size: " + filesize + " MB");
			} else {
				System.out.println("Estimated file size: < 1 MB");
			}


			return stl;
		} catch (Exception ex) {
			System.out.println("ERROR: " + ex.toString());
			return null;
		}
	}
}
