package com.ehunzango.unigo.router.adapters;

import com.ehunzango.unigo.router.entities.Line;

import java.util.List;

public interface IDataAdapter {
    boolean load(String path, List<Line> lines);
}
