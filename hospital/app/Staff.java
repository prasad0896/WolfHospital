package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class Staff {

    private final String id;

    Staff(String id) {
        this.id = id;
    }

    public static void StaffMenuDisplay(Connection conn) throws Exception {
        if (conn == null) {
            System.out.println("Connection NULL");
        }
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Medical Staff--------------------------");
        System.out.println("1. Checked-in Patient List");
        System.out.println("2. Treated Patient List");
        System.out.println("3. Add Symptoms");
        System.out.println("4. Add Severity Scale");
        System.out.println("5. Add Assessment Rule");
        System.out.println("6. Go Back");
        System.out.println("7. Exit");
        int select = scan.nextInt();
        switch (select) {
            case 1:
                getCheckedInPatientList(conn);
                break;
            case 7:
                conn.close();
            case 2:
                getTreatedPatientList(conn);
                break;
            case 3:
                addSymptoms(conn);
                break;
            case 4:
                addSeverityScale(conn);
                break;
            case 5:
                addAssessmentRule();
                break;
            //case 6: goback();break;
        }
    }

    public static ResultSet executeStringQuery(Connection conn, String query) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    public static void getCheckedInPatientList(Connection conn) throws Exception {
        String getCheckedInP = "SELECT PATIENT.PID,PATIENT_SESSION.ID FROM PATIENT INNER JOIN PATIENT_SESSION ON PATIENT.PID = PATIENT_SESSION.PID" +
                " AND PATIENT_SESSION.CHECKIN_START IS NOT NULL AND PATIENT_SESSION.TREATED IS NULL";
        ResultSet rs = executeStringQuery(conn, getCheckedInP);
        ResultSet copyrs = executeStringQuery(conn, getCheckedInP);
        System.out.println("CheckedIn patient IDs");
        while (rs.next()) {
            System.out.println(rs.getInt(1));

        }
        selectPatient(conn, copyrs);
    }

    public static void selectPatient(Connection conn, ResultSet rs) throws Exception {
        System.out.println("Enter the patient id from the above list");
        Scanner s = new Scanner(System.in);
        int id = s.nextInt();
        System.out.println("Looking for " + id);
        int patient_found = 0;
        System.out.println("List of Checked_IN patients is");
        while (rs.next()) {
            System.out.println(rs.getInt(1));
            if (rs.getInt(1) == id) {
                System.out.println("ID matched");
                patient_found = 1;
                EnterVitalTreatPatient enterVitalTreatPatient = new EnterVitalTreatPatient();
                enterVitalTreatPatient.EnterVitalMenu(conn, rs.getInt(2));
            }
        }
        if (patient_found == 0) {
            System.out.println("Enter valid ID");
        }

    }

    public static void getTreatedPatientList(Connection conn) throws Exception {
        String getTreatedPList = "SELECT id FROM PATIENT INNER JOIN PATIENT_SESSION ON PATIENT.ID = PATIENT_SESSION.PID" +
                " AND PATIENT_SESSION.TREATED='Y'";
        ResultSet rs = executeStringQuery(conn, getTreatedPList);
        while (rs.next()) {
            System.out.println(rs.getInt(1));
        }
        TreatedPatientMenu treatedPatientMenu = new TreatedPatientMenu();
        treatedPatientMenu.displayTreatedPatientMenu(conn);
    }

    public static void addSymptoms(Connection conn) throws Exception {
        Symptom symptom = new Symptom();
        symptom.addSymptomMenu(conn);
    }

    public static void addSeverityScale(Connection conn) throws Exception {
        String scale = "";
        Scanner s = new Scanner(System.in);
        System.out.println("1. There is another level for this scale");
        System.out.println("2. There is no other level for this scale. GO BACK");
        int choice = s.nextInt();
        switch (choice) {
            case 1:
                scale = makeSeverityScale(scale);
                break;
            case 2:
                updateScaleInTable(conn, scale);
                StaffMenuDisplay(conn);
                break;
        }
    }

    public static String makeSeverityScale(String currentScale) {
        Scanner s = new Scanner(System.in);
        currentScale += " " + s.nextLine();
        return currentScale;
    }

    public static void updateScaleInTable(Connection conn, String scale) throws Exception {
        String query = "INSERT INTO SEVERITY_SCALE (SCALE) VALUES (" + scale + ")";
        ResultSet rs = executeStringQuery(conn, query);
    }

    public static void addAssessmentRule() {

    }
}