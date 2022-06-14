package com.invictoprojects.keyscollector.model;

public class CodeUpdate {

    public String name;
    public String path;
    public String url;
    public Repository repository;

    @Override
    public String toString() {
        return "CodeUpdate{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", url='" + url + '\'' +
                ", repository=" + repository +
                '}';
    }

}
