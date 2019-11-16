package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class TreatedPatientMenu extends Staff {
    private final int patientSessionID;
    private final String staffID;

    TreatedPatientMenu(String staffID, Integer patientSessionID) {
        super(staffID);
        this.staffID = staffID;
        this.patientSessionID = patientSessionID;
    }

    public void displayTreatedPatientMenu(Connection conn) throws Exception {
        // only medical staff has access
        String check_medical = "SELECT DESIGNATION FROM STAFF WHERE EMPLOYEE_ID = " + this.staffID;
        ResultSet medical_or_not = executeStringQuery(conn, check_medical);
        medical_or_not.next();
        if (medical_or_not.getString(1).equalsIgnoreCase("non-medical")) {
            System.out.println("Access Denied. Only Medical Staff has access. Going back!\n");
            StaffMenuDisplay(conn);
        }
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
    }

    void displayStaffPatientCheckout(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        String dischargeStatus = null;
        String reason = null;
        Integer negExpCode = null;
        Integer referralStatusID = 0;
        String treatmentDesc = "";
        int addReportDetails = 0;
        int select = 0;
        do {
            System.out.println("----------------------------STAFF-PATIENT REPORT MENU --------------------------");
            System.out.println("1. Discharge Status");
            System.out.println("2. Referral Status");
            System.out.println("3. Treatment");
            System.out.println("4. Negative Experience");
            System.out.println("5. Go back");
            System.out.println("6. Submit");
            select = scan.nextInt();
            scan.nextLine();
            if (select == 1) {
                dischargeStatus = displayDischargeStatus(conn);
            }
            if (select == 2) {
                if (dischargeStatus == null) {
                    System.out.println("Enter discharge status first\n\n");
                    displayStaffPatientCheckout(conn);
                } else if (dischargeStatus.toLowerCase().equals("referred")) {
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
                if (dischargeStatus.toLowerCase().equals("referred") && referralStatusID == 0) {
                    System.out.println("Enter the Refferal Status");
                    displayStaffPatientCheckout(conn);
                }
                displayReportConfirmation(conn, referralStatusID, negExpCode, dischargeStatus, treatmentDesc);
            }
        } while (dischargeStatus.isEmpty() || treatmentDesc.isEmpty() || (select != 6 && select != 5) || (dischargeStatus.equalsIgnoreCase("referred") && referralStatusID == 0));
    }

    String displayDischargeStatus(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Successful Treatment");
        System.out.println("2. Deceased");
        System.out.println("3. Referred");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        String dischargeStatusValue = null;
        if (select >= 1 && select < 4) {
            switch (select) {
                case 1:
                    dischargeStatusValue = "Successful Treatment";
                    break;
                case 2:
                    dischargeStatusValue = "Deceased";
                    break;
                case 3:
                    dischargeStatusValue = "Referred";
                    break;

            }
        } else if (select == 4) {
            displayStaffPatientCheckout(conn);
        } else {
            System.out.println("Please enter a valid input.");
            displayDischargeStatus(conn);
        }
        return dischargeStatusValue;
    }

    int displayReferralStatus(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("1. Facility ID");
        System.out.println("2. Referrer ID");
        System.out.println("3. Add reason");
        System.out.println("4. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        int facilityID = 0;
        int referrerID = 0;
        List referralReasonCode = null;
        if (select == 1) {
            PreparedStatement stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL WHERE FACILITY_ID NOT IN (SELECT FACILITY_ID FROM STAFF WHERE EMPLOYEE_ID = " + this.staffID + ")");
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
        insertReferralStatus.executeQuery();
        int refStatusID = 0;
        PreparedStatement getRefStatSeqID = conn.prepareStatement("select referral_status_id_seq.currval from dual");
        ResultSet rs3 = getRefStatSeqID.executeQuery();
        while (rs3.next()) {
            refStatusID = rs3.getInt("CURRVAL");
        }
        for (Object x : referralReasonCode) {
            PreparedStatement insertRefStatusReasonMapping = conn.prepareStatement("insert into referral_reason_mapping(REFFERAL_STATUS_ID, REASON_CODE_ID) values(?, ?)");
            insertRefStatusReasonMapping.setInt(1, refStatusID);
            insertRefStatusReasonMapping.setInt(2, (Integer) x);
            insertRefStatusReasonMapping.executeQuery();
        }
        return refStatusID;
    }

    List displayReferralReason(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        List<Integer> reasonIDs = null;
        System.out.println("1. Reason");
        System.out.println("2. Go back");
        int select = scan.nextInt();
        scan.nextLine();
        int reasonCount = 0;
        while (reasonCount < 4 && select == 1) {
            System.out.println("Following are the reasons available:");
            List services = null;
            PreparedStatement getServices = conn.prepareStatement("select sd.name from hospital_service_dept_mapping hsdm inner join service_dept sd on hsdm.service_dept_id = sd.id where facility_id not in ?");
            getServices.setInt(1, getFacilityId(conn));
            ResultSet rsServ = getServices.executeQuery();
            while (rsServ.next()) {
                services.add(rsServ.getString("NAME"));
            }

            System.out.println("Please choose a service to refer: " + services);
            String service = scan.nextLine();

            System.out.println("Enter a reason code:");
            System.out.println("1. Service unavailable at time of visit");
            System.out.println("2. Service not present at facility");
            System.out.println("3. Non-payment");
            int selectRsn = scan.nextInt();
            String referralReason = scan.nextLine();

            if (selectRsn >= 1 && selectRsn < 4) {
                switch (selectRsn) {
                    case 1:
                        referralReason = "Service unavailable at time of visit";
                        break;
                    case 2:
                        referralReason = "Service not present at facility";
                        break;
                    case 3:
                        referralReason = "Non-payment";
                        break;
                }
            }

            System.out.println("Enter a decription:");
            String description = scan.nextLine();

            PreparedStatement insertReason = conn.prepareStatement("insert into reason (reason_code, service_name, description) values (?,?,?)");
            insertReason.setString(1, referralReason);
            insertReason.setString(2, service);
            insertReason.setString(3, description);
            insertReason.executeQuery();

            PreparedStatement getAddSeqID = conn.prepareStatement("select reason_id_seq.currval from dual");
            ResultSet rs3 = getAddSeqID.executeQuery();
            int reasonID = 0;
            while (rs3.next()) {
                reasonID = rs3.getInt("CURRVAL");
            }
            reasonIDs.add(reasonID);
            System.out.println("Do you wish to enter more reasons?");
            System.out.println("1. Reason");
            System.out.println("2. Go back");
            select = scan.nextInt();
            scan.nextLine();
            reasonCount++;
        }
        return reasonIDs;
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
            String neg_exp = neg_exp_code == 1 ? "Misdiagnosis" : "Infected_at_hospital";
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

    void displayReportConfirmation(Connection conn, Integer referralStatusID, Integer negExpCode, String dischargeStatus, String treatmentDesc) throws Exception {
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
            insertPatientSym.setString(4, dischargeStatus);
            insertPatientSym.setString(5, treatmentDesc);
            insertPatientSym.executeQuery();
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