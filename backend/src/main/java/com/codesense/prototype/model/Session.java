package com.codesense.prototype.model;

import com.codesense.prototype.workshop.Flowchart;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sessions")
public class Session {

    @Id
    private String id;
    private String labQuestion;
    private Phase phase;
    private Slot slot;
    private String currentQuestion;
    private List<PseudoLine> proposedLines = new ArrayList<>();
    private List<PseudoLine> confirmedLines = new ArrayList<>();
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<PseudoLine> referenceLines = new ArrayList<>();
    private List<PseudoLine> puzzleLines = new ArrayList<>();
    private List<Turn> turns = new ArrayList<>();
    private Boolean hasRepetition;
    private int dontKnowCount;
    private List<String> askedQuestions = new ArrayList<>();
    private int memoryFollowUps;
    private boolean usedFallbackQuestion;
    private String refusal;
    private LlmMode llmMode;
    private int wrongAttempts;
    private int maxAttempts = WorkshopConstants.MAX_ANSWER_TRIES;
    private String verdict;
    private String feedback;
    private String explanation;
    private String correctAnswer;
    private List<ArrangeNote> arrangeNotes = new ArrayList<>();
    private String puzzleKind = "path";
    private FlowchartView flowchart;
    private String pythonCode;
    private String codeLanguage = "python";
    private String ownerId = "";
    private String sampleInput;
    private String expectedOutput;
    private List<PythonStep> pythonTrace = new ArrayList<>();
    private int predictIndex;
    private LabBrief labBrief;
    private int socraticStage = 1;
    private List<WalkLine> pythonWalk = new ArrayList<>();
    private int helpLevel;
    private boolean helpFollowUp;
    private String originalQuestion = "";
    private String helpText = "";
    private String helpFollowUpQuestion = "";
    private String helpExpected = "";
    private String askKind = "text";
    private List<McqOption> choices = new ArrayList<>();
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String mcqKey = "";
    private boolean whyFollowUp;
    private String whyQuestion = "";
    private String originalAskKind = "text";
    private List<McqOption> originalChoices = new ArrayList<>();
    private int mcqAsked;
    private int asksMade;

    public String getPuzzleKind() {
        return puzzleKind == null || puzzleKind.isBlank() ? "path" : puzzleKind;
    }

    public void setPuzzleKind(String puzzleKind) {
        this.puzzleKind = "flow".equalsIgnoreCase(puzzleKind) ? "flow" : "path";
    }

    public FlowchartView getFlowchart() {
        return flowchart;
    }

    public void setFlowchart(FlowchartView flowchart) {
        this.flowchart = flowchart;
    }

