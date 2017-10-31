package com.heightmap.stl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;

public abstract class ModelObject implements ModelInterface, Serializable {

    public static int X_AXIS = 0;
    public static int Y_AXIS = 1;
    public static int Z_AXIS = 2;

    public String header;
    public long vertexcount;
    public long facecount;

    public float[] size = new float[3];
    public float[] center = new float[3];

    public float volume;
    public double area;

    //In Java
    public String filename = "";
    public String path = "";
    public String filetype = "";
    public String units = "";
    public boolean modified = false;
    public String mimeType;

    public ModelObject(String header, long vertexcount, String mimeType) {
        this.header = header;
        this.mimeType = mimeType;
        this.vertexcount = vertexcount;
    }

    public void init(boolean checking) {
        calcSizes();
        if (checking) {
            checkModel();
        }
    }

    public float maxSize() {
        float s = 0.0f;
        for (int i = 0; i < size.length; i++) {
            if (size[i] > s) {
                s = size[i];
            }
        }
        return s;
    }

    public float minSize() {
        float s = Float.MAX_VALUE;
        for (int i = 0; i < size.length; i++) {
            if (size[i] < s) {
                s = size[i];
            }
        }
        return s;
    }

    public String getFileType() {
        return filetype;
    }

    protected void calcSizes() {
        float[] points = getPointArray();
        //this.area = calculateSizes(points.length, points, size, center);
        //this.volume = calculateVolume(points.length, points);
    }

    public void center() {
        float[] points = getPointArray();
        for (int i = 0; i < points.length; i++) {

            if (i % 3 == 0) {
                points[i] += center[0] * -1;
            } else if (i % 3 == 1) {
                points[i] += center[1] * -1;
            } else {
                points[i] += center[2] * -1;
            }
        }

        calcSizes();
        modified = true;
    }

    public void moveModel(int AXIS, float value) {
        float[] points = getPointArray();
        for (int i = AXIS; i < points.length; i += 3) {
            points[i] += value;
        }
        calcSizes();
        modified = true;
    }

    public void scaleModel(float scale) {
        float[] points = getPointArray();
        for (int i = 0; i < points.length; i++) {
            points[i] *= scale;
        }
        calcSizes();
        modified = true;
    }

    public float[] rotateNormalPoint(float x, float y, float z, int AXIS, double radius) {

        float[] erg = {0.0f, 0.0f, 0.0f};
        double[] p0 = new double[]{0, 0, 0};
        p0[AXIS] += 1;

        getRotatedPoint(erg, new float[]{x, y, z}, radius * Math.PI / 180, p0);
        return erg;
    }

    public void rotate(int AXIS, double radius) {
        float[] erg = new float[3];
        float[] points = getPointArray();
        float[] normal = getNormalsArray();

        double[] p0 = new double[]{0, 0, 0};
        p0[AXIS] += 1;

        double[] p1 = new double[]{center[0], center[1], center[2]};

        double[] p2 = new double[]{center[0], center[1], center[2]};
        p2[AXIS] += 1;


        for (int i = 0; i < points.length; i += 3) {
            getRotatedPoint(erg, new float[]{points[i], points[i + 1], points[i + 2]}, radius * Math.PI / 180, p1, p2);
            points[i] = erg[0];
            points[i + 1] = erg[1];
            points[i + 2] = erg[2];

            if (normal != null) {
                getRotatedPoint(erg, new float[]{normal[i], normal[i + 1], normal[i + 2]}, radius * Math.PI / 180, p0);
                normal[i] = erg[0];
                normal[i + 1] = erg[1];
                normal[i + 2] = erg[2];
            }
        }
        calcSizes();
        modified = true;
    }

