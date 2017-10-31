package com.heightmap.stl;

public interface ModelInterface {

    public float[] getPointArray();

    public float[] getNormalsArray();

    public void save(int type) throws Exception;

}
