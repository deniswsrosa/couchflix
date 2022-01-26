package com.cb.fts.sample.entities.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result implements Serializable {

    private List<FTSResult> results;
    private List<Facet> facets;
}
