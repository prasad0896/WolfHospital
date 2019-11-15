package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;

class Patient {

    private final int id;
    private final int facilityID;
    private int sid;

    Patient(int id, int facilityID) {
        this.id = id;
        this.facilityID = facilityID;
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
            displayCheckOutForm(conn);
        } else {
            App.loginDisplay(conn);
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

        if (facilityid != this.facilityID) {
            System.out.println();
            System.out.println("You are not signed in at this Facility. Selecting the Facility you have signed in at: " + this.facilityID);
            System.out.println();
        }

        PreparedStatement isCheckedIn = conn.prepareStatement("SELECT ID FROM PATIENT_SESSION WHERE PID = ? AND CHECKIN_END IS NOT NULL AND FACILITY_ID = ?");
        isCheckedIn.setInt(1, this.id);
        isCheckedIn.setInt(2, this.facilityID);
        ResultSet rs1 = isCheckedIn.executeQuery();
        if (rs1.next()) {
            System.out.println();
            System.out.println("ERROR: Patient is already Checked-In. You can only Check-Out.");
            System.out.println();
            displayMenu(conn);
        }

        PreparedStatement createPatientSession = conn.prepareStatement("insert into patient_session (checkin_start, pid, facility_id) values (?,?,?)");
        createPatientSession.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
        createPatientSession.setInt(2, this.id);
        createPatientSession.setInt(3, this.facilityID);
        createPatientSession.executeQuery();

        PreparedStatement getAddSeqID = conn.prepareStatement("select patient_session_id_seq.currval from dual");
        ResultSet rs3 = getAddSeqID.executeQuery();
        int patientSeqID = 0;
        while (rs3.next()) {
            patientSeqID = rs3.getInt("CURRVAL");
        }
        this.sid = patientSeqID;
        int addSymptom = 0;
        do {
            System.out.println("Please choose a symptom (Enter the CODE/Other/Done):");
            HashMap<String, List> symptoms = new HashMap<>();
            PreparedStatement stmt2 = conn.prepareStatement("SELECT CODE, NAME, BP_CODE FROM SYMPTOM");
            ResultSet rs2 = stmt2.executeQuery();
            while (rs2.next()) {
                symptoms.put(rs2.getString("CODE"), Arrays.asList(rs2.getString("NAME"), rs2.getString("BP_CODE")));
            }
            int i = 0;
            for (Map.Entry<String, List> map : symptoms.entrySet()) {
                System.out.println(i + ". " + map.getKey() + ": " + map.getValue().get(0));
                i++;
            }
            System.out.println(i + ". OTHER");
            System.out.println(i + 1 + ". DONE");
            String select = scan.nextLine().trim().toUpperCase();
            if (select.equals("OTHER")) {
                System.out.println("Enter symptom name:");
                String symtom_name = scan.nextLine();
                String symtom_code = symtom_name.substring(0, 3).toUpperCase();
                displayMetaData(symtom_name, symtom_code, conn);
            } else if (select.equals("DONE")) {
                validatePatient(conn);
            } else {
                displayMetaData(select, symptoms.get(select).get(0).toString(), conn);
            }
            System.out.println("Do you wish to add more symptoms?: (0/1)");
            addSymptom = scan.nextInt();
            scan.nextLine();
        } while (addSymptom == 1);
        System.out.println("Check-in process completed.");
        displayMenu(conn);
    }

    private void displayMetaData(String symtom_code, String symtom_name, Connection conn) throws Exception {
        Scanner scan = new Scanner(System.in);
        String bodypart_code = "";
        Integer isRecurring = null;
        Integer duration = null;
        String severity = "";
        String cause = "";
        //bodypart could be skipped
        while (isRecurring == null || duration == null || cause.isEmpty() || severity.isEmpty()) {
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
                    bodypart_code = displayBodyparts(scan, conn, symtom_code);
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
                    severity = displaySeverityScales(conn, symtom_code);
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
    }

    private String displayBodyparts(Scanner scan, Connection conn, String symptom_code) throws Exception {
        System.out.println("Enter the associated body part (CODE) from below:");
        HashMap<String, String> bodyparts = new HashMap<>();
        PreparedStatement stmt2 = conn.prepareStatement("SELECT CODE, NAME FROM BODYPART WHERE CODE IN (SELECT BP_CODE FROM SYMPTOM WHERE CODE = ?)");
        stmt2.setString(1, symptom_code);
        ResultSet rs2 = stmt2.executeQuery();
        if (!rs2.next()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT CODE, NAME FROM BODYPART");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bodyparts.put(rs.getString("CODE"), rs.getString("NAME"));
            }
        } else {
            do {
                bodyparts.put(rs2.getString("CODE"), rs2.getString("NAME"));
            }
            while (rs2.next());
        }
        int i = 0;
        for (Map.Entry<String, String> map : bodyparts.entrySet()) {
            System.out.println(i + ". " + map.getKey() + ": " + map.getValue());
            i++;
        }
        System.out.println(i + ". None");
        String bodypart_code = scan.nextLine();
        while (!bodyparts.containsKey(bodypart_code) || !bodypart_code.toLowerCase().equals("none")) {
            System.out.println("Please select a valid bodypart from above:");
            bodypart_code = scan.nextLine();
        }
        if (bodypart_code.toLowerCase().equals("none")) {
            bodypart_code = null;
        }
        return bodypart_code;
    }

    private void validatePatient(Connection conn) throws Exception {
        PreparedStatement validateSym = conn.prepareStatement("select id from patient_session where checked_out is null and (select count(*) from patient_sym_mapping where pid = ? and facility_id = ?)>0");
        validateSym.setInt(1, this.id);
        validateSym.setInt(2, this.facilityID);
        ResultSet rsValPat = validateSym.executeQuery();
        if (!rsValPat.next()) {
            System.out.println("Please enter your symptoms:");
            displayCheckIn(conn);
        }
    }

    private static String displaySeverityScales(Connection conn, String symtom_code) throws Exception {
        System.out.println("Following are the severity scales:");
        Scanner scan = new Scanner(System.in);
        List severityScales = new ArrayList();
        PreparedStatement stmt = conn.prepareStatement("SELECT SCALE FROM SEVERITY_SCALE WHERE ID IN (SELECT SEVERITY FROM SYM_SEVERITY WHERE CODE = ?)");
        stmt.setString(1, symtom_code);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            severityScales = Collections.singletonList(rs.getString("SCALE"));
        }
        System.out.println("Enter the severity of your pain:" + severityScales);
        return scan.nextLine();
    }

    private void displayCheckOutForm(Connection conn) throws Exception {
        //todo display report
        System.out.println("Display the report that is filled by the staff?:");
        System.out.println("1. Yes");
        System.out.println("2. No");
        System.out.println("3. Go Back");
        Scanner scan = new Scanner(System.in);
        int select = scan.nextInt();
        if (select == 1) {
            displayMenu(conn);
        } else if (select == 2) {
            System.out.println("Please enter a valid reason:");
            String reason = scan.nextLine();
            //todo store reason?
        } else {
            displayMenu(conn);
        }
    }
}