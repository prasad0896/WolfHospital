package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TreatedPatientMenu extends Staff {
    private final int patientSessionID;
    private final String staffID;

    TreatedPatientMenu(String staffID, Integer patientSessionID) {
        super(staffID);
        this.staffID = staffID;
        this.patientSessionID = patientSessionID;
    }

    public void displayTreatedPatientMenu(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------TREATED PATIENT MENU ----------------------------");
        System.out.println("1. Checkout");
        System.out.println("2. Go Back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            displayStaffPatientCheckout(conn);
        } else if (select == 2) {
            StaffMenuDisplay(conn);
        } else {
            System.out.println("Please enter a valid input:");
            displayTreatedPatientMenu(conn);
        }
        scan.close();
    }

    void displayStaffPatientCheckout(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        Integer dischargeStatus = 0;
        String reason = null;
        Integer negExpCode = null;
        Integer referralStatusID = 0;
        String treatmentDesc = "";
        int addReportDetails = 0;
        do {
            System.out.println("----------------------------STAFF-PATIENT REPORT MENU --------------------------");
            System.out.println("1. Discharge Status");
            System.out.println("2. Referral Status");
            System.out.println("3. Treatment");
            System.out.println("4. Negative Experience");
            System.out.println("5. Go back");
            System.out.println("6. Submit");
            int select = scan.nextInt();
            scan.nextLine();
            if (select == 1) {
                dischargeStatus = displayDischargeStatus(conn);
            }
            if (select == 2) {
                if (dischargeStatus == 3) {
                    referralStatusID = displayReferralStatus(conn);
                } else {
                    System.out.println("Can NOT enter Referral is the Discharge Status is not Referred.");
                }
            }
            if (select == 3) {
                System.out.println("Enter treatment description:");
                treatmentDesc = scan.nextLine();
            }
            if (select == 4) {
                negExpCode = displayNegativeExperience(conn);
            }
            if (select == 5) {
                displayTreatedPatientMenu(conn);
            }
            if (select == 6) {
                displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
            }
            System.out.println("Please enter valid input:");
            System.out.println("Do you wish to add more details?: (0/1)");
            addReportDetails = scan.nextInt();
            scan.nextLine();
        } while (dischargeStatus == 0 || treatmentDesc.isEmpty() || addReportDetails == 1);
    }

    int displayDischargeStatus(Connection conn) {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Successful Treatment");
        System.out.println("2. Deceased");
        System.out.println("3. Referred");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select < 1 || select > 4) {
            System.out.println("Please enter a valid input.");
            displayDischargeStatus(conn);
        }
        return select;
    }

    int displayReferralStatus(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Facility ID");
        System.out.println("2. Referrer ID");
        System.out.println("3. Add reason");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        int facilityID = 0;
        int referrerID = 0;
        String referralReasonCode = null;
        if (select == 1) {
            PreparedStatement stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
            ResultSet rs1 = stmt.executeQuery();
            HashMap<Integer, String> facilities = new HashMap<Integer, String>();
            while (rs1.next()) {
                facilities.put(rs1.getInt("FACILITY_ID"), rs1.getString("NAME"));
            }
            System.out.println("Facilities available (ID: NAME):");
            for (Map.Entry<Integer, String> map : facilities.entrySet()) {
                System.out.println(map.getKey() + ": " + map.getValue());
            }
            System.out.println("Enter the Facility ID:");
            facilityID = scan.nextInt();
            scan.nextLine();
            PreparedStatement getFacilityID = conn.prepareStatement("select facility_id from staff where employee_id = ?");
            getFacilityID.setString(1, this.staffID);
            ResultSet rs = getFacilityID.executeQuery();
            int selfFacilityID = 0;
            while (rs.next()) {
                selfFacilityID = rs.getInt("FACILITY_ID");
            }
            if (selfFacilityID == facilityID) {
                System.out.println("Can NOT refer to the Facility you are employed at. Please enter another Facility ID:");
                facilityID = scan.nextInt();
                scan.nextLine();
            }
            displayReferralStatus(conn);
        } else if (select == 2) {
            System.out.println("Enter the Referrer ID:");
            referrerID = scan.nextInt();
            scan.nextLine();
            displayReferralReason(conn);
        } else if (select == 3) {
            referralReasonCode = displayReferralReason(conn);
        } else {
            System.out.println("Please enter a valid input.");
            displayReferralStatus(conn);
        }
        PreparedStatement insertReferralStatus = conn.prepareStatement("insert into referral_status (facility_id, employee_id, reason_code) " +
                "values (?,?,?)");
        insertReferralStatus.setInt(1, facilityID);
        insertReferralStatus.setInt(2, referrerID);
        insertReferralStatus.setString(3, referralReasonCode);
        insertReferralStatus.executeQuery();
        int refStatusID = 0;
        PreparedStatement getRefStatSeqID = conn.prepareStatement("select referral_status_id_seq.currval from dual");
        ResultSet rs3 = getRefStatSeqID.executeQuery();
        while (rs3.next()) {
            refStatusID = rs3.getInt("CURRVAL");
        }
        return refStatusID;
    }

    String displayReferralReason(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Following are the reasons available:");
        HashMap<String, List> reasons = new HashMap<>();
        PreparedStatement stmt2 = conn.prepareStatement("SELECT REASON_CODE, SERVICE_NAME, DESCRIPTION FROM REASON");
        ResultSet rs2 = stmt2.executeQuery();
        while (rs2.next()) {
            reasons.put(rs2.getString("REASON_CODE"), Arrays.asList(rs2.getString("SERVICE_NAME"), rs2.getString("DESCRIPTION")));
        }
        int i = 0;
        for (Map.Entry<String, List> map : reasons.entrySet()) {
            System.out.println(i + ". " + map.getKey() + ": " + map.getValue());
            i++;
        }
        System.out.println("1. Reason");
        System.out.println("2. Go back");
        String reasonCode = null;
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            System.out.println("Enter a reason code from above:");
            reasonCode = scan.nextLine();
            return reasonCode;
        } else if (select == 2) {
            return null;
        } else {
            System.out.println("Please enter a valid input.");
            displayReferralReason(conn);
        }
        return null;
    }

    Integer displayNegativeExperience(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Negative Experience Code");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            System.out.println("Please select a reason (Number):");
            System.out.println("1. Misdiagnosis");
            System.out.println("2. Patient acquired an infection during hospital stay");
            int neg_exp_code = scan.nextInt();
            scan.nextLine();
            String neg_exp = neg_exp_code == 1 ? "Misdiagnosis" : "Infection_at_hospital";
            System.out.println("Please enter a text description for the chosen reason:");
            String desc = scan.nextLine();
            PreparedStatement insertNegExp = conn.prepareStatement("INSERT INTO NEGATIVE_EXP (CODE, DESCRIPTION) VALUES (?, ?)");
            insertNegExp.setString(1, neg_exp);
            insertNegExp.setString(2, desc);
            insertNegExp.executeQuery();
            int negExpSeqID = 0;
            try {
                PreparedStatement getAddSeqID = conn.prepareStatement("select neg_exp_id_seq.currval from dual");
                ResultSet rs3 = getAddSeqID.executeQuery();
                while (rs3.next()) {
                    negExpSeqID = rs3.getInt("CURRVAL");
                }
            } catch (SQLException s) {
                negExpSeqID = 0;
            }
            return negExpSeqID;
        } else if (select == 2) {
            return null;
        } else {
            System.out.println("Please enter a valid input.");
            displayNegativeExperience(conn);
        }
        return null;
    }

    void displayReportConfirmation(Connection conn, Integer referralStatusID, Integer negExpCode, Integer dischargeStatus, String treatmentDesc) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Confirm");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        if (select == 1) {
            PreparedStatement insertPatientSym = conn.prepareStatement("insert into report (patient_id, referral_status_id, negative_exp_id, discharge_status, treatment_given) " +
                    "values (?,?,?,?,?)");
            insertPatientSym.setInt(1, this.patientSessionID);
            insertPatientSym.setInt(2, referralStatusID);
            insertPatientSym.setInt(3, negExpCode);
            insertPatientSym.setInt(4, dischargeStatus);
            insertPatientSym.setString(5, treatmentDesc);
            insertPatientSym.executeQuery();
            scan.close();
            System.out.println("Check-out process completed.");
            StaffMenuDisplay(conn);
        } else if (select == 2) {
            displayStaffPatientCheckout(conn);
        } else {
            System.out.println("Please enter a valid input.");
            displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
        }
    }
}