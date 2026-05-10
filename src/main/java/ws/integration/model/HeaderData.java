package ws.integration.model;

public class HeaderData {
    private String messageId;
    private String sentDateTime;
    private String responseDateTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSentDateTime() { return sentDateTime; }
    public void setSentDateTime(String sentDateTime) { this.sentDateTime = sentDateTime; }

    public String getResponseDateTime() { return responseDateTime; }
    public void setResponseDateTime(String responseDateTime) { this.responseDateTime = responseDateTime; }
}
