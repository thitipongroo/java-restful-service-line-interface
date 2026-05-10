package ws.integration.model;

public class RequestRecord {
    private String citizenId;
    private String govtId;
    private String firstName;
    private String lastName;
    private String fnameTh;
    private String lnameTh;
    private String dob;
    private String mobilePhone;
    private String email;
    private String lineId;
    private String policyNo;
    private String certNo;

    public String getCitizenId() { return citizenId; }
    public void setCitizenId(String citizenId) { this.citizenId = citizenId; }

    public String getGovtId() { return govtId; }
    public void setGovtId(String govtId) { this.govtId = govtId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFnameTh() { return fnameTh; }
    public void setFnameTh(String fnameTh) { this.fnameTh = fnameTh; }

    public String getLnameTh() { return lnameTh; }
    public void setLnameTh(String lnameTh) { this.lnameTh = lnameTh; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }

    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }

    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
}