    private void checkModel() {
        float[] points = getPointArray();
        RedBlackTree set = new RedBlackTree();
        boolean found;

        for (int i = 0; i < points.length; i += 3) {

            if (set.find(points[i] + points[i + 1] * 10 + points[i + 2] * 100) == null) {
                found = false;
                for (int j = i; j < points.length; j += 3) {
                    if (i != j && points[i] == points[j] && points[i + 1] == points[j + 1] && points[i + 2] == points[j + 2]) {
                        found = true;
                        set.insert(points[i] + points[i + 1] * 10 + points[i + 2] * 100);
                        break;
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    protected static void getRotatedPoint(float[] q, float[] p, double theta, double[] r) {
        double costheta, sintheta;
        q[0] = 0f;
        q[1] = 0f;
        q[2] = 0f;

        r[0] = r[0] / abs(r);
        r[1] = r[1] / abs(r);
        r[2] = r[2] / abs(r);

        costheta = Math.cos(theta);
        sintheta = Math.sin(theta);

        q[0] += (costheta + (1 - costheta) * r[0] * r[0]) * p[0];
        q[0] += ((1 - costheta) * r[0] * r[1] - r[2] * sintheta) * p[1];
        q[0] += ((1 - costheta) * r[0] * r[2] + r[1] * sintheta) * p[2];

        q[1] += ((1 - costheta) * r[0] * r[1] + r[2] * sintheta) * p[0];
        q[1] += (costheta + (1 - costheta) * r[1] * r[1]) * p[1];
        q[1] += ((1 - costheta) * r[1] * r[2] - r[0] * sintheta) * p[2];

        q[2] += ((1 - costheta) * r[0] * r[2] - r[1] * sintheta) * p[0];
        q[2] += ((1 - costheta) * r[1] * r[2] + r[0] * sintheta) * p[1];
        q[2] += (costheta + (1 - costheta) * r[2] * r[2]) * p[2];

    }

    protected static void getRotatedPoint(float[] q, float[] p, double theta, double[] p1, double[] p2) {
        double costheta, sintheta;
        double[] r = new double[3];

        q[0] = 0f;
        q[1] = 0f;
        q[2] = 0f;

        r[0] = p2[0] - p1[0];
        r[1] = p2[1] - p1[1];
        r[2] = p2[2] - p1[2];
        p[0] -= p1[0];
        p[1] -= p1[1];
        p[2] -= p1[2];

        r[0] = r[0] / abs(r);
        r[1] = r[1] / abs(r);
        r[2] = r[2] / abs(r);

        costheta = Math.cos(theta);
        sintheta = Math.sin(theta);

        q[0] += (costheta + (1 - costheta) * r[0] * r[0]) * p[0];
        q[0] += ((1 - costheta) * r[0] * r[1] - r[2] * sintheta) * p[1];
        q[0] += ((1 - costheta) * r[0] * r[2] + r[1] * sintheta) * p[2];

        q[1] += ((1 - costheta) * r[0] * r[1] + r[2] * sintheta) * p[0];
        q[1] += (costheta + (1 - costheta) * r[1] * r[1]) * p[1];
        q[1] += ((1 - costheta) * r[1] * r[2] - r[0] * sintheta) * p[2];

        q[2] += ((1 - costheta) * r[0] * r[2] - r[1] * sintheta) * p[0];
        q[2] += ((1 - costheta) * r[1] * r[2] + r[0] * sintheta) * p[1];
        q[2] += (costheta + (1 - costheta) * r[2] * r[2]) * p[2];

        q[0] += p1[0];
        q[1] += p1[1];
        q[2] += p1[2];
    }

    private static double abs(double[] a) {
        return Math.sqrt((a[0] * a[0]) + (a[1] * a[1]) + (a[2] * a[2]));
    }

    protected static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    protected void writeFloat(RandomAccessFile out, float val) throws IOException {
        byte[] b = ByteBuffer.allocate(4).putFloat(val).array();
        for (int i = b.length - 1; i >= 0; i--) {
            out.write(b[i]);
        }
    }

    protected static float readFloat(RandomAccessFile in, byte[] b) throws IOException {
        in.read(b);
        return Float.intBitsToFloat(((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff));
    }

    protected static float readFloat(InputStream in, byte[] b) throws IOException {
        in.read(b);
        return Float.intBitsToFloat(((b[3] & 0xff) << 24) | ((b[2] & 0xff) << 16) | ((b[1] & 0xff) << 8) | (b[0] & 0xff));
    }

    protected static String getHexString(byte[] b) throws Exception {
        String result = "";
        for (int i = b.length - 1; i >= 0; i--) {
            result +=
                    Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    protected static long unsignedIntToLong(byte[] b) {
        long l = 0;
        l |= b[3] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[0] & 0xFF;
        return l;
    }

    protected static int unsignedShortToInt(byte[] b) {
        int i = 0;
        i |= b[0] & 0xFF;
        i <<= 8;
        i |= b[1] & 0xFF;
        return i;
    }

    protected static int unsignedShortToIntLittleEndian(byte[] b) {
        int i = 0;
        i |= b[1] & 0xFF;
        i <<= 8;
        i |= b[0] & 0xFF;
        return i;
    }

    public static native float parseFloat(String value);

    public static native int parseInteger(String value);

    private static native float calculateVolume(int size, float[] points);

    private static native float calculateSizes(int p_length, float[] points, float[] size, float[] center);

    protected static native void calculateNormals(int size, float[] points);

    protected static native int[] triangulate(int size, int[] vidxs, float[] vectors);

}
