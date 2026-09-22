package com.codesense.prototype.web.dto;

import com.codesense.prototype.model.PseudoLine;
import java.util.ArrayList;
import java.util.List;

public class LinesRequest {

    private List<PseudoLine> lines = new ArrayList<>();

    public List<PseudoLine> getLines() {
        return lines;
    }

    public void setLines(List<PseudoLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }
}
