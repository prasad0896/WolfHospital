package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;

class Patient {

    private final int id;
    private int sid;

    Patient(int id) {
        this.id = id;
    }

    void displayMenu(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        System.out.println("----------------------------Patient Check In--------------------------");
        System.out.println("1. Check In");
        System.out.println("2. Check Out Acknowledgement");
        System.out.println("3. Go back");
        int select = scan.nextInt();
        if (select == 1) {
            displayCheckIn(conn);
        } else if (select == 2) {
            displayCheckOutForm();
        } else {
            App.signIn(conn);
        }
        scan.close();
        //System.exit(0);
    }

    private void displayCheckIn(Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        HashMap<Integer, String> facilities = new HashMap<Integer, String>();
        PreparedStatement stmt;
        stmt = conn.prepareStatement("SELECT NAME,FACILITY_ID FROM HOSPITAL");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            facilities.put(rs.getInt("FACILITY_ID"), rs.getString("NAME"));
        }

        System.out.println("Facilities available (ID: NAME):");
        for (Entry<Integer, String> map : facilities.entrySet()) {
            System.out.println(map.getKey() + ": " + map.getValue());
        }
        System.out.println("Please choose a facility id:");
        int facilityid = scan.nextInt();
        scan.nextLine();

        //todo: move facility id from patient to session
        PreparedStatement isCheckedIn = conn.prepareStatement("SELECT ID FROM PATIENT_SESSION WHERE PID = ? AND CHECKIN_END IS NOT NULL");
        isCheckedIn.setInt(1, this.id);
//        isCheckedIn.setInt(2, facilityid);
        ResultSet rs1 = isCheckedIn.executeQuery();
        if (rs1.next()) {
            System.out.println();
            System.out.println("ERROR: Patient is already Checked-In. You can only Check-Out.");
            System.out.println();
            displayMenu(conn);
        }

        PreparedStatement createPatientSession = conn.prepareStatement("insert into patient_session (checkin_start, pid) values (?,?)");
        createPatientSession.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
        createPatientSession.setInt(2, this.id);
        createPatientSession.executeQuery();

        PreparedStatement getAddSeqID = conn.prepareStatement("select patient_session_id_seq.currval from dual");
        ResultSet rs3 = getAddSeqID.executeQuery();
        int patientSeqID = 0;
        while (rs3.next()) {
            patientSeqID = rs3.getInt("CURRVAL");
        }
        this.sid = patientSeqID;

