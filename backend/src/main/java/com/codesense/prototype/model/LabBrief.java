package com.codesense.prototype.model;

import java.util.ArrayList;
import java.util.List;

public class LabBrief {

    private String input = "";
    private String output = "";
    private String constraints = "";
    private List<String> constraintItems = new ArrayList<>();
    private List<String> passedTests = new ArrayList<>();
    private List<String> failedTests = new ArrayList<>();
    private List<LabCase> tests = new ArrayList<>();
    private String sample = "";

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input == null ? "" : input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output == null ? "" : output;
    }

    public String getConstraints() {
        return constraints;
    }

    public void setConstraints(String constraints) {
        this.constraints = constraints == null ? "" : constraints;
    }

    public List<String> getConstraintItems() {
        if (constraintItems == null) {
            constraintItems = new ArrayList<>();
        }
        return constraintItems;
    }

    public void setConstraintItems(List<String> constraintItems) {
        this.constraintItems = constraintItems != null ? constraintItems : new ArrayList<>();
        if ((constraints == null || constraints.isBlank()) && !this.constraintItems.isEmpty()) {
            this.constraints = String.join(" ", this.constraintItems);
        }
    }

    public List<String> getPassedTests() {
        if (passedTests == null) {
            passedTests = new ArrayList<>();
        }
        return passedTests;
    }

    public void setPassedTests(List<String> passedTests) {
        this.passedTests = passedTests != null ? passedTests : new ArrayList<>();
    }

    public List<String> getFailedTests() {
        if (failedTests == null) {
            failedTests = new ArrayList<>();
        }
        return failedTests;
    }

    public void setFailedTests(List<String> failedTests) {
        this.failedTests = failedTests != null ? failedTests : new ArrayList<>();
    }

    public List<LabCase> getTests() {
        if (tests == null) {
            tests = new ArrayList<>();
        }
        return tests;
    }

    public void setTests(List<LabCase> tests) {
        this.tests = tests != null ? tests : new ArrayList<>();
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample == null ? "" : sample;
    }

    public void finish() {
        if (getConstraintItems().isEmpty() && constraints != null && !constraints.isBlank()) {
            for (String part : constraints.split("(?<=\\.)\\s+")) {
                if (!part.isBlank()) {
                    constraintItems.add(part.trim());
                }
            }
        }
        if (getTests().isEmpty()) {
            for (String row : getPassedTests()) {
                LabCase parsed = fromArrow(row, "Pass");
                if (parsed != null) {
                    tests.add(parsed);
                }
            }
            for (String row : getFailedTests()) {
                LabCase parsed = fromArrow(row, "Fail");
                if (parsed != null) {
                    tests.add(parsed);
                }
            }
        }
        if ((sample == null || sample.isBlank()) && !getTests().isEmpty()) {
            sample = tests.get(0).getInput();
        }
        if ((output == null || output.isBlank()) && !getTests().isEmpty()) {
            output = tests.get(0).getExpected();
        }
    }

    public static LabCase fromArrow(String row, String status) {
        if (row == null || row.isBlank()) {
            return null;
        }
        String[] parts = row.split("\\s*(→|->)\\s*", 2);
        if (parts.length == 2 && !parts[0].isBlank()) {
            return new LabCase(parts[0].trim(), parts[1].trim(), status);
        }
        return new LabCase(row.trim(), "", status);
    }
}
