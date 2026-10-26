package com.codesense.prototype.web.dto;

import com.codesense.prototype.model.PseudoLine;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class LinesRequest {

    @Size(max = 80)
    private List<PseudoLine> lines = new ArrayList<>();

    public List<PseudoLine> getLines() {
        return lines;
    }

    public void setLines(List<PseudoLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : lines;
    }
}
