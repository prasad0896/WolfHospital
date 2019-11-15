package app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Scanner;

public class Staff {

    private final String id;
    private int rule_done = 0;

    Staff(String id) {
        this.id = id;
    }

    public void StaffMenuDisplay(Connection conn) throws Exception {
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
                int sum_of_all = 0;
                int facility_id = getFacilityId(conn);
                sum_of_all += getCheckedInPatientList(conn, "High",facility_id);
                sum_of_all += getCheckedInPatientList(conn, "Quarantine",facility_id);
                sum_of_all += getCheckedInPatientList(conn, "Normal",facility_id);
                sum_of_all += getCheckInWithNoPriority(conn,facility_id);
                if (sum_of_all == 0) {
                    System.out.println("No checked in patients found! Going back!");
                    StaffMenuDisplay(conn);
                }
                break;
            case 7:
                conn.close();
            case 2:
                getTreatedPatientList(conn);
                break;
            case 3:
                if(checkMedicalStaff(conn)) {
                	addSymptoms(conn);
                }else {
                	StaffMenuDisplay(conn);
                }
                break;
            case 4:
            	if(checkMedicalStaff(conn)) {
            		addSeverityScale(conn);
                }else {
                	StaffMenuDisplay(conn);
                }
                break;
            case 5:
            	if(checkMedicalStaff(conn)) {
            		callAddAssesment(conn, "");
                }else {
                	StaffMenuDisplay(conn);
                }
                break;
            case 6:
                App.loginDisplay(conn);
                
        }
    }
    
    public boolean checkMedicalStaff(Connection conn) throws Exception {
    	String check_medical = "SELECT DESIGNATION FROM STAFF WHERE EMPLOYEE_ID = "+this.id;
		ResultSet medical_or_not = executeStringQuery(conn, check_medical);
		medical_or_not.next();
		if(medical_or_not.getString(1).equalsIgnoreCase("non-medical")) {
			System.out.println("Access Denied. Only Medical Staff has access. Going back!\n");
			return false;
		}
		return true;
    }

    public ResultSet executeStringQuery(Connection conn, String query) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        return rs;
    }

    public int getFacilityId(Connection conn) throws Exception {
    	String fid = "SELECT FACILITY_ID FROM STAFF WHERE EMPLOYEE_ID = " + this.id;
    	ResultSet rs = executeStringQuery(conn, fid);
    	rs.next();
    	return rs.getInt(1);
    }
    public int getCheckInWithNoPriority(Connection conn,int facility_id) throws Exception {
        String getCheckedInP = "SELECT PATIENT.PID,PATIENT_SESSION.ID,PATIENT_SESSION.PRIORITY FROM PATIENT INNER JOIN PATIENT_SESSION ON PATIENT.PID = PATIENT_SESSION.PID" +
        		" AND PATIENT.FACILITY_ID = "+facility_id +
                " AND PATIENT_SESSION.CHECKIN_START IS NOT NULL AND PATIENT_SESSION.TREATED IS NULL AND PATIENT_SESSION.PRIORITY IS NULL";
        ResultSet rs = executeStringQuery(conn, getCheckedInP);
        ResultSet copyrs = executeStringQuery(conn, getCheckedInP);
        if (!rs.isBeforeFirst()) {
            return 0;
        } else {
            System.out.println("Checked In Patient IDs");
            while (rs.next()) {
                System.out.println(rs.getInt(1));

            }
            selectPatient(conn, copyrs);
            return 1;
        }
    }

    public int getCheckedInPatientList(Connection conn, String priority_status, int facility_id) throws Exception {
        String getCheckedInP = "SELECT PATIENT.PID,PATIENT_SESSION.ID,PATIENT_SESSION.PRIORITY FROM PATIENT INNER JOIN PATIENT_SESSION ON PATIENT.PID = PATIENT_SESSION.PID" +
        		" AND PATIENT.FACILITY_ID = "+facility_id +
                " AND PATIENT_SESSION.CHECKIN_START IS NOT NULL AND PATIENT_SESSION.TREATED IS NULL AND PATIENT_SESSION.PRIORITY= '" + priority_status + "'";
        ResultSet rs = executeStringQuery(conn, getCheckedInP);
        ResultSet copyrs = executeStringQuery(conn, getCheckedInP);
        if (!rs.isBeforeFirst()) {
            return 0;
        } else {
            System.out.println("Checked In Patient IDs with priority = " + priority_status);
            while (rs.next()) {
                System.out.println(rs.getInt(1));

            }
            selectPatient(conn, copyrs);
            return 1;
        }
    }

    public void selectPatient(Connection conn, ResultSet rs) throws Exception {
        System.out.println("Enter the patient id from the above list");
        Scanner s = new Scanner(System.in);
        int id = s.nextInt();
        System.out.println("Looking for " + id);
        int patient_found = 0;
        while (rs.next()) {
            System.out.println(rs.getInt(1));
            if (rs.getInt(1) == id) {
                System.out.println("ID matched");
                patient_found = 1;
                EnterVitalTreatPatient enterVitalTreatPatient = new EnterVitalTreatPatient();
                enterVitalTreatPatient.EnterVitalMenu(conn, rs.getInt(2), this.id);
            }
        }
        if (patient_found == 0) {
            System.out.println("Enter valid ID");
        }

    }

    public void getTreatedPatientList(Connection conn) throws Exception {
        String getTreatedPList = "SELECT PATIENT.PID,PATIENT_SESSION.ID FROM PATIENT INNER JOIN PATIENT_SESSION ON PATIENT.PID = PATIENT_SESSION.PID AND PATIENT_SESSION.FACILITY_ID =" + getFacilityId(conn) 
        + " AND PATIENT_SESSION.TREATED='Y' AND PATIENT_SESSION.ID NOT IN (SELECT PATIENT_ID FROM REPORT)";
        ResultSet rs = executeStringQuery(conn, getTreatedPList);
        HashMap<Integer,Integer> pid_to_sid = new HashMap<Integer, Integer>();
        if(!rs.isBeforeFirst()) {
        	System.out.println("No TREATED Patients found!");
        	StaffMenuDisplay(conn);
        }else {
        	
	        while (rs.next()) {
	            System.out.println(rs.getInt(1));
	            pid_to_sid.put(rs.getInt(1), rs.getInt(2));
	        }
	        Scanner s = new Scanner(System.in);
	        System.out.println("Enter the patient ID from the list above");
	        int chosen_id = s.nextInt();
	        TreatedPatientMenu treatedPatientMenu = new TreatedPatientMenu(this.id, pid_to_sid.get(chosen_id));
	        treatedPatientMenu.displayTreatedPatientMenu(conn);
        }
    }

    public void addSymptoms(Connection conn) throws Exception {
        Symptom symptom = new Symptom();
        symptom.addSymptomMenu(conn, this.id);
    }

    public void addSeverityScale(Connection conn) throws Exception {
        String scale = "";
        Scanner s = new Scanner(System.in);
        System.out.println("1. There is another level for this scale");
        System.out.println("2. There is no other level for this scale.");
        System.out.println("3. Go Back");
        int choice = s.nextInt();
        if(choice==1) {
        while (choice==1) {
        	System.out.println("Enter the scale value");
            scale = makeSeverityScale(scale);
            System.out.println("1. There is another level for this scale");
            System.out.println("2. There is no other level for this scale.");
            System.out.println("3. Go Back");
            choice = s.nextInt();
        }
        updateScaleInTable(conn, scale);
        System.out.println("Severity scale Added in Database!\n");
        StaffMenuDisplay(conn);
        }
        else if(choice==2) {
        	System.out.println("Invalid. You need to enter atleast one scale value");
        	addSeverityScale(conn);
        } else if(choice==3) {
        	StaffMenuDisplay(conn);
        }
    }

    public String makeSeverityScale(String currentScale) {
        Scanner s = new Scanner(System.in);
        currentScale += " " + s.nextLine();
        return currentScale;
    }

    public void updateScaleInTable(Connection conn, String scale) throws Exception {
    	scale = scale.strip(); // remove the trailing spaces
        String query = "INSERT INTO SEVERITY_SCALE (SCALE) VALUES ('" + scale + "')";
        ResultSet rs = executeStringQuery(conn, query);
    }

    public String addAssessmentRule(Connection conn, String rule) throws Exception {
        Scanner s = new Scanner(System.in);
        String display_symptom = "SELECT CODE,NAME FROM SYMPTOM";
        ResultSet rs = executeStringQuery(conn, display_symptom);
        System.out.println("   SYMPTOM_CODE \t SYMPTOM_NAME");
        int count = 0;
        HashMap<Integer, String> num_symcode = new HashMap<>();
        while (rs.next()) {
            System.out.println(++count + ". " + rs.getString(1) + "\t" + rs.getString(2));
            num_symcode.put(count, rs.getString(1));
        }
        System.out.println(++count + "\t SELECT PRIORITY");
        System.out.println("Enter choice to select the Symptom from the list");
        int choice = s.nextInt();
        s.nextLine();
        if (choice == count && rule.length() > 0) {
            System.out.println("Enter the priority of the rule (1/2/3) \n" + rule);
            System.out.println("1.High");
            System.out.println("2.Normal");
            System.out.println("3.Quarantine");
            HashMap<Integer,String> priority_map = new HashMap<Integer, String>();
            priority_map.put(1, "High");
            priority_map.put(2, "Normal");
            priority_map.put(3, "Quanrantine");
            int priority = s.nextInt();
            String query = "INSERT INTO ASSESSMENT_RULE (RULES,PRIORITY) VALUES('" + rule + "', '" + priority_map.get(priority) + "')";
            ResultSet f = executeStringQuery(conn, query);
            this.rule_done = 1;
            return rule;
        }
        if (num_symcode.containsKey(choice)) {
        	String code = num_symcode.get(choice);
        	if (rule.length() == 0) {
                rule += code + " ";
            } else {
                rule += " & " + code + " ";
            }
        	// Display all body parts that could be associated with the symptom
        	System.out.println("Following are the the body parts associated with the symptom");
        	String symp_bodypart = "SELECT SYMPTOM.BP_CODE FROM SYMPTOM WHERE SYMPTOM.CODE = '"+code+"'";
        	ResultSet sym_bp = executeStringQuery(conn, symp_bodypart);
        	sym_bp.next();
        	// if no bp associated with symptom, display all
        	if(sym_bp.getString(1)==null) {
        		String show_all_bp = "SELECT CODE,NAME FROM BODYPART";
        		ResultSet body_parts = executeStringQuery(conn, show_all_bp);
        		while(body_parts.next()) {
        			System.out.println(body_parts.getString(1) + "\t"+ body_parts.getString(2));
        		}
        	}
        	// else display the one associated
        	else {
        		System.out.println(sym_bp.getString(1));
	        	while(sym_bp.next()) {
	        		System.out.println(sym_bp.getString(1));
	        	}
        	}
            System.out.println("Enter the body part CODE from above or else enter none");
            String bp_code = s.nextLine();
            if(!bp_code.equalsIgnoreCase("none")) {
            	// add to rule : symCODE - bodypartcode
            	rule = rule.strip();
                rule+="-"+bp_code+" ";
            }
            System.out.println("The severity scale for this symptom is:");
            String getScale = "SELECT SCALE FROM SEVERITY_SCALE INNER JOIN SYM_SEVERITY ON SYM_SEVERITY.SEVERITY=SEVERITY_SCALE.ID WHERE SYM_SEVERITY.CODE = '" + code + "'";
            ResultSet r1 = executeStringQuery(conn, getScale);
            while (r1.next()) {
                System.out.println(r1.getString(1));
            }
            System.out.println("Enter one of the following: >= \t <= \t > \t < \t =");
            String sign = s.nextLine();
            rule += sign + " ";
            System.out.println("Current rule: " + rule);
            System.out.println("Enter the scale value");
            String val = s.nextLine();
            rule += val;

        }
        return rule;
    }

    public void callAddAssesment(Connection conn, String rule) throws Exception {
        while (this.rule_done != 1) {
            rule = addAssessmentRule(conn, rule);
        }
        System.out.println("Assessment Rule added!");
        this.rule_done = 0;
        StaffMenuDisplay(conn);
    }
}