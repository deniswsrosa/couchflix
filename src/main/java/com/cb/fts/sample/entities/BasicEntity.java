package com.cb.fts.sample.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import static lombok.AccessLevel.PROTECTED;

public class BasicEntity {

    @Getter(PROTECTED)
    @Setter(PROTECTED)
    @JsonIgnore
    protected String _class;
}
