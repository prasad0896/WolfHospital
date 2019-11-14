package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TreatedPatientMenu extends Staff {
    private final int pid;

    TreatedPatientMenu(String id, Integer pid) {
        super(id);
        this.pid = pid;
    }

    public void displayTreatedPatientMenu(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------TREATED PATIENT MENU --------------------------");
        System.out.println("1. Checkout");
        System.out.println("2. Go Back");
        int select = scan.nextInt();
        if (select == 1) {
            displayStaffPatientCheckout(conn);
        } else if (select == 2) {
            StaffMenuDisplay(conn);
        } else {
            System.out.println("Please enter a valid input.");
            displayTreatedPatientMenu(conn);
        }
        scan.close();
    }

    void displayStaffPatientCheckout(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------STAFF-PATIENT REPORT MENU --------------------------");
        System.out.println("1. Discharge Status");
        System.out.println("2. Referral Status");
        System.out.println("3. Treatment");
        System.out.println("4. Negative Experience");
        System.out.println("5. Go back");
        System.out.println("6. Submit");
        int select = scan.nextInt();
        int dischargeStatus = 0;
        String reason = null;
        String negExpCode = null;
        int referralStatusID = 0;
        String treatmentDesc = null;
        if (select == 1) {
            dischargeStatus = displayDischargeStatus(conn);
            displayStaffPatientCheckout(conn);
        } else if (select == 2) {
            referralStatusID = displayReferralStatus(conn);
        } else if (select == 3) {
            System.out.println("Enter treatment description:");
            treatmentDesc = scan.nextLine();
            displayStaffPatientCheckout(conn);
        } else if (select == 4) {
            negExpCode = displayNegativeExperience(conn);
        } else if (select == 5) {
            displayTreatedPatientMenu(conn);
        } else if (select == 6) {
            displayReportConfirmation(conn);
        } else {
            System.out.println("Please enter valid input:");
            displayStaffPatientCheckout(conn);
        }
        PreparedStatement insertPatientSym = conn.prepareStatement("insert into report (patient_id, referral_status_id, negative_experience, discharge_status, treatment_given) " +
                "values (?,?,?,?,?)");
        insertPatientSym.setInt(1, this.pid);
        insertPatientSym.setInt(2, referralStatusID);
        insertPatientSym.setString(3, negExpCode);
        insertPatientSym.setInt(4, dischargeStatus);
        insertPatientSym.setString(5, treatmentDesc);
        insertPatientSym.executeQuery();
        scan.close();
        System.out.println("Check-in process completed.");
    }

    int displayDischargeStatus(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Successful Treatment");
        System.out.println("2. Deceased");
        System.out.println("3. Referred");
        System.out.println("4. Go back");
        int select = scan.nextInt();
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
        int facilityID = 0;
        int referrerID = 0;
        String referralReasonCode = null;
        if (select == 1) {
            System.out.println("Enter the Facility ID:");
            facilityID = scan.nextInt();
            displayReferralStatus(conn);
        } else if (select == 2) {
            System.out.println("Enter the Referrer ID:");
            referrerID = scan.nextInt();
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
        //todo: add auto increment ID + trigger
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

    String displayNegativeExperience(Connection conn) throws SQLException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Following are the negative experience possibilities:");
        HashMap<String, String> negExps = new HashMap<>();
        //todo create neg exp table - CODE TXT, DESC TXT
        PreparedStatement stmt2 = conn.prepareStatement("SELECT CODE, DESCRIPTION FROM NEGATIVE_EXPERIENCE");
        ResultSet rs2 = stmt2.executeQuery();
        while (rs2.next()) {
            negExps.put(rs2.getString("CODE"), rs2.getString("DESCRIPTION"));
        }
        int i = 0;
        for (Map.Entry<String, String> map : negExps.entrySet()) {
            System.out.println(i + ". " + map.getKey() + ": " + map.getValue());
            i++;
        }
        System.out.println("1. Negative Experience Code");
        System.out.println("2. Go back");
        String negExpCode = null;
        int select = scan.nextInt();
        if (select == 1) {
            System.out.println("Enter a reason code from above:");
            negExpCode = scan.nextLine();
            return negExpCode;
        } else if (select == 2) {
            return null;
        } else {
            System.out.println("Please enter a valid input.");
            displayNegativeExperience(conn);
        }
        return null;
    }

    void displayReportConfirmation(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Confirm");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        if (select == 1) {
            StaffMenuDisplay(conn);
        } else if (select == 2) {
            displayStaffPatientCheckout(conn);
        } else {
            System.out.println("Please enter a valid input.");
            displayReportConfirmation(conn);
        }
    }
}