    public record FlowchartView(List<Flowchart.Node> nodes, List<Flowchart.Edge> edges) {
        public static FlowchartView of(Flowchart.Chart chart) {
            if (chart == null) {
                return new FlowchartView(List.of(), List.of());
            }
            return new FlowchartView(chart.nodes(), chart.edges());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabQuestion() {
        return labQuestion;
    }

    public void setLabQuestion(String labQuestion) {
        this.labQuestion = labQuestion;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public List<PseudoLine> getProposedLines() {
        return proposedLines;
    }

    public void setProposedLines(List<PseudoLine> proposedLines) {
        this.proposedLines = proposedLines != null ? proposedLines : new ArrayList<>();
    }

    public List<PseudoLine> getConfirmedLines() {
        return confirmedLines;
    }

    public void setConfirmedLines(List<PseudoLine> confirmedLines) {
        this.confirmedLines = confirmedLines != null ? confirmedLines : new ArrayList<>();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<PseudoLine> getReferenceLines() {
        if (referenceLines == null) {
            referenceLines = new ArrayList<>();
        }
        return referenceLines;
    }

    public void setReferenceLines(List<PseudoLine> referenceLines) {
        this.referenceLines = referenceLines != null ? referenceLines : new ArrayList<>();
    }

    public List<PseudoLine> getPuzzleLines() {
        if (puzzleLines == null) {
            puzzleLines = new ArrayList<>();
        }
        return puzzleLines;
    }

    public void setPuzzleLines(List<PseudoLine> puzzleLines) {
        this.puzzleLines = puzzleLines != null ? puzzleLines : new ArrayList<>();
    }

    public List<Turn> getTurns() {
        return turns;
    }

    public void setTurns(List<Turn> turns) {
        this.turns = turns != null ? turns : new ArrayList<>();
    }

    public Boolean getHasRepetition() {
        return hasRepetition;
    }

    public void setHasRepetition(Boolean hasRepetition) {
        this.hasRepetition = hasRepetition;
    }

    public int getDontKnowCount() {
        return dontKnowCount;
    }

    public void setDontKnowCount(int dontKnowCount) {
        this.dontKnowCount = dontKnowCount;
    }

    public List<String> getAskedQuestions() {
        if (askedQuestions == null) {
            askedQuestions = new ArrayList<>();
        }
        return askedQuestions;
    }

    public void setAskedQuestions(List<String> askedQuestions) {
        this.askedQuestions = askedQuestions != null ? askedQuestions : new ArrayList<>();
    }

    public int getMemoryFollowUps() {
        return memoryFollowUps;
    }

    public void setMemoryFollowUps(int memoryFollowUps) {
        this.memoryFollowUps = memoryFollowUps;
    }

    public boolean isUsedFallbackQuestion() {
        return usedFallbackQuestion;
    }

    public void setUsedFallbackQuestion(boolean usedFallbackQuestion) {
        this.usedFallbackQuestion = usedFallbackQuestion;
    }

    public String getRefusal() {
        return refusal;
    }

    public void setRefusal(String refusal) {
        this.refusal = refusal;
    }

    public LlmMode getLlmMode() {
        return llmMode;
    }

    public void setLlmMode(LlmMode llmMode) {
        this.llmMode = llmMode;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public void setWrongAttempts(int wrongAttempts) {
        this.wrongAttempts = wrongAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public List<ArrangeNote> getArrangeNotes() {
        if (arrangeNotes == null) {
            arrangeNotes = new ArrayList<>();
        }
        return arrangeNotes;
    }

    public void setArrangeNotes(List<ArrangeNote> arrangeNotes) {
        this.arrangeNotes = arrangeNotes != null ? arrangeNotes : new ArrayList<>();
    }

    public String getPythonCode() {
        return pythonCode;
    }

    public void setPythonCode(String pythonCode) {
        this.pythonCode = pythonCode;
    }

    public String getCodeLanguage() {
        return codeLanguage == null || codeLanguage.isBlank() ? "python" : codeLanguage;
    }

    public void setCodeLanguage(String codeLanguage) {
        this.codeLanguage = codeLanguage == null || codeLanguage.isBlank() ? "python" : codeLanguage;
    }

    public String getOwnerId() {
        return ownerId == null ? "" : ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId == null ? "" : ownerId;
    }

    public String getSampleInput() {
        return sampleInput;
    }

    public void setSampleInput(String sampleInput) {
        this.sampleInput = sampleInput;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public List<PythonStep> getPythonTrace() {
        if (pythonTrace == null) {
            pythonTrace = new ArrayList<>();
        }
        return pythonTrace;
    }

    public void setPythonTrace(List<PythonStep> pythonTrace) {
        this.pythonTrace = pythonTrace != null ? pythonTrace : new ArrayList<>();
    }

    public int getPredictIndex() {
        return predictIndex;
    }

    public void setPredictIndex(int predictIndex) {
        this.predictIndex = Math.max(0, predictIndex);
    }

    public LabBrief getLabBrief() {
        return labBrief;
    }

    public void setLabBrief(LabBrief labBrief) {
        this.labBrief = labBrief;
    }

    public int getSocraticStage() {
        return socraticStage < 1 ? 1 : socraticStage;
    }

    public void setSocraticStage(int socraticStage) {
        this.socraticStage = Math.max(1, socraticStage);
    }

    public List<WalkLine> getPythonWalk() {
        if (pythonWalk == null) {
            pythonWalk = new ArrayList<>();
        }
        return pythonWalk;
    }

    public void setPythonWalk(List<WalkLine> pythonWalk) {
        this.pythonWalk = pythonWalk != null ? pythonWalk : new ArrayList<>();
    }

    public int getHelpLevel() {
        return helpLevel;
    }

    public void setHelpLevel(int helpLevel) {
        this.helpLevel = Math.max(0, helpLevel);
    }

    public boolean isHelpFollowUp() {
        return helpFollowUp;
    }

    public void setHelpFollowUp(boolean helpFollowUp) {
        this.helpFollowUp = helpFollowUp;
    }

    public String getOriginalQuestion() {
        return originalQuestion;
    }

    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion == null ? "" : originalQuestion;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText == null ? "" : helpText;
    }

    public String getHelpFollowUpQuestion() {
        return helpFollowUpQuestion;
    }

    public void setHelpFollowUpQuestion(String helpFollowUpQuestion) {
        this.helpFollowUpQuestion = helpFollowUpQuestion == null ? "" : helpFollowUpQuestion;
    }

    public String getHelpExpected() {
        return helpExpected;
    }

    public void setHelpExpected(String helpExpected) {
        this.helpExpected = helpExpected == null ? "" : helpExpected;
    }

    public String getAskKind() {
        return askKind == null || askKind.isBlank() ? "text" : askKind;
    }

    public void setAskKind(String askKind) {
        this.askKind = "mcq".equalsIgnoreCase(askKind) ? "mcq" : "text";
    }

    public List<McqOption> getChoices() {
        if (choices == null) {
            choices = new ArrayList<>();
        }
        return choices;
    }

    public void setChoices(List<McqOption> choices) {
        this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getMcqKey() {
        return mcqKey == null ? "" : mcqKey;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setMcqKey(String mcqKey) {
        this.mcqKey = mcqKey == null ? "" : mcqKey.trim().toUpperCase();
    }

    public boolean isWhyFollowUp() {
        return whyFollowUp;
    }

    public void setWhyFollowUp(boolean whyFollowUp) {
        this.whyFollowUp = whyFollowUp;
    }

    public String getWhyQuestion() {
        return whyQuestion == null ? "" : whyQuestion;
    }

    public void setWhyQuestion(String whyQuestion) {
        this.whyQuestion = whyQuestion == null ? "" : whyQuestion;
    }

    public String getOriginalAskKind() {
        return originalAskKind == null || originalAskKind.isBlank() ? "text" : originalAskKind;
    }

    public void setOriginalAskKind(String originalAskKind) {
        this.originalAskKind = "mcq".equalsIgnoreCase(originalAskKind) ? "mcq" : "text";
    }

    public List<McqOption> getOriginalChoices() {
        if (originalChoices == null) {
            originalChoices = new ArrayList<>();
        }
        return originalChoices;
    }

    public void setOriginalChoices(List<McqOption> originalChoices) {
        this.originalChoices = originalChoices != null ? new ArrayList<>(originalChoices) : new ArrayList<>();
    }

    public int getMcqAsked() {
        return mcqAsked;
    }

    public void setMcqAsked(int mcqAsked) {
        this.mcqAsked = Math.max(0, mcqAsked);
    }

    public int getAsksMade() {
        return asksMade;
    }

    public void setAsksMade(int asksMade) {
        this.asksMade = Math.max(0, asksMade);
    }
}