        System.out.println("Please choose a symptom (Enter the CODE/Other/Done):");
        HashMap<String, List> symptoms = new HashMap<>();
        PreparedStatement stmt2 = conn.prepareStatement("SELECT CODE, NAME, BP_CODE FROM SYMPTOM");
        ResultSet rs2 = stmt2.executeQuery();
        while (rs2.next()) {
            symptoms.put(rs2.getString("CODE"), Arrays.asList(rs2.getString("NAME"), rs2.getString("BP_CODE")));
        }
        int i = 0;
        for (Map.Entry<String, List> map : symptoms.entrySet()) {
            System.out.println(i + ". " + map.getKey() + ": " + map.getValue());
            i++;
        }
        System.out.println(i + ". OTHER");
        System.out.println(i + 1 + ". DONE");
        String select = scan.nextLine().trim().toUpperCase();
        if (select.equals("OTHER")) {
            System.out.println("Enter symptom name:");
            String symtom_name = scan.nextLine();
            String symtom_code = symtom_name.substring(0, 3).toUpperCase();
            displayMetaData(symtom_name, symtom_code);
        } else if (select.equals("DONE")) {
            validatePatient();
        } else {
            displayMetaData(select, symptoms.get(select).get(0).toString());
        }
    }

    private void displayMetaData(String symtom_code, String symtom_name) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        Scanner scan = new Scanner(System.in);
        String bodypart_code = "";
        Integer isRecurring = null;
        Integer duration = null;
        int severityScaleID = 0;
        String severity = "";
        String cause = "";
        while (bodypart_code.isEmpty() || isRecurring == null || duration == null || cause.isEmpty() || severity.isEmpty()) {
            System.out.println("Please complete all of the below details:");
            System.out.println("1. Body part");
            System.out.println("2. Duration");
            System.out.println("3. Reoccurring");
            System.out.println("4. Severity");
            System.out.println("5. Cause (Incident)");
            int select = scan.nextInt();
            scan.nextLine();
            switch (select) {
                case 1: {
                    bodypart_code = displayBodyparts(scan);
                    PreparedStatement stmtSym = conn.prepareStatement("select * from symptom where code = ? and bp_code = ?");
                    stmtSym.setString(1, bodypart_code);
                    stmtSym.setString(2, symtom_code);
                    ResultSet rs = stmtSym.executeQuery();
                    if (!rs.next()) {
                        PreparedStatement insertSym = conn.prepareStatement("insert into symptom (code, name, bp_code) values (?,?,?)");
                        insertSym.setString(1, symtom_code);
                        insertSym.setString(2, symtom_name);
                        insertSym.setString(3, bodypart_code);
                        insertSym.executeQuery();
                    }
                    break;
                }
                case 2: {
                    System.out.println("Enter duration:");
                    duration = scan.nextInt();
                    break;
                }
                case 3: {
                    System.out.println("Is the symptom reoccurring? Enter 0/1:");
                    isRecurring = scan.nextInt();
                    break;
                }
                case 4: {
                    HashMap severityScales = displaySeverityScales();
                    System.out.println("Enter how you want enter the severity:");
                    System.out.println("1. Add new severity scale");
                    System.out.println("2. Choose existing scale");
                    int severitySelect = scan.nextInt();
                    switch (severitySelect) {
                        case 1:
                            severityScaleID = addSeverityScale();
                        case 2:
                            System.out.println("Enter the scale_id (number) from above:");
                            severityScaleID = scan.nextInt();
                            scan.nextLine();
                            System.out.println("Enter the severity of your pain:" + severityScales.get(severityScaleID));
                            severity = scan.nextLine();
                    }
                    break;
                }
                case 5: {
                    System.out.println("Enter cause:");
                    cause = scan.nextLine();
                    break;
                }
            }
        }
        PreparedStatement insertPatientSym = conn.prepareStatement("insert into patient_sym_mapping (sid, sym_code, severity, duration, reoccuring, cause, bp_code) values (?,?,?,?,?,?,?)");
        insertPatientSym.setInt(1, this.sid);
        insertPatientSym.setString(2, symtom_code);
        insertPatientSym.setString(3, severity);
        insertPatientSym.setInt(4, duration);
        insertPatientSym.setInt(5, isRecurring);
        insertPatientSym.setString(6, cause);
        insertPatientSym.setString(7, bodypart_code);
        insertPatientSym.executeQuery();
        scan.close();
        System.out.println("Check-in process completed.");
        displayMenu(conn);
    }

    private String displayBodyparts(Scanner scan) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        System.out.println("Enter body part code:");
        HashMap<String, String> bodyparts = new HashMap<>();
        PreparedStatement stmt2 = conn.prepareStatement("SELECT CODE, NAME FROM BODYPART");
        ResultSet rs2 = stmt2.executeQuery();
        while (rs2.next()) {
            bodyparts.put(rs2.getString("CODE"), rs2.getString("NAME"));
        }
        int i = 0;
        for (Map.Entry<String, String> map : bodyparts.entrySet()) {
            System.out.println(i + ". " + map.getKey() + ": " + map.getValue());
            i++;
        }
        String bodypart_code = scan.nextLine();
        conn.close();
        return bodypart_code;
    }

    private void validatePatient() throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        PreparedStatement validateSym = conn.prepareStatement("select id from patient_session where checked_out is null and (select count(*) from patient_sym_mapping where pid = ?)>0");
        validateSym.setInt(1, this.id);
        ResultSet rsValPat = validateSym.executeQuery();
        if (!rsValPat.next()) {
            System.out.println("Please enter your symptoms:");
            displayCheckIn(conn);
        }
        conn.close();
    }

    private static int addSeverityScale() throws Exception {
        String scale = "";
        Scanner s = new Scanner(System.in);
        System.out.println("1. There is another level for this scale");
        System.out.println("2. There is no other level for this scale. GO BACK");
        int choice = s.nextInt();
        int severityScaleID = 0;
        switch (choice) {
            case 1:
                scale = makeSeverityScale(scale);
                break;
            case 2:
                updateScaleInTable(scale);
                System.out.println("Updated severity scales:");
                displaySeverityScales();
                System.out.println("Enter the scale_id (number) from above:");
                severityScaleID = s.nextInt();
                break;
        }
        return severityScaleID;
    }

    private static String makeSeverityScale(String currentScale) {
        Scanner s = new Scanner(System.in);
        currentScale += " " + s.nextLine();
        return currentScale;
    }

    private static void updateScaleInTable(String scale) throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        PreparedStatement query = conn.prepareStatement("INSERT INTO SEVERITY_SCALE (SCALE) VALUES (" + scale + ")");
        ResultSet rs = query.executeQuery();
    }

    private static HashMap displaySeverityScales() throws Exception {
        Connection conn = DriverManager.getConnection(
                "jdbc:oracle:thin:@orca.csc.ncsu.edu:1521:orcl01", "ssmehend", "200262272");
        System.out.println("Following are the severity scales:");
        HashMap<Integer, String> severityScales = new HashMap<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT ID, SCALE FROM SEVERITY_SCALE");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            severityScales.put(rs.getInt("ID"), rs.getString("SCALE"));
        }
        System.out.println("Severity scales available (ID: SCALE):");
        for (Map.Entry<Integer, String> map : severityScales.entrySet()) {
            System.out.println(map.getKey() + ": " + map.getValue());
        }
        conn.close();
        return severityScales;
    }

    private static void displayCheckOutForm() {

    }
}