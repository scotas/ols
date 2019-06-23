package com.scotas.solr.handler.component.ojvm;

import java.util.Iterator;
import java.util.Map;

public class FacetComponentContext {
    long startTime;
    private Iterator<Map.Entry<String, Object>> facetFieldIterator;
    private Iterator<Map.Entry<String, Object>> facetQueryIterator;
    private Iterator<Map.Entry<String, Object>> facetDateIterator;
    private Iterator<Map.Entry<String, Object>> facetRangeIterator;

    public FacetComponentContext() {
        super();
    }

    public void setFacetFieldIterator(Iterator<Map.Entry<String, Object>> facetFieldIterator) {
        this.facetFieldIterator = facetFieldIterator;
    }

    public Iterator<Map.Entry<String, Object>> getFacetFieldIterator() {
        return facetFieldIterator;
    }

    public void setFacetQueryIterator(Iterator<Map.Entry<String, Object>> facetQueryIterator) {
        this.facetQueryIterator = facetQueryIterator;
    }

    public Iterator<Map.Entry<String, Object>> getFacetQueryIterator() {
        return facetQueryIterator;
    }

    public void setFacetDateIterator(Iterator<Map.Entry<String, Object>> facetDateIterator) {
        this.facetDateIterator = facetDateIterator;
    }

    public Iterator<Map.Entry<String, Object>> getFacetDateIterator() {
        return facetDateIterator;
    }

    public void setFacetRangeIterator(Iterator<Map.Entry<String, Object>> facetRangeIterator) {
        this.facetRangeIterator = facetRangeIterator;
    }

    public Iterator<Map.Entry<String, Object>> getFacetRangeIterator() {
        return facetRangeIterator;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
}